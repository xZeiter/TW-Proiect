from __future__ import annotations
from typing import Dict, List, Any
import cv2
import numpy as np
from .utils import pt_to_px, bubble_fill_ratio

FILL_THRESHOLD = 0.33

def read_answers(img_bgr: np.ndarray, layout_json: dict) -> Dict[int, List[str]]:
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    out: Dict[int, List[str]] = {}

    for q in layout_json.get("questions", []):
        qpk = int(q["questionPk"])
        chosen: List[str] = []

        for opt in q.get("options", []):
            label = str(opt["label"])
            # Fara axis=...
            cx = pt_to_px(opt["x"])
            cy = pt_to_px(opt["y"])
            r = pt_to_px(opt["r"])

            ratio = bubble_fill_ratio(gray, cx, cy, r)
            if ratio >= FILL_THRESHOLD:
                chosen.append(label)
        out[qpk] = chosen
    return out

def read_answers_confidence(img_bgr: np.ndarray, layout_json: dict) -> Any:
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    conf: Dict[int, Dict[str, float]] = {}

    for q in layout_json.get("questions", []):
        qpk = int(q["questionPk"])
        per_opt: Dict[str, float] = {}

        for opt in q.get("options", []):
            label = str(opt["label"])
            # Fara axis=...
            cx = pt_to_px(opt["x"])
            cy = pt_to_px(opt["y"])
            r = pt_to_px(opt["r"])

            ratio = bubble_fill_ratio(gray, cx, cy, r)
            per_opt[label] = round(float(ratio), 4)
        conf[qpk] = per_opt
    return conf