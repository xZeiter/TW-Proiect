import os
from dotenv import load_dotenv

load_dotenv()

SCANNER_HOST = os.getenv("SCANNER_HOST", "127.0.0.1")
SCANNER_PORT = int(os.getenv("SCANNER_PORT", "8001"))

BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://localhost:8080").rstrip("/")
BACKEND_JWT = os.getenv("BACKEND_JWT", "").strip()

CROP_DIR = os.getenv("CROP_DIR", "storage/crops")
