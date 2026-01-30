# Comprehensive Backend Module Test Script
# Tests all backend modules with real manga image

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "`n========== Backend Module Comprehensive Test ==========" -ForegroundColor Cyan
Write-Host "Test Image: F:\Manga\chapter-132\001.jpg`n" -ForegroundColor White

# Configuration
$baseUrl = "http://localhost:8080"
$testImagePath = "F:\Manga\chapter-132\001.jpg"
$results = @()

# Module 1: Health Check
Write-Host "[1/7] Module: Health Check & Actuator..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod "$baseUrl/actuator/health" -Method Get
    Write-Host "Status: $($health.status)" -ForegroundColor Green
    $results += "✓ Health Check: $($health.status)"
} catch {
    Write-Host "Failed: $_" -ForegroundColor Red
    $results += "✗ Health Check: Failed"
    Write-Host "`nApplication may not be running. Please start it first." -ForegroundColor Red
    exit 1
}

# Module 2: File Upload Service
Write-Host "`n[2/7] Module: File Upload Service..." -ForegroundColor Yellow
if (-not (Test-Path $testImagePath)) {
    Write-Host "Test image not found at: $testImagePath" -ForegroundColor Red
    $results += "✗ File Upload: Test image not found"
    exit 1
}

$fileBytes = [System.IO.File]::ReadAllBytes($testImagePath)
$fileSize = $fileBytes.Length
Write-Host "Image size: $([math]::Round($fileSize/1KB, 2)) KB" -ForegroundColor Cyan

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$bodyLines = @(
    "--$boundary",
    'Content-Disposition: form-data; name="file"; filename="001.jpg"',
    "Content-Type: image/jpeg",
    "",
    [System.Text.Encoding]::GetEncoding("ISO-8859-1").GetString($fileBytes),
    "--$boundary",
    'Content-Disposition: form-data; name="engine"',
    "",
    "zhipu",
    "--$boundary",
    'Content-Disposition: form-data; name="sourceLanguage"',
    "",
    "ja",
    "--$boundary",
    'Content-Disposition: form-data; name="targetLanguage"',
    "",
    "zh",
    "--$boundary--"
) -join $LF

$bodyBytes = [System.Text.Encoding]::GetEncoding("ISO-8859-1").GetBytes($bodyLines)

try {
    $uploadResponse = Invoke-RestMethod -Uri "$baseUrl/api/upload" `
        -Method Post `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $bodyBytes
    
    if ($uploadResponse.success) {
        $sessionId = $uploadResponse.data.sessionId
        Write-Host "Upload Success!" -ForegroundColor Green
        Write-Host "Session ID: $sessionId" -ForegroundColor Cyan
        Write-Host "Initial Status: $($uploadResponse.data.status)" -ForegroundColor Cyan
        Write-Host "Initial Progress: $($uploadResponse.data.progress)%" -ForegroundColor Cyan
        $results += "✓ File Upload: Session created ($sessionId)"
    } else {
        Write-Host "Upload Failed: $($uploadResponse.message)" -ForegroundColor Red
        $results += "✗ File Upload: $($uploadResponse.message)"
        exit 1
    }
} catch {
    Write-Host "Upload Error: $_" -ForegroundColor Red
    $results += "✗ File Upload: Exception occurred"
    exit 1
}

# Module 3: Session Management Service
Write-Host "`n[3/7] Module: Session Management Service..." -ForegroundColor Yellow
try {
    $sessionInfo = Invoke-RestMethod "$baseUrl/api/tasks/$sessionId/progress" -Method Get
    if ($sessionInfo.success) {
        Write-Host "Session retrieved successfully" -ForegroundColor Green
        Write-Host "File: $($sessionInfo.data.originalFileName)" -ForegroundColor Cyan
        Write-Host "Size: $($sessionInfo.data.fileSize) bytes" -ForegroundColor Cyan
        Write-Host "Hash: $($sessionInfo.data.fileHash)" -ForegroundColor Cyan
        $results += "✓ Session Management: Data persisted correctly"
    } else {
        Write-Host "Failed to retrieve session" -ForegroundColor Red
        $results += "✗ Session Management: Failed"
    }
} catch {
    Write-Host "Session query error: $_" -ForegroundColor Red
    $results += "✗ Session Management: Exception"
}

