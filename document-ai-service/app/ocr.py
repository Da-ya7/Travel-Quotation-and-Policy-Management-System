import io
import os
from typing import Protocol

import pytesseract
from PIL import Image
from pdf2image import convert_from_bytes

if os.environ.get("TESSERACT_CMD"):
    pytesseract.pytesseract.tesseract_cmd = os.environ["TESSERACT_CMD"]


class OcrEngine(Protocol):
    def extract_text(self, content: bytes, content_type: str) -> str:
        ...


class TesseractOcrEngine:
    def extract_text(self, content: bytes, content_type: str) -> str:
        if content_type == "application/pdf":
            images = convert_from_bytes(content)
            return "\n".join(pytesseract.image_to_string(image) for image in images)
        with Image.open(io.BytesIO(content)) as image:
            return pytesseract.image_to_string(image)
