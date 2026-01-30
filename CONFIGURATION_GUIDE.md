# 配置指南

## ✅ 问题已解决！

所有编译错误已修复，项目构建成功！

---

## 📋 配置说明

### 1. 智谱清言API配置

你的配置文件已经正确设置了智谱清言：

```properties
# Translation Service Configuration
translation.default-engine=zhipu  # 默认使用智谱清言
translation.cache.ttl-days=7

# 智谱清言配置
translation.zhipu.api-key=af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit
translation.zhipu.model=glm-4.5
translation.zhipu.base-url=https://open.bigmodel.cn/api/paas/v4
```

**推荐模型选择**:
- `glm-4-flash` - 速度快，适合批量翻译（推荐）
- `glm-4` - 质量高，适合重要内容
- `glm-4.5` - 最新版本（你当前使用的）

### 2. OCR配置（内置模式）

你的配置已改为Java内置OCR模式：

```properties
# OCR Service Configuration
ocr.service.mode=embedded  # 内置模式
ocr.service.timeout=30000
ocr.cache.ttl-hours=24

# manga_ocr模型路径配置
ocr.model.path=./models/manga_ocr
ocr.model.device=cpu
# 如果有GPU，可以改为：ocr.model.device=cuda
```

**模型准备步骤**:

#### 选项A：使用Python桥接（推荐，最简单）

1. **安装Python依赖**:
```bash
pip install manga-ocr pillow
```

2. **创建OCR包装脚本** (已自动生成在项目根目录):
```bash
# 文件：ocr_wrapper.py
# 已经在项目中创建，可以直接使用
```

3. **测试OCR**:
```bash
python ocr_wrapper.py --image test.jpg --model ./models/manga_ocr
```

#### 选项B：使用外部OCR服务

如果你想用独立的OCR服务，修改配置：

```properties
# OCR Service Configuration
ocr.service.mode=external  # 改为外部模式
ocr.service.url=http://localhost:5000  # OCR服务地址
```

然后启动独立的OCR服务（参考 `OCR_SERVICE_SETUP.md`）

---

## 🚀 启动步骤

### 1. 准备环境

```bash
# 启动MongoDB
docker run -d -p 27017:27017 --name mongodb mongo:7.0

# 启动Redis
docker run -d -p 6379:6379 --name redis redis:7.2-alpine

# 安装Python OCR依赖
pip install manga-ocr pillow
```

### 2. 验证配置

检查 `application.properties` 中的关键配置：

✅ MongoDB URI: `mongodb://localhost:27017/manga_translation`
✅ Redis Host: `localhost`
✅ 智谱API Key: 已设置
✅ OCR模式: `embedded`

### 3. 启动应用

```bash
# 方式1：使用Gradle
./gradlew bootRun

# 方式2：构建JAR后运行
./gradlew build
java -jar build/libs/mangaTrans-0.0.1-SNAPSHOT.jar
```

应用将在 http://localhost:8080 启动

---

## 🧪 测试API

### 测试上传和翻译

```bash
# Windows PowerShell
$file = Get-Item "test_manga.png"
$uri = "http://localhost:8080/api/upload"

$form = @{
    file = $file
    engine = "ZHIPU"
    sourceLanguage = "ja"
    targetLanguage = "zh"
}

Invoke-RestMethod -Uri $uri -Method Post -Form $form
```

### 查询进度

```bash
curl http://localhost:8080/api/tasks/{sessionId}/progress
```

---

## ⚙️ 配置优化建议

### 1. 性能优化

如果要处理大量图片，建议调整：

```properties
# 增加线程池大小
spring.task.execution.pool.core-size=8
spring.task.execution.pool.max-size=16

# 增加缓存时间
translation.cache.ttl-days=30
ocr.cache.ttl-hours=168  # 7天
```

### 2. GPU加速（如果有NVIDIA显卡）

```properties
# 启用GPU加速OCR
ocr.model.device=cuda

# 注意：需要安装CUDA和相应的PyTorch GPU版本
# pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
```

### 3. 生产环境配置

```properties
# 使用环境变量管理API密钥
translation.zhipu.api-key=${ZHIPU_API_KEY}

# 启用日志文件
logging.file.name=logs/manga-trans.log
logging.level.root=WARN
logging.level.com.example.mangaTrans=INFO

# 增加文件上传限制（如果需要）
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

---

## 🔧 常见问题排查

### Q1: OCR不工作

**症状**: 上传图片后卡在OCR阶段

**解决方案**:
1. 确认Python和manga-ocr已安装：
   ```bash
   python --version
   pip show manga-ocr
   ```

2. 检查ocr_wrapper.py是否在项目根目录

3. 查看应用日志是否有错误信息

### Q2: 翻译API调用失败

**症状**: 任务停在"翻译中"状态

**解决方案**:
1. 验证API Key是否正确
2. 检查网络连接
3. 查看智谱API额度：https://open.bigmodel.cn/usercenter/apikeys

### Q3: MongoDB连接失败

**症状**: 应用启动报错 "Connection refused"

**解决方案**:
```bash
# 检查MongoDB是否运行
docker ps | grep mongodb

# 如果没运行，启动它
docker start mongodb

# 或者重新创建
docker run -d -p 27017:27017 --name mongodb mongo:7.0
```

### Q4: Redis连接失败

**症状**: 缓存功能不工作

**解决方案**:
```bash
# 检查Redis是否运行
docker ps | grep redis

# 启动Redis
docker start redis

# 测试连接
redis-cli ping  # 应该返回 PONG
```

---

## 📊 监控和日志

### 查看应用日志

```bash
# 如果使用gradlew bootRun
# 日志会直接输出到控制台

# 如果使用java -jar运行
java -jar build/libs/mangaTrans-0.0.1-SNAPSHOT.jar > app.log 2>&1
```

### 关键日志位置

- OCR处理日志：搜索 "OCR detection"
- 翻译调用日志：搜索 "Translating with"
- 缓存命中日志：搜索 "cache hit"
- 错误日志：搜索 "ERROR"

---

## 🎯 下一步

1. ✅ **已完成**: 项目构建成功
2. ✅ **已配置**: 智谱清言API
3. ✅ **已配置**: 内置OCR模式
4. ⏭️ **待完成**: 
   - 安装manga-ocr Python包
   - 启动MongoDB和Redis
   - 测试完整翻译流程

---

## 💡 最佳实践

### 1. API密钥管理

生产环境使用环境变量：

```bash
# Linux/Mac
export ZHIPU_API_KEY=your-key-here

# Windows PowerShell
$env:ZHIPU_API_KEY="your-key-here"
```

然后修改配置：
```properties
translation.zhipu.api-key=${ZHIPU_API_KEY}
```

### 2. 定期清理

确保定时任务正常运行（每天凌晨2点）：
- 清理24小时前的临时文件
- 删除30天前的历史记录

### 3. 备份策略

定期备份MongoDB数据：
```bash
mongodump --uri="mongodb://localhost:27017/manga_translation" --out=backup/
```

---

## 📞 获取帮助

- 查看完整文档：[README.md](README.md)
- 快速开始：[QUICKSTART.md](QUICKSTART.md)
- 架构说明：[ARCHITECTURE.md](ARCHITECTURE.md)
- OCR设置：[OCR_SERVICE_SETUP.md](OCR_SERVICE_SETUP.md)

**祝使用愉快！** 🎉
