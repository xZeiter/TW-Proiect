from __future__ import annotations

import json
import numpy as np
import cv2
# --- MODIFICARE 1: Importam Header si CORSMiddleware ---
from fastapi import FastAPI, UploadFile, File, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware

# Importuri locale
from .debug import router as debug_router
from .qr import decode_qr_payload_and_rect, parse_payload
from .backend_client import get_layout, save_result
from .bubbles import read_answers, read_answers_confidence
from .extid import read_ext_id
from .crop import save_name_crop
from .schemas import ScanResponse, QrInfo, ScanResultPayload
from .align import warp_to_layout

app = FastAPI(title="SmartGrade Scanner Service", version="0.1")

# ==========================================
#  MODIFICARE FINALĂ CORS (Acceptă orice Frontend)
# ==========================================
app.add_middleware(
    CORSMiddleware,
    # Folosim regex ca să permitem orice port (5500, 5501, 8080 etc.)
    allow_origin_regex='https?://.*', 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# ==========================================

app.include_router(debug_router)

def _file_to_bgr(file: UploadFile, content: bytes):
    """
    Converteste fisierul primit (PDF sau Imagine) intr-un array numpy BGR (OpenCV).
    """
    name = (file.filename or "").lower()
    ctype = (file.content_type or "").lower()

    # Logica pentru PDF
    is_pdf = name.endswith(".pdf") or ctype == "application/pdf"
    if is_pdf:
        try:
            import fitz  # PyMuPDF
        except ImportError:
            raise HTTPException(
                status_code=500,
                detail="PyMuPDF not installed. Please run: pip install pymupdf"
            )

        try:
            doc = fitz.open(stream=content, filetype="pdf")
            if doc.page_count <= 0:
                raise HTTPException(status_code=422, detail="Uploaded PDF is empty")
            
            # Randam prima pagina la 260 DPI (suficient pt OMR)
            page = doc.load_page(0)
            pix = page.get_pixmap(dpi=260)
            img_bytes = pix.tobytes("png")
            
            np_img = np.frombuffer(img_bytes, dtype=np.uint8)
            img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
            
            if img is None:
                raise HTTPException(status_code=422, detail="Could not decode rendered PDF page")
            return img
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=422, detail=f"PDF render failed: {str(e)}")

    # Logica pentru Imagini (JPG, PNG)
    np_img = np.frombuffer(content, dtype=np.uint8)
    img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=422, detail="Invalid image file (could not decode)")
    
    return img


@app.post("/scan", response_model=ScanResponse)
async def scan(
    file: UploadFile = File(...),
    # --- MODIFICARE 3: Acceptam Token-ul din Header ---
    authorization: str = Header(None) 
):
    """
    Endpoint-ul principal:
    Scaneaza QR -> Ia Layout (cu Auth) -> Aliniaza -> Citeste -> Salveaza (cu Auth)
    """
    
    # Extragem token-ul (scoatem "Bearer " din fata daca exista)
    token = None
    if authorization:
        parts = authorization.split(" ")
        if len(parts) == 2 and parts[0].lower() == "bearer":
            token = parts[1]
        else:
            token = authorization

    content = await file.read()
    img_raw = _file_to_bgr(file, content)

    # --- PASUL 1: Decodare QR + Detectie Bbox QR ---
    try:
        payload, qr_rect = decode_qr_payload_and_rect(img_raw)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"QR Decode Error: {str(e)}")

    try:
        quiz_id, sheet_id, layout_version = parse_payload(payload)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Invalid QR payload format: {payload} | Error: {e}")

    # --- PASUL 2: Obtinere Layout din Backend ---
    try:
        # Trimitem token-ul la backend ca sa ne dea voie sa luam layout-ul
        # Nota: Asigura-te ca backend_client.py accepta parametrul `token`!
        layout_wrapper = get_layout(quiz_id, layout_version, token=token)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Backend get_layout failed: {str(e)}")

    # Gestionare format raspuns
    layout = layout_wrapper
    if isinstance(layout_wrapper, dict) and isinstance(layout_wrapper.get("layoutJson"), str):
        try:
            layout = json.loads(layout_wrapper["layoutJson"])
        except Exception as e:
            raise HTTPException(status_code=502, detail=f"Invalid layoutJson string from backend: {e}")

    # --- PASUL 3: ALINIERE (Warp Perspective) ---
    try:
        img_aligned = warp_to_layout(img_raw, layout, qr_rect=qr_rect, qr_pad=30)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Alignment/Warp failed: {str(e)}")

    # --- PASUL 4: Citire Raspunsuri (OMR) ---
    try:
        answers = read_answers(img_aligned, layout)
        answers_conf = read_answers_confidence(img_aligned, layout)
    except Exception as e:
         raise HTTPException(status_code=422, detail=f"Bubble reading failed: {str(e)}")

    print("\n==============================")
    print("RĂSPUNSURI DETECTATE:", json.dumps(answers, indent=2))
    print("==============================\n")

    # Citire ExtID
    ext_id = None
    ext_conf = None
    try:
        ext_id, ext_conf = read_ext_id(img_aligned, layout)
        print(f"🕵️ EXT ID DETECTAT: '{ext_id}' (Incredere: {ext_conf})")
    except Exception as e:
        print(f"Eroare citire ExtID: {e}")
    except Exception:
        pass
        

    # --- PASUL 5: Crop Nume ---
    name_crop_path = None
    try:
        name_crop_path = save_name_crop(img_aligned, layout, quiz_id, sheet_id)
    except Exception:
        pass 

    # --- PASUL 6: Salvare Rezultat in Backend ---
    save_payload = ScanResultPayload(
        extId=ext_id,
        extIdConfidence=ext_conf,
        answers=answers,
        answersConfidence=answers_conf,
        studentPk=None,
        nameCropPath=name_crop_path
    ).model_dump()

    try:
        # Trimitem token-ul si la salvare
        backend_resp = save_result(sheet_id, save_payload, token=token)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Backend save_result failed: {str(e)}")

    return ScanResponse(
        ok=True,
        qr=QrInfo(quizId=quiz_id, sheetId=sheet_id, layoutVersion=layout_version),
        saved=True,
        backendResponse=backend_resp
    )