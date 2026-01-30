# 漫画翻译完整流程测试指南

## 三个测试阶段

### 阶段1: 文本检测测试 ✅ (已完成)

测试comic-text-detector是否能正确检测文本区域。

```bash
python test_detection_only.py "F:\Manga\chapter-132\002.jpg"
```

**预期结果:**
- 检测到12个文本区域
- 生成 `detection_result.json` (区域坐标)
- 生成 `detection_visualization.jpg` (可视化标注)

**状态:** ✅ 成功 - 检测到12个竖排文本区域

---

### 阶段2: 检测 + OCR 测试 ⏳ (进行中)

测试文本检测后，对每个区域进行OCR识别。

```bash
python python_ocr_detector.py "F:\Manga\chapter-132\002.jpg"
```

**预期结果:**
```json
{
  "regions": [
    {
      "text": "識別された日本語テキスト",
      "boundingBox": {"x": 794, "y": 96, "width": 67, "height": 183},
      "vertical": true,
      "fontSize": 31,
      "confidence": 0.9
    }
  ],
  "imageWidth": 960,
  "imageHeight": 1378
}
```

**状态:** ⏳ 等待manga-ocr模型加载...

---

### 阶段3: 完整流程 (检测 + OCR + 翻译) ⏸️ (待测试)

#### 3.1 测试翻译API

首先确保翻译API可用：

```bash
# 设置API密钥
$env:ZHIPU_API_KEY="your_api_key_here"

# 测试翻译功能
python test_translation.py
```

#### 3.2 运行完整流程

```bash
python python_ocr_detector.py "F:\Manga\chapter-132\002.jpg" --translate --api-key "your_api_key"
```

或使用环境变量：

```bash
$env:ZHIPU_API_KEY="your_api_key"
python python_ocr_detector.py "F:\Manga\chapter-132\002.jpg" --translate
```

**预期结果:**
```json
{
  "regions": [
    {
      "text": "このマンガは面白いです",
      "translatedText": "这部漫画很有趣",
      "boundingBox": {"x": 794, "y": 96, "width": 67, "height": 183},
      "vertical": true,
      "fontSize": 31,
      "confidence": 0.9
    }
  ],
  "imageWidth": 960,
  "imageHeight": 1378
}
```

---

## 工作流程说明

```
输入图像
    ↓
[步骤1] 文本检测 (comic-text-detector)
    ├─ YOLOv5: 检测文本块
    ├─ Unet: 分割文本区域
    └─ DBNet: 精确文本行
    ↓
检测到12个区域 (每个区域有坐标、方向、字体大小)
    ↓
[步骤2] OCR识别 (manga-ocr)
    └─ 对每个区域逐个识别日文
    ↓
识别出12段日文文本
    ↓
[步骤3] 翻译 (Zhipu AI - 可选)
    └─ 将每段日文翻译成中文
    ↓
输出JSON (包含原文、译文、坐标)
```

---

## 性能预期

- **检测速度**: ~1-2秒/图 (GPU) / ~5-10秒/图 (CPU)
- **OCR速度**: ~0.5秒/区域 (12个区域约6秒)
- **翻译速度**: ~1-2秒/区域 (12个区域约12-24秒)
- **总耗时**: ~20-40秒/图 (含翻译) / ~8-12秒/图 (仅OCR)

---

## 故障排查

### 问题1: 模型加载失败
```
错误: 'blk_det'
```
**解决:** 下载正确的模型文件
```bash
Invoke-WebRequest -Uri "https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt" -OutFile "comictextdetector.pt"
```

### 问题2: 导入错误
```
错误: No module named 'wandb'
```
**解决:** 安装依赖
```bash
pip install wandb tqdm torchsummary
```

### 问题3: PyTorch版本问题
```
错误: Weights only load failed
```
**解决:** 已自动修复 (basemodel.py添加 weights_only=False)

### 问题4: 翻译失败
```
警告: 未提供API密钥
```
**解决:** 设置环境变量或使用 --api-key 参数

---

## 下一步集成到SpringBoot

完成Python脚本测试后，需要：

1. **修改Java后端** - 更新 `PythonOcrBridge.java`
   ```java
   // 调用新的检测+OCR脚本
   ProcessBuilder pb = new ProcessBuilder(
       pythonPath,
       "python_ocr_detector.py",
       imagePath,
       "--translate",
       "--api-key", apiKey
   );
   ```

2. **更新数据模型** - 添加 `translatedText` 字段
   ```java
   public class TextRegion {
       private String text;
       private String translatedText;  // 新增
       private BoundingBox boundingBox;
       private boolean vertical;
       private int fontSize;
   }
   ```

3. **前端展示** - 显示原文和译文
   - 原文: 日文竖排文本
   - 译文: 中文水平显示

---

## 当前状态总结

✅ **已完成:**
- comic-text-detector集成
- 文本区域检测 (12个区域)
- 可视化标注

⏳ **进行中:**
- OCR模块加载 (manga-ocr)

⏸️ **待完成:**
- OCR识别测试
- 翻译功能测试
- Java后端集成
- 前端界面更新
