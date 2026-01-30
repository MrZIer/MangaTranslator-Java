#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
集成comic-text-detector进行漫画文本检测 + OCR + 翻译
使用方法: python python_ocr_detector.py <image_path> [--translate] [--api-key <key>]
返回: JSON格式的检测结果，包含多个文本区域、OCR识别及翻译

工作流程:
1. 文本检测 (comic-text-detector) - 检测所有文本区域
2. OCR识别 (manga-ocr) - 识别每个区域的日文文本
3. 翻译 (可选) - 将日文翻译为中文
"""

import sys
import os
import json
import platform
import argparse

# 添加comic-text-detector到Python路径
CTD_PATH = os.path.join(os.path.dirname(__file__), 'comic-text-detector')
if os.path.exists(CTD_PATH):
    sys.path.insert(0, CTD_PATH)

# Force UTF-8 output on Windows
if platform.system() == 'Windows':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)

def translate_text(text, api_key=None):
    """翻译日文到中文"""
    if not text or not text.strip():
        return ""
    
    try:
        if api_key:
            import requests
            url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
            headers = {
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": "glm-4-flash",
                "messages": [{"role": "user", "content": f"你是一位专业的日语翻译专家，请将以下日语文本翻译成中文，直接输出中文翻译结果，不要有任何其他内容！如果翻译结果与原文相同，请直接输出原文。\n\n{text}"}],
                "temperature": 0.3
            }
            response = requests.post(url, headers=headers, json=payload, timeout=30)
            if response.status_code == 200:
                return response.json()['choices'][0]['message']['content'].strip()
        return text
    except Exception as e:
        print(f"翻译失败: {str(e)}", file=sys.stderr)
        return text

def perform_ocr_with_detection(image_path, enable_translation=False, api_key=None):
    """文本检测 + OCR + 翻译"""
    try:
        import torch
        import cv2
        import numpy as np
        from PIL import Image
        from manga_ocr import MangaOcr
        
        print("正在加载manga-ocr...", file=sys.stderr)
        mocr = MangaOcr()
        print("✓ manga-ocr已加载", file=sys.stderr)
        
        # 尝试加载检测器
        try:
            # 切换到comic-text-detector目录以正确导入模块
            original_dir = os.getcwd()
            os.chdir(CTD_PATH)
            
            from inference import TextDetector
            from utils.textmask import REFINEMASK_INPAINT
            
            # 切换回原目录
            os.chdir(original_dir)
            
            device = 'cuda' if torch.cuda.is_available() else 'cpu'
            model_path = os.path.join(os.path.dirname(__file__), 'comictextdetector.pt')
            if not os.path.exists(model_path):
                model_path = 'comictextdetector.pt'
            
            if not os.path.exists(model_path):
                raise FileNotFoundError(f"模型文件不存在: {model_path}")
            
            print(f"正在加载文本检测器（设备: {device}）...", file=sys.stderr)
            detector = TextDetector(
                model_path=model_path,
                input_size=1024,
                device=device,
                conf_thresh=0.4,
                nms_thresh=0.35
            )
            print("✓ 文本检测器已加载", file=sys.stderr)
            
            # 读取图像
            img = cv2.imread(image_path)
            if img is None:
                return json.dumps({"error": f"无法读取图像: {image_path}"}, ensure_ascii=False)
            
            img_h, img_w = img.shape[:2]
            print(f"图像尺寸: {img_w}x{img_h}", file=sys.stderr)
            
            # 步骤1: 文本检测
            print("[步骤1] 文本检测中...", file=sys.stderr)
            mask, mask_refined, blk_list = detector(img, refine_mode=REFINEMASK_INPAINT, keep_undetected_mask=False)
            print(f"✓ 检测到 {len(blk_list)} 个文本区域", file=sys.stderr)
            
            regions = []
            
            if len(blk_list) > 0:
                # 步骤2: OCR识别
                print(f"[步骤2] OCR识别中（共{len(blk_list)}个区域）...", file=sys.stderr)
                for idx, blk in enumerate(blk_list):
                    x1, y1, x2, y2 = blk.xyxy
                    x1 = max(0, min(int(x1), img_w - 1))
                    y1 = max(0, min(int(y1), img_h - 1))
                    x2 = max(x1 + 1, min(int(x2), img_w))
                    y2 = max(y1 + 1, min(int(y2), img_h))
                    
                    region_img = img[y1:y2, x1:x2]
                    if region_img.size == 0:
                        continue
                    
                    region_pil = Image.fromarray(cv2.cvtColor(region_img, cv2.COLOR_BGR2RGB))
                    
                    try:
                        text = mocr(region_pil)
                        print(f"  区域{idx+1}: {text[:30]}{'...' if len(text) > 30 else ''}", file=sys.stderr)
                    except Exception as e:
                        text = ""
                        print(f"  警告: 区域{idx+1}识别失败 - {str(e)}", file=sys.stderr)
                    
                    region_data = {
                        "text": text,
                        "boundingBox": {"x": x1, "y": y1, "width": x2 - x1, "height": y2 - y1},
                        "vertical": bool(blk.vertical) if hasattr(blk, 'vertical') else False,
                        "fontSize": int(blk.font_size) if hasattr(blk, 'font_size') and blk.font_size > 0 else 16,
                        "confidence": 0.9
                    }
                    
                    # 步骤3: 翻译（如果启用）
                    if enable_translation and text:
                        print(f"  翻译{idx+1}中...", file=sys.stderr)
                        translated = translate_text(text, api_key)
                        region_data["translatedText"] = translated
                        print(f"  翻译{idx+1}: {translated[:30]}{'...' if len(translated) > 30 else ''}", file=sys.stderr)
                    
                    regions.append(region_data)
                
                print(f"✓ OCR识别完成", file=sys.stderr)
                if enable_translation:
                    print(f"✓ 翻译完成", file=sys.stderr)
            else:
                print("警告: 未检测到文本区域，使用整图", file=sys.stderr)
                pil_img = Image.open(image_path)
                text = mocr(pil_img)
                regions.append({
                    "text": text,
                    "boundingBox": {"x": 0, "y": 0, "width": img_w, "height": img_h},
                    "vertical": False,
                    "confidence": 0.5
                })
            
            result = {
                "regions": regions,
                "imageWidth": img_w,
                "imageHeight": img_h
            }
            
            return json.dumps(result, ensure_ascii=False)
            
        except (ImportError, FileNotFoundError) as e:
            print(f"警告: 文本检测器不可用 ({str(e)})", file=sys.stderr)
            print("回退到简单OCR模式", file=sys.stderr)
            
            pil_img = Image.open(image_path)
            text = mocr(pil_img)
            img_w, img_h = pil_img.size
            
            result = {
                "regions": [{
                    "text": text,
                    "boundingBox": {"x": 0, "y": 0, "width": img_w, "height": img_h},
                    "vertical": False,
                    "confidence": 0.5
                }],
                "imageWidth": img_w,
                "imageHeight": img_h
            }
            
            return json.dumps(result, ensure_ascii=False)
            
    except Exception as e:
        import traceback
        error_details = traceback.format_exc()
        return json.dumps({
            "error": f"处理失败: {str(e)}",
            "details": error_details
        }, ensure_ascii=False)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='漫画文本检测、OCR和翻译')
    parser.add_argument('image_path', help='输入图像路径')
    parser.add_argument('--translate', action='store_true', help='启用翻译功能')
    parser.add_argument('--api-key', help='翻译API密钥（Zhipu AI）')
    
    args = parser.parse_args()
    
    # 尝试从环境变量读取API key
    api_key = args.api_key or os.environ.get('ZHIPU_API_KEY')
    
    if args.translate and not api_key:
        print("警告: 未提供API密钥，翻译功能将不可用", file=sys.stderr)
        print("提示: 使用 --api-key 参数或设置 ZHIPU_API_KEY 环境变量", file=sys.stderr)
    
    result = perform_ocr_with_detection(args.image_path, args.translate, api_key)
    print(result)
