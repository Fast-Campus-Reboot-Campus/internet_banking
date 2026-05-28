"""doc-agent 전용 OCR 엔드포인트.

POST /ocr/extract  — 이미지·PDF 바이트(base64) → 텍스트 영역 리스트
POST /ocr/health   — PaddleOCR 로드 상태
"""

from __future__ import annotations

import base64
import io
import logging
from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

log = logging.getLogger("ocr")

router = APIRouter(prefix="/ocr", tags=["ocr"])

# PaddleOCR 지연 로드 — 미설치 환경에서 서버 기동은 가능하게 유지
_ocr_engine: Any = None


def _get_engine() -> Any:
    global _ocr_engine
    if _ocr_engine is None:
        try:
            from paddleocr import PaddleOCR  # type: ignore
            _ocr_engine = PaddleOCR(use_angle_cls=True, lang="korean", show_log=False)
            log.info("PaddleOCR 로드 완료")
        except ImportError:
            log.warning("paddleocr 미설치 — /ocr/extract 호출 시 503 반환")
    return _ocr_engine


class OcrRequest(BaseModel):
    image_b64: str          # base64 인코딩 이미지(jpg/png) 또는 PDF 첫 페이지
    submission_id: str


class OcrRegion(BaseModel):
    text: str
    confidence: float
    bbox: list[list[int]]   # [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]


class OcrResponse(BaseModel):
    submission_id: str
    regions: list[OcrRegion]
    engine: str = "paddleocr-ko"


@router.get("/health")
def ocr_health() -> dict:
    engine = _get_engine()
    return {"status": "UP" if engine is not None else "DEGRADED", "engine": "paddleocr-ko"}


@router.post("/extract", response_model=OcrResponse)
def extract(req: OcrRequest) -> OcrResponse:
    engine = _get_engine()
    if engine is None:
        raise HTTPException(status_code=503, detail="PaddleOCR 미설치")

    try:
        img_bytes = base64.b64decode(req.image_b64)
    except Exception:
        raise HTTPException(status_code=400, detail="image_b64 디코딩 실패")

    import numpy as np
    from PIL import Image  # type: ignore

    try:
        img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        img_array = np.array(img)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"이미지 파싱 실패: {e}")

    try:
        raw = engine.ocr(img_array, cls=True)
    except Exception as e:
        log.error("PaddleOCR 추론 오류: %s", e)
        raise HTTPException(status_code=500, detail="OCR 추론 실패")

    regions: list[OcrRegion] = []
    for page in (raw or []):
        for line in (page or []):
            bbox_raw, (text, conf) = line
            regions.append(OcrRegion(
                text=text,
                confidence=float(conf),
                bbox=[[int(p[0]), int(p[1])] for p in bbox_raw],
            ))

    return OcrResponse(submission_id=req.submission_id, regions=regions)
