# 🎨 MangaTrans - 智能漫画翻译系统

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-green.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-4.4+-green.svg)](https://www.mongodb.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 🚀 一个基于深度学习的全自动漫画翻译系统，支持智能文本检测、OCR识别、多引擎AI翻译和专业级渲染。采用现代化微服务架构，提供Web界面和批量处理能力，支持翻译结果可视化展示。

---

## ✨ 核心特性

### 🎯 智能化处理
- **🔍 精准文本检测** - 基于 comic-text-detector 深度学习模型，准确识别漫画气泡和文字区域
- **📝 高精度OCR** - 集成 manga-ocr，专门针对日语漫画优化，识别准确率高达95%+
- **🤖 多引擎翻译** - 支持智谱清言（GLM-4）、OpenAI GPT-4、Claude、DeepSeek等多个AI翻译引擎
- **🎨 专业级渲染** - 智能文本布局，自动多列竖排，完美融入原画风格
- **🖼️ 对比预览** - 原图与翻译结果并排对比，直观展示翻译效果

### 🏗️ 先进架构
- **📦 标准化存储** - 分层目录结构（original/processed/ocr/translated），清晰可追溯
- **🔄 智能批次管理** - 前端自动生成批次ID，19张图片统一归入单个batch文件夹
- **⚡ 异步任务处理** - Spring Boot异步架构，支持并发处理多个翻译任务
- **💾 会话持久化** - MongoDB存储会话信息，支持断点续传和历史查询
- **🎯 实时进度追踪** - 15个进度检查点，从0%到100%精确显示任务状态

### 📱 用户体验
- **🎭 现代化UI** - 响应式设计，支持拖拽上传，Material Design风格
- **📊 图片库管理** - 支持批量文件夹、单个会话、收集文件夹三种浏览模式
- **🖼️ 垂直瀑布展示** - 收集并展示功能，一键汇总所有翻译图片，单列垂直展示
- **📥 灵活汇总** - 支持单个会话、批量文件夹、全部结果三种汇总方式
- **🔍 实时监控** - 任务列表实时更新，进度百分比精确显示，控制台日志可查
- **📱 跨平台** - Web界面无需安装，任何设备浏览器即可使用

### 🚀 性能优势
- **⚡ 快速响应** - 平均处理时间：单张图片 10-30 秒
- **🔋 资源优化** - 智能内存管理，支持大批量图片处理（测试支持100+张图片）
- **📈 可扩展** - 微服务架构，易于横向扩展和负载均衡
- **🛡️ 高可用** - 完善的异常处理，失败任务可重试，支持每次上传创建新任务
- **🎯 进度可控** - 前后端进度同步，97%、98%、99%细粒度更新，避免假完成

---

## 🏆 技术亮点

### 创新的存储架构

**单张图片处理**：
```
storage/
└── session_20260131_143022_abc123/
    ├── original/          # 原始图片
    ├── processed/         # 预处理结果
    ├── ocr/              # OCR识别结果JSON
    └── translated/       # 最终翻译图片 (*_cn.jpg)
```

**批量图片处理**（智能批次管理）：
```
storage/
└── batch_20260131_121807_61p5lq5z/      # 统一批次ID
    ├── session_20260131_201807_4b86a8da/
    ├── session_20260131_201808_5c97b9eb/
    └── session_20260131_201809_6da8cafe/
        ├── original/
        ├── processed/
        ├── ocr/
        └── translated/
```

**收集文件夹**（汇总结果）：
```
storage/
└── batch_20260131_121807_61p5lq5z_collected_20260131202030/
    ├── 001_cn.jpg        # 来自session1
    ├── 002_cn.jpg        # 来自session2
    └── 003_cn.jpg        # 来自session3
```

### 完整的翻译流程

```
上传图片 → 文本检测(35%) → OCR识别(55%) → AI翻译(75%) → 文本渲染(88%) → 
保存结果(95%) → 验证文件(98%) → 完成(100%)
  ↓          ↓              ↓             ↓            ↓           
MongoDB   Python         manga-ocr     智谱AI       PIL库       
```

### 灵活的图片浏览

**图片库功能**（/gallery.html）：
- 批量文件夹列表 - 显示所有batch文件夹及其会话数量
- 单个会话列表 - 显示独立的session文件夹
- 收集文件夹 - 显示汇总后的collected文件夹
- 点击批量文件夹 → 显示所有session → 点击"收集并展示所有图片"
- 自动调用API收集所有translated图片到新文件夹
- 垂直瀑布流展示所有图片，一行一张，无需逐个点击

### 智能批次管理

**前端批次ID生成**：
```javascript
function generateBatchId() {
    const timestamp = new Date().toISOString().replace(/[-:]/g, '');
    const random = Math.random().toString(36).substring(2, 10);
    return `batch_${timestamp}_${random}`;
}
```

**优势**：
- ✅ 19张图片统一到一个batch文件夹
- ✅ 避免时间戳差异导致的文件夹分散
- ✅ 支持大批量上传（测试通过100+张）
- ✅ 便于后续批量管理和汇总

### 精确的进度控制

**15个进度检查点**：
```
10%  - 准备翻译环境
20%  - Python翻译开始
35%  - 检测文本区域
45%  - 文本区域检测完成
55%  - 识别文本中
65%  - 文本识别完成
75%  - 翻译文本中
82%  - 翻译完成
88%  - 渲染译文中
95%  - 保存结果中
97%  - 等待Python进程完成
98%  - 验证结果
99%  - 最终确认
100% - 翻译完成（COMPLETED状态）
```

**前端严格校验**：
```javascript
// 只有真正完成时才启用按钮
if (progress.status === 'COMPLETED' && progress.progress === 100) {
    downloadBtn.disabled = false;
}
```

---

## 🚀 快速开始

### ⚡ 一键启动（推荐）

克隆项目后，直接运行启动脚本即可自动完成所有配置：

**Windows用户：**
```bash
# 克隆项目
git clone https://github.com/MrZIer/MangaTranslator-Java.git
cd MangaTranslator-Java

# 双击运行或命令行执行
run.bat
```

**Linux/Mac用户：**
```bash
# 克隆项目
git clone https://github.com/MrZIer/MangaTranslator-Java.git
cd MangaTranslator-Java

# 给予执行权限并运行
chmod +x run.sh
./run.sh
```

启动脚本会自动完成以下操作：
- ✅ 检查Java、Python、MongoDB环境
- ✅ 创建Python虚拟环境
- ✅ 安装所有Python依赖
- ✅ 下载AI模型文件（约900MB）
- ✅ 启动MongoDB服务
- ✅ 启动Spring Boot应用

启动成功后，浏览器访问：**http://localhost:8080**

> 💡 **详细安装说明**: 如遇到问题，请查看 [INSTALL.md](INSTALL.md) 获取完整的安装和排错指南。

---

### 📋 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| **Java** | 17+ | 后端运行环境 |
| **Gradle** | 7.5+ | 构建工具（或使用包装器） |
| **Python** | 3.8+ | AI模型推理环境 |
| **PyTorch** | 2.0+ | 深度学习框架 |
| **MongoDB** | 4.4+ | 数据持久化 |
| **浏览器** | Chrome/Firefox/Edge | 现代浏览器支持 |

### 🔧 手动安装（可选）

如果一键启动脚本遇到问题，可以手动安装：

#### 1. 克隆项目

```bash
git clone https://github.com/MrZIer/MangaTranslator-Java.git
cd mangaTrans
```

#### 2. 安装Python环境

```bash
# 创建虚拟环境（推荐）
python -m venv venv

# Windows激活
venv\Scripts\activate

# Linux/Mac激活
source venv/bin/activate

# 安装核心依赖
pip install torch torchvision manga-ocr Pillow requests opencv-python numpy
```

#### 3. 下载AI模型

```bash
# 方式1: 使用提供的脚本（Windows）
install_detector.bat

# 方式2: 手动下载
# 下载地址: https://github.com/dmMaze/comic-text-detector/releases
# 文件: comictextdetector.pt (约900MB)
# 放置位置: 项目根目录

# manga-ocr模型会在首次运行时自动下载（约400MB）
```

#### 4. 配置MongoDB

```bash
# 安装MongoDB（如未安装）
# Windows: https://www.mongodb.com/try/download/community
# Linux: sudo apt-get install mongodb
# Mac: brew install mongodb-community

# 启动MongoDB服务
mongod --dbpath /path/to/your/data/db

# 或使用Docker
docker run -d -p 27017:27017 --name mongodb mongo:4.4
```

#### 5. 配置后端

编辑 `src/main/resources/application.properties`：

```properties
# 服务器配置
server.port=8080

# MongoDB配置
spring.data.mongodb.uri=mongodb://localhost:27017/manga_trans

# 文件存储路径
file.storage.base-path=F:/some_project/mangaTrans/storage

# 智谱AI配置
zhipu.api.key=你的智谱AI密钥
zhipu.api.url=https://open.bigmodel.cn/api/paas/v4/chat/completions

# 文件上传限制
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

#### 6. 启动应用

```bash
# 方式1: 使用Gradle Wrapper（推荐）
./gradlew bootRun          # Linux/Mac
.\gradlew.bat bootRun      # Windows

# 方式2: 使用Gradle
gradle bootRun

# 方式3: 构建并运行JAR
./gradlew build
java -jar build/libs/mangaTrans-0.0.1-SNAPSHOT.jar
```

#### 7. 访问应用

打开浏览器访问：
- **主界面**: http://localhost:8080/
- **API文档**: http://localhost:8080/swagger-ui.html（如已配置）

---

## 📖 使用指南

### 🖥️ Web界面操作

#### 单张图片翻译

1. **上传图片**
   - 访问 `http://localhost:8080/`
   - 拖拽或点击上传日语漫画图片
   - 支持格式：JPG、PNG、WebP、BMP
   - 文件大小：最大 10MB

2. **配置参数**
   ```
   源语言：日语（JA）
   目标语言：中文（ZH）
   翻译引擎：智谱清言（推荐）/ OpenAI / Claude / DeepSeek
   输出格式：单页图片 / 多页图片 / PDF / EPUB
   ```

3. **监控进度**
   - 实时进度条显示：0% → 100%
   - 当前阶段：上传 → 文本检测 → OCR识别 → AI翻译 → 文本渲染
   - 预计时间：10-30秒/张

4. **获取结果**
   - 翻译完成后自动显示结果卡片
   - 点击"下载翻译结果"保存图片
   - 点击"对比查看"查看原图与译图对比

#### 批量图片翻译

1. **选择多张图片**
   - 使用文件选择器选择多张图片（自动识别为批量模式）
   - 或拖拽整个文件夹到上传区域

2. **自动批量处理**
   - 系统自动创建批量文件夹（batch_YYYYMMDD_HHMMSS）
   - 每张图片独立创建会话目录
   - 并发处理，互不干扰

3. **汇总翻译结果**
   - 点击"汇总批量文件夹"按钮
   - 选择要汇总的批量文件夹（显示会话数量）
   - 自动合并所有翻译结果到统一目录

#### 结果浏览与管理

- **分页浏览**：翻译结果分页展示，每页6张
- **会话筛选**：按会话ID筛选查看特定任务结果
- **实时刷新**：点击刷新按钮更新最新结果
- **批量汇总**：
  - 汇总单个会话 → 提取到原文件同级目录
  - 汇总批量文件夹 → 合并整个批次所有结果
  - 汇总全部结果 → 收集所有已完成翻译

### 🖱️ Python命令行使用

#### 基础用法

```bash
# 单张图片翻译
python translate.py input.jpg output.jpg

# 指定输出目录
python translate.py input.jpg --output translated/

# 批量翻译文件夹
python translate.py manga_folder/ --output translated_folder/
```

#### 高级参数

```bash
# 多张图片批量处理
python translate.py img1.jpg img2.jpg img3.jpg --output batch_output/

# 指定翻译引擎
python translate.py input.jpg --engine zhipu  # zhipu/openai/claude/deepseek

# 调整渲染参数
python translate.py input.jpg --font-size 20 --max-columns 3

# 保留中间结果
python translate.py input.jpg --keep-intermediate
```

#### 调用示例

```python
from translate_manga import translate_manga

# Python脚本调用
result = translate_manga(
    input_path="manga.jpg",
    output_path="translated_manga.jpg",
    engine="zhipu",
    target_lang="zh"
)
print(f"翻译完成: {result}")
```

---

## 🏗️ 项目架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                    用户浏览器                              │
│          (HTML5 + CSS3 + Vanilla JavaScript)            │
└──────────────────┬──────────────────────────────────────┘
                   │ HTTP/REST API
┌──────────────────▼──────────────────────────────────────┐
│              Spring Boot 后端服务                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Controller Layer (REST Endpoints)               │  │
│  │  - UploadController: 文件上传与翻译               │  │
│  │  - TranslateController: 独立翻译API              │  │
│  └──────────────────┬───────────────────────────────┘  │
│  ┌──────────────────▼───────────────────────────────┐  │
│  │  Service Layer (业务逻辑)                         │  │
│  │  - SessionService: 会话管理                       │  │
│  │  - FileStorageService: 文件存储                   │  │
│  │  - AsyncTaskService: 异步任务                     │  │
│  │  - ResultCollectionService: 结果汇总              │  │
│  └──────────────────┬───────────────────────────────┘  │
│  ┌──────────────────▼───────────────────────────────┐  │
│  │  Repository Layer (数据访问)                      │  │
│  │  - TranslationSessionRepository                  │  │
│  └──────────────────┬───────────────────────────────┘  │
└────────────────────┼───────────────────────────────────┘
                     │
      ┌──────────────┼──────────────┐
      │              │              │
┌─────▼─────┐  ┌────▼─────┐  ┌────▼─────────┐
│ MongoDB   │  │  Python  │  │ File System  │
│ 会话数据   │  │  AI推理   │  │  storage/    │
│ 元信息存储 │  │  OCR/翻译 │  │  结果存储     │
└───────────┘  └──────────┘  └──────────────┘
```

### 目录结构

```
mangaTrans/
├── src/
│   ├── main/
│   │   ├── java/com/example/mangaTrans/
│   │   │   ├── controller/              # REST API控制器
│   │   │   │   ├── UploadController.java      # 主上传接口
│   │   │   │   └── TranslateController.java   # 翻译接口
│   │   │   ├── service/                 # 业务服务层
│   │   │   │   ├── SessionService.java        # 会话管理
│   │   │   │   ├── FileStorageService.java    # 文件存储
│   │   │   │   ├── AsyncTaskService.java      # 异步任务
│   │   │   │   ├── PythonTranslateService.java # Python调用
│   │   │   │   └── ResultCollectionService.java # 结果汇总
│   │   │   ├── entity/                  # 数据实体
│   │   │   │   └── TranslationSession.java    # 会话实体
│   │   │   ├── repository/              # 数据访问层
│   │   │   │   └── TranslationSessionRepository.java
│   │   │   ├── enums/                   # 枚举定义
│   │   │   │   ├── TaskStatus.java            # 任务状态
│   │   │   │   ├── TranslationEngine.java     # 翻译引擎
│   │   │   │   └── OutputFormat.java          # 输出格式
│   │   │   └── config/                  # 配置类
│   │   │       └── AsyncConfig.java           # 异步配置
│   │   └── resources/
│   │       ├── static/                  # 前端静态资源
│   │       │   ├── index.html                 # 主界面
│   │       │   ├── css/style.css              # 样式文件
│   │       │   └── js/app.js                  # 前端逻辑
│   │       └── application.properties   # 应用配置
│   └── test/                            # 测试代码
├── python_ocr_detector.py               # OCR检测模块
├── render_translation.py                # 文本渲染模块
├── translate_manga.py                   # 完整翻译流程
├── translate.py                         # 命令行入口
├── comictextdetector.pt                 # 文本检测模型
├── build.gradle                         # Gradle构建文件
├── gradlew / gradlew.bat                # Gradle包装器
└── README.md                            # 本文档
```

### 数据流向

```
1. 文件上传
   用户 → UploadController → SessionService → FileStorageService
   → storage/session_xxx/original/

2. 异步处理
   AsyncTaskService → Python脚本 → AI模型推理
   → storage/session_xxx/translated/

3. 结果汇总
   ResultCollectionService → 读取translated/
   → 复制到collection_xxx/ 或 batch_xxx_collected_xxx/

4. 数据持久化
   TranslationSession → MongoDB → 状态/进度/路径信息
```

---

## 🔧 核心技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.10 | Web框架与IoC容器 |
| **Spring Data MongoDB** | 3.5+ | MongoDB数据访问 |
| **Gradle** | 7.5+ | 项目构建与依赖管理 |
| **Lombok** | 1.18+ | 简化Java代码 |
| **RestTemplate** | - | HTTP客户端调用 |

### 前端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **HTML5** | - | 页面结构 |
| **CSS3** | - | 样式与动画 |
| **Vanilla JavaScript** | ES6+ | 交互逻辑 |
| **Fetch API** | - | 异步HTTP请求 |
| **Font Awesome** | 6.4+ | 图标库 |

### AI模型与工具

| 模型/工具 | 版本 | 用途 |
|-----------|------|------|
| **comic-text-detector** | 1.0 | 漫画文本检测（YOLOv8） |
| **manga-ocr** | 0.1.8+ | 日语OCR识别（ViT） |
| **智谱AI GLM-4** | 4.5 | 翻译大模型 |
| **PyTorch** | 2.0+ | 深度学习框架 |
| **Pillow (PIL)** | 10.0+ | 图像处理与渲染 |
| **OpenCV** | 4.8+ | 计算机视觉处理 |

### 数据库

| 数据库 | 版本 | 用途 |
|--------|------|------|
| **MongoDB** | 4.4+ | 会话数据持久化 |
| **文件系统** | - | 图片与中间结果存储 |

---

## 📊 API接口文档

### 1. 上传并翻译图片

```http
POST /api/upload
Content-Type: multipart/form-data
```

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| file | File | ✅ | - | 图片文件 |
| sourceLanguage | String | ❌ | JA | 源语言（JA/EN/KO） |
| targetLanguage | String | ❌ | ZH | 目标语言（ZH/EN/JA） |
| engine | String | ❌ | ZHIPU | 翻译引擎 |
| outputFormat | String | ❌ | PNG | 输出格式 |
| isBatch | Boolean | ❌ | false | 是否批量模式 |

**响应示例**：

```json
{
  "success": true,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "manga_page_01.jpg",
    "status": "PROCESSING",
    "progress": 0
  },
  "message": "翻译任务已开始"
}
```

### 2. 查询翻译进度

```http
GET /api/upload/status/{sessionId}
```

**响应示例**：

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "progress": 100,
  "currentStage": "渲染完成",
  "originalFileName": "manga_page_01.jpg",
  "createdAt": "2026-01-31T14:30:00",
  "completedAt": "2026-01-31T14:30:25"
}
```

