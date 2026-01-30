# 🎨 MangaTrans - AI漫画翻译系统

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个基于深度学习的日语漫画自动翻译系统，支持文本检测、OCR识别、AI翻译和高质量渲染。

## ✨ 主要特性

- 🔍 **智能文本检测** - 基于 [comic-text-detector](https://github.com/zyddnys/manga-image-translator/releases/tag/beta-0.3) 精准识别漫画文字区域

- 📝 **OCR识别** - 使用 [manga-ocr](https://github.com/kha-white/manga-ocr) 高精度日语识别
- 🌐 **AI翻译** - 集成智谱AI GLM-4.5，提供专业日译中翻译
- 🎨 **高质量渲染** - 20pt固定字体，自动多列竖排布局，完美适配原图
- ⚡ **异步处理** - 后端异步任务处理，支持批量翻译
- 📱 **现代化UI** - 响应式Web界面，拖拽上传，实时进度显示

## 🚀 快速开始

### 环境要求

- **后端**: Java 17+, Gradle 7.5+
- **前端**: 现代浏览器（Chrome/Firefox/Edge）
- **Python**: Python 3.8+, PyTorch 2.0+
- **数据库**: MongoDB 4.4+

### 安装步骤

#### 1. 克隆仓库

```bash
git clone https://github.com/你的用户名/mangaTrans.git
cd mangaTrans
```

#### 2. 安装Python依赖

```bash
# 创建虚拟环境（推荐）
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install torch torchvision manga-ocr Pillow requests opencv-python
```

#### 3. 下载模型文件

```bash
# 下载 comic-text-detector 模型（约900MB）
# 从 https://github.com/dmMaze/comic-text-detector/releases 下载
# 或使用命令：
curl -L -o comictextdetector.pt https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt

# manga-ocr 模型会在首次运行时自动下载
```

#### 4. 配置后端

```bash
# 编辑 src/main/resources/application.properties
# 配置 MongoDB 连接和智谱AI API密钥
spring.data.mongodb.uri=mongodb://localhost:27017/manga_trans
zhipu.api.key=你的智谱AI密钥
```

#### 5. 启动服务

```bash
# 启动MongoDB（如果未运行）
mongod --dbpath /path/to/db

# 启动Spring Boot后端
./gradlew bootRun

# 访问前端
# 浏览器打开: http://localhost:8080/manga.html
```

## 📖 使用指南

### Web界面使用

1. **上传图片**
   - 访问 `http://localhost:8080/manga.html`
   - 拖拽或点击上传日语漫画图片（支持 JPG/PNG，最大10MB）

2. **配置选项**
   - 源语言：日语（默认）
   - 目标语言：中文（默认）
   - 翻译引擎：智谱AI

3. **开始翻译**
   - 点击"开始翻译"按钮
   - 实时查看进度：上传 → 检测 → OCR → 翻译 → 渲染
   - 完成后点击"下载翻译结果"

### Python命令行使用

```bash
# 单张图片翻译
python translate.py "input.jpg" "output.jpg"

# 批量翻译（文件夹）
python translate.py "manga_folder/" "translated_folder/"

# 多张图片翻译
python translate.py "001.jpg" "002.jpg" "003.jpg" --output "translated/"
```

## 🏗️ 项目架构

```
mangaTrans/
├── src/main/java/              # Java后端代码
│   ├── controller/             # REST API控制器
│   ├── service/                # 业务逻辑层
│   ├── model/                  # 数据模型
│   └── config/                 # 配置类
├── src/main/resources/
│   ├── static/                 # 前端静态资源
│   │   ├── manga.html          # 主界面
│   │   ├── css/                # 样式文件
│   │   └── js/                 # JavaScript
│   └── application.properties  # 配置文件
├── python_ocr_detector.py      # OCR检测模块
├── render_translation.py       # 文本渲染模块
├── translate_manga.py          # 完整翻译流程
├── translate.py                # 命令行入口
└── comictextdetector.pt        # 文本检测模型（需下载）
```

## 🔧 核心技术

### 后端技术栈
- **Spring Boot 3.2** - Web框架
- **MongoDB** - 数据存储
- **Gradle** - 构建工具
- **RestTemplate** - HTTP客户端

### 前端技术栈
- **HTML5/CSS3** - 界面结构
- **Vanilla JavaScript** - 交互逻辑
- **Fetch API** - 异步请求

### AI模型
- **comic-text-detector** - 漫画文本检测（YOLOv8架构）
- **manga-ocr** - 日语OCR识别（Vision Transformer）
- **智谱AI GLM-4.5** - 翻译大模型

## 📊 API文档

### 上传并翻译
```http
POST /api/upload
Content-Type: multipart/form-data

参数:
- file: 图片文件（必需）
- sourceLanguage: 源语言（可选，默认JA）
- targetLanguage: 目标语言（可选，默认ZH）
- engine: 翻译引擎（可选，默认ZHIPU）

返回:
{
  "success": true,
  "sessionId": "uuid",
  "message": "翻译任务已开始"
}
```

### 查询进度
```http
GET /api/upload/status/{sessionId}

返回:
{
  "sessionId": "uuid",
  "status": "COMPLETED",
  "progress": 100,
  "message": "翻译完成",
  "timestamp": "2026-01-31T00:00:00"
}
```

### 下载结果
```http
GET /api/upload/download/{sessionId}

返回: 翻译后的图片文件（application/octet-stream）
```

## 🎨 渲染特性

- ✅ **固定字体大小** - 20pt微软雅黑，清晰易读
- ✅ **智能多列布局** - 超长文本自动分列（从右到左）
- ✅ **自动扩展文本框** - 确保所有文字完整显示
- ✅ **竖排中文** - 符合日式漫画阅读习惯
- ✅ **背景抹除** - 自动遮盖原始日文文本
- ✅ **AI水印** - 标注为AI翻译作品

## 🔮 roadmap

- [ ] 支持更多翻译引擎（Google、DeepL、百度）
- [ ] 批量处理优化（多线程并发）
- [ ] 支持英文、韩文等其他语言
- [ ] WebSocket实时进度推送
- [ ] Docker容器化部署
- [ ] GPU加速推理
- [ ] 用户系统和历史记录
- [ ] 自定义字体和样式

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

- [comic-text-detector](https://github.com/dmMaze/comic-text-detector) - 文本检测模型
- [manga-ocr](https://github.com/kha-white/manga-ocr) - OCR识别引擎
- [智谱AI](https://open.bigmodel.cn/) - 翻译服务提供商

## 📧 联系方式

- 项目地址: https://github.com/你的用户名/mangaTrans
- 问题反馈: [Issues](https://github.com/你的用户名/mangaTrans/issues)

---

**⚠️ 免责声明**: 本项目仅供学习交流使用，请勿用于商业用途。翻译作品版权归原作者所有。
