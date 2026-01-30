#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
完整的漫画翻译流程：检测 → OCR → 翻译 → 渲染
一键生成翻译后的图片
"""

import sys
import os
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import requests

# 添加comic-text-detector到Python路径
CTD_PATH = os.path.join(os.path.dirname(__file__), 'comic-text-detector')
if os.path.exists(CTD_PATH):
    sys.path.insert(0, CTD_PATH)

def complete_translation_pipeline(image_path, api_key, output_path="final_translated.jpg"):
    """
    完整翻译流程
    
    Args:
        image_path: 输入图片路径
        api_key: 翻译API密钥
        output_path: 输出图片路径
    """
    
    print("=" * 70)
    print("漫画翻译完整流程")
    print("=" * 70)
    print(f"输入: {image_path}")
    print(f"输出: {output_path}")
    print()
    
    # 步骤1: 文本检测
    print("[步骤 1/4] 文本检测中...")
    
    import torch
    from manga_ocr import MangaOcr
    
    original_dir = os.getcwd()
    os.chdir(CTD_PATH)
    from inference import TextDetector
    from utils.textmask import REFINEMASK_INPAINT
    os.chdir(original_dir)
    
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    detector = TextDetector(
        model_path='comictextdetector.pt',
        input_size=1024,
        device=device,
        conf_thresh=0.4,
        nms_thresh=0.35
    )
    
    img = cv2.imread(image_path)
    mask, mask_refined, blk_list = detector(img, refine_mode=REFINEMASK_INPAINT, keep_undetected_mask=False)
    print(f"✓ 检测到 {len(blk_list)} 个文本区域\n")
    
    # 步骤2: OCR识别
    print("[步骤 2/4] OCR识别中...")
    mocr = MangaOcr()
    
    regions = []
    for idx, blk in enumerate(blk_list):
        x1, y1, x2, y2 = blk.xyxy
        x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
        w, h = x2 - x1, y2 - y1
        
        region_img = img[y1:y2, x1:x2]
        region_pil = Image.fromarray(cv2.cvtColor(region_img, cv2.COLOR_BGR2RGB))
        text = mocr(region_pil)
        
        regions.append({
            "id": idx + 1,
            "text": text,
            "boundingBox": {"x": x1, "y": y1, "width": w, "height": h},
            "vertical": bool(blk.vertical) if hasattr(blk, 'vertical') else False,
            "fontSize": int(blk.font_size) if hasattr(blk, 'font_size') and blk.font_size > 0 else 20
        })
        
        print(f"  区域 {idx+1}: {text[:20]}{'...' if len(text) > 20 else ''}")
    
    print(f"✓ 完成 {len(regions)} 个区域的识别\n")
    
    # 步骤3: 翻译
    print("[步骤 3/4] 翻译中...")
    
    url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    for region in regions:
        text = region['text']
        if text.strip() and text.strip() not in ['．．．．．', '...', '・・・']:
            try:
                payload = {
                    "model": "glm-4-flash",
                    "messages": [{"role": "user", "content": f"你是一位专业的日语翻译专家，请将以下日语文本翻译成中文，直接输出中文翻译结果，不要有任何其他内容！如果翻译结果与原文相同，请直接输出原文。\n\n{text}"}],
                    "temperature": 0.3
                }
                response = requests.post(url, headers=headers, json=payload, timeout=30)
                if response.status_code == 200:
                    region['translatedText'] = response.json()['choices'][0]['message']['content'].strip()
                    print(f"  区域 {region['id']}: {region['translatedText'][:20]}{'...' if len(region['translatedText']) > 20 else ''}")
                else:
                    region['translatedText'] = text
            except Exception as e:
                region['translatedText'] = text
                print(f"  区域 {region['id']}: 翻译失败，保持原文")
        else:
            region['translatedText'] = text
    
    print(f"✓ 完成翻译\n")
    
    # 步骤4: 渲染
    print("[步骤 4/4] 渲染翻译结果...")
    
    img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(img_pil)
    
    # 加载字体
    font_paths = [
        "C:\\Windows\\Fonts\\msyh.ttc",
        "C:\\Windows\\Fonts\\simhei.ttf",
        "C:\\Windows\\Fonts\\simsun.ttc"
    ]
    
    font_path = None
    for path in font_paths:
        if os.path.exists(path):
            font_path = path
            break
    
    for region in regions:
        bbox = region['boundingBox']
        x, y, w, h = bbox['x'], bbox['y'], bbox['width'], bbox['height']
        translated = region['translatedText']
        is_vertical = region['vertical']
        
        if translated == region['text']:
            continue
        
        # 填充白色背景
        cv2.rectangle(img, (x, y), (x + w, y + h), (255, 255, 255), -1)
        img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        draw = ImageDraw.Draw(img_pil)
        
        # 字体大小 - 固定为20pt
        font_size = 20
        
        try:
            font = ImageFont.truetype(font_path, font_size) if font_path else ImageFont.load_default()
        except:
            font = ImageFont.load_default()
        
        # 渲染文本
        if is_vertical:
            # 竖排 - 动态调整字体
            padding_top = 5
            padding_bottom = 5
            char_spacing = 2
            column_spacing = 25  # 列间距
            
            # 计算单列最大高度
            max_column_height = h - padding_top - padding_bottom
            
            # 计算每个字符的高度，预先分列
            chars_data = []
            for char in translated:
                if char.strip():
                    bbox_char = draw.textbbox((0, 0), char, font=font)
                    char_w = bbox_char[2] - bbox_char[0]
                    char_h = bbox_char[3] - bbox_char[1]
                    chars_data.append((char, char_w, char_h))
            
            # 分配到多列
            columns = []
            current_column = []
            current_column_height = 0
            
            for char, char_w, char_h in chars_data:
                if current_column_height + char_h + char_spacing <= max_column_height:
                    current_column.append((char, char_w, char_h))
                    current_column_height += char_h + char_spacing
                else:
                    if current_column:
                        columns.append(current_column)
                    current_column = [(char, char_w, char_h)]
                    current_column_height = char_h + char_spacing
            
            if current_column:
                columns.append(current_column)
            
            # 如果需要多列，扩展文本框宽度
            num_columns = len(columns)
            if num_columns > 1:
                w_new = w + (num_columns - 1) * column_spacing
                # 扩展白色背景
                draw.rectangle([x, y, x + w_new, y + h], fill=(255, 255, 255))
                w = w_new
            
            # 渲染多列（从右到左）
            for col_idx, column in enumerate(columns):
                # 从右到左排列列
                column_x = x + w - (col_idx * column_spacing) - 15
                current_y = y + padding_top
                
                for char, char_w, char_h in column:
                    char_x = column_x - char_w // 2
                    draw.text((char_x, current_y), char, fill=(0, 0, 0), font=font)
                    current_y += char_h + char_spacing
        else:
            # 横排 - 自动换行和字体调整
            padding_left = 5
            padding_right = 5
            padding_top = 5
            line_spacing = 3
            
            bbox_text = draw.textbbox((0, 0), translated, font=font)
            text_w = bbox_text[2] - bbox_text[0]
            text_h = bbox_text[3] - bbox_text[1]
            
            available_width = w - padding_left - padding_right
            available_height = h - padding_top * 2
            
            if text_w > available_width:
                lines = []
                current_line = ""
                for char in translated:
                    test_line = current_line + char
                    bbox_test = draw.textbbox((0, 0), test_line, font=font)
                    if bbox_test[2] - bbox_test[0] <= available_width:
                        current_line = test_line
                    else:
                        if current_line:
                            lines.append(current_line)
                        current_line = char
                if current_line:
                    lines.append(current_line)
                
                line_height = text_h + line_spacing
                total_height = line_height * len(lines)
                
                while total_height > available_height and font_size > 10:
                    font_size -= 2
                    try:
                        font = ImageFont.truetype(font_path, font_size) if font_path else ImageFont.load_default()
                    except:
                        font = ImageFont.load_default()
                    lines = []
                    current_line = ""
                    for char in translated:
                        test_line = current_line + char
                        bbox_test = draw.textbbox((0, 0), test_line, font=font)
                        if bbox_test[2] - bbox_test[0] <= available_width:
                            current_line = test_line
                        else:
                            if current_line:
                                lines.append(current_line)
                            current_line = char
                    if current_line:
                        lines.append(current_line)
                    bbox_test = draw.textbbox((0, 0), "测", font=font)
                    line_height = (bbox_test[3] - bbox_test[1]) + line_spacing
                    total_height = line_height * len(lines)
                
                start_y = y + (h - total_height) // 2
                for i, line in enumerate(lines):
                    bbox_line = draw.textbbox((0, 0), line, font=font)
                    line_w = bbox_line[2] - bbox_line[0]
                    line_x = x + (w - line_w) // 2
                    line_y = start_y + i * line_height
                    if line_y + line_height <= y + h:
                        draw.text((line_x, line_y), line, fill=(0, 0, 0), font=font)
            else:
                text_x = x + (w - text_w) // 2
                text_y = y + (h - text_h) // 2
                draw.text((text_x, text_y), translated, fill=(0, 0, 0), font=font)
        
        img = cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)
    
    # 保存结果
    cv2.imwrite(output_path, img)
    print(f"✓ 翻译图片已保存\n")
    
    # 同时保存JSON结果
    json_output = output_path.replace('.jpg', '.json').replace('.png', '.json')
    with open(json_output, 'w', encoding='utf-8') as f:
        json.dump({
            "imageWidth": img.shape[1],
            "imageHeight": img.shape[0],
            "regions": regions
        }, f, ensure_ascii=False, indent=2)
    
    print("=" * 70)
    print("✓ 完整流程完成！")
    print(f"✓ 翻译图片: {output_path}")
    print(f"✓ JSON数据: {json_output}")
    print("=" * 70)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python translate_manga.py <图片路径> [输出路径]")
        print("示例: python translate_manga.py image.jpg output.jpg")
        sys.exit(1)
    
    image_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else "translated_manga.jpg"
    api_key = os.environ.get('ZHIPU_API_KEY', 'af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit')
    
    complete_translation_pipeline(image_path, api_key, output_path)
