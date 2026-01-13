from __future__ import annotations
from typing import Any, Dict, Tuple, Optional
import json
import cv2
import numpy as np

# Constanta de scalare (1 punct tipografic = 2.5 pixeli la ~180 DPI)
# Daca layout-ul tau e generat standard, 2.5 e valoarea corecta.
PX_PER_PT = 2.5

def normalize_layout(layout_resp: Dict[str, Any]) -> Dict[str, Any]:
    if layout_resp is None:
        raise ValueError("layout_resp is None")
    if isinstance(layout_resp.get("layoutJson"), str):
        try:
            return json.loads(layout_resp["layoutJson"])
        except Exception as e:
            raise ValueError(f"Invalid layoutJson string: {e}")
    return layout_resp

def canonical_size_px(layout: Dict[str, Any]) -> Tuple[int, int]:
    page = layout.get("page", {"w": 595.28, "h": 841.89})
    w_pt = float(page.get("w", 595.28))
    h_pt = float(page.get("h", 841.89))
    return int(round(w_pt * PX_PER_PT)), int(round(h_pt * PX_PER_PT))

def pt_to_px(v: float) -> int:
    return int(round(float(v) * PX_PER_PT))

def order_quad(pts: np.ndarray) -> np.ndarray:
    """ Ordoneaza punctele: TL, TR, BR, BL """
    pts = pts.reshape(4, 2).astype(np.float32)
    s = pts.sum(axis=1)
    diff = np.diff(pts, axis=1).reshape(4)
    tl = pts[np.argmin(s)]
    br = pts[np.argmax(s)]
    tr = pts[np.argmin(diff)]
    bl = pts[np.argmax(diff)]
    return np.array([tl, tr, br, bl], dtype=np.float32)

def mask_rect(img_or_mask: np.ndarray, rect: Optional[Tuple[int, int, int, int]], pad: int = 25) -> np.ndarray:
    if rect is None:
        return img_or_mask
    x, y, w, h = rect
    H, W = img_or_mask.shape[:2]
    x0 = max(0, int(x - pad))
    y0 = max(0, int(y - pad))
    x1 = min(W, int(x + w + pad))
    y1 = min(H, int(y + h + pad))
    out = img_or_mask.copy()
    out[y0:y1, x0:x1] = 0
    return out

def draw_rect(img: np.ndarray, rect: Tuple[int, int, int, int], color=(0, 255, 255), thickness: int = 2):
    x, y, w, h = rect
    cv2.rectangle(img, (x, y), (x + w, y + h), color, thickness)

def draw_points(img: np.ndarray, pts: np.ndarray, color=(0, 255, 0), r: int = 7):
    if pts is not None:
        for p in pts:
            cv2.circle(img, (int(p[0]), int(p[1])), r, color, -1)

def bubble_fill_ratio(gray: np.ndarray, cx: int, cy: int, r: int) -> float:
    h, w = gray.shape[:2]
    if cx < 0 or cy < 0 or cx >= w or cy >= h:
        return 0.0
    pad = int(r + 2)
    x1, y1 = max(cx - pad, 0), max(cy - pad, 0)
    x2, y2 = min(cx + pad, w), min(cy + pad, h)
    roi = gray[y1:y2, x1:x2]
    if roi.size == 0:
        return 0.0
    _, th = cv2.threshold(roi, 0, 255, cv2.THRESH_BINARY_INV | cv2.THRESH_OTSU)
    mask = np.zeros_like(th)
    cv2.circle(mask, (cx - x1, cy - y1), r, 255, -1)
    filled_pixels = cv2.countNonZero(cv2.bitwise_and(th, th, mask=mask))
    total_area = np.pi * (r ** 2)
    if total_area == 0: return 0.0
    return min(1.0, filled_pixels / total_area)