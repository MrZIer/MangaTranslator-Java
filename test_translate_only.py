#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试翻译功能 - 直接使用OCR结果
"""

import json
import os
import requests

api_key = "af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit"

# 读取OCR结果
with open('ocr_test_result.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

print("=" * 60)
print("翻译测试")
print("=" * 60)

url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json"
}

translated_results = []

for result in data['results']:
    text = result['text']
    region_id = result['region']
    
    print(f"\n区域 {region_id}:")
    print(f"  原文: {text}")
    
    if text.strip() in ['．．．．．', '...', '']:
        print(f"  翻译: （省略号，跳过）")
        continue
    
    payload = {
        "model": "glm-4-flash",
        "messages": [{"role": "user", "content": f"你是一位专业的日语翻译专家，请将以下日语文本翻译成中文，直接输出中文翻译结果，不要有任何其他内容！如果翻译结果与原文相同，请直接输出原文。\n\n{text}"}],
        "temperature": 0.3
    }
    
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        if response.status_code == 200:
            translated = response.json()['choices'][0]['message']['content'].strip()
            print(f"  翻译: {translated}")
            translated_results.append({
                "region": region_id,
                "original": text,
                "translated": translated,
                "boundingBox": result['boundingBox']
            })
        else:
            print(f"  错误: HTTP {response.status_code}")
    except Exception as e:
        print(f"  异常: {str(e)}")

# 保存结果
output = {
    "imageWidth": data['imageWidth'],
    "imageHeight": data['imageHeight'],
    "results": translated_results
}

with open('translation_result.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"\n{'=' * 60}")
print("✓ 翻译完成！结果已保存到 translation_result.json")
print(f"{'=' * 60}")