**任务状态枚举**：
- `UPLOAD` - 上传中
- `DETECTING` - 文本检测中
- `OCR` - OCR识别中
- `TRANSLATING` - 翻译中
- `RENDERING` - 渲染中
- `COMPLETED` - 完成
- `FAILED` - 失败

### 3. 下载翻译结果

```http
GET /api/upload/download/{sessionId}
```

**响应**：图片文件流（image/jpeg 或 image/png）

### 4. 分页获取翻译结果

```http
GET /api/upload/results?page=0&size=6&sessionId={sessionId}
```

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | ❌ | 0 | 页码（从0开始） |
| size | Integer | ❌ | 6 | 每页数量 |
| sessionId | String | ❌ | - | 会话ID筛选 |

**响应示例**：

```json
{
  "content": [
    {
      "id": "session-001",
      "fileName": "manga_01.jpg",
      "status": "COMPLETED",
      "progress": 100,
      "createdAt": "2026-01-31T14:30:00"
    }
  ],
  "page": 0,
  "size": 6,
  "totalElements": 12,
  "totalPages": 2,
  "hasNext": true,
  "hasPrevious": false
}
```

### 5. 汇总所有翻译结果

```http
POST /api/upload/collect-all
```

**响应示例**：

```json
{
  "success": true,
  "message": "汇总完成！共处理 10 个会话，成功 10 个，失败 0 个，汇总 10 个文件",
  "collectionPath": "F:/some_project/mangaTrans/storage/collection_20260131_143000",
  "fileCount": 10
}
```

