import os
import requests
import logging
from dotenv import load_dotenv

log = logging.getLogger("uvicorn.error")

# Incarcam .env ca rezerva (fallback)
ENV_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), ".env")
load_dotenv(ENV_PATH)

BASE_URL = os.getenv("BACKEND_BASE_URL", "http://localhost:8080").strip().rstrip("/")
# Acesta este token-ul de rezerva (din fisier)
JWT_FALLBACK = os.getenv("BACKEND_JWT", "").strip()

def _clean_token(raw: str) -> str:
    """ Curata stringul de token (scoate 'Bearer ' daca exista) """
    if not raw:
        return ""
    t = raw.strip()
    t = t.replace("Authorization:", "").strip()
    if t.lower().startswith("bearer "):
        parts = t.split(" ", 1)
        if len(parts) > 1:
            t = parts[1].strip()
    return t

def _headers(dynamic_token: str = None) -> dict:
    """
    Creeaza header-ul.
    PRIORITATE:
    1. dynamic_token (cel venit de la Frontend/Main.py)
    2. JWT_FALLBACK (cel din .env)
    """
    # Alegem care token il folosim
    raw = dynamic_token if dynamic_token else JWT_FALLBACK
    
    token = _clean_token(raw)
    h = {"Content-Type": "application/json"}
    
    if token:
        h["Authorization"] = f"Bearer {token}"
    
    return h

def get_layout(quiz_id: str, version: int, token: str = None) -> dict:
    """
    Cere layout-ul de la backend.
    Accepta 'token' optional (trimis de frontend).
    """
    url = f"{BASE_URL}/api/quizzes/{quiz_id}/layout"
    
    # Generam headerele folosind token-ul primit (sau cel din env)
    headers = _headers(token)
    
    # Doar pentru debug, afisam (trunchiat) ce token folosim
    auth_debug = headers.get("Authorization", "None")
    if len(auth_debug) > 20: 
        auth_debug = auth_debug[:20] + "..."

    log.info(f"get_layout URL={url} ver={version} auth={auth_debug}")

    try:
        r = requests.get(url, headers=headers, params={"version": version}, timeout=15)
    except Exception as e:
        raise Exception(f"Backend Connection Error: {e}")

    if r.status_code == 401:
        raise Exception(f"GET layout failed 401 (Unauthorized). Token invalid? Resp: {r.text}")
    if r.status_code >= 400:
        raise Exception(f"GET layout failed {r.status_code}: {r.text}")

    return r.json()

def save_result(sheet_id: int, payload: dict, token: str = None) -> dict:
    """
    Salveaza rezultatul in backend.
    Accepta 'token' optional (trimis de frontend).
    """
    url = f"{BASE_URL}/api/sheets/{sheet_id}/results"
    
    headers = _headers(token)
    
    auth_debug = headers.get("Authorization", "None")
    if len(auth_debug) > 20: 
        auth_debug = auth_debug[:20] + "..."

    log.info(f"save_result URL={url} auth={auth_debug}")

    try:
        r = requests.post(url, headers=headers, json=payload, timeout=20)
    except Exception as e:
        raise Exception(f"Backend Connection Error: {e}")

    log.info(f"save_result status={r.status_code} body={r.text[:200]}")

    if r.status_code == 401:
        raise Exception(f"POST save_result failed 401 (Unauthorized). Token invalid? Resp: {r.text}")
    if r.status_code >= 400:
        raise Exception(f"POST save_result failed {r.status_code}: {r.text}")

    return r.json() if r.text else {"ok": True}