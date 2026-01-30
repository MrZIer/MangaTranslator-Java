# 后端API全流程测试文档

## 前置准备

✅ 应用已成功启动在端口 8080
✅ Actuator依赖已添加
✅ MongoDB已连接 (localhost:27017)

## 当前状态

- 应用已运行 PID: 16512
- 端口: 8080
- 环境: default profile
- Tomcat: Apache Tomcat/10.1.50
- Spring Boot: 3.5.10

## 测试步骤

### 1. 健康检查
```powershell
C:\Windows\System32\curl.exe http://localhost:8080/actuator/health
```

**预期结果**: 返回 `{"status":"UP"}`

### 2. 创建测试图片
```powershell
# 创建1x1像素的PNG测试图片
[byte[]]$pngBytes = @(137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, 144, 119, 83, 222, 0, 0, 0, 12, 73, 68, 65, 84, 8, 215, 99, 248, 15, 0, 0, 1, 1, 1, 0, 24, 221, 141, 17, 0, 0, 0, 0, 73, 69, 78, 68, 174, 66, 96, 130)
[System.IO.File]::WriteAllBytes("$PWD\test.png", $pngBytes)
Write-Host "Test image created: test.png ($(Get-Item test.png).Length bytes)"
```

### 3. 上传文件（核心测试）
```powershell
# 使用curl上传文件
C:\Windows\System32\curl.exe -X POST "http://localhost:8080/api/upload" `
  -F "file=@test.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE" `
  -v
```

**预期结果**: 
- HTTP 200 OK
- JSON响应:
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "sessionId": "xxx-xxx-xxx",
    "fileName": "test.png",
    "status": "UPLOADED",
    "progress": 0,
    "currentStage": "UPLOADING"
  }
}
```

**重要**: 保存返回的 `sessionId` 用于后续测试

### 4. 查询进度
```powershell
# 替换 {sessionId} 为上一步返回的实际值
$sessionId = "your-session-id-here"
C:\Windows\System32\curl.exe "http://localhost:8080/api/tasks/${sessionId}/progress"
```

**预期结果**:
```json
{
  "success": true,
  "data": {
    "sessionId": "xxx-xxx-xxx",
    "fileName": "test.png",
    "status": "PROCESSING",
    "progress": 50,
    "currentStage": "OCR_PROCESSING",
    "createdAt": "2026-01-30T18:31:00"
  }
}
```

**状态流转**:
- UPLOADED (0%) → PROCESSING (1-99%) → COMPLETED (100%)
- 阶段: UPLOADING → OCR_PROCESSING → TRANSLATING → RENDERING → COMPLETED

### 5. 查询历史记录
```powershell
C:\Windows\System32\curl.exe "http://localhost:8080/api/history?page=0&size=10"
```

**预期结果**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "xxx",
        "fileName": "test.png",
        "status": "COMPLETED",
        "createdAt": "2026-01-30T18:31:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0
  }
}
```

### 6. 下载结果（完成后）
```powershell
C:\Windows\System32\curl.exe "http://localhost:8080/api/tasks/${sessionId}/download" -o result.png
```

## 异常测试

### 7. 测试文件大小限制（>10MB）
```powershell
# 创建11MB的大文件
$bigFile = New-Object byte[] (11 * 1024 * 1024)
[System.IO.File]::WriteAllBytes("$PWD\big_test.png", $bigFile)

C:\Windows\System32\curl.exe -X POST "http://localhost:8080/api/upload" `
  -F "file=@big_test.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
```

**预期结果**: 
- HTTP 400 Bad Request
- 错误信息: "File size exceeds maximum limit of 10MB"

### 8. 测试空文件
```powershell
# 创建空文件
New-Item -Path "empty.png" -ItemType File

C:\Windows\System32\curl.exe -X POST "http://localhost:8080/api/upload" `
  -F "file=@empty.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
```

**预期结果**: 
- HTTP 400 Bad Request
- 错误信息: "File is empty"

### 9. 测试不支持的文件类型
```powershell
# 创建.txt文件
"test" | Out-File -FilePath "test.txt"

C:\Windows\System32\curl.exe -X POST "http://localhost:8080/api/upload" `
  -F "file=@test.txt" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
```

**预期结果**: 
- HTTP 400 Bad Request
- 错误信息: "Unsupported file type"

### 10. 测试查询不存在的任务
```powershell
C:\Windows\System32\curl.exe "http://localhost:8080/api/tasks/non-existent-id/progress"
```

**预期结果**: 
- HTTP 404 Not Found
- 错误信息: "Session not found"

## 完整测试脚本

```powershell
# 后端API完整测试脚本

Write-Host "====== 漫画翻译工具后端API测试 ======" -ForegroundColor Cyan

# 1. 健康检查
Write-Host "`n[1/10] 健康检查..." -ForegroundColor Yellow
$health = C:\Windows\System32\curl.exe -s http://localhost:8080/actuator/health
Write-Host "健康状态: $health" -ForegroundColor Green

