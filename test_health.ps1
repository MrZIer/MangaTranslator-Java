$response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
Write-Host "应用状态: $($response.status)" -ForegroundColor Green
$response | ConvertTo-Json