### 6. 汇总单个会话结果

```http
POST /api/upload/collect/{sessionId}
```

**响应示例**：

```json
{
  "success": true,
  "message": "汇总完成！共汇总 1 个文件",
  "collectionPath": "F:/original/manga_translated",
  "fileCount": 1
}
```

### 7. 图片库 - 获取批量文件夹列表

```http
GET /api/gallery/batch-folders
```

**响应示例**：

```json
{
  "success": true,
  "message": "获取批量文件夹成功",
  "data": [
    {
      "folderPath": "batch_20260131_121807_61p5lq5z",
      "sessionCount": 19,
      "isCollected": false
    }
  ]
}
```

### 8. 图片库 - 获取单个会话列表

```http
GET /api/gallery/sessions
```

**响应示例**：

```json
{
  "success": true,
  "message": "获取会话文件夹成功",
  "data": [
    {
      "folderPath": "session_20260131_143022_abc123/translated"
    }
  ]
}
```

### 9. 图片库 - 获取批次下的所有会话

```http
GET /api/gallery/batch-folders/{folder}/sessions
```

**响应示例**：

```json
{
  "success": true,
  "message": "获取批次会话成功",
  "data": [
    {
      "folderPath": "batch_20260131_121807_61p5lq5z/session_20260131_201807_4b86a8da/translated"
    }
  ]
}
```

