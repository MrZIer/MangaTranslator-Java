#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
完整流程快速测试 - 检测 + OCR + 翻译
仅测试前3个区域以加快速度
"""

import sys
import os
import json
import cv2
from PIL import Image

# 添加comic-text-detector到Python路径
CTD_PATH = os.path.join(os.path.dirname(__file__), 'comic-text-detector')
if os.path.exists(CTD_PATH):
    sys.path.insert(0, CTD_PATH)

def quick_test(image_path, api_key=None):
    """快速测试前3个区域"""
    try:
        import torch
        from manga_ocr import MangaOcr
        
        print("=" * 60)
        print("快速测试 (前3个区域)")
        print("=" * 60)
        
        # 加载OCR
        print("\n[1/4] 加载manga-ocr...")
        mocr = MangaOcr()
        print("✓ OCR已加载")
        
        # 加载检测器
        print("\n[2/4] 加载文本检测器...")
        original_dir = os.getcwd()
        os.chdir(CTD_PATH)
        
        from inference import TextDetector
        from utils.textmask import REFINEMASK_INPAINT
        
        os.chdir(original_dir)
        
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        model_path = 'comictextdetector.pt'
        
        detector = TextDetector(
            model_path=model_path,
            input_size=1024,
            device=device,
            conf_thresh=0.4,
            nms_thresh=0.35
        )
        print(f"✓ 检测器已加载 (设备: {device})")
        
        # 检测文本
        print(f"\n[3/4] 检测文本区域...")
        img = cv2.imread(image_path)
        mask, mask_refined, blk_list = detector(img, refine_mode=REFINEMASK_INPAINT, keep_undetected_mask=False)
        print(f"✓ 检测到 {len(blk_list)} 个区域")
        
        # OCR前3个区域
        print(f"\n[4/4] OCR识别 (测试前3个区域)...")
        test_count = min(3, len(blk_list))
        
        for idx in range(test_count):
            blk = blk_list[idx]
            x1, y1, x2, y2 = blk.xyxy
            x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
            
            region_img = img[y1:y2, x1:x2]
            region_pil = Image.fromarray(cv2.cvtColor(region_img, cv2.COLOR_BGR2RGB))
            
            text = mocr(region_pil)
            print(f"\n区域 {idx+1}:")
            print(f"  坐标: ({x1}, {y1}) -> ({x2}, {y2})")
            print(f"  识别: {text}")
            
            # 翻译
            if api_key:
                try:
                    import requests
                    url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
                    headers = {
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json"
                    }
                    payload = {
                        "model": "glm-4-flash",
                        "messages": [{"role": "user", "content": f"请将以下日文翻译成中文，只输出翻译结果：\n{text}"}],
                        "temperature": 0.3
                    }
                    response = requests.post(url, headers=headers, json=payload, timeout=30)
                    if response.status_code == 200:
                        translated = response.json()['choices'][0]['message']['content'].strip()
                        print(f"  翻译: {translated}")
                except Exception as e:
                    print(f"  翻译失败: {str(e)}")
        
        print(f"\n{'=' * 60}")
        print("✓ 快速测试完成！")
        print(f"{'=' * 60}")
        
    except Exception as e:
        import traceback
        print(f"\n错误: {e}")
        traceback.print_exc()

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python quick_test.py <图像路径> [API密钥]")
        sys.exit(1)
    
    image_path = sys.argv[1]
    api_key = sys.argv[2] if len(sys.argv) > 2 else os.environ.get('ZHIPU_API_KEY')
    
    quick_test(image_path, api_key)
