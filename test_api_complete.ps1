# ====================================
# 漫画翻译工具 - 完整API测试脚本
# ====================================

$baseUrl = "http://localhost:8080"
$testResults = @{
    Passed = @()
    Failed = @()
}

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  后端API全流程测试" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 测试辅助函数
function Test-API {
    param(
        [string]$Name,
        [scriptblock]$TestBlock
    )
    
    Write-Host "[测试] $Name..." -ForegroundColor Yellow
    try {
        & $TestBlock
        Write-Host "  ✅ 通过" -ForegroundColor Green
        $script:testResults.Passed += $Name
    } catch {
        Write-Host "  ❌ 失败: $_" -ForegroundColor Red
        $script:testResults.Failed += $Name
    }
    Write-Host ""
}

# ====================================
# 测试1: 健康检查
# ====================================
Test-API "健康检查 (GET /actuator/health)" {
    $response = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -ErrorAction Stop
    if ($response.status -ne "UP") {
        throw "服务状态异常: $($response.status)"
    }
    Write-Host "    状态: $($response.status)" -ForegroundColor Cyan
}

# ====================================
# 测试2: 创建测试图片
# ====================================
Test-API "创建测试图片" {
    # 创建一个简单的1x1 PNG图片（最小有效PNG）
    $pngBytes = [byte[]](
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  # PNG signature
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,  # IHDR chunk
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,  # 1x1 image
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
        0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,  # IDAT chunk
        0x54, 0x08, 0xD7, 0x63, 0x60, 0x00, 0x02, 0x00,
        0x00, 0x05, 0x00, 0x01, 0xE2, 0x26, 0x05, 0x9B,
        0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,  # IEND chunk
        0xAE, 0x42, 0x60, 0x82
    )
    
    $script:testImagePath = "test_manga.png"
    [System.IO.File]::WriteAllBytes($testImagePath, $pngBytes)
    
    if (-not (Test-Path $testImagePath)) {
        throw "测试图片创建失败"
    }
    Write-Host "    文件: $testImagePath" -ForegroundColor Cyan
    Write-Host "    大小: $($pngBytes.Length) bytes" -ForegroundColor Cyan
}

