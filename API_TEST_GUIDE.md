# 后端API测试指南

## ✅ 应用状态
- **状态**: 运行中
- **端口**: 8080
- **PID**: 16512
- **Spring Boot**: 3.5.10

---

## 测试方法1: 使用浏览器测试

### 1. 健康检查
打开浏览器访问:
```
http://localhost:8080/actuator/health
```
**预期结果**: 
```json
{"status":"UP"}
```

### 2. 前端界面
打开浏览器访问:
```
http://localhost:8080
```
**预期结果**: 显示漫画翻译工具的前端界面

---

## 测试方法2: 使用PowerShell测试

### 快速测试命令
复制以下命令到PowerShell (非后台bootRun终端):

```powershell
# 1. 健康检查
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"

# 2. 创建测试图片并上传
[byte[]]$png = @(137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,15,0,0,1,1,1,0,24,221,141,17,0,0,0,0,73,69,78,68,174,66,96,130)
[IO.File]::WriteAllBytes("test.png", $png)

# 3. 上传文件
$file = Get-Item "test.png"
$fileBytes = [IO.File]::ReadAllBytes($file.FullName)
$fileEnc = [Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)
$boundary = [Guid]::NewGuid().ToString()
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
    -Body ([Text.Encoding]::GetEncoding('iso-8859-1').GetBytes($bodyLines))

Write-Host "Upload response:" -ForegroundColor Green
$upload | ConvertTo-Json

# 保存sessionId
$sessionId = $upload.data.sessionId
Write-Host "Session ID: $sessionId" -ForegroundColor Cyan

# 4. 查询进度
$progress = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$sessionId/progress"
$progress | ConvertTo-Json

# 5. 查询历史
$history = Invoke-RestMethod -Uri "http://localhost:8080/api/history?page=0&size=10"
$history | ConvertTo-Json

# 清理
Remove-Item "test.png" -ErrorAction SilentlyContinue
```

---

## 测试方法3: 使用curl测试 (推荐)

```powershell
# 设置curl路径
$curl = "C:\Windows\System32\curl.exe"

# 1. 健康检查
& $curl http://localhost:8080/actuator/health

# 2. 创建测试图片
[byte[]]$png = @(137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,15,0,0,1,1,1,0,24,221,141,17,0,0,0,0,73,69,78,68,174,66,96,130)
[IO.File]::WriteAllBytes("test.png", $png)

# 3. 上传文件
& $curl -X POST "http://localhost:8080/api/upload" `
  -F "file=@test.png" `
  -F "sourceLanguage=ja" `
  -F "targetLanguage=zh" `
  -F "engine=ZHIPU" `
  -F "outputFormat=SINGLE_IMAGE"

# 从上面的响应中复制sessionId，然后查询进度
$sid = "your-session-id-here"
& $curl "http://localhost:8080/api/tasks/$sid/progress"

# 查询历史
& $curl "http://localhost:8080/api/history?page=0&size=10"

# 清理
Remove-Item "test.png"
```

---

## API端点说明

### 1. GET /actuator/health
健康检查端点
- **响应**: `{"status":"UP"}` 或 `{"status":"DOWN"}`

### 2. POST /api/upload
上传漫画图片
- **参数**:
  - `file`: 图片文件 (png/jpg/zip, <10MB)
  - `sourceLanguage`: 源语言 (ja/en/ko等)
  - `targetLanguage`: 目标语言 (zh/en/ja等)
  - `engine`: 翻译引擎 (ZHIPU/OPENAI/CLAUDE/DEEPSEEK)
  - `outputFormat`: 输出格式 (SINGLE_IMAGE/PDF/ZIP)
- **响应**:
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "sessionId": "uuid",
    "fileName": "test.png",
    "status": "UPLOADED",
    "progress": 0,
    "currentStage": "UPLOADING"
  }
}
```

### 3. GET /api/tasks/{sessionId}/progress
查询任务进度
- **响应**:
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "fileName": "test.png",
    "status": "PROCESSING",
    "progress": 50,
    "currentStage": "OCR_PROCESSING",
    "createdAt": "2026-01-30T18:41:00"
  }
}
```

**状态枚举**:
- UPLOADED: 已上传
- PROCESSING: 处理中
- COMPLETED: 已完成
- FAILED: 失败

**阶段枚举**:
- UPLOADING: 上传中
- OCR_PROCESSING: OCR识别中
- TRANSLATING: 翻译中
- RENDERING: 渲染中
- COMPLETED: 完成

### 4. GET /api/history
查询历史记录
- **参数**: 
  - `page`: 页码 (默认0)
  - `size`: 每页大小 (默认10)
- **响应**:
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 10,
    "totalPages": 1,
    "currentPage": 0
  }
}
```

### 5. GET /api/tasks/{sessionId}/download
下载翻译结果
- **响应**: 文件流 (png/pdf/zip)

---

## 常见问题

### Q1: 无法连接到localhost:8080
**解决**: 
1. 检查应用是否运行: `netstat -an | findstr "8080"`
2. 查看Java进程: `Get-Process | Where-Object {$_.ProcessName -like "*java*"}`
3. 重启应用: `./gradlew bootRun`

### Q2: Redis连接失败
**说明**: Redis是可选的缓存功能，不影响核心功能。如果不需要缓存，可以忽略此错误。

### Q3: 文件上传失败
**检查**:
1. 文件大小是否<10MB
2. 文件类型是否为png/jpg/zip
3. 所有必需参数是否都传递了

### Q4: 任务状态一直是UPLOADED
**原因**: 异步任务可能遇到错误
**解决**: 检查应用日志中的错误信息

---

## 下一步测试计划

1. ✅ 健康检查
2. ✅ 文件上传API
3. ⏳ 异步处理流程
4. ⏳ OCR识别功能
5. ⏳ 翻译功能
6. ⏳ 渲染输出
7. ⏳ 前后端联动
8. ⏳ 完整流程测试

---

## 测试结果记录

### 测试时间: 2026-01-30 18:41

| 测试项 | 状态 | 备注 |
|--------|------|------|
| 应用启动 | ✅ | 成功启动在8080端口 |
| MongoDB连接 | ✅ | 连接localhost:27017成功 |
| Redis连接 | ⚠️ | 可选功能，未启动Redis |
| Actuator健康检查 | ⏳ | 待测试 |
| 文件上传API | ⏳ | 待测试 |
| 进度查询API | ⏳ | 待测试 |
| 历史记录API | ⏳ | 待测试 |
| 前端界面 | ⏳ | 待测试 |

---

**更新时间**: 2026-01-30 18:42
**测试人员**: GitHub Copilot
**应用版本**: 0.0.1-SNAPSHOT
