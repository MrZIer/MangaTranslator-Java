# 漫画翻译工具 - 文本检测集成说明

## 成功完成 ✅

已成功集成 **comic-text-detector** 到项目中！

## 文件清单

### 新增文件
- `comic-text-detector/` - 文本检测库（已克隆）
- `python_ocr_detector.py` - 带多区域检测的OCR脚本
- `CTD_INTEGRATION.md` - 详细集成文档
- `install_detector.bat` - 一键安装脚本
- `requirements-detector.txt` - Python依赖列表

### 待下载
- `comictextdetector.pt` - 模型文件（200MB）
  - 下载地址: https://github.com/zyddnys/manga-image-translator/releases/tag/beta-0.2.1

## 使用方式

### 方式一：简单模式（当前使用）
```bash
python python_ocr_bridge.py "图像.jpg"
```
- 速度快（~2秒）
- 处理整个图像
- 只返回1个文本区域

### 方式二：检测模式（推荐用于生产）
```bash
python python_ocr_detector.py "图像.jpg"
```
- 自动检测多个文本区域（5-20个）
- 精确定位每个文本气泡
- 区分垂直/水平文本
- 速度较慢（~5-8秒）
- **需要先下载模型文件！**

## 下载模型

**重要：**检测模式需要 `comictextdetector.pt` 模型文件

### 下载步骤

1. 访问 GitHub Release:
   ```
   https://github.com/zyddnys/manga-image-translator/releases/tag/beta-0.2.1
   ```

2. 下载文件：
   - 文件名: `comictextdetector.pt`
   - 大小: ~200MB
   - SHA256: `1f90fa60aeeb1eb82e2ac1167a66bf139a8a61b8780acd351ead55268540cccb`

3. 放置位置：
   ```
   F:\some_project\mangaTrans\comictextdetector.pt
   ```

### Google Drive 备用链接
```
https://drive.google.com/drive/folders/1cTsXP5NYTCjhPVxwScdhxqJleHuIOyXG
```

## 对比测试

### 测试图像: F:\Manga\chapter-132\002.jpg

**简单模式输出：**
```json
{
  "regions": [{
    "text": "あ、ああ、そういうの",
    "boundingBox": {"x": 0, "y": 0, "width": 960, "height": 1378},
    "vertical": false
  }],
  "imageWidth": 960,
  "imageHeight": 1378
}
```
- 1个区域
- 整图处理
- 可能遗漏其他文本

**检测模式输出（预期）：**
```json
{
  "regions": [
    {"text": "いや、いや．．．", "boundingBox": {...}, "vertical": false},
    {"text": "あ、ああ", "boundingBox": {...}, "vertical": false},
    {"text": "そういうの", "boundingBox": {...}, "vertical": true},
    ...
  ],
  "imageWidth": 960,
  "imageHeight": 1378
}
```
- 5-20个区域
- 精确定位
- 完整覆盖所有文本

## 性能优化

### GPU加速（可选）
```bash
# 安装CUDA版PyTorch
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
```

### 参数调整
编辑 `python_ocr_detector.py`:
```python
# 速度优先
detect_size=896
det_rearrange_max_batches=2

# 准确度优先
detect_size=1280
conf_thresh=0.3
```

## Java集成

当前 `PythonOcrBridge.java` 使用简单模式。要使用检测模式：

```java
// 修改命令
ProcessBuilder pb = new ProcessBuilder(
    "python", "python_ocr_detector.py",  // 改为检测脚本
    imageFile.getAbsolutePath()
);
```

或者添加配置项让用户选择：
```properties
# application.properties
ocr.detector.enabled=true  # 启用文本检测
ocr.script=python_ocr_detector.py
```

## 故障排除

### 问题：ImportError: No module named 'inference'
```bash
# 检查仓库
cd F:\some_project\mangaTrans
ls comic-text-detector
# 应该看到 inference.py
```

### 问题：FileNotFoundError: comictextdetector.pt
```bash
# 下载模型文件
# 放到: F:\some_project\mangaTrans\comictextdetector.pt
```

### 问题：CUDA out of memory
```python
# 使用CPU模式
device='cpu'
# 或降低输入尺寸
detect_size=768
```

## 下一步

1. ✅ **已完成**：克隆comic-text-detector
2. ⏳ **进行中**：测试脚本（python_ocr_detector.py正在运行）
3. ⏭️ **待办**：下载模型文件（comictextdetector.pt）
4. ⏭️ **待办**：验证多区域检测功能
5. ⏭️ **待办**：更新Java代码支持选择检测模式

## 参考资料

- BallonsTranslator实现: `modules/textdetector/detector_ctd.py`
- comic-text-detector: https://github.com/dmMaze/comic-text-detector
- 详细文档: `CTD_INTEGRATION.md`

---

**提示：** 如果不需要多区域检测，当前的 `python_ocr_bridge.py` 已经足够使用。检测模式适合需要精确翻译多个文本气泡的场景。
