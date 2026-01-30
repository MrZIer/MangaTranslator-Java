# 快速开始指南

## 一、环境准备

### 1. 安装必要软件

- Java 17 或更高版本
- MongoDB 4.4+
- Redis 6.0+
- (可选) Docker 和 Docker Compose

### 2. 使用Docker Compose快速启动（推荐）

```bash
# 1. 创建.env文件，配置API密钥
cat > .env << EOF
OPENAI_API_KEY=your-openai-key-here
CLAUDE_API_KEY=your-claude-key-here
DEEPSEEK_API_KEY=your-deepseek-key-here
EOF

# 2. 启动所有服务（MongoDB、Redis、OCR服务）
docker-compose up -d mongodb redis

# 3. 等待服务启动（约10秒）
sleep 10
```

### 3. 手动启动（不使用Docker）

```bash
# 启动MongoDB
docker run -d -p 27017:27017 --name mongodb mongo:7.0

# 启动Redis
docker run -d -p 6379:6379 --name redis redis:7.2-alpine

# 设置环境变量
export OPENAI_API_KEY=your-key
export CLAUDE_API_KEY=your-key
export DEEPSEEK_API_KEY=your-key
```

## 二、启动应用

### 方法1: 使用Gradle

```bash
# Windows
.\gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

### 方法2: 构建JAR后运行

```bash
# 构建
./gradlew build

# 运行
java -jar build/libs/mangaTrans-0.0.1-SNAPSHOT.jar
```

应用将在 http://localhost:8080 启动。

## 三、测试API

### 1. 上传图片并开始翻译

```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@/path/to/manga.png" \
  -F "engine=OPENAI" \
  -F "sourceLanguage=ja" \
  -F "targetLanguage=zh"
```

响应示例:
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "sessionId": "abc-123-def",
    "fileName": "manga.png",
    "status": "UPLOAD",
    "progress": 0
  }
}
```

### 2. 查询处理进度

```bash
curl http://localhost:8080/api/tasks/abc-123-def/progress
```

### 3. 获取历史记录

```bash
curl http://localhost:8080/api/history?page=0&size=10
```

## 四、使用Postman测试

1. 导入以下请求集合：

**上传文件**:
- Method: POST
- URL: http://localhost:8080/api/upload
- Body → form-data:
  - file: 选择图片文件
  - engine: OPENAI
  - targetLanguage: zh

**查询进度**:
- Method: GET
- URL: http://localhost:8080/api/tasks/{sessionId}/progress

## 五、常见问题

### Q1: 应用启动失败

**检查清单**:
1. MongoDB和Redis是否正在运行？
   ```bash
   docker ps  # 查看运行中的容器
   ```

2. 端口是否被占用？
   ```bash
   netstat -an | grep 8080
   netstat -an | grep 27017
   netstat -an | grep 6379
   ```

3. 查看应用日志：
   ```bash
   ./gradlew bootRun --info
   ```

### Q2: OCR服务未启动

OCR服务需要单独部署，参考 `OCR_SERVICE_SETUP.md`。

如果暂时没有OCR服务，可以：
1. 注释掉OCR相关代码
2. 或者使用Mock数据进行测试

### Q3: 翻译API调用失败

确认：
1. API密钥是否正确设置
2. 网络连接是否正常
3. API配额是否充足

查看详细错误日志：
```bash
tail -f logs/spring.log
```

### Q4: 文件上传失败

检查：
1. 文件大小是否超过10MB
2. 文件格式是否为PNG/JPG/ZIP
3. 查看控制台错误信息

## 六、开发建议

### 1. 启用热重载

添加Spring DevTools依赖（已包含在build.gradle中），修改代码后自动重启。

### 2. 使用IDE调试

在IDE中直接运行 `MangaTransApplication.java` 的main方法。

### 3. 查看数据库

**MongoDB**:
```bash
# 连接MongoDB
mongosh mongodb://localhost:27017

# 切换数据库
use manga_translation

# 查看会话
db.translation_sessions.find().pretty()

# 查看历史记录
db.translation_history.find().pretty()
```

**Redis**:
```bash
# 连接Redis
redis-cli

# 查看所有键
keys *

# 查看特定缓存
get "ocr:your-hash"
```

## 七、生产部署

### 使用Docker部署

```bash
# 1. 构建镜像
docker build -t manga-trans:latest .

# 2. 启动完整服务栈
docker-compose up -d

# 3. 查看日志
docker-compose logs -f manga-trans-app
```

### 配置建议

生产环境建议修改：
- 增加线程池大小
- 调整缓存过期时间
- 启用HTTPS
- 配置反向代理（Nginx）
- 设置合理的日志级别

## 八、下一步

1. ✅ 部署OCR服务（参考 OCR_SERVICE_SETUP.md）
2. ✅ 配置翻译API密钥
3. ✅ 测试完整流程
4. ✅ 根据需求调整配置
5. ✅ 开发前端界面（可选）

## 九、获取帮助

- 查看完整文档: [README.md](README.md)
- OCR服务设置: [OCR_SERVICE_SETUP.md](OCR_SERVICE_SETUP.md)
- 提交Issue: GitHub Issues

---

**祝你使用愉快！** 🎉
