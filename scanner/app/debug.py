from __future__ import annotations

from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.responses import Response
import numpy as np
import cv2

# Importuri locale
from .qr import decode_qr_payload_and_rect, parse_payload
from .backend_client import get_layout
from .align import warp_to_layout, detect_markers_for_debug_ex
from .utils import pt_to_px, bubble_fill_ratio, normalize_layout, draw_rect, draw_points

router = APIRouter(prefix="/debug", tags=["debug"])

FILL_THRESHOLD = 0.33

def _file_to_bgr(file: UploadFile, content: bytes):
    name = (file.filename or "").lower()
    ctype = (file.content_type or "").lower()

    is_pdf = name.endswith(".pdf") or ctype == "application/pdf"
    if is_pdf:
        try:
            import fitz
        except Exception:
            raise HTTPException(status_code=500, detail="PyMuPDF not installed")

        try:
            doc = fitz.open(stream=content, filetype="pdf")
            if doc.page_count <= 0:
                raise HTTPException(status_code=422, detail="Empty PDF")
            page = doc.load_page(0)
            pix = page.get_pixmap(dpi=260)
            img_bytes = pix.tobytes("png")

            np_img = np.frombuffer(img_bytes, dtype=np.uint8)
            img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
            if img is None:
                raise HTTPException(status_code=422, detail="Could not decode rendered PDF page")
            return img
        except Exception as e:
            raise HTTPException(status_code=422, detail=f"PDF Error: {e}")

    np_img = np.frombuffer(content, dtype=np.uint8)
    img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=422, detail="Invalid image file")
    return img


@router.post("/markers")
async def debug_markers(file: UploadFile = File(...)):
    """
    Returneaza un PNG cu:
      - stanga: overlay (QR bbox galben, candidati albastru, markere alese verde)
      - dreapta: masca threshold (alb = zone negre detectate)
    """
    content = await file.read()
    img_raw = _file_to_bgr(file, content)

    # Detectie QR
    qr_rect = None
    try:
        payload, qr_rect = decode_qr_payload_and_rect(img_raw)
    except Exception:
        # Nu e critic pt debug daca nu gaseste QR
        pass

    # --- FIX PENTRU COMPATIBILITATE ---
    # Verificam cate valori returneaza functia si ne adaptam
    result = detect_markers_for_debug_ex(img_raw, qr_rect=qr_rect, qr_pad=60)
    
    cand = []
    if len(result) == 3:
        pts, th, cand = result
    else:
        # Daca primim doar (pts, th), setam cand lista goala
        pts, th = result
        cand = []
    # ----------------------------------

    vis = img_raw.copy()

    # QR bbox
    if qr_rect is not None:
        draw_rect(vis, qr_rect, color=(0, 255, 255), thickness=3)

    # candidati (albastru) - doar daca exista in lista
    if cand:
        try:
            cand_sorted = sorted(cand, key=lambda t: t[0], reverse=True)
            for item in cand_sorted[:250]:
                # Extragem coordonatele in functie de structura tuplului
                # item poate fi (score, area, cx, cy, ar) SAU alt format
                if len(item) >= 4:
                    cx, cy = item[2], item[3]
                    cv2.circle(vis, (int(cx), int(cy)), 6, (255, 0, 0), 2)
        except Exception:
            pass # Ignoram erorile de desenare candidati daca formatul difera

    # markere alese (verde)
    if pts is not None:
        draw_points(vis, pts, color=(0, 255, 0), r=10)

    # masca in dreapta
    th_bgr = cv2.cvtColor(th, cv2.COLOR_GRAY2BGR)

    # normalize heights then concat
    H = max(vis.shape[0], th_bgr.shape[0])
    if vis.shape[0] != H:
        vis = cv2.copyMakeBorder(vis, 0, H - vis.shape[0], 0, 0, cv2.BORDER_CONSTANT, value=(0, 0, 0))
    if th_bgr.shape[0] != H:
        th_bgr = cv2.copyMakeBorder(th_bgr, 0, H - th_bgr.shape[0], 0, 0, cv2.BORDER_CONSTANT, value=(0, 0, 0))

    combo = np.hstack([vis, th_bgr])

    ok, png = cv2.imencode(".png", combo)
    if not ok:
        raise HTTPException(status_code=500, detail="PNG encode failed")
    return Response(content=png.tobytes(), media_type="image/png")


@router.post("/warp")
async def debug_warp(file: UploadFile = File(...)):
    content = await file.read()
    img_raw = _file_to_bgr(file, content)

    try:
        payload, qr_rect = decode_qr_payload_and_rect(img_raw)
        quiz_id, sheet_id, layout_version = parse_payload(payload)
        
        layout_resp = get_layout(quiz_id, layout_version)
        layout = normalize_layout(layout_resp)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Debug Info Error: {e}")

    warped = warp_to_layout(img_raw, layout, qr_rect=qr_rect, qr_pad=60)

    ok, png = cv2.imencode(".png", warped)
    if not ok:
        raise HTTPException(status_code=500, detail="PNG encode failed")

    return Response(content=png.tobytes(), media_type="image/png")


@router.post("/overlay")
async def debug_overlay(file: UploadFile = File(...)):
    content = await file.read()
    img_raw = _file_to_bgr(file, content)

    try:
        payload, qr_rect = decode_qr_payload_and_rect(img_raw)
        quiz_id, sheet_id, layout_version = parse_payload(payload)

        layout_resp = get_layout(quiz_id, layout_version)
        layout = normalize_layout(layout_resp)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Debug Info Error: {e}")

    warped = warp_to_layout(img_raw, layout, qr_rect=qr_rect, qr_pad=60)
    gray = cv2.cvtColor(warped, cv2.COLOR_BGR2GRAY)
    vis = warped.copy()

    # extId bubbles
    ext = layout.get("extId") or {}
    for cell in ext.get("cells", []):
        for b in cell.get("bubbles", []):
            cx = pt_to_px(b["x"])
            cy = pt_to_px(b["y"])
            r = pt_to_px(b["r"])
            ratio = bubble_fill_ratio(gray, cx, cy, r)
            color = (0, 255, 0) if ratio >= FILL_THRESHOLD else (0, 0, 255)
            cv2.circle(vis, (cx, cy), r, color, 2)
            cv2.putText(vis, f"{ratio:.2f}", (cx + r + 2, cy + 4),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1, cv2.LINE_AA)

    # question bubbles
    for q in layout.get("questions", []):
        for opt in q.get("options", []):
            cx = pt_to_px(opt["x"])
            cy = pt_to_px(opt["y"])
            r = pt_to_px(opt["r"])
            ratio = bubble_fill_ratio(gray, cx, cy, r)
            color = (0, 255, 0) if ratio >= FILL_THRESHOLD else (0, 0, 255)
            cv2.circle(vis, (cx, cy), r, color, 2)
            cv2.putText(vis, f"{opt['label']}:{ratio:.2f}", (cx + r + 2, cy + 4),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1, cv2.LINE_AA)

    ok, png = cv2.imencode(".png", vis)
    if not ok:
        raise HTTPException(status_code=500, detail="PNG encode failed")

    return Response(content=png.tobytes(), media_type="image/png")