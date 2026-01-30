# 🎨 漫画翻译渲染功能使用指南

## 功能说明

将检测到的日文文本替换为中文翻译，生成全新的翻译漫画图片。

---

## 📋 功能特点

✅ **完整流程**
- 自动文本检测
- 日文OCR识别
- 中文翻译
- 文本渲染替换

✅ **智能渲染**
- 支持竖排/横排文本
- 自动字体大小调整
- 文本居中对齐
- 自动换行处理

✅ **高质量输出**
- 保持原图分辨率
- 中文字体渲染
- 背景自动填充

---

## 🚀 使用方法

### 方式1: 一键翻译（推荐）

```bash
# 使用批处理脚本
translate_manga.bat "F:\Manga\chapter-132\002.jpg"

# 或指定输出路径
translate_manga.bat "输入.jpg" "输出.jpg"
```

**流程:**
1. 自动检测文本区域
2. OCR识别日文
3. 翻译成中文
4. 渲染到原图
5. 生成翻译图片

**输出:**
- `translated_manga.jpg` - 翻译后的图片
- `translated_manga.json` - 详细数据

---

### 方式2: 分步处理

#### 步骤1: 检测文本
```bash
python test_detection_only.py "image.jpg"
# 生成: detection_result.json
```

#### 步骤2: OCR识别
```bash
python test_ocr_with_detection.py
# 生成: ocr_test_result.json
```

#### 步骤3: 翻译
```bash
python test_translate_only.py
# 生成: translation_result.json
```

#### 步骤4: 渲染
```bash
python render_translation.py "image.jpg" "translation_result.json" "output.jpg"
# 生成: output.jpg
```

---

### 方式3: 完整Python脚本

```bash
# 一键完成所有步骤
python translate_manga.py "F:\Manga\chapter-132\002.jpg" "final_output.jpg"
```

---

## 📊 示例效果

### 处理前后对比

**原图:**
```
┌─────────────────┐
│ クオーネちゃん  │  (竖排日文)
│ お水だよ        │
└─────────────────┘
```

**翻译后:**
```
┌─────────────────┐
│  Quone酱，      │  (竖排中文)
│  是水哦。       │
└─────────────────┘
```

### 实际测试结果

| 原文 | 译文 | 方向 |
|------|------|------|
| クオーネちゃんお水だよ | Quone酱，是水哦。 | 竖排 |
| ワタクシは平気ですから | 我没事的 | 竖排 |

---

## ⚙️ 高级配置

### 自定义字体

编辑 `render_translation.py` 或 `translate_manga.py`:

```python
font_paths = [
    "C:\\Windows\\Fonts\\msyh.ttc",      # 微软雅黑
    "C:\\Windows\\Fonts\\simhei.ttf",    # 黑体
    "你的字体路径.ttf",                   # 自定义字体
]
```

### 调整字体大小

```python
# 在渲染函数中
font_size = max(12, min(region['fontSize'], min(w, h) // 2))
# 调整最小值(12)和比例(// 2)
```

### 文本对齐方式

```python
# 居中对齐（默认）
text_x = x + (w - text_w) // 2
text_y = y + (h - text_h) // 2

# 左对齐
text_x = x + 5
text_y = y + 5
```

---

## 🔧 可用脚本

| 脚本 | 功能 | 用途 |
|------|------|------|
| `translate_manga.py` | 完整流程 | 一键翻译 |
| `translate_manga.bat` | 批处理 | Windows快捷方式 |
| `render_translation.py` | 仅渲染 | 使用已有翻译 |
| `test_detection_only.py` | 仅检测 | 测试检测 |
| `test_ocr_with_detection.py` | 检测+OCR | 测试识别 |
| `test_translate_only.py` | 仅翻译 | 测试API |

---

## 📁 输出文件

### 完整流程输出

```
translated_manga.jpg        # 翻译后的图片
translated_manga.json       # 详细数据
```

### JSON数据结构

```json
{
  "imageWidth": 960,
  "imageHeight": 1378,
  "regions": [
    {
      "id": 1,
      "text": "クオーネちゃんお水だよ",
      "translatedText": "Quone酱，是水哦。",
      "boundingBox": {"x": 794, "y": 96, "width": 67, "height": 183},
      "vertical": true,
      "fontSize": 31
    }
  ]
}
```

---

## ⚠️ 注意事项

### 字体要求
- Windows系统会自动使用系统中文字体
- Linux需要安装中文字体包
- macOS使用PingFang字体

### 性能考虑
- 完整流程耗时约20-40秒/图
- 检测: ~2秒
- OCR: ~6秒 (12区域)
- 翻译: ~15秒 (12区域)
- 渲染: ~1秒

### 质量优化
- 原图分辨率越高，效果越好
- 字体大小自动适配区域
- 长文本自动换行

---

## 🐛 常见问题

### Q1: 中文字体显示为方框
**解决:** 
- 确认系统已安装中文字体
- 手动指定字体路径

### Q2: 文本超出边界
**解决:**
- 自动换行功能会处理
- 可调整字体大小参数

### Q3: 渲染位置不准确
**解决:**
- 检查detection_result.json的坐标
- 调整padding参数

### Q4: 某些区域未渲染
**解决:**
- 检查translatedText是否存在
- 查看是否跳过了省略号等特殊文本

---

## 📈 批量处理

### 处理整个文件夹

```bash
# PowerShell
Get-ChildItem "F:\Manga\chapter-132\*.jpg" | ForEach-Object {
    python translate_manga.py $_.FullName "translated\$($_.Name)"
}
```

### 批处理脚本

```batch
@echo off
for %%f in (F:\Manga\chapter-132\*.jpg) do (
    python translate_manga.py "%%f" "translated\%%~nxf"
)
```

---

## 🎯 快速开始

### 1分钟快速测试

```bash
# 1. 进入项目目录
cd F:\some_project\mangaTrans

# 2. 运行翻译
translate_manga.bat "F:\Manga\chapter-132\002.jpg"

# 3. 查看结果
# translated_manga.jpg 自动打开
```

### 完整示例

```bash
# 设置环境变量（如果需要）
$env:ZHIPU_API_KEY="your_api_key"

# 翻译单张图片
python translate_manga.py "input.jpg" "output.jpg"

# 或使用批处理
translate_manga.bat "input.jpg" "output.jpg"
```

---

## ✨ 功能演示

### 输入
```
原始漫画图片 (960x1378)
├─ 12个日文文本区域
└─ 竖排排列
```

### 处理
```
[检测] → [OCR] → [翻译] → [渲染]
  2秒      6秒      15秒     1秒
```

### 输出
```
翻译后漫画 (960x1378)
├─ 12个中文文本区域
├─ 保持原始排版
└─ 高质量渲染
```

---

## 🔗 相关文档

- [FINAL_TEST_REPORT.md](FINAL_TEST_REPORT.md) - 完整测试报告
- [COMPLETE_WORKFLOW_TEST.md](COMPLETE_WORKFLOW_TEST.md) - 流程测试指南
- [PROJECT_COMPLETE_SUMMARY.md](PROJECT_COMPLETE_SUMMARY.md) - 项目总结

---

## 📞 技术支持

如有问题，请查看:
1. 日志输出（控制台）
2. JSON数据文件
3. 可视化标注图

**常用命令:**
```bash
# 查看检测结果
python test_detection_only.py "image.jpg"

# 仅测试渲染
python render_translation.py "image.jpg" "translation.json" "output.jpg"

# 完整流程
python translate_manga.py "image.jpg" "output.jpg"
```

---

🎉 **享受漫画翻译的乐趣！**
