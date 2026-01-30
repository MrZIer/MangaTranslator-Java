# 后端API测试脚本
Write-Host "====== 漫画翻译工具API测试 ======`n" -ForegroundColor Cyan

# 1. 健康检查
Write-Host "[1/5] 测试健康检查..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method Get -UseBasicParsing
    Write-Host "✓ 健康检查成功: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "✗ 健康检查失败: $_" -ForegroundColor Red
    exit 1
}

# 2. 创建测试图片
Write-Host "`n[2/5] 创建测试图片..." -ForegroundColor Yellow
[byte[]]$pngBytes = @(137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, 144, 119, 83, 222, 0, 0, 0, 12, 73, 68, 65, 84, 8, 215, 99, 248, 15, 0, 0, 1, 1, 1, 0, 24, 221, 141, 17, 0, 0, 0, 0, 73, 69, 78, 68, 174, 66, 96, 130)
$testImagePath = Join-Path $PSScriptRoot "test.png"
[System.IO.File]::WriteAllBytes($testImagePath, $pngBytes)
Write-Host "✓ 测试图片已创建: test.png ($((Get-Item $testImagePath).Length) bytes)" -ForegroundColor Green

# 3. 上传文件
Write-Host "`n[3/5] 上传文件..." -ForegroundColor Yellow
try {
    # 使用multipart/form-data
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    
    # 读取文件内容
    $fileContent = [System.IO.File]::ReadAllBytes($testImagePath)
    
    # 构建multipart body
    $bodyLines = @(
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"test.png`"",
        "Content-Type: image/png",
        "",
        [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileContent),
        "--$boundary",
        "Content-Disposition: form-data; name=`"sourceLanguage`"",
        "",
        "ja",
        "--$boundary",
        "Content-Disposition: form-data; name=`"targetLanguage`"",
        "",
        "zh",
        "--$boundary",
        "Content-Disposition: form-data; name=`"engine`"",
        "",
        "ZHIPU",
        "--$boundary",
        "Content-Disposition: form-data; name=`"outputFormat`"",
        "",
        "SINGLE_IMAGE",
        "--$boundary--"
    )
    
    $body = $bodyLines -join $LF
    $bodyBytes = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($body)
    
    $uploadResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/upload" `
        -Method Post `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $bodyBytes `
        -UseBasicParsing
    
    $uploadData = $uploadResponse.Content | ConvertFrom-Json
    Write-Host "✓ 上传成功" -ForegroundColor Green
    Write-Host "响应: $($uploadResponse.Content)" -ForegroundColor Cyan
    
    $sessionId = $uploadData.data.sessionId
    Write-Host "SessionID: $sessionId" -ForegroundColor Yellow
    
    # 4. 查询进度
    Write-Host "`n[4/5] 查询进度（3次）..." -ForegroundColor Yellow
    for ($i = 1; $i -le 3; $i++) {
        Start-Sleep -Seconds 2
        try {
            $progressResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks/$sessionId/progress" -UseBasicParsing
            $progressData = $progressResponse.Content | ConvertFrom-Json
            Write-Host "进度查询 #${i}: 状态=$($progressData.data.status), 进度=$($progressData.data.progress)%, 阶段=$($progressData.data.currentStage)" -ForegroundColor Green
        } catch {
            Write-Host "✗ 进度查询 #${i} 失败: $_" -ForegroundColor Red
        }
    }
    
    # 5. 查询历史
    Write-Host "`n[5/5] 查询历史..." -ForegroundColor Yellow
    try {
        $historyResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/history?page=0&size=10" -UseBasicParsing
        Write-Host "✓ 历史记录获取成功" -ForegroundColor Green
        Write-Host "响应: $($historyResponse.Content)" -ForegroundColor Cyan
    } catch {
        Write-Host "✗ 历史记录查询失败: $_" -ForegroundColor Red
    }
    
} catch {
    Write-Host "✗ 上传失败: $_" -ForegroundColor Red
    Write-Host "错误详情: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "服务器响应: $responseBody" -ForegroundColor Red
    }
} finally {
    # 清理测试文件
    if (Test-Path $testImagePath) {
        Remove-Item $testImagePath -Force
        Write-Host "`n✓ 测试文件已清理" -ForegroundColor Green
    }
}

Write-Host "`n====== 测试完成 ======" -ForegroundColor Cyan
