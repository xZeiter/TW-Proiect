from __future__ import annotations
from typing import Optional, Tuple
import cv2
from .utils import pt_to_px, bubble_fill_ratio

FILL_THRESHOLD = 0.33


def read_ext_id(img_bgr, layout_json: dict) -> Tuple[Optional[str], Optional[float]]:
    """
    img_bgr = imagine deja aliniata (warp_to_layout facut in main)
    Citeste extId pe baza bulelor din layout_json["extId"]["cells"].
    Return: (extId, confidence_avg)
    """
    ext = layout_json.get("extId")
    if not ext:
        return None, None

    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)

    chars = []
    confs = []

    for cell in ext.get("cells", []):
        best_sym = None
        best_ratio = 0.0

        for b in cell.get("bubbles", []):
            cx = pt_to_px(b["x"])
            cy = pt_to_px(b["y"])
            r = pt_to_px(b["r"])

            ratio = bubble_fill_ratio(gray, cx, cy, r)

            if ratio > best_ratio:
                best_ratio = ratio
                best_sym = b.get("sym")

        if best_sym is None or best_ratio < FILL_THRESHOLD:
            chars.append("")
        else:
            chars.append(str(best_sym))
            confs.append(float(best_ratio))

    ext_id = "".join(chars).strip()
    if ext_id == "":
        return None, None

    avg_conf = (sum(confs) / len(confs)) if confs else None
    if avg_conf is not None:
        avg_conf = float(round(avg_conf, 4))

    return ext_id, avg_conf
