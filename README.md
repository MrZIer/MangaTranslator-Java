# 漫画翻译工具 (Manga Translation Tool)

基于Spring Boot 3.5.10开发的智能漫画翻译系统，使用 **manga-ocr** 进行日文OCR识别，支持多个AI翻译引擎。

## 🎯 项目特点

- ✅ **无需登录** - 通过浏览器指纹识别用户
- 🚀 **异步处理** - 支持大规模并发任务
- 🤖 **manga-ocr** - 专业日文漫画OCR识别 ([kha-white/manga-ocr](https://github.com/kha-white/manga-ocr))
- 🧠 **多引擎翻译** - 支持智谱AI、OpenAI GPT-4o、Claude 3.5、DeepSeek
- 🎨 **智能渲染** - 自动字体选择和文本排版
- 🌐 **可视化界面** - 拖拽上传、实时进度显示
- 📦 **智能去重** - 基于MD5哈希的文件去重
- 🔄 **实时进度** - WebSocket风格的进度查询
- 💾 **历史管理** - 30天自动保留历史记录

## 🏗️ 技术栈

- **框架**: Spring Boot 3.5.10
- **数据库**: MongoDB (会话和历史记录存储)
- **缓存**: Redis (OCR和翻译结果缓存)
- **图像处理**: Java 2D + imgscalr
- **异步处理**: Spring @Async
- **HTTP客户端**: WebClient (调用外部API)
- **构建工具**: Gradle

## 📋 前置要求

1. **Java 17+**
2. **MongoDB 4.4+** (本地或远程)
3. **Redis 6.0+**
4. **manga_ocr服务** (Python微服务，需单独部署)
5. **API密钥**:
   - OpenAI API Key (可选)
   - Claude API Key (可选)
   - DeepSeek API Key (可选)

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd mangaTrans
```

### 2. 配置环境变量

编辑 `src/main/resources/application.properties`:

```properties
# MongoDB配置
spring.data.mongodb.uri=mongodb://localhost:27017/manga_translation

# Redis配置
spring.data.redis.host=localhost
spring.data.redis.port=6379

# OCR服务地址
ocr.service.url=http://localhost:5000

# 翻译API密钥
translation.openai.api-key=your-openai-key
translation.claude.api-key=your-claude-key
translation.deepseek.api-key=your-deepseek-key
```

或者使用环境变量：

```bash
export OPENAI_API_KEY=your-openai-key
export CLAUDE_API_KEY=your-claude-key
export DEEPSEEK_API_KEY=your-deepseek-key
```

### 3. 启动MongoDB和Redis

```bash
# MongoDB
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Redis
docker run -d -p 6379:6379 --name redis redis:latest
```

### 4. 部署manga_ocr服务

需要单独部署Python OCR服务。参考OCR服务文档。

### 5. 构建并运行

```bash
# 使用Gradle构建
./gradlew build

# 运行应用
./gradlew bootRun
```

应用将在 `http://localhost:8080` 启动。

## 📡 API接口文档

### 1. 上传文件并开始翻译

**POST** `/api/upload`

**Content-Type**: `multipart/form-data`

**请求参数**:
- `file` (文件): PNG/JPG/ZIP格式，最大10MB
- `engine` (字符串): 翻译引擎 (OPENAI/CLAUDE/DEEPSEEK)
- `sourceLanguage` (字符串): 源语言 (默认: ja)
- `targetLanguage` (字符串): 目标语言 (默认: zh)
- `outputFormat` (字符串): 输出格式 (SINGLE_IMAGE/ZIP/PDF)

**响应示例**:
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "sessionId": "uuid-here",
    "fileName": "manga.png",
    "status": "UPLOAD",
    "progress": 0,
    "currentStage": null,
    "errorMessage": null
  }
}
```

### 2. 查询任务进度

**GET** `/api/tasks/{sessionId}/progress`

**响应示例**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "sessionId": "uuid-here",
    "fileName": "manga.png",
    "status": "TRANSLATE",
    "progress": 50,
    "currentStage": "翻译进度: 5/10",
    "errorMessage": null
  }
}
```

### 3. 获取任务详情

**GET** `/api/tasks/{sessionId}`

返回完整的会话信息，包括文本区域、翻译结果等。

### 4. 获取历史记录

**GET** `/api/history?page=0&size=10`

