#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
完整流程测试 - 所有12个区域
"""

import json
import cv2
import os
import requests
from PIL import Image
from manga_ocr import MangaOcr

print("=" * 70)
print("完整流程测试 (检测 + OCR + 翻译) - 12个区域")
print("=" * 70)

api_key = "af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit"

# 读取检测结果
with open('detection_result.json', 'r', encoding='utf-8') as f:
    detection = json.load(f)

print(f"\n✓ 检测到 {detection['regionsCount']} 个文本区域")

# 加载OCR
print("正在加载manga-ocr...")
mocr = MangaOcr()
print("✓ OCR已加载\n")

# 读取图像
image_path = "F:\\Manga\\chapter-132\\002.jpg"
img = cv2.imread(image_path)

# 翻译API配置
url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json"
}

# 处理所有区域
results = []
for idx, region in enumerate(detection['regions']):
    bbox = region['boundingBox']
    x, y, w, h = bbox['x'], bbox['y'], bbox['width'], bbox['height']
    
    print(f"区域 {idx + 1}/{detection['regionsCount']}:")
    
    # OCR识别
    region_img = img[y:y+h, x:x+w]
    region_pil = Image.fromarray(cv2.cvtColor(region_img, cv2.COLOR_BGR2RGB))
    text = mocr(region_pil)
    print(f"  原文: {text}")
    
    # 翻译
    translated = ""
    if text.strip() and text.strip() not in ['．．．．．', '...', '・・・']:
        try:
            payload = {
                "model": "glm-4-flash",
                "messages": [{"role": "user", "content": f"你是一位专业的日语翻译专家，请将以下日语文本翻译成中文，直接输出中文翻译结果，不要有任何其他内容！如果翻译结果与原文相同，请直接输出原文。\n\n{text}"}],
                "temperature": 0.3
            }
            response = requests.post(url, headers=headers, json=payload, timeout=30)
            if response.status_code == 200:
                translated = response.json()['choices'][0]['message']['content'].strip()
                print(f"  译文: {translated}")
        except Exception as e:
            print(f"  翻译失败: {str(e)}")
    else:
        translated = text
        print(f"  译文: （保持原样）")
    
    results.append({
        "region": idx + 1,
        "text": text,
        "translatedText": translated,
        "boundingBox": bbox,
        "vertical": region.get('vertical', False),
        "fontSize": region.get('fontSize', 16)
    })
    print()

# 保存完整结果
output = {
    "imageWidth": detection['imageWidth'],
    "imageHeight": detection['imageHeight'],
    "totalRegions": detection['regionsCount'],
    "regions": results
}

with open('final_result.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print("=" * 70)
print("✓ 完整流程测试完成！")
print(f"✓ 结果已保存到: final_result.json")
print("=" * 70)