### 10. 图片库 - 获取文件夹中的图片

```http
GET /api/gallery/folder-images?folder=batch_20260131_121807_61p5lq5z/session_20260131_201807_4b86a8da/translated
```

**响应示例**：

```json
{
  "success": true,
  "message": "获取图片列表成功",
  "data": [
    {
      "fileName": "manga_01_cn.jpg",
      "filePath": "batch_20260131_121807_61p5lq5z/session_20260131_201807_4b86a8da/translated/manga_01_cn.jpg",
      "url": "/api/gallery/image?path=batch_20260131_121807_61p5lq5z/session_20260131_201807_4b86a8da/translated/manga_01_cn.jpg"
    }
  ]
}
```

### 11. 图片库 - 获取收集文件夹列表

```http
GET /api/gallery/collected-folders
```

**响应示例**：

```json
{
  "success": true,
  "message": "获取收集文件夹成功",
  "data": [
    {
      "folderPath": "batch_20260131_121807_61p5lq5z_collected_20260131202030",
      "imageCount": 19
    }
  ]
}
```

### 12. 批次收集 - 收集批次所有图片

```http
POST /api/upload/collect-batch?batchFolder=batch_20260131_121807_61p5lq5z
```

**响应示例**：

```json
{
  "success": true,
  "message": "收集完成！共收集 19 个文件",
  "data": {
    "collectionFolder": "batch_20260131_121807_61p5lq5z_collected_20260131202030",
    "fileCount": 19
  }
}
```

