# Backend API Test Script
Write-Host "`n===== Backend API Test =====" -ForegroundColor Cyan

# 1. Health Check
Write-Host "`n[1/5] Testing Health Check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
    Write-Host "Health Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "Health Check Failed: $_" -ForegroundColor Red
    exit 1
}

# 2. Create Test Image
Write-Host "`n[2/5] Creating Test Image..." -ForegroundColor Yellow
[byte[]]$pngBytes = @(137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,15,0,0,1,1,1,0,24,221,141,17,0,0,0,0,73,69,78,68,174,66,96,130)
[System.IO.File]::WriteAllBytes("$PWD\test.png", $pngBytes)
Write-Host "Test image created: test.png ($((Get-Item test.png).Length) bytes)" -ForegroundColor Green

# 3. Upload File
Write-Host "`n[3/5] Testing File Upload..." -ForegroundColor Yellow
try {
    $file = Get-Item "test.png"
    $fileBytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $fileEnc = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"test.png`"$LF",
        $fileEnc,
        "--$boundary",
        "Content-Disposition: form-data; name=`"sourceLanguage`"$LF",
        "ja",
        "--$boundary",
        "Content-Disposition: form-data; name=`"targetLanguage`"$LF",
        "zh",
        "--$boundary",
        "Content-Disposition: form-data; name=`"engine`"$LF",
        "ZHIPU",
        "--$boundary",
        "Content-Disposition: form-data; name=`"outputFormat`"$LF",
        "SINGLE_IMAGE",
        "--$boundary--$LF"
    ) -join $LF
    
    $upload = Invoke-RestMethod -Uri "http://localhost:8080/api/upload" `
        -Method Post `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body ([System.Text.Encoding]::GetEncoding('iso-8859-1').GetBytes($bodyLines))
    
    Write-Host "Upload Success: $($upload.success)" -ForegroundColor Green
    Write-Host "Message: $($upload.message)" -ForegroundColor Green
    $sessionId = $upload.data.sessionId
    Write-Host "Session ID: $sessionId" -ForegroundColor Cyan
    Write-Host "Status: $($upload.data.status)" -ForegroundColor Green
    
    # 4. Check Progress
    Write-Host "`n[4/5] Testing Progress Query..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    $progress = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$sessionId/progress"
    Write-Host "Task Status: $($progress.data.status)" -ForegroundColor Green
    Write-Host "Progress: $($progress.data.progress)%" -ForegroundColor Green
    Write-Host "Current Stage: $($progress.data.currentStage)" -ForegroundColor Green
    
    # 5. Check History
    Write-Host "`n[5/5] Testing History Query..." -ForegroundColor Yellow
    $history = Invoke-RestMethod -Uri "http://localhost:8080/api/history?page=0&size=10"
    Write-Host "Total Records: $($history.data.totalElements)" -ForegroundColor Green
    Write-Host "Total Pages: $($history.data.totalPages)" -ForegroundColor Green
    
} catch {
    Write-Host "Test Failed: $_" -ForegroundColor Red
} finally {
    # Cleanup
    if (Test-Path "test.png") {
        Remove-Item "test.png"
        Write-Host "`nTest image cleaned up" -ForegroundColor Gray
    }
}

Write-Host "`n===== Test Complete =====" -ForegroundColor Cyan
