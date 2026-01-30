# 集成 comic-text-detector 安装指南

## 快速开始

### 1. 克隆 comic-text-detector 仓库

```bash
cd F:\some_project\mangaTrans
git clone https://github.com/dmMaze/comic-text-detector.git
```

### 2. 安装Python依赖

```bash
pip install torch torchvision manga-ocr opencv-python pillow numpy scipy shapely pyclipper
```

### 3. 下载模型文件

下载 `comictextdetector.pt` 并放到项目根目录：

```
F:\some_project\mangaTrans\comictextdetector.pt
```

下载地址：
- GitHub: https://github.com/zyddnys/manga-image-translator/releases/tag/beta-0.2.1
- 文件名: comictextdetector.pt （约200MB）

### 4. 测试脚本

```bash
python python_ocr_detector.py "F:\Manga\chapter-132\002.jpg"
```

## 项目结构

```
mangaTrans/
├── comic-text-detector/          # 克隆的仓库
│   ├── inference.py              # 检测器主文件
│   ├── basemodel.py              # 模型定义
│   ├── utils/                    # 工具函数
│   │   ├── textblock.py         # TextBlock类
│   │   └── textmask.py          # 掩码处理
│   └── ...
├── comictextdetector.pt          # 模型文件（需下载）
├── python_ocr_detector.py        # 带检测功能的OCR脚本
└── python_ocr_bridge.py          # 简单OCR脚本（回退方案）
```

## 工作原理

### 检测流程（参考 BallonsTranslator）

```
输入图像
  ↓
YOLOv5文本块检测 → 检测所有文本气泡边界框
  ↓
Unet文本分割 → 精细化区域边界（mask）
  ↓
DBNet文本行检测 → 检测每个区域的文本行
  ↓
后处理与分组 → 将文本行分组为文本块（TextBlock）
  ↓
manga-ocr识别 → 对每个文本块进行OCR
  ↓
返回JSON结果
```

### 关键组件

1. **TextDetector** (`inference.py`)
   - 主检测类
   - 整合YOLOv5、Unet、DBNet三个模型
   - 配置：`detect_size=1024`, `conf_thresh=0.4`

2. **TextBlock** (`utils/textblock.py`)
   - 文本块数据结构
   - 属性：`xyxy`（坐标）, `vertical`（方向）, `font_size`（字号）

3. **掩码处理** (`utils/textmask.py`)
   - `REFINEMASK_INPAINT`: 修复模式，用于翻译
   - `REFINEMASK_ANNOTATION`: 标注模式，用于训练

## 输出格式

```json
{
  "regions": [
    {
      "text": "いや、いや．．．",
      "boundingBox": {
        "x": 100,
        "y": 200,
        "width": 300,
        "height": 50
      },
      "vertical": false,
      "fontSize": 24,
      "confidence": 0.9
    }
  ],
  "imageWidth": 960,
  "imageHeight": 1378
}
```

## 性能对比

| 模式 | 检测时间 | 识别准确度 | 区域数量 |
|------|---------|-----------|---------|
| 简单模式（无检测） | ~2秒 | 中等 | 1个（整图） |
| 检测模式（CTD） | ~5-8秒 | 高 | 5-20个 |

## 参数调优

### 提高检测速度
```python
detect_size=896  # 降低输入尺寸（默认1024）
det_rearrange_max_batches=2  # 减少批处理（默认4）
```

### 提高检测准确度
```python
detect_size=1280  # 增加输入尺寸
conf_thresh=0.3  # 降低置信度阈值（默认0.4）
```

### GPU加速
```python
device='cuda'  # 自动检测CUDA
half=True  # 使用FP16精度（需CUDA）
```

## 常见问题

### Q1: ImportError: No module named 'inference'

**解决方案：**
```bash
# 确认comic-text-detector已克隆
cd F:\some_project\mangaTrans
ls comic-text-detector  # 应该看到inference.py
```

### Q2: FileNotFoundError: comictextdetector.pt

**解决方案：**
1. 下载模型文件
2. 放到项目根目录 `F:\some_project\mangaTrans\`
3. 或修改脚本中的 `model_path`

### Q3: CUDA out of memory

**解决方案：**
```python
# 方法1: 降低输入尺寸
detect_size=768

# 方法2: 使用CPU
device='cpu'
```

### Q4: 检测不到某些文本

**解决方案：**
```python
# 降低置信度阈值
conf_thresh=0.3  # 默认0.4
nms_thresh=0.3   # 默认0.35
```

## 与Java集成

Python脚本已经准备好，Java后端使用 `PythonOcrBridge.java` 调用：

```java
// 自动选择：
// - 如果有comic-text-detector → 使用python_ocr_detector.py
// - 如果没有 → 回退到python_ocr_bridge.py
ProcessBuilder pb = new ProcessBuilder(
    "python", "python_ocr_detector.py",
    imageFile.getAbsolutePath()
);
```

## 参考资料

- BallonsTranslator: https://github.com/dmMaze/BallonsTranslator
  - 实现文件: `modules/textdetector/detector_ctd.py`
  - CTD包装: `modules/textdetector/ctd/`
- comic-text-detector: https://github.com/dmMaze/comic-text-detector
- manga-image-translator: https://github.com/zyddnys/manga-image-translator

## 下一步

1. 测试检测功能是否正常工作
2. 如果效果好，更新Java代码使用新脚本
3. 添加配置项让用户选择模式（简单/检测）