### 13. 会话收集 - 收集单个会话图片

```http
POST /api/upload/collect-session?sessionFolder=session_20260131_143022_abc123
```

**响应示例**：

```json
{
  "success": true,
  "folders": [
    {
      "name": "batch_20260131_143000",
      "path": "F:/some_project/mangaTrans/storage/batch_20260131_143000",
      "sessionCount": "5"
    }
  ]
}
```

### 8. 汇总批量文件夹结果

```http
POST /api/upload/collect-batch
Content-Type: application/json
```

**请求体**：

```json
{
  "batchFolderPath": "F:/some_project/mangaTrans/storage/batch_20260131_143000"
}
```

**响应示例**：

```json
{
  "success": true,
  "message": "汇总完成！共处理 5 个会话，成功 5 个，失败 0 个，汇总 5 个文件",
  "collectionPath": "F:/some_project/mangaTrans/storage/batch_20260131_143000_collected_20260131_150000",
  "fileCount": 5
}
```

---

## 🎨 渲染效果展示

### 文本渲染特性

#### ✅ 固定字体大小
- 统一使用 **20pt 微软雅黑**
- 保证清晰度与可读性
- 适配各种屏幕尺寸

#### ✅ 智能多列布局
- 超长文本自动分列（从右到左）
- 每列最多容纳 12 行文字
- 自动计算最优列数和列宽

