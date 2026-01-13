from __future__ import annotations
import os
import cv2
from .config import CROP_DIR
from .utils import pt_to_px


def ensure_dirs():
    os.makedirs(CROP_DIR, exist_ok=True)


def save_name_crop(img_bgr, layout_json: dict, quiz_id: str, sheet_id: int) -> str | None:
    """
    img_bgr = imagine deja aliniata (warp facut in main)
    foloseste layout_json["name"]["box"] (x,y,w,h) si salveaza crop
    """
    name = layout_json.get("name", {})
    box = name.get("box")
    if not box:
        return None

    ensure_dirs()

    x = pt_to_px(box["x"])
    y = pt_to_px(box["y"])
    w = pt_to_px(box["w"])
    h = pt_to_px(box["h"])

    H, W = img_bgr.shape[:2]
    x1 = max(x, 0)
    y1 = max(y, 0)
    x2 = min(x + w, W)
    y2 = min(y + h, H)

    if x2 <= x1 or y2 <= y1:
        return None

    crop = img_bgr[y1:y2, x1:x2]

    out_dir = os.path.join(CROP_DIR, quiz_id, str(sheet_id))
    os.makedirs(out_dir, exist_ok=True)

    out_path = os.path.join(out_dir, "name.png")
    cv2.imwrite(out_path, crop)

    return out_path.replace("\\", "/")
