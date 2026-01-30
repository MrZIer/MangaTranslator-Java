#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
使用已有的检测结果进行OCR测试
"""

import json
import cv2
from PIL import Image
from manga_ocr import MangaOcr

print("=" * 60)
print("OCR测试 (使用已有检测结果)")
print("=" * 60)

# 读取检测结果
with open('detection_result.json', 'r', encoding='utf-8') as f:
    detection = json.load(f)

print(f"\n检测到 {detection['regionsCount']} 个文本区域")

# 加载OCR
print("\n正在加载manga-ocr...")
mocr = MangaOcr()
print("✓ OCR已加载\n")

# 读取图像
image_path = "F:\\Manga\\chapter-132\\002.jpg"
img = cv2.imread(image_path)

# OCR前3个区域
results = []
for idx, region in enumerate(detection['regions'][:3]):
    bbox = region['boundingBox']
    x, y, w, h = bbox['x'], bbox['y'], bbox['width'], bbox['height']
    
    print(f"区域 {idx + 1}:")
    print(f"  位置: ({x}, {y}), 尺寸: {w}x{h}")
    
    # 裁剪区域
    region_img = img[y:y+h, x:x+w]
    region_pil = Image.fromarray(cv2.cvtColor(region_img, cv2.COLOR_BGR2RGB))
    
    # OCR识别
    text = mocr(region_pil)
    print(f"  识别: {text}\n")
    
    results.append({
        "region": idx + 1,
        "text": text,
        "boundingBox": bbox
    })

# 保存结果
output = {
    "imageWidth": detection['imageWidth'],
    "imageHeight": detection['imageHeight'],
    "totalRegions": detection['regionsCount'],
    "testedRegions": 3,
    "results": results
}

with open('ocr_test_result.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print("=" * 60)
print("✓ 测试完成！结果已保存到 ocr_test_result.json")
print("=" * 60)
