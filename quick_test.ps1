Write-Host "`n========== Backend API Full Test ==========" -ForegroundColor Cyan

# 1. Health Check
Write-Host "`n[1/5] Health Check..." -ForegroundColor Yellow
$h = Invoke-RestMethod "http://localhost:8080/actuator/health"
Write-Host "Status: $($h.status)" -ForegroundColor Green

# 2. Create Test Image
Write-Host "`n[2/5] Create Test Image..." -ForegroundColor Yellow
[byte[]]$png = @(137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,15,0,0,1,1,1,0,24,221,141,17,0,0,0,0,73,69,78,68,174,66,96,130)
[IO.File]::WriteAllBytes("test.png", $png)
Write-Host "Test image created" -ForegroundColor Green

# 3. Upload File
Write-Host "`n[3/5] Upload File..." -ForegroundColor Yellow
$file = Get-Item "test.png"
$fileBytes = [IO.File]::ReadAllBytes($file.FullName)
$fileEnc = [Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)
$boundary = [Guid]::NewGuid().ToString()
$LF = "`r`n"
$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"test.png`"",
    "Content-Type: image/png$LF",
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

try {
    $upload = Invoke-RestMethod -Uri "http://localhost:8080/api/upload" -Method Post -ContentType "multipart/form-data; boundary=$boundary" -Body ([Text.Encoding]::GetEncoding('iso-8859-1').GetBytes($bodyLines))
    Write-Host "Upload Response:" -ForegroundColor Green
    Write-Host ($upload | ConvertTo-Json -Depth 3) -ForegroundColor Gray
    
    if ($upload.data -and $upload.data.sessionId) {
        $sessionId = $upload.data.sessionId
        Write-Host "SessionID: $sessionId" -ForegroundColor Cyan
    } else {
        Write-Host "ERROR: No sessionId in response!" -ForegroundColor Red
        Write-Host "Full response: $($upload | ConvertTo-Json)" -ForegroundColor Yellow
        exit
    }
} catch {
    Write-Host "Upload Failed: $_" -ForegroundColor Red
    exit
}

# 4. Check Progress
Write-Host "`n[4/5] Check Progress..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
$progress = Invoke-RestMethod "http://localhost:8080/api/tasks/$sessionId/progress"
Write-Host "Task Status: $($progress.data.status)" -ForegroundColor Green
Write-Host "Progress: $($progress.data.progress)%" -ForegroundColor Cyan
Write-Host "Current Stage: $($progress.data.currentStage)" -ForegroundColor Cyan
if ($progress.data.errorMessage) {
    Write-Host "Error: $($progress.data.errorMessage)" -ForegroundColor Red
}

# 5. Query History
Write-Host "`n[5/5] Query History..." -ForegroundColor Yellow
$historyUrl = "http://localhost:8080/api/history?page=0&size=10"
$history = Invoke-RestMethod $historyUrl
Write-Host "Total Records: $($history.data.totalElements)" -ForegroundColor Green
Write-Host "Recent Tasks:" -ForegroundColor Cyan
$history.data.content | Select-Object -First 3 | ForEach-Object {
    Write-Host "  - $($_.fileName) [$($_.status)]" -ForegroundColor Gray
}

# Cleanup
Remove-Item "test.png" -ErrorAction SilentlyContinue

Write-Host "`n========== Test Complete ==========" -ForegroundColor Cyan