**响应示例**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "history-id",
        "fileName": "manga.png",
        "fileSize": 1024000,
        "engine": "OpenAI GPT-4o",
        "sourceLanguage": "ja",
        "targetLanguage": "zh",
        "accessCount": 3,
        "createdAt": "2026-01-30T10:00:00",
        "expiresAt": "2026-02-29T10:00:00"
      }
    ],
    "totalPages": 5,
    "totalElements": 50,
    "number": 0,
    "size": 10
  }
}
```

### 5. 下载结果文件

**GET** `/api/history/{historyId}/download`

返回翻译后的图片文件。

### 6. 延长历史保留期

**POST** `/api/history/{historyId}/extend?days=30`

延长历史记录的保留期限。

## 🔄 处理流程

1. **文件上传** → 计算MD5哈希 → 检查去重 → 创建会话
2. **图像预处理** → 标准化尺寸 → 转换格式
3. **OCR识别** → 调用manga_ocr服务 → 提取文本区域
4. **翻译** → 调用AI API → 更新术语表
5. **渲染** → 根据语言选择字体 → 绘制译文
6. **打包** → 添加水印 → 保存结果
7. **存储** → 保存历史记录 → 设置过期时间

## 📁 项目结构

```
src/main/java/com/example/mangaTrans/
├── config/              # 配置类
│   ├── AsyncConfig.java
│   ├── RedisConfig.java
│   ├── WebClientConfig.java
│   └── WebConfig.java
├── controller/          # 控制器
│   ├── UploadController.java
│   ├── TaskController.java
│   └── HistoryController.java
├── dto/                 # 数据传输对象
│   ├── UploadRequest.java
│   ├── SessionResponse.java
│   ├── HistoryResponse.java
│   └── ApiResponse.java
├── entity/              # 实体类
│   ├── TranslationSession.java
│   └── TranslationHistory.java
├── enums/               # 枚举
│   ├── TaskStatus.java
│   ├── TranslationEngine.java
│   └── OutputFormat.java
├── exception/           # 异常处理
│   └── GlobalExceptionHandler.java
├── model/               # 数据模型
│   ├── TextRegion.java
│   └── BoundingBox.java
├── repository/          # 数据访问层
│   ├── TranslationSessionRepository.java
│   └── TranslationHistoryRepository.java
├── scheduler/           # 定时任务
│   └── CleanupScheduler.java
├── service/             # 业务逻辑层
│   ├── AsyncTaskService.java
│   ├── FileStorageService.java
│   ├── FingerprintService.java
│   ├── HistoryService.java
│   ├── ImageProcessingService.java
│   ├── OcrService.java
│   ├── SessionService.java
│   └── TranslationService.java
└── MangaTransApplication.java
```

## 🔧 配置说明

### 文件存储

默认存储路径: `./storage/{sessionId}/`

目录结构:
- `originals/` - 原始上传文件
- `processed/` - 预处理后的文件
- `results/` - 最终翻译结果

### 缓存策略

- **OCR结果**: Redis缓存24小时
- **翻译结果**: Redis缓存7天
- **临时文件**: 24小时后自动清理
- **历史记录**: 30天后自动删除

### 定时任务

每天凌晨2点自动执行清理任务，删除：
- 超过24小时的临时会话
- 已过期的历史记录及其文件

## 🎨 自定义配置

### 修改翻译引擎

在 `application.properties` 中:

```properties
translation.default-engine=openai  # 可选: openai, claude, deepseek
```

### 调整图像处理参数

```properties
image.max-width=2000
image.max-height=2000
image.output-format=png
```

### 启用/禁用水印

```properties
watermark.enabled=true
watermark.text=AI Translated
watermark.opacity=0.3
```

## 🧪 测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew test --tests "com.example.mangaTrans.service.SessionServiceTest"
```

## 📊 监控和日志

应用使用SLF4J+Logback记录日志。

日志级别配置:
```properties
logging.level.root=INFO
logging.level.com.example.mangaTrans=DEBUG
```

## 🛠️ 故障排查

### 问题1: MongoDB连接失败
- 检查MongoDB是否正在运行
- 验证连接字符串是否正确

### 问题2: Redis连接失败
- 检查Redis是否正在运行
- 验证主机和端口配置

### 问题3: OCR服务调用失败
- 确认manga_ocr服务已启动
- 检查OCR服务URL配置
- 查看OCR服务日志

### 问题4: 翻译API调用失败
- 验证API密钥是否正确
- 检查API额度是否用尽
- 确认网络连接正常

## 📝 待实现功能

- [ ] PDF输出支持
- [ ] ZIP批量处理
- [ ] WebSocket实时推送
- [ ] 多语言界面
- [ ] 术语库管理界面
- [ ] 翻译质量评分
- [ ] 用户反馈机制

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

## 👥 联系方式

如有问题，请通过GitHub Issues联系。

---

**注意**: 本项目仅供学习和研究使用，请遵守相关API服务商的使用条款。