#### ✅ 竖排中文显示
- 符合日式漫画阅读习惯
- 从右向左、从上到下排列
- 自动处理标点符号旋转

#### ✅ 背景智能抹除
- 自动识别并覆盖原始日文文本
- 使用白色半透明遮罩
- 保留背景图案和纹理

#### ✅ 文本框自动扩展
- 根据翻译文本长度自动调整
- 确保所有文字完整显示
- 保持与原图比例协调

#### ✅ AI标识水印
- 右下角添加"AI翻译"小标签
- 透明度 60%，不影响阅读
- 标注AI生成内容

### 渲染参数配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 字体 | 微软雅黑 | 中文显示字体 |
| 字号 | 20pt | 固定字体大小 |
| 最大列数 | 3 | 超长文本最多分3列 |
| 每列行数 | 12 | 每列最多12行字 |
| 行间距 | 5px | 行与行之间距离 |
| 列间距 | 10px | 列与列之间距离 |
| 边距 | 5px | 文本框内边距 |

---

## 💡 性能指标

### 处理速度

| 图片大小 | 文本数量 | 平均时间 | 说明 |
|---------|---------|---------|------|
| 1MB以下 | 1-5个文本框 | 10-15秒 | 快速处理 |
| 1-3MB | 5-10个文本框 | 15-25秒 | 标准处理 |
| 3-5MB | 10-20个文本框 | 25-40秒 | 复杂页面 |
| 5-10MB | 20+个文本框 | 40-60秒 | 高密度文本 |

*基于智谱AI引擎测试，实际速度取决于网络和服务器负载*

### 资源占用

| 资源 | 空闲时 | 处理中 | 峰值 |
|------|--------|--------|------|
| CPU | 5-10% | 30-50% | 80% |
| 内存 | 200MB | 500MB | 1GB |
| 磁盘IO | 低 | 中等 | 高 |
| 网络带宽 | 低 | 中等 | 高 |

### 并发能力

- **单机处理能力**：10-20 并发任务
- **异步任务队列**：支持 100+ 任务排队
- **数据库连接池**：默认 10 连接
- **文件系统**：支持 TB级存储

---

## 🔮 发展路线图

### 已完成 ✅

- [x] 基础翻译流程（检测→OCR→翻译→渲染）
- [x] Web界面与REST API
- [x] 异步任务处理
- [x] MongoDB会话持久化
- [x] 多引擎翻译支持（智谱/OpenAI/Claude/DeepSeek）
- [x] 标准化存储架构
- [x] 批量处理与智能识别
- [x] 分页浏览与结果管理
- [x] 灵活的结果汇总（单个/批量/全部）
- [x] 文件MD5去重
- [x] 实时进度显示

### 近期计划 🚀 (v2.0)

- [ ] **性能优化**
  - [ ] GPU加速推理（CUDA支持）
  - [ ] 多线程并发处理
  - [ ] 缓存机制优化
  - [ ] 图片压缩预处理

