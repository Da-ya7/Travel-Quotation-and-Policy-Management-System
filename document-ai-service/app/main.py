import os

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile

from .extraction import GroqSemanticExtractor, SemanticExtractor
from .ocr import OcrEngine, TesseractOcrEngine
from .schemas import DocumentType, ExtractionResponse, validate_fields


class DocumentExtractionService:
    def __init__(self, ocr: OcrEngine, extractor: SemanticExtractor):
        self.ocr = ocr
        self.extractor = extractor

    def extract(self, content: bytes, content_type: str, document_type: DocumentType) -> ExtractionResponse:
        ocr_text = self.ocr.extract_text(content, content_type)
        result = self.extractor.extract_fields(ocr_text, document_type)
        detected_type = result["documentType"]
        fields = validate_fields(detected_type, result["extractedFields"])
        return ExtractionResponse(documentType=detected_type, extractedFields=fields,
                                  extraction={"ocrProvider": "tesseract", "semanticProvider": "groq"})


app = FastAPI()
service = DocumentExtractionService(TesseractOcrEngine(), GroqSemanticExtractor())


@app.post("/internal/v1/extract", response_model=ExtractionResponse)
async def extract(document: UploadFile = File(...), documentType: DocumentType = Form(...),
                  x_internal_service_key: str | None = Header(default=None)):
    expected_key = os.environ.get("DOCUMENT_AI_INTERNAL_SERVICE_KEY")
    if expected_key and x_internal_service_key != expected_key:
        raise HTTPException(status_code=403, detail="Forbidden")
    content = await document.read()
    if len(content) == 0 or len(content) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Document must be between 1 byte and 10 MB")
    if document.content_type not in {"application/pdf", "image/jpeg", "image/png"}:
        raise HTTPException(status_code=415, detail="Document must be PDF, JPEG, or PNG")
    try:
        return service.extract(content, document.content_type, documentType)
    except ValueError as exception:
        raise HTTPException(status_code=422, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="Document extraction provider failed") from exception
