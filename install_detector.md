# 漫画文本检测器安装指南

## 概述

集成 comic-text-detector 进行精准的漫画文本区域检测，解决只能检测单个区域的问题。

## 安装步骤

### 1. 安装Python依赖

```bash
pip install comic-text-detector
```

或者从源码安装最新版：

```bash
pip install git+https://github.com/dmMaze/comic-text-detector.git
```

### 2. 下载模型文件

下载 `comictextdetector.pt` 模型文件（约200MB）：

**方式一：GitHub Release**
- 访问：https://github.com/zyddnys/manga-image-translator/releases/tag/beta-0.2.1
- 下载 `comictextdetector.pt` 文件

**方式二：Google Drive**
- 访问：https://drive.google.com/drive/folders/1cTsXP5NYTCjhPVxwScdhxqJleHuIOyXG
- 下载 `comictextdetector.pt` 文件

### 3. 放置模型文件

将下载的 `comictextdetector.pt` 文件放到项目根目录：

```
F:/some_project/mangaTrans/comictextdetector.pt
```

## 使用方式

### 方式一：自动检测（推荐）

脚本会自动检测是否安装了 comic-text-detector：

- **如果已安装**：使用文本检测器，可以检测多个文本区域
- **如果未安装**：回退到简单模式，处理整个图像

### 方式二：手动指定

在 `application.properties` 中配置：

```properties
# 使用文本检测器（需要安装comic-text-detector）
ocr.detector.enabled=true
ocr.detector.model.path=comictextdetector.pt

# 检测器参数
ocr.detector.input.size=1024
ocr.detector.conf.thresh=0.4
ocr.detector.nms.thresh=0.35
```

## 技术说明

### comic-text-detector 功能

1. **文本区域检测**：自动检测漫画中的所有文本气泡和文本框
2. **文本行分割**：将每个文本区域分割为多个文本行
3. **方向识别**：区分垂直文本和水平文本
4. **精确边界框**：提供准确的文本区域坐标

### 检测流程

```
输入图像
  ↓
YOLOv5文本块检测 → 检测所有文本区域
  ↓
文本分割 → 精细化区域边界
  ↓
文本行检测 → 分割每个区域的文本行
  ↓
manga-ocr识别 → 对每个文本行进行OCR
  ↓
输出JSON结果
```

### 输出格式

```json
{
  "regions": [
    {
      "text": "识别的日文文本",
      "boundingBox": {
        "x": 100,
        "y": 200,
        "width": 300,
        "height": 50
      },
      "vertical": false,
      "confidence": 0.9
    }
  ],
  "imageWidth": 1920,
  "imageHeight": 1080
}
```

## 性能优化

### GPU加速

如果有NVIDIA GPU，安装CUDA版本的PyTorch：

```bash
# CUDA 11.8
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118

# CUDA 12.1
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121
```

### 批量处理

对于大量图像，可以调整：

```properties
# 增加输入尺寸以提高准确率（但会降低速度）
ocr.detector.input.size=1536

# 降低置信度阈值以检测更多文本（可能增加误检）
ocr.detector.conf.thresh=0.3
```

## 故障排除

### 问题1：ImportError: No module named 'comic_text_detector'

**解决方案**：
```bash
pip install comic-text-detector
```

### 问题2：FileNotFoundError: comictextdetector.pt

**解决方案**：
1. 确认模型文件已下载
2. 检查文件路径是否正确
3. 或在 application.properties 中指定完整路径

### 问题3：CUDA out of memory

**解决方案**：
```properties
# 降低输入尺寸
ocr.detector.input.size=768
```

或强制使用CPU：
```python
device = 'cpu'
```

### 问题4：检测不到文本区域

**解决方案**：
1. 降低置信度阈值：`conf_thresh=0.3`
2. 确保图像清晰，对比度足够
3. 尝试预处理：调整亮度、对比度

## 参考资料

- comic-text-detector: https://github.com/dmMaze/comic-text-detector
- manga-ocr: https://github.com/kha-white/manga-ocr
- mokuro (完整示例): https://github.com/kha-white/mokuro

## 性能对比

### 简单模式（不使用检测器）
- ✅ 速度快（~2秒/页）
- ❌ 只能处理单个文本气泡
- ❌ 无法分离多个文本区域

### 检测器模式（使用comic-text-detector）
- ✅ 可检测多个文本区域（5-20个/页）
- ✅ 准确定位文本位置
- ✅ 区分垂直/水平文本
- ⚠️ 速度较慢（~5-8秒/页）
- ⚠️ 需要下载200MB模型

## 建议

1. **首次使用**：先尝试简单模式，确认OCR准确性
2. **多文本场景**：使用检测器模式，获得完整翻译
3. **批量处理**：使用GPU加速，提高处理速度
4. **在线服务**：建议使用检测器模式，提供更好的用户体验