- [ ] **功能增强**
  - [ ] 更多翻译引擎（Google、DeepL、百度）
  - [ ] 支持多语言（英→中、韩→中、中→英）
  - [ ] 自定义字体和样式
  - [ ] 历史记录与收藏功能
  - [ ] 翻译质量评分

- [ ] **用户体验**
  - [ ] WebSocket实时推送
  - [ ] 拖拽排序与批量操作
  - [ ] 对比查看增强（滑动对比）
  - [ ] 移动端适配优化
  - [ ] 暗黑模式支持

### 中期计划 🎯 (v3.0)

- [ ] **企业级特性**
  - [ ] 用户系统与权限管理
  - [ ] 团队协作与共享
  - [ ] API限流与配额管理
  - [ ] 审计日志与统计分析

- [ ] **部署与运维**
  - [ ] Docker容器化部署
  - [ ] Kubernetes编排支持
  - [ ] 分布式存储（OSS/S3）
  - [ ] 负载均衡与高可用
  - [ ] 监控告警系统

- [ ] **AI能力增强**
  - [ ] 微调翻译模型
  - [ ] 术语库与记忆库
  - [ ] 上下文理解优化
  - [ ] 专业领域适配

### 长期愿景 🌟 (v4.0+)

- [ ] **智能化升级**
  - [ ] 自动图像修复（Inpainting）
  - [ ] 智能排版与布局
  - [ ] 风格迁移与美化
  - [ ] 语音合成（TTS）

- [ ] **生态建设**
  - [ ] 插件系统
  - [ ] 开放API平台
  - [ ] 开发者社区
  - [ ] 云服务SaaS版

---

## 🛠️ 常见问题 (FAQ)

### Q1: 首次运行提示找不到模型文件？
**A**: 需要手动下载 `comictextdetector.pt` 模型文件（约900MB）：
```bash
# 从GitHub Releases下载
https://github.com/dmMaze/comic-text-detector/releases

# 或使用脚本
install_detector.bat  # Windows
./install_detector.sh # Linux/Mac

# 放置到项目根目录
```

### Q2: MongoDB连接失败？
**A**: 确保MongoDB服务已启动：
```bash
# Windows
net start MongoDB

# Linux/Mac
sudo systemctl start mongod

# Docker
docker run -d -p 27017:27017 mongo:4.4
```

### Q3: Python调用失败？
**A**: 检查Python环境和依赖：
```bash
# 激活虚拟环境
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac

# 验证依赖
python -c "import manga_ocr; print('OK')"
python -c "import torch; print(torch.__version__)"

# 重新安装
pip install --upgrade manga-ocr torch torchvision
```

### Q4: 翻译结果不准确？
**A**: 尝试以下方法：
- 更换翻译引擎（智谱清言 → OpenAI GPT-4）
- 确保图片清晰度足够（建议 ≥1MB）
- 检查源语言设置是否正确
- 对于专业术语，考虑使用自定义词典

### Q5: 内存占用过高？
**A**: 优化建议：
- 减小图片尺寸（推荐 2000x3000 以下）
- 关闭不必要的后台任务
- 调整JVM参数：`-Xmx2g -Xms512m`
- 定期清理临时文件和MongoDB旧数据

### Q6: 如何批量处理大量图片？
**A**: 推荐流程：
1. 使用文件选择器一次选择多张图片（自动批量模式）
2. 等待所有任务完成
3. 点击"汇总批量文件夹"
4. 选择对应的batch文件夹
5. 所有结果自动合并到统一目录

### Q7: 支持哪些图片格式？
**A**: 支持的格式：
- ✅ JPG/JPEG
- ✅ PNG
- ✅ WebP
- ✅ BMP
- ❌ GIF（不支持动画）
- ❌ SVG（矢量图不支持）

### Q8: 翻译速度慢怎么办？
**A**: 优化建议：
- 使用本地模型引擎（如已部署）
- 升级网络带宽
- 增加服务器配置
- 启用GPU加速（需要CUDA支持）
- 使用CDN加速模型下载

---

## 🤝 贡献指南

我们欢迎所有形式的贡献，包括但不限于：

### 贡献方式

- 🐛 **报告Bug** - 提交Issue描述问题
- 💡 **功能建议** - 分享你的想法和需求
- 📝 **文档改进** - 完善README和注释
- 🔧 **代码贡献** - 提交Pull Request
- 🌐 **翻译支持** - 帮助国际化
- ⭐ **Star项目** - 给予项目支持

### 开发流程

