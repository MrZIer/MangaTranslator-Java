# Manga Translation Tool - OCR Setup

## Installation Steps

### 1. Install Python Dependencies

```powershell
# Install manga-ocr and dependencies
pip install -r requirements.txt
```

### 2. Test Python OCR Bridge

```powershell
# Test with a sample image
python python_ocr_bridge.py "F:\Manga\chapter-132\001.jpg"
```

Expected output:
```json
{
  "success": true,
  "text": "認識されたテキスト",
  "regions": [...]
}
```

### 3. Verify Installation

If the installation is successful, you should see:
- No errors when running the test command
- JSON output with recognized text

### 4. Run the Application

After successful installation, start the Spring Boot application:

```powershell
.\gradlew.bat bootRun
```

## Troubleshooting

### Issue: "manga-ocr" not found
- Make sure Python is installed and in PATH
- Try: `python -m pip install manga-ocr`

### Issue: CUDA/GPU errors
- manga-ocr will automatically fall back to CPU
- No action needed for CPU-only environments

### Issue: Model download fails
- manga-ocr downloads models from Hugging Face (kha-white/manga-ocr-base)
- Ensure internet connection is available
- Model is ~400MB and will be cached after first download

## Configuration

Edit `application.properties` to configure OCR:

```properties
# OCR Service Configuration
ocr.service.mode=embedded
ocr.model.path=./models/manga_ocr
ocr.model.device=cpu
```

- Set `ocr.model.device=cuda` if you have NVIDIA GPU
- Set `ocr.model.device=mps` if you have Apple Silicon Mac
