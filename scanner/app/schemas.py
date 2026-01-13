from pydantic import BaseModel
from typing import Dict, List, Optional, Any

class QrInfo(BaseModel):
    quizId: str
    sheetId: int
    layoutVersion: int

class ScanResultPayload(BaseModel):
    extId: Optional[str] = None
    extIdConfidence: Optional[float] = None
    answers: Dict[int, List[str]]
    answersConfidence: Optional[Any] = None
    studentPk: Optional[int] = None
    nameCropPath: Optional[str] = None

class ScanResponse(BaseModel):
    ok: bool
    qr: QrInfo
    saved: bool
    backendResponse: Optional[dict] = None
