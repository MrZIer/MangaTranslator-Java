# 正确的模型文件下载指南

## 问题
当前的 `comictextdetector.pt` 是YOLOv8格式，不是comic-text-detector的模型。

## 解决方案

### 方法1：从GitHub Releases下载（推荐）

访问：https://github.com/dmMaze/comic-text-detector/releases

下载文件：`comictextdetector.pt.zip` (约200MB压缩，解压后约900MB)

### 方法2：使用命令下载

```powershell
# 下载模型
Invoke-WebRequest -Uri "https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt" -OutFile "comictextdetector.pt"
```

## 模型文件说明

正确的模型文件应该包含以下键：
- `blk_det`: YOLOv5文本块检测模型
- `text_seg`: Unet文本分割模型  
- `text_det`: DBNet文本行检测模型

当前错误的模型文件包含：
- `epoch`, `model`, `ema`, `optimizer` 等（这是YOLOv8训练checkpoint）

## 验证模型

下载后运行：
```bash
python check_model.py
```

应该看到输出包含 `blk_det`, `text_seg`, `text_det` 三个键。