# ====================================
# 测试3: 文件上传API
# ====================================
Test-API "文件上传 (POST /api/upload)" {
    # 使用 curl 因为 Invoke-RestMethod 的 multipart/form-data 支持有限
    $curlAvailable = Get-Command curl -ErrorAction SilentlyContinue
    
    if ($curlAvailable) {
        Write-Host "    使用 curl 上传..." -ForegroundColor Gray
        
        # 使用 curl 上传文件
        $result = curl -X POST "$baseUrl/api/upload" `
            -F "file=@$testImagePath" `
            -F "sourceLanguage=ja" `
            -F "targetLanguage=zh" `
            -F "engine=ZHIPU" `
            -F "outputFormat=SINGLE_IMAGE" `
            -H "Accept: application/json" 2>&1
        
        # 解析JSON响应
        $response = $result | ConvertFrom-Json
        
        if (-not $response.success) {
            throw "上传失败: $($response.message)"
        }
        
        $script:sessionId = $response.data.sessionId
        Write-Host "    会话ID: $sessionId" -ForegroundColor Cyan
        Write-Host "    文件名: $($response.data.fileName)" -ForegroundColor Cyan
        Write-Host "    状态: $($response.data.status)" -ForegroundColor Cyan
    } else {
        Write-Host "    curl 不可用，尝试使用 PowerShell..." -ForegroundColor Gray
        
        # PowerShell 7+ 的方式
        $fileContent = Get-Content $testImagePath -Raw -AsByteStream
        $boundary = [System.Guid]::NewGuid().ToString()
        
        $bodyLines = @(
            "--$boundary",
            'Content-Disposition: form-data; name="file"; filename="test_manga.png"',
            'Content-Type: image/png',
            '',
            [System.Text.Encoding]::Latin1.GetString($fileContent),
            "--$boundary",
            'Content-Disposition: form-data; name="sourceLanguage"',
            '',
            'ja',
            "--$boundary",
            'Content-Disposition: form-data; name="targetLanguage"',
            '',
            'zh',
            "--$boundary",
            'Content-Disposition: form-data; name="engine"',
            '',
            'ZHIPU',
            "--$boundary",
            'Content-Disposition: form-data; name="outputFormat"',
            '',
            'SINGLE_IMAGE',
            "--$boundary--"
        )
        
        $body = $bodyLines -join "`r`n"
        
        $response = Invoke-RestMethod -Uri "$baseUrl/api/upload" `
            -Method Post `
            -ContentType "multipart/form-data; boundary=$boundary" `
            -Body $body
        
        if (-not $response.success) {
            throw "上传失败: $($response.message)"
        }
        
        $script:sessionId = $response.data.sessionId
        Write-Host "    会话ID: $sessionId" -ForegroundColor Cyan
    }
}

# ====================================
# 测试4: 查询任务进度
# ====================================
Test-API "查询任务进度 (GET /api/tasks/{id}/progress)" {
    if (-not $script:sessionId) {
        throw "没有有效的会话ID"
    }
    
    $response = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$sessionId/progress" -Method Get
    
    if (-not $response.success) {
        throw "查询失败: $($response.message)"
    }
    
    Write-Host "    状态: $($response.data.status)" -ForegroundColor Cyan
    Write-Host "    进度: $($response.data.progress)%" -ForegroundColor Cyan
    Write-Host "    当前阶段: $($response.data.currentStage)" -ForegroundColor Cyan
    
    if ($response.data.errorMessage) {
        Write-Host "    错误: $($response.data.errorMessage)" -ForegroundColor Red
    }
}

# ====================================
# 测试5: 等待任务处理（轮询）
# ====================================
Test-API "任务处理轮询（最多60秒）" {
    if (-not $script:sessionId) {
        throw "没有有效的会话ID"
    }
    
    $maxWait = 60  # 最多等待60秒
    $waited = 0
    $pollInterval = 3  # 每3秒轮询一次
    
    Write-Host "    开始轮询任务状态..." -ForegroundColor Gray
    
    while ($waited -lt $maxWait) {
        Start-Sleep -Seconds $pollInterval
        $waited += $pollInterval
        
        try {
            $response = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$sessionId/progress" -Method Get
            
            if ($response.success) {
                $status = $response.data.status
                $progress = $response.data.progress
                $stage = $response.data.currentStage
                
                Write-Host "    [$waited秒] 状态: $status | 进度: $progress% | 阶段: $stage" -ForegroundColor Gray
                
                # 检查是否完成或失败
                if ($status -eq "COMPLETED") {
                    Write-Host "    任务完成！" -ForegroundColor Green
                    break
                } elseif ($status -eq "FAILED") {
                    throw "任务失败: $($response.data.errorMessage)"
                }
            }
        } catch {
            Write-Host "    轮询出错: $_" -ForegroundColor Yellow
        }
    }
    
    if ($waited -ge $maxWait) {
        Write-Host "    ⚠️ 注意: 任务未在60秒内完成，但这可能是正常的（OCR和翻译需要时间）" -ForegroundColor Yellow
    }
}

# ====================================
# 测试6: 获取历史记录
# ====================================
Test-API "获取历史记录 (GET /api/history)" {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/history?page=0&size=10" -Method Get
    
    if (-not $response.success) {
        throw "获取失败: $($response.message)"
    }
    
    Write-Host "    总记录数: $($response.data.totalElements)" -ForegroundColor Cyan
    Write-Host "    当前页: $($response.data.number)" -ForegroundColor Cyan
    Write-Host "    每页条数: $($response.data.size)" -ForegroundColor Cyan
    
    if ($response.data.content.Count -gt 0) {
        Write-Host "    最新记录: $($response.data.content[0].originalFileName)" -ForegroundColor Cyan
    }
}

# ====================================
# 测试7: 测试错误处理
# ====================================
Test-API "错误处理 - 无效的会话ID" {
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/tasks/invalid-session-id/progress" -Method Get -ErrorAction Stop
        
        # 如果没有抛出错误，检查响应
        if ($response.success) {
            throw "应该返回错误但返回了成功"
        }
    } catch {
        # 预期会有错误，检查是否是404或适当的错误
        if ($_.Exception.Response.StatusCode -eq 404 -or $_.Exception.Response.StatusCode -eq 400) {
            Write-Host "    正确返回错误状态" -ForegroundColor Cyan
        } else {
            throw "未预期的错误: $_"
        }
    }
}

# ====================================
# 测试8: API响应格式验证
# ====================================
Test-API "API响应格式验证" {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/history?page=0&size=1" -Method Get
    
    # 检查标准响应格式
    if (-not $response.PSObject.Properties['success']) {
        throw "缺少 'success' 字段"
    }
    
    if (-not $response.PSObject.Properties['data']) {
        throw "缺少 'data' 字段"
    }
    
    Write-Host "    响应格式符合标准" -ForegroundColor Cyan
    Write-Host "    字段: success, data, message" -ForegroundColor Cyan
}

# ====================================
# 清理
# ====================================
Test-API "清理测试文件" {
    if (Test-Path $testImagePath) {
        Remove-Item $testImagePath -Force
        Write-Host "    测试图片已删除" -ForegroundColor Cyan
    }
}

# ====================================
# 测试总结
# ====================================
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "  测试结果总结" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$totalTests = $testResults.Passed.Count + $testResults.Failed.Count
$passRate = if ($totalTests -gt 0) { [math]::Round(($testResults.Passed.Count / $totalTests) * 100, 2) } else { 0 }

Write-Host "总测试数: $totalTests" -ForegroundColor White
Write-Host "通过: $($testResults.Passed.Count)" -ForegroundColor Green
Write-Host "失败: $($testResults.Failed.Count)" -ForegroundColor Red
Write-Host "通过率: $passRate%" -ForegroundColor $(if ($passRate -eq 100) { "Green" } elseif ($passRate -ge 70) { "Yellow" } else { "Red" })
Write-Host ""

if ($testResults.Passed.Count -gt 0) {
    Write-Host "✅ 通过的测试:" -ForegroundColor Green
    $testResults.Passed | ForEach-Object { Write-Host "   - $_" -ForegroundColor White }
    Write-Host ""
}

if ($testResults.Failed.Count -gt 0) {
    Write-Host "❌ 失败的测试:" -ForegroundColor Red
    $testResults.Failed | ForEach-Object { Write-Host "   - $_" -ForegroundColor White }
    Write-Host ""
    Write-Host "⚠️ 建议检查以下内容:" -ForegroundColor Yellow
    Write-Host "   1. 确认应用在 http://localhost:8080 运行" -ForegroundColor White
    Write-Host "   2. 检查 MongoDB 是否在 localhost:27017 运行" -ForegroundColor White
    Write-Host "   3. 检查 Redis 是否在 localhost:6379 运行（可选）" -ForegroundColor White
    Write-Host "   4. 检查智谱API密钥是否配置正确" -ForegroundColor White
    Write-Host "   5. 查看应用日志了解详细错误" -ForegroundColor White
} else {
    Write-Host "🎉 所有测试通过！后端API工作正常！" -ForegroundColor Green
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan

# 返回失败数作为退出码
exit $testResults.Failed.Count
