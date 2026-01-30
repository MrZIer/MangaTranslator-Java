# 快速API测试脚本
$baseUrl = "http://localhost:8080"

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  快速API测试" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 1. 健康检查
Write-Host "[1] 健康检查..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    Write-Host "  ✅ 状态: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "  ❌ 失败: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 2. 创建测试图片
Write-Host "[2] 创建测试图片..." -ForegroundColor Yellow
$pngBytes = [byte[]](
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
    0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
    0x54, 0x08, 0xD7, 0x63, 0x60, 0x00, 0x02, 0x00,
    0x00, 0x05, 0x00, 0x01, 0xE2, 0x26, 0x05, 0x9B,
    0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
    0xAE, 0x42, 0x60, 0x82
)
[System.IO.File]::WriteAllBytes("test.png", $pngBytes)
Write-Host "  ✅ 测试图片已创建" -ForegroundColor Green
Write-Host ""

# 3. 使用curl上传（最可靠的方式）
Write-Host "[3] 上传文件..." -ForegroundColor Yellow

try {
    $result = curl -X POST "$baseUrl/api/upload" `
        -F "file=@test.png" `
        -F "sourceLanguage=ja" `
        -F "targetLanguage=zh" `
        -F "engine=ZHIPU" `
        -F "outputFormat=SINGLE_IMAGE" `
        -H "Accept: application/json" 2>&1 | Out-String
    
    Write-Host "  原始响应:" -ForegroundColor Gray
    Write-Host $result -ForegroundColor White
    
    # 尝试解析JSON
    try {
        $response = $result | ConvertFrom-Json
        
        if ($response.success) {
            Write-Host "  ✅ 上传成功！" -ForegroundColor Green
            Write-Host "    会话ID: $($response.data.sessionId)" -ForegroundColor Cyan
            $sessionId = $response.data.sessionId
        } else {
            Write-Host "  ❌ 上传失败: $($response.message)" -ForegroundColor Red
            Remove-Item "test.png" -Force
            exit 1
        }
    } catch {
        Write-Host "  ❌ 无法解析响应JSON: $_" -ForegroundColor Red
        Remove-Item "test.png" -Force
        exit 1
    }
    
} catch {
    Write-Host "  ❌ 上传请求失败: $_" -ForegroundColor Red
    Remove-Item "test.png" -Force
    exit 1
}
Write-Host ""

# 4. 查询进度
if ($sessionId) {
    Write-Host "[4] 查询任务进度..." -ForegroundColor Yellow
    try {
        $progress = Invoke-RestMethod -Uri "$baseUrl/api/tasks/$sessionId/progress" -Method Get
        
        if ($progress.success) {
            Write-Host "  ✅ 查询成功" -ForegroundColor Green
            Write-Host "    状态: $($progress.data.status)" -ForegroundColor Cyan
            Write-Host "    进度: $($progress.data.progress)%" -ForegroundColor Cyan
            Write-Host "    阶段: $($progress.data.currentStage)" -ForegroundColor Cyan
            if ($progress.data.errorMessage) {
                Write-Host "    错误: $($progress.data.errorMessage)" -ForegroundColor Red
            }
        } else {
            Write-Host "  ❌ 查询失败: $($progress.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ❌ 查询请求失败: $_" -ForegroundColor Red
    }
    Write-Host ""
}

# 5. 检查历史
Write-Host "[5] 查询历史记录..." -ForegroundColor Yellow
try {
    $history = Invoke-RestMethod -Uri "$baseUrl/api/history?page=0&size=5" -Method Get
    
    if ($history.success) {
        Write-Host "  ✅ 查询成功" -ForegroundColor Green
        Write-Host "    总记录: $($history.data.totalElements)" -ForegroundColor Cyan
    } else {
        Write-Host "  ⚠️  查询失败: $($history.message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠️  历史记录不可用: $_" -ForegroundColor Yellow
}
Write-Host ""

# 清理
Remove-Item "test.png" -Force -ErrorAction SilentlyContinue
Write-Host "测试完成！" -ForegroundColor Green
