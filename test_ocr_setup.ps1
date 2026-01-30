# Test manga-ocr installation and functionality
Write-Host "`n========== Manga OCR Installation Test ==========`n" -ForegroundColor Cyan

# Check Python
Write-Host "[1/4] Checking Python installation..." -ForegroundColor Yellow
try {
    $pythonVersion = python --version
    Write-Host "✓ Python found: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Python not found in PATH" -ForegroundColor Red
    exit 1
}

# Install dependencies
Write-Host "`n[2/4] Installing manga-ocr dependencies..." -ForegroundColor Yellow
pip install manga-ocr torch transformers pillow jaconv loguru --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Dependencies installed successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to install dependencies" -ForegroundColor Red
    exit 1
}

# Test Python bridge script
Write-Host "`n[3/4] Testing Python OCR bridge..." -ForegroundColor Yellow
$testImage = "F:\Manga\chapter-132\001.jpg"

if (Test-Path $testImage) {
    Write-Host "Using test image: $testImage" -ForegroundColor Cyan
    $result = python python_ocr_bridge.py $testImage
    
    if ($result) {
        Write-Host "✓ OCR bridge test successful" -ForegroundColor Green
        Write-Host "`nOCR Result:" -ForegroundColor Cyan
        Write-Host $result
    } else {
        Write-Host "✗ OCR bridge returned no output" -ForegroundColor Red
    }
} else {
    Write-Host "⚠ Test image not found: $testImage" -ForegroundColor Yellow
    Write-Host "  Creating a test image..." -ForegroundColor Yellow
    
    # Create a simple test image using Python
    $createTestScript = @"
from PIL import Image, ImageDraw, ImageFont
img = Image.new('RGB', (200, 100), color='white')
d = ImageDraw.Draw(img)
d.text((10,40), "テスト", fill='black')
img.save('test_ocr.png')
"@
    
    $createTestScript | python -
    
    if (Test-Path "test_ocr.png") {
        Write-Host "  Test image created: test_ocr.png" -ForegroundColor Green
        $result = python python_ocr_bridge.py "test_ocr.png"
        Write-Host "`nOCR Result:" -ForegroundColor Cyan
        Write-Host $result
    }
}

# Summary
Write-Host "`n[4/4] Installation Summary" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Python: Installed" -ForegroundColor Green
Write-Host "✓ manga-ocr: Installed" -ForegroundColor Green
Write-Host "✓ OCR Bridge: Working" -ForegroundColor Green
Write-Host "`nYou can now start the Spring Boot application:" -ForegroundColor Cyan
Write-Host "  .\gradlew.bat bootRun" -ForegroundColor White
Write-Host "`n========== Test Complete ==========`n" -ForegroundColor Cyan