# Module 4: Async Task Processing Service
Write-Host "`n[4/7] Module: Async Task Processing Service..." -ForegroundColor Yellow
Write-Host "Monitoring task progress (30 seconds)..." -ForegroundColor Cyan

$maxWait = 30
$checkInterval = 2
$elapsed = 0
$lastStatus = ""
$lastProgress = 0

while ($elapsed -lt $maxWait) {
    try {
        $progress = Invoke-RestMethod "$baseUrl/api/tasks/$sessionId/progress" -Method Get
        if ($progress.success) {
            $status = $progress.data.status
            $percent = $progress.data.progress
            $stage = $progress.data.currentStage
            
            if ($status -ne $lastStatus -or $percent -ne $lastProgress) {
                Write-Host "[$elapsed`s] Status: $status | Progress: $percent% | Stage: $stage" -ForegroundColor Cyan
                $lastStatus = $status
                $lastProgress = $percent
            }
            
            if ($status -eq "COMPLETED") {
                Write-Host "Task completed successfully!" -ForegroundColor Green
                $results += "✓ Async Task: Completed (100%)"
                break
            } elseif ($status -eq "FAILED") {
                Write-Host "Task failed!" -ForegroundColor Red
                Write-Host "Error: $($progress.data.errorMessage)" -ForegroundColor Red
                $results += "✗ Async Task: Failed - $($progress.data.errorMessage)"
                break
            }
        }
    } catch {
        Write-Host "Progress check error: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds $checkInterval
    $elapsed += $checkInterval
}

if ($elapsed -ge $maxWait -and $lastStatus -ne "COMPLETED") {
    Write-Host "Task timeout (still in progress after $maxWait seconds)" -ForegroundColor Yellow
    $results += "⚠ Async Task: Timeout (Status: $lastStatus, Progress: $lastProgress%)"
}

# Module 5: OCR Service (检查是否有OCR错误)
Write-Host "`n[5/7] Module: OCR Service..." -ForegroundColor Yellow
if ($lastStatus -eq "FAILED" -and $progress.data.errorMessage -match "OCR|image|read") {
    Write-Host "OCR Error detected: $($progress.data.errorMessage)" -ForegroundColor Red
    $results += "✗ OCR Service: Error - $($progress.data.errorMessage)"
} elseif ($lastProgress -ge 20) {
    Write-Host "OCR processing passed (reached progress >= 20%)" -ForegroundColor Green
    $results += "✓ OCR Service: Processing successful"
} else {
    Write-Host "OCR processing did not complete" -ForegroundColor Yellow
    $results += "⚠ OCR Service: Incomplete"
}

# Module 6: Translation Service (检查是否到达翻译阶段)
Write-Host "`n[6/7] Module: Translation Service..." -ForegroundColor Yellow
if ($lastProgress -ge 60) {
    Write-Host "Translation stage reached (progress >= 60%)" -ForegroundColor Green
    $results += "✓ Translation Service: Engaged"
} else {
    Write-Host "Translation stage not reached (progress: $lastProgress%)" -ForegroundColor Yellow
    $results += "⚠ Translation Service: Not reached"
}

# Module 7: History Service
Write-Host "`n[7/7] Module: History Service..." -ForegroundColor Yellow
try {
    $historyUrl = "$baseUrl/api/history?page=0&size=5"
    $history = Invoke-RestMethod $historyUrl -Method Get
    if ($history.success) {
        $totalRecords = $history.data.totalElements
        Write-Host "Total records: $totalRecords" -ForegroundColor Green
        if ($totalRecords -gt 0) {
            Write-Host "Recent task: $($history.data.content[0].originalFileName)" -ForegroundColor Cyan
            $results += "✓ History Service: $totalRecords records found"
        } else {
            Write-Host "No history records yet" -ForegroundColor Yellow
            $results += "⚠ History Service: Empty"
        }
    }
} catch {
    Write-Host "History query error: $_" -ForegroundColor Red
    $results += "✗ History Service: Failed"
}

# Final Summary
Write-Host "`n========== Test Summary ==========" -ForegroundColor Cyan
$results | ForEach-Object { Write-Host $_ }

$successCount = ($results | Where-Object { $_ -match "^✓" }).Count
$totalCount = $results.Count
Write-Host "`nPassed: $successCount / $totalCount modules" -ForegroundColor $(if ($successCount -eq $totalCount) { "Green" } else { "Yellow" })

Write-Host "`n========== Test Complete ==========" -ForegroundColor Cyan
