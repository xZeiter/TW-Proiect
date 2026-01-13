from __future__ import annotations
from typing import Any, Dict, Tuple, Optional, List
import cv2
import numpy as np
from .utils import canonical_size_px, order_quad, mask_rect, PX_PER_PT

# ==========================================
# ZONA DE CALIBRARE FINĂ
# ==========================================
# Dacă bulele negre sunt mai JOS și mai la DREAPTA decât cercurile roșii:
# Folosește valori NEGATIVE (ex: -5, -10) pentru a trage imaginea în SUS și STÂNGA.

CALIB_X = -18.0  # Trage imaginea spre Stanga (pixeli)
CALIB_Y = -18.0  # Trage imaginea in Sus (pixeli)
# ==========================================


def _threshold_for_markers(gray: np.ndarray) -> np.ndarray:
    # Threshold adaptiv optimizat
    th = cv2.adaptiveThreshold(
        gray, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY_INV,
        91, 11
    )
    th = cv2.morphologyEx(th, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8), iterations=2)
    th = cv2.morphologyEx(th, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8), iterations=1)
    return th


def _detect_corner_squares_zones(
    img_bgr: np.ndarray,
    qr_rect: Optional[Tuple[int, int, int, int]] = None,
    qr_pad: int = 45,
) -> Tuple[Optional[np.ndarray], np.ndarray]:
    
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    gray = cv2.GaussianBlur(gray, (7, 7), 0)
    th = _threshold_for_markers(gray)

    if qr_rect:
        th = mask_rect(th, qr_rect, pad=qr_pad)

    h, w = gray.shape[:2]
    cx, cy = w // 2, h // 2

    zones = [
        (0, 0, cx, cy),       # TL
        (cx, 0, w, cy),       # TR
        (cx, cy, w, h),       # BR
        (0, cy, cx, h)        # BL
    ]

    found_anchors = []
    
    for (zx1, zy1, zx2, zy2) in zones:
        roi = th[zy1:zy2, zx1:zx2]
        cnts, _ = cv2.findContours(roi, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        candidates = []
        target_corner_x = zx1 if zx1 == 0 else w
        target_corner_y = zy1 if zy1 == 0 else h

        for c in cnts:
            area = cv2.contourArea(c)
            if area < 80 or area > (w*h)*0.05: continue
            
            peri = cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, 0.04 * peri, True)
            
            if len(approx) == 4:
                (x, y, ww, hh) = cv2.boundingRect(approx)
                ar = ww / float(hh)
                
                if 0.7 <= ar <= 1.4:
                    # Folosim Momente pentru precizie sub-pixel
                    M = cv2.moments(c)
                    if M["m00"] != 0:
                        local_cx = M["m10"] / M["m00"]
                        local_cy = M["m01"] / M["m00"]
                        
                        global_cx = zx1 + local_cx
                        global_cy = zy1 + local_cy
                        
                        dist = np.sqrt((global_cx - target_corner_x)**2 + (global_cy - target_corner_y)**2)
                        candidates.append((dist, [global_cx, global_cy]))
        
        candidates.sort(key=lambda x: x[0])
        
        if candidates:
            found_anchors.append(candidates[0][1])
        else:
            found_anchors.append([target_corner_x, target_corner_y])

    src_pts = np.array(found_anchors, dtype="float32")
    src_pts = order_quad(src_pts)
    return src_pts, th


def warp_to_layout(
    img_bgr: np.ndarray,
    layout: Dict[str, Any],
    qr_rect: Optional[Tuple[int, int, int, int]] = None,
    qr_pad: int = 45,
) -> np.ndarray:
    
    out_w, out_h = canonical_size_px(layout)

    dst_map = {}
    anchors = layout.get("anchors", [])
    
    for a in anchors:
        aid = a["id"]
        # Convertim punctele layout-ului in pixeli
        # AICI aplicam calibrarea: modificam unde "aterizeaza" ancorele
        x = (a["x"] + a["size"] / 2.0) * PX_PER_PT + CALIB_X
        y = (a["y"] + a["size"] / 2.0) * PX_PER_PT + CALIB_Y
        dst_map[aid] = [x, y]

    needed = ["TL", "TR", "BR", "BL"]
    if all(k in dst_map for k in needed):
        dst_pts = np.array([dst_map["TL"], dst_map["TR"], dst_map["BR"], dst_map["BL"]], dtype="float32")
    else:
        dst_pts = np.array([
            [0, 0],
            [out_w - 1, 0],
            [out_w - 1, out_h - 1],
            [0, out_h - 1]], dtype="float32")

    # Detectia ramane la fel (pe imaginea originala)
    src_pts, _ = _detect_corner_squares_zones(img_bgr, qr_rect, qr_pad)
    
    M = cv2.getPerspectiveTransform(src_pts, dst_pts)
    
    # Folosim WARP_INVERSE_MAP implicit prin dst_pts modificat
    warped = cv2.warpPerspective(img_bgr, M, (out_w, out_h), flags=cv2.INTER_LINEAR)
    
    # Daca vrem sa scapam de marginile negre in exces (crop safe)
    # Putem colora bordura cu alb, daca e cazul:
    # warped[warped == 0] = 255 (Atentie, asta face tot negrul alb)
    
    return warped

# Wrappers pentru debug
def detect_markers_for_debug(img, qr_rect=None, qr_pad=45):
    pts, _ = _detect_corner_squares_zones(img, qr_rect, qr_pad)
    return pts

def detect_markers_for_debug_ex(img, qr_rect=None, qr_pad=45):
    pts, th = _detect_corner_squares_zones(img, qr_rect, qr_pad)
    return pts, th, []