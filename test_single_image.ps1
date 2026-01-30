# Test single image OCR
param(
    [Parameter(Mandatory=$true)]
    [string]$ImagePath
)

Write-Host "`n========== OCR测试 ==========" -ForegroundColor Cyan
Write-Host "图片: $ImagePath" -ForegroundColor White

if (-not (Test-Path $ImagePath)) {
    Write-Host "❌ 图片不存在！" -ForegroundColor Red
    exit 1
}

$imageInfo = Get-Item $ImagePath
Write-Host "大小: $([math]::Round($imageInfo.Length/1KB,2)) KB" -ForegroundColor Gray

Write-Host "`n正在加载OCR模型..." -ForegroundColor Yellow
Write-Host "(首次运行会下载模型，请耐心等待...)" -ForegroundColor Gray

$result = python python_ocr_bridge.py $ImagePath 2>&1 | Where-Object { $_ -match '^\{' }

if ($result) {
    Write-Host "`n✅ OCR识别成功！" -ForegroundColor Green
    
    $json = $result | ConvertFrom-Json
    
    Write-Host "`n检测到 $($json.regions.Count) 个文本区域：" -ForegroundColor Cyan
    
    for ($i = 0; $i -lt $json.regions.Count; $i++) {
        $region = $json.regions[$i]
        $box = $region.boundingBox
        
        Write-Host "`n[$($i+1)] 区域位置: ($($box.x), $($box.y)) 大小: $($box.width)x$($box.height)" -ForegroundColor Gray
        Write-Host "    文本: $($region.text)" -ForegroundColor White
    }
    
    Write-Host "`n完整文本:" -ForegroundColor Cyan
    Write-Host $json.text -ForegroundColor White
    
} else {
    Write-Host "`n❌ OCR识别失败" -ForegroundColor Red
}

Write-Host "`n========== 测试完成 ==========`n" -ForegroundColor Cyan
