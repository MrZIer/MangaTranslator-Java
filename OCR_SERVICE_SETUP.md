# manga_ocr微服务部署指南

## Python OCR服务实现示例

创建一个简单的Flask服务来提供OCR功能：

```python
# ocr_service.py
from flask import Flask, request, jsonify
from manga_ocr import MangaOcr
import cv2
import numpy as np
from PIL import Image

app = Flask(__name__)
mocr = MangaOcr()

@app.route('/api/ocr/recognize', methods=['POST'])
def recognize():
    try:
        data = request.json
        image_path = data.get('image_path')
        
        # 读取图像
        image = cv2.imread(image_path)
        
        # 简化版: 假设整个图像作为一个文本区域
        # 实际应用中需要先进行文本检测分割
        
        # 使用manga_ocr识别
        pil_image = Image.fromarray(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))
        text = mocr(pil_image)
        
        # 返回结构化结果
        height, width = image.shape[:2]
        result = {
            'text': text,
            'confidence': 0.95,  # manga_ocr不返回置信度，使用默认值
            'boundingBox': {
                'x': 0,
                'y': 0,
                'width': width,
                'height': height
            },
            'language': 'ja'
        }
        
        return jsonify([result]), 200
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

## 安装依赖

```bash
pip install flask manga-ocr opencv-python pillow
```

## 运行服务

```bash
python ocr_service.py
```

服务将在 http://localhost:5000 启动。

## Docker部署

创建 Dockerfile:

```dockerfile
FROM python:3.9-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ocr_service.py .

EXPOSE 5000

CMD ["python", "ocr_service.py"]
```

构建并运行:

```bash
docker build -t manga-ocr-service .
docker run -d -p 5000:5000 manga-ocr-service
```

## 注意事项

1. **性能优化**: 实际生产环境建议使用gunicorn等WSGI服务器
2. **文本检测**: 示例代码简化了文本区域检测，实际应用需要更复杂的算法
3. **GPU加速**: 如需GPU加速，需要安装CUDA版本的依赖
4. **模型缓存**: manga_ocr首次运行会下载模型，建议提前下载

## 更多信息

- manga_ocr项目: https://github.com/kha-white/manga-ocr
