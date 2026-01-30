#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试翻译API功能
"""

import sys
import os
import requests

def test_translation(api_key):
    """测试智谱AI翻译功能"""
    
    test_texts = [
        "こんにちは",
        "漫画を読むのが好きです",
        "ありがとうございます"
    ]
    
    print("=" * 60)
    print("翻译API测试")
    print("=" * 60)
    
    url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    for idx, text in enumerate(test_texts, 1):
        print(f"\n测试 {idx}: {text}")
        
        payload = {
            "model": "glm-4-flash",
            "messages": [{"role": "user", "content": f"请将以下日文翻译成中文，只输出翻译结果：\n{text}"}],
            "temperature": 0.3
        }
        
        try:
            response = requests.post(url, headers=headers, json=payload, timeout=30)
            if response.status_code == 200:
                result = response.json()['choices'][0]['message']['content'].strip()
                print(f"✓ 翻译: {result}")
            else:
                print(f"✗ 错误: HTTP {response.status_code}")
                print(f"  响应: {response.text}")
        except Exception as e:
            print(f"✗ 异常: {str(e)}")
    
    print(f"\n{'=' * 60}")
    print("测试完成")
    print(f"{'=' * 60}")

if __name__ == '__main__':
    api_key = os.environ.get('ZHIPU_API_KEY')
    
    if len(sys.argv) > 1:
        api_key = sys.argv[1]
    
    if not api_key:
        print("错误: 未提供API密钥")
        print("\n用法:")
        print("  python test_translation.py <API_KEY>")
        print("  或设置环境变量: $env:ZHIPU_API_KEY='your_key'")
        sys.exit(1)
    
    test_translation(api_key)