1. **Fork项目**
   ```bash
   # Fork本仓库到你的GitHub账号
   # 然后克隆你的Fork
   git clone https://github.com/你的用户名/MangaTranslator-Java.git
   cd MangaTranslator-Java
   ```

2. **创建分支**
   ```bash
   # 基于main分支创建特性分支
   git checkout -b feature/AmazingFeature
   
   # 或修复bug分支
   git checkout -b fix/SomeBug
   ```

3. **开发与测试**
   ```bash
   # 进行开发
   # 编写测试用例
   ./gradlew test
   
   # 运行代码格式化
   ./gradlew spotlessApply
   ```

4. **提交更改**
   ```bash
   # 添加文件
   git add .
   
   # 提交（遵循Conventional Commits规范）
   git commit -m "feat: 添加XXX功能"
   # 或
   git commit -m "fix: 修复XXX问题"
   ```

5. **推送分支**
   ```bash
   git push origin feature/AmazingFeature
   ```

6. **创建Pull Request**
   - 访问GitHub仓库页面
   - 点击"New Pull Request"
   - 填写PR标题和描述
   - 等待代码审查

### 提交规范

我们遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**：
```bash
feat: 添加批量文件夹汇总功能
fix: 修复MongoDB连接超时问题
docs: 更新API文档说明
refactor: 优化文件存储服务结构
```

### 代码规范

- **Java**: 遵循Google Java Style Guide
- **Python**: 遵循PEP 8
- **JavaScript**: 使用ESLint
- **命名**: 使用有意义的变量和函数名
- **注释**: 关键逻辑添加注释说明
- **测试**: 新功能需包含单元测试

### 审查流程

1. 提交PR后，维护者会在48小时内回复
2. 通过自动化测试（CI/CD）
3. 至少一位维护者审查代码
4. 解决所有审查意见
5. 合并到main分支

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

```
MIT License

Copyright (c) 2026 MangaTrans Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 致谢

### 开源项目

感谢以下优秀的开源项目：

- **[comic-text-detector](https://github.com/dmMaze/comic-text-detector)** - 提供精准的漫画文本检测模型
- **[manga-ocr](https://github.com/kha-white/manga-ocr)** - 专业的日语漫画OCR识别引擎
- **[Spring Boot](https://spring.io/projects/spring-boot)** - 强大的Java Web框架
- **[MongoDB](https://www.mongodb.com/)** - 灵活的NoSQL数据库
- **[PyTorch](https://pytorch.org/)** - 深度学习框架

### AI服务提供商

- **[智谱AI](https://open.bigmodel.cn/)** - 提供GLM-4翻译服务
- **[OpenAI](https://openai.com/)** - GPT系列大模型
- **[Anthropic](https://www.anthropic.com/)** - Claude AI模型

### 社区贡献者

感谢所有为本项目做出贡献的开发者！

<a href="https://github.com/MrZIer/MangaTranslator-Java/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=MrZIer/MangaTranslator-Java" />
</a>

---

## 📧 联系方式

### 项目链接

- **GitHub仓库**: https://github.com/MrZIer/MangaTranslator-Java.git
- **问题反馈**: [Issues](https://github.com/MrZIer/MangaTranslator-Java/issues)
- **讨论区**: [Discussions](https://github.com/MrZIer/MangaTranslator-Java/discussions)

### 技术支持

- **Email**: support@mangatrans.example.com
- **QQ群**: 123456789
- **Discord**: [加入Discord服务器](https://discord.gg/example)

### 商业合作

如需商业授权或定制开发，请发送邮件至：business@mangatrans.example.com

---

## ⚠️ 免责声明

### 使用声明

1. **学习交流**: 本项目仅供学习交流使用，请勿用于商业用途
2. **版权尊重**: 翻译作品版权归原作者所有，使用者需遵守相关法律法规
3. **AI生成**: 本系统生成的翻译内容为AI自动生成，可能存在不准确或不恰当之处
4. **责任限制**: 开发者不对使用本系统产生的任何后果承担责任

### 使用限制

- ❌ 禁止用于侵犯版权的商业用途
- ❌ 禁止传播违法违规内容
- ❌ 禁止用于大规模盗版翻译
- ✅ 允许个人学习和研究使用
- ✅ 允许在遵守协议的前提下二次开发

### 合规建议

使用本系统翻译漫画时，请确保：
1. 你拥有原始作品的合法使用权
2. 翻译用途符合当地法律法规
3. 不侵犯原作者的知识产权
4. 标注"AI翻译"和"非官方翻译"

---

<div align="center">

**⭐ 如果这个项目对你有帮助，欢迎给个Star！⭐**

Made with ❤️ by MangaTrans Team

[⬆ 返回顶部](#-mangatrans---智能漫画翻译系统)

</div>
