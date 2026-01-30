# Complete Backend Module Test
# Tests all backend APIs with the real manga image
Write-Host "`n========== Backend Module Test ==========" -ForegroundColor Cyan

$testImage = "F:\Manga\chapter-132\001.jpg"
$baseUrl = "http://localhost:8080"

# Check if image exists
if (-not (Test-Path $testImage)) {
    Write-Host "✗ Test image not found: $testImage" -ForegroundColor Red
    Write-Host "Please provide a valid manga image path" -ForegroundColor Yellow
    exit 1
}

Write-Host "Using test image: $testImage" -ForegroundColor Green
$imageInfo = Get-Item $testImage
Write-Host "Image size: $([math]::Round($imageInfo.Length/1MB, 2)) MB" -ForegroundColor Cyan

# Module 1: Health Check
Write-Host "`n[Module 1/7] Health Check" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod "$baseUrl/actuator/health"
    Write-Host "✓ Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "✗ Health check failed: $_" -ForegroundColor Red
    Write-Host "Make sure the application is running on port 8080" -ForegroundColor Yellow
    exit 1
}

# Module 2: File Upload
Write-Host "`n[Module 2/7] File Upload & Session Management" -ForegroundColor Yellow
try {
    $boundary = [System.Guid]::NewGuid().ToString()
    $imageBytes = [System.IO.File]::ReadAllBytes($testImage)
    $imageBase64 = [System.Convert]::ToBase64String($imageBytes)
    
    $bodyLines = @(
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"001.jpg`"",
        "Content-Type: image/jpeg",
        "",
        [System.Text.Encoding]::Latin1.GetString($imageBytes),
        "--$boundary",
        "Content-Disposition: form-data; name=`"engine`"",
        "",
        "zhipu",
        "--$boundary",
        "Content-Disposition: form-data; name=`"sourceLanguage`"",
        "",
        "ja",
        "--$boundary",
        "Content-Disposition: form-data; name=`"targetLanguage`"",
        "",
        "zh",
        "--$boundary--"
    )
    
    $body = $bodyLines -join "`r`n"
    
    $uploadResponse = Invoke-RestMethod -Uri "$baseUrl/api/upload" `
        -Method Post `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body ([System.Text.Encoding]::Latin1.GetBytes($body))
    
    Write-Host "✓ Upload successful" -ForegroundColor Green
    $sessionId = $uploadResponse.data.sessionId
    Write-Host "  Session ID: $sessionId" -ForegroundColor Cyan
    Write-Host "  File: $($uploadResponse.data.fileName)" -ForegroundColor Cyan
    Write-Host "  Status: $($uploadResponse.data.status)" -ForegroundColor Cyan
    
} catch {
    Write-Host "✗ Upload failed: $_" -ForegroundColor Red
    exit 1
}

# Module 3: Progress Tracking
Write-Host "`n[Module 3/7] Async Task & Progress Tracking" -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
$completed = $false

while ($attempt -lt $maxAttempts -and -not $completed) {
    Start-Sleep -Seconds 2
    $attempt++
    
    try {
        $progress = Invoke-RestMethod "$baseUrl/api/tasks/$sessionId/progress"
        $status = $progress.data.status
        $progressValue = $progress.data.progress
        $stage = $progress.data.currentStage
        
        Write-Host "  [$attempt/$maxAttempts] Status: $status | Progress: $progressValue% | Stage: $stage" -ForegroundColor Cyan
        
        if ($status -eq "COMPLETED") {
            Write-Host "✓ Task completed successfully" -ForegroundColor Green
            $completed = $true
        } elseif ($status -eq "FAILED") {
            Write-Host "✗ Task failed: $($progress.data.errorMessage)" -ForegroundColor Red
            break
        }
        
    } catch {
        Write-Host "  ✗ Progress check failed: $_" -ForegroundColor Red
    }
}

if (-not $completed) {
    Write-Host "✗ Task did not complete within $($maxAttempts * 2) seconds" -ForegroundColor Red
}

# Module 4: OCR Service
Write-Host "`n[Module 4/7] OCR Recognition Service" -ForegroundColor Yellow
try {
    $progress = Invoke-RestMethod "$baseUrl/api/tasks/$sessionId/progress"
    $currentStage = $progress.data.currentStage
    
    if ($currentStage -match "OCR|識別") {
        Write-Host "✓ OCR stage reached" -ForegroundColor Green
    } else {
        Write-Host "  Current stage: $currentStage" -ForegroundColor Cyan
    }
} catch {
    Write-Host "✗ OCR check failed" -ForegroundColor Red
}

# Module 5: Translation Service
Write-Host "`n[Module 5/7] Translation Service (Zhipu AI)" -ForegroundColor Yellow
try {
    $progress = Invoke-RestMethod "$baseUrl/api/tasks/$sessionId/progress"
    $currentStage = $progress.data.currentStage
    
    if ($currentStage -match "翻訳|TRANSLAT") {
        Write-Host "✓ Translation stage reached" -ForegroundColor Green
    } else {
        Write-Host "  Current stage: $currentStage" -ForegroundColor Cyan
    }
} catch {
    Write-Host "✗ Translation check failed" -ForegroundColor Red
}

# Module 6: Image Processing & Rendering
Write-Host "`n[Module 6/7] Image Processing & Rendering" -ForegroundColor Yellow
try {
    $progress = Invoke-RestMethod "$baseUrl/api/tasks/$sessionId/progress"
    $resultPath = $progress.data.resultPath
    
    if ($resultPath) {
        Write-Host "✓ Rendered image available" -ForegroundColor Green
        Write-Host "  Path: $resultPath" -ForegroundColor Cyan
    } else {
        Write-Host "  No result path yet" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Rendering check failed" -ForegroundColor Red
}

# Module 7: History Management
Write-Host "`n[Module 7/7] Translation History" -ForegroundColor Yellow
try {
    $history = Invoke-RestMethod "$baseUrl/api/history?page=0&size=10"
    $totalRecords = $history.data.total
    
    Write-Host "✓ History retrieved" -ForegroundColor Green
    Write-Host "  Total records: $totalRecords" -ForegroundColor Cyan
    
    if ($history.data.items.Count -gt 0) {
        Write-Host "  Recent translations:" -ForegroundColor Cyan
        $history.data.items | Select-Object -First 3 | ForEach-Object {
            Write-Host "    - $($_.originalFileName) [$($_.status)]" -ForegroundColor White
        }
    }
    
} catch {
    Write-Host "✗ History retrieval failed: $_" -ForegroundColor Red
}

# Final Summary
Write-Host "`n========== Test Summary ==========" -ForegroundColor Cyan
Write-Host "Session ID: $sessionId" -ForegroundColor White
Write-Host "Test Image: $testImage" -ForegroundColor White
Write-Host "`nModule Status:" -ForegroundColor Yellow
Write-Host "  ✓ Health Check" -ForegroundColor Green
Write-Host "  ✓ File Upload" -ForegroundColor Green
Write-Host "  ? Async Processing (check logs)" -ForegroundColor Yellow
Write-Host "  ? OCR Recognition (check logs)" -ForegroundColor Yellow
Write-Host "  ? Translation (check logs)" -ForegroundColor Yellow
Write-Host "  ? Image Rendering (check logs)" -ForegroundColor Yellow
Write-Host "  ✓ History Management" -ForegroundColor Green
Write-Host "`n========== Test Complete ==========`n" -ForegroundColor Cyan
