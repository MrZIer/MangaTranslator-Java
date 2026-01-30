# 测试应用健康状态
Write-Host "=== 测试应用健康状态 ===" -ForegroundColor Cyan

try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host "✅ 应用状态: $($health.status)" -ForegroundColor Green
    $health | ConvertTo-Json
} catch {
    Write-Host "❌ 健康检查失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "应用已成功启动在: http://localhost:8080" -ForegroundColor Green
Write-Host ""
Write-Host "可用的API端点:" -ForegroundColor Yellow
Write-Host "- POST /api/upload - 上传漫画图片" -ForegroundColor White
Write-Host "- GET /api/tasks/{sessionId}/progress - 查询任务进度" -ForegroundColor White
Write-Host "- GET /api/history - 获取历史记录" -ForegroundColor White
Write-Host "- GET /actuator/health - 健康检查" -ForegroundColor White
