## SmartGrade Scanner (Python)

### 1) Create venv
python -m venv .venv

### 2) Activate venv
Windows:
  .venv\Scripts\activate
Linux/Mac:
  source .venv/bin/activate

### 3) Install deps
pip install -r requirements.txt

### 4) Configure .env
Copy .env.example -> .env and set BACKEND_JWT

### 5) Run
uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload

### 6) Test
POST http://127.0.0.1:8001/scan (multipart form-data file=scan.jpg)
