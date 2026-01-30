#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Simplified Python OCR Bridge based on official manga-ocr usage
"""

import sys
import io
import json
from PIL import Image
from manga_ocr import MangaOcr

# Force UTF-8 encoding for stdout on Windows
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

def perform_ocr(image_path):
    """Perform OCR using official manga-ocr method"""
    try:
        # Initialize MangaOcr (as shown in official docs)
        mocr = MangaOcr()
        
        # Perform OCR directly on image path (official usage)
        text = mocr(image_path)
        
        # Get image dimensions
        img = Image.open(image_path)
        width, height = img.size
        
        # Return as single region
        result = {
            "success": True,
            "text": text,
            "regions": [{
                "text": text,
                "language": "ja",
                "confidence": 0.95,
                "boundingBox": {
                    "x": 0,
                    "y": 0,
                    "width": width,
                    "height": height
                }
            }]
        }
        
        return json.dumps(result, ensure_ascii=False)
        
    except Exception as e:
        error_result = {
            "success": False,
            "error": str(e)
        }
        return json.dumps(error_result, ensure_ascii=False)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(json.dumps({
            "success": False,
            "error": "Usage: python python_ocr_simple.py <image_path>"
        }, ensure_ascii=False))
        sys.exit(1)
    
    image_path = sys.argv[1]
    result = perform_ocr(image_path)
    print(result)