# 2. 创建测试图片
Write-Host "`n[2/10] 创建测试图片..." -ForegroundColor Yellow
[byte[]]$pngBytes = @(137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, 144, 119, 83, 222, 0, 0, 0, 12, 73, 68, 65, 84, 8, 215, 99, 248, 15, 0, 0, 1, 1, 1, 0, 24, 221, 141, 17, 0, 0, 0, 0, 73, 69, 78, 68, 174, 66, 96, 130)
[System.IO.File]::WriteAllBytes("$PWD\test.png", $pngBytes)
Write-Host "测试图片已创建: test.png ($(Get-Item test.png).Length bytes)" -ForegroundColor Green

# 3. 上传文件
Write-Host "`n[3/10] 上传文件..." -ForegroundColor Yellow
$uploadResponse = C:\Windows\System32\curl.exe -s -X POST "http://localhost:8080/api/upload" `
  -F "file=@test.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
Write-Host "上传响应:" -ForegroundColor Green
Write-Host $uploadResponse

# 提取sessionId（需要JSON解析）
$uploadJson = $uploadResponse | ConvertFrom-Json
$sessionId = $uploadJson.data.sessionId
Write-Host "SessionID: $sessionId" -ForegroundColor Cyan

# 4. 查询进度（轮询3次）
Write-Host "`n[4/10] 查询进度..." -ForegroundColor Yellow
for ($i = 1; $i -le 3; $i++) {
    Start-Sleep -Seconds 2
    $progress = C:\Windows\System32\curl.exe -s "http://localhost:8080/api/tasks/${sessionId}/progress"
    Write-Host "进度查询 #${i}: $progress" -ForegroundColor Green
}

# 5. 查询历史
Write-Host "`n[5/10] 查询历史记录..." -ForegroundColor Yellow
$history = C:\Windows\System32\curl.exe -s "http://localhost:8080/api/history?page=0&size=10"
Write-Host "历史记录: $history" -ForegroundColor Green

# 6-10. 异常测试
Write-Host "`n[6/10] 测试文件大小限制..." -ForegroundColor Yellow
$bigFile = New-Object byte[] (11 * 1024 * 1024)
[System.IO.File]::WriteAllBytes("$PWD\big_test.png", $bigFile)
$bigResponse = C:\Windows\System32\curl.exe -s -X POST "http://localhost:8080/api/upload" `
  -F "file=@big_test.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
Write-Host "大文件响应: $bigResponse" -ForegroundColor Yellow

Write-Host "`n[7/10] 测试空文件..." -ForegroundColor Yellow
New-Item -Path "empty.png" -ItemType File -Force | Out-Null
$emptyResponse = C:\Windows\System32\curl.exe -s -X POST "http://localhost:8080/api/upload" `
  -F "file=@empty.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
Write-Host "空文件响应: $emptyResponse" -ForegroundColor Yellow

Write-Host "`n[8/10] 测试不支持的文件类型..." -ForegroundColor Yellow
"test" | Out-File -FilePath "test.txt" -Force
$txtResponse = C:\Windows\System32\curl.exe -s -X POST "http://localhost:8080/api/upload" `
  -F "file=@test.txt" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"
Write-Host "文本文件响应: $txtResponse" -ForegroundColor Yellow

Write-Host "`n[9/10] 测试不存在的任务..." -ForegroundColor Yellow
$notFoundResponse = C:\Windows\System32\curl.exe -s "http://localhost:8080/api/tasks/non-existent-id/progress"
Write-Host "不存在的任务响应: $notFoundResponse" -ForegroundColor Yellow

Write-Host "`n[10/10] 清理测试文件..." -ForegroundColor Yellow
Remove-Item -Path "test.png", "big_test.png", "empty.png", "test.txt" -ErrorAction SilentlyContinue
Write-Host "测试文件已清理" -ForegroundColor Green

Write-Host "`n====== 测试完成 ======" -ForegroundColor Cyan
```

## 测试检查清单

- [ ] 1. 健康检查返回UP
- [ ] 2. 文件上传成功并返回sessionId
- [ ] 3. 进度查询返回正确状态
- [ ] 4. 历史记录包含上传的文件
- [ ] 5. 大文件被拒绝（>10MB）
- [ ] 6. 空文件被拒绝
- [ ] 7. 不支持的文件类型被拒绝
- [ ] 8. 查询不存在的任务返回404
- [ ] 9. 异步处理正常工作（状态从UPLOADED → PROCESSING → COMPLETED）
- [ ] 10. 错误信息清晰明确

## 问题排查

如果测试失败，检查：
1. **应用是否启动**: `netstat -an | findstr 8080`
2. **MongoDB是否运行**: 检查27017端口
3. **Redis是否运行**: 检查6379端口（可选）
4. **日志输出**: 查看控制台错误信息
5. **文件权限**: 确保上传目录可写
6. **API参数**: 检查是否所有必需参数都传递了

## 下一步

完成后端API测试后：
1. 验证所有端点功能正常
2. 检查异步任务是否正确执行
3. 测试前后端联动
4. 使用真实漫画图片测试OCR和翻译功能
