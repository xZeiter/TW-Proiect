import re
from typing import Optional, Tuple, Iterator, Dict, Any

import cv2
import numpy as np

try:
    from pyzbar.pyzbar import decode as zbar_decode
except Exception:
    zbar_decode = None

QR_RE = re.compile(r"^SG\|q=(?P<q>[^|]+)\|s=(?P<s>\d+)\|v=(?P<v>\d+)$")


def _try_pyzbar(img_bgr: np.ndarray) -> Optional[str]:
    if zbar_decode is None:
        return None
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    codes = zbar_decode(gray)
    if not codes:
        return None
    return codes[0].data.decode("utf-8", errors="ignore").strip()


def _try_opencv_qr(img_bgr: np.ndarray) -> Tuple[Optional[str], Optional[np.ndarray]]:
    """
    Returneaza (payload, pts) unde pts este (4,2) float32 in coordonate imagine.
    """
    det = cv2.QRCodeDetector()
    data, points, _ = det.detectAndDecode(img_bgr)
    if data and data.strip() and points is not None:
        pts = points.reshape(-1, 2).astype(np.float32)
        return data.strip(), pts
    return None, None


def _variants_with_mapping(img_bgr: np.ndarray) -> Iterator[Tuple[np.ndarray, Dict[str, Any]]]:
    """
    Yield (variant_img, map_info) ca sa putem converti bbox/points inapoi in coordonate original.
    map_info:
      - type: "orig" | "crop" | "resize" | "resize_crop" | "gray" | "th" | "inv"
      - scale: float
      - offset: (ox, oy) in original
    """
    h, w = img_bgr.shape[:2]

    # 1) original
    yield img_bgr, {"type": "orig", "scale": 1.0, "offset": (0, 0), "orig_shape": (h, w)}

    # 2) crop top-right (unde e QR la tine)
    x1 = int(w * 0.60)
    y1 = int(h * 0.00)
    x2 = int(w * 1.00)
    y2 = int(h * 0.40)
    crop = img_bgr[y1:y2, x1:x2]
    if crop.size > 0:
        yield crop, {"type": "crop", "scale": 1.0, "offset": (x1, y1), "orig_shape": (h, w)}

    # 3) upscale + (optional) crop top-right din up
    for scale in (2.0, 3.0):
        up = cv2.resize(img_bgr, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)
        yield up, {"type": "resize", "scale": scale, "offset": (0, 0), "orig_shape": (h, w)}

        uh, uw = up.shape[:2]
        cx1 = int(uw * 0.60)
        cy1 = int(uh * 0.00)
        cx2 = int(uw * 1.00)
        cy2 = int(uh * 0.40)
        ucrop = up[cy1:cy2, cx1:cx2]
        if ucrop.size > 0:
            # offset in ORIGINAL: (cx1/scale, cy1/scale)
            yield ucrop, {
                "type": "resize_crop",
                "scale": scale,
                "offset": (cx1 / scale, cy1 / scale),
                "orig_shape": (h, w)
            }

    # 4) grayscale blur (ca BGR)
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    gray = cv2.GaussianBlur(gray, (3, 3), 0)
    yield cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR), {"type": "gray", "scale": 1.0, "offset": (0, 0), "orig_shape": (h, w)}

    # 5) adaptive threshold (BGR)
    th = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 5)
    yield cv2.cvtColor(th, cv2.COLOR_GRAY2BGR), {"type": "th", "scale": 1.0, "offset": (0, 0), "orig_shape": (h, w)}

    # 6) invert threshold
    inv = 255 - th
    yield cv2.cvtColor(inv, cv2.COLOR_GRAY2BGR), {"type": "inv", "scale": 1.0, "offset": (0, 0), "orig_shape": (h, w)}


def _points_to_rect(points: np.ndarray) -> Tuple[int, int, int, int]:
    pts = points.astype(np.float32)
    x_min = float(np.min(pts[:, 0]))
    y_min = float(np.min(pts[:, 1]))
    x_max = float(np.max(pts[:, 0]))
    y_max = float(np.max(pts[:, 1]))
    x = int(np.floor(x_min))
    y = int(np.floor(y_min))
    w = int(np.ceil(x_max - x_min))
    h = int(np.ceil(y_max - y_min))
    return x, y, w, h


def _map_rect_to_original(rect: Tuple[int, int, int, int], map_info: Dict[str, Any]) -> Tuple[int, int, int, int]:
    x, y, w, h = rect
    scale = float(map_info["scale"])
    ox, oy = map_info["offset"]
    # rect in variant coords -> original coords:
    # original = (x/scale + ox, y/scale + oy)
    x0 = int(round(x / scale + ox))
    y0 = int(round(y / scale + oy))
    w0 = int(round(w / scale))
    h0 = int(round(h / scale))
    return x0, y0, w0, h0


def decode_qr_payload_and_rect(img_bgr: np.ndarray) -> Tuple[str, Optional[Tuple[int, int, int, int]]]:
    """
    Returneaza (payload, qr_rect_in_original) unde qr_rect=(x,y,w,h) in coordonate img_bgr original.
    Daca nu reuseste sa obtina rect, rect poate fi None (dar payload exista).
    """
    img_bgr = np.asarray(img_bgr)

    # 1) incercare rapida: OpenCV (ne da si points/bbox)
    data, pts = _try_opencv_qr(img_bgr)
    if data:
        rect = _points_to_rect(pts) if pts is not None else None
        return data, rect

    # 2) pyzbar pe original (payload doar)
    payload = _try_pyzbar(img_bgr)
    if payload:
        return payload, None

    # 3) variante (cu mapping inapoi)
    for v_img, map_info in _variants_with_mapping(img_bgr):
        data, pts = _try_opencv_qr(v_img)
        if data:
            rect_v = _points_to_rect(pts) if pts is not None else None
            rect_o = _map_rect_to_original(rect_v, map_info) if rect_v is not None else None
            return data, rect_o

        payload = _try_pyzbar(v_img)
        if payload:
            return payload, None

    raise ValueError("QR not found")


def decode_qr_payload(img_bgr: np.ndarray) -> str:
    payload, _ = decode_qr_payload_and_rect(img_bgr)
    return payload


def parse_payload(payload: str) -> Tuple[str, int, int]:
    m = QR_RE.match(payload)
    if not m:
        raise ValueError(f"Invalid QR payload: {payload}")
    quiz_id = m.group("q")
    sheet_id = int(m.group("s"))
    version = int(m.group("v"))
    return quiz_id, sheet_id, version
