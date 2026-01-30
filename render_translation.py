#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
文本替换和渲染 - 将原文替换为译文生成新图片
"""

import json
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import os

def render_translated_image(original_image_path, translation_json_path, output_path="translated_image.jpg"):
    """
    将原图中的文本替换为翻译结果
    
    Args:
        original_image_path: 原始图片路径
        translation_json_path: 翻译结果JSON路径
        output_path: 输出图片路径
    """
    
    print("=" * 70)
    print("文本替换渲染")
    print("=" * 70)
    
    # 读取翻译结果
    with open(translation_json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # 兼容两种JSON格式
    if 'results' in data:
        regions = data['results']
    elif 'regions' in data:
        regions = data['regions']
    else:
        print("错误: JSON格式不正确")
        return
    
    # 读取原图
    img = cv2.imread(original_image_path)
    if img is None:
        print(f"错误: 无法读取图像 {original_image_path}")
        return
    
    print(f"\n✓ 原图尺寸: {img.shape[1]} x {img.shape[0]}")
    print(f"✓ 文本区域数: {len(regions)}")
    
    # 转换为PIL格式以便使用中文字体
    img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(img_pil)
    
    # 尝试加载中文字体
    font_paths = [
        "C:\\Windows\\Fonts\\msyh.ttc",      # 微软雅黑
        "C:\\Windows\\Fonts\\simhei.ttf",    # 黑体
        "C:\\Windows\\Fonts\\simsun.ttc",    # 宋体
        "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",  # Linux
        "/System/Library/Fonts/PingFang.ttc"  # macOS
    ]
    
    font_path = None
    for path in font_paths:
        if os.path.exists(path):
            font_path = path
            print(f"✓ 使用字体: {os.path.basename(path)}")
            break
    
    if not font_path:
        print("警告: 未找到中文字体，使用默认字体")
    
    print("\n开始处理文本区域...")
    
    # 处理每个区域
    for idx, region in enumerate(regions):
        bbox = region['boundingBox']
        x, y, w, h = bbox['x'], bbox['y'], bbox['width'], bbox['height']
        
        # 兼容不同的字段名
        translated = region.get('translated', region.get('translatedText', ''))
        original = region.get('original', region.get('text', ''))
        is_vertical = region.get('vertical', False)
        
        print(f"\n区域 {idx + 1}:")
        print(f"  位置: ({x}, {y}), 尺寸: {w}x{h}")
        print(f"  原文: {original}")
        print(f"  译文: {translated}")
        
        if not translated or translated == original:
            print(f"  跳过: 无译文或译文与原文相同")
            continue
        
        # 步骤1: 用白色填充原文区域
        cv2.rectangle(img, (x, y), (x + w, y + h), (255, 255, 255), -1)
        
        # 更新PIL图像
        img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        draw = ImageDraw.Draw(img_pil)
        
        # 步骤2: 固定字体大小为20px，以文本框中心为基准布局
        font_size = 20
        font_size = 20
        
        # 加载字体
        try:
            if font_path:
                font = ImageFont.truetype(font_path, font_size)
            else:
                font = ImageFont.load_default()
        except:
            font = ImageFont.load_default()
            print(f"  警告: 字体加载失败，使用默认字体")
        
        # 步骤3: 计算文本实际需要的空间，必要时扩展文本框
        if is_vertical:
            # 竖排文本 - 多列支持
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
                    # 当前列已满，开始新列
                    if current_column:
                        columns.append(current_column)
                    current_column = [(char, char_w, char_h)]
                    current_column_height = char_h + char_spacing
            
            if current_column:
                columns.append(current_column)
            
            num_columns = len(columns)
            
            # 如果需要多列，扩展文本框宽度
            if num_columns > 1:
                w_new = w + (num_columns - 1) * column_spacing
                print(f"  分为 {num_columns} 列，扩展文本框: {w}x{h} -> {w_new}x{h}")
                cv2.rectangle(img, (x, y), (x + w_new, y + h), (255, 255, 255), -1)
                img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
                draw = ImageDraw.Draw(img_pil)
                w = w_new
            
            # 渲染多列文本（从右到左，日文习惯）
            for col_idx, column in enumerate(columns):
                column_x = x + w - (col_idx * column_spacing) - 15
                current_y = y + padding_top
                
                for char, char_w, char_h in column:
                    char_x = column_x - char_w // 2
                    draw.text((char_x, current_y), char, fill=(0, 0, 0), font=font)
                    current_y += char_h + char_spacing
        else:
            # 横排文本 - 计算实际宽度，自动换行
            padding_left = 5
            padding_right = 5
            padding_top = 5
            line_spacing = 3
            
            available_width = w - padding_left - padding_right
            
            # 自动换行
            lines = []
            current_line = ""
            
            for char in translated:
                test_line = current_line + char
                bbox_test = draw.textbbox((0, 0), test_line, font=font)
                test_w = bbox_test[2] - bbox_test[0]
                
                if test_w <= available_width:
                    current_line = test_line
                else:
                    if current_line:
                        lines.append(current_line)
                    current_line = char
            
            if current_line:
                lines.append(current_line)
            
            # 计算多行文本总高度
            bbox_sample = draw.textbbox((0, 0), "测", font=font)
            line_height = (bbox_sample[3] - bbox_sample[1]) + line_spacing
            total_height = line_height * len(lines) + padding_top * 2
            
            # 如果超出原始高度，向下扩展文本框
            if total_height > h:
                h_new = total_height
                print(f"  扩展文本框: {w}x{h} -> {w}x{h_new}")
                # 重新填充白色背景（扩展后的区域）
                cv2.rectangle(img, (x, y), (x + w, y + h_new), (255, 255, 255), -1)
                img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
                draw = ImageDraw.Draw(img_pil)
                h = h_new
            
            # 绘制多行文本
            start_y = y + padding_top
            
            for i, line in enumerate(lines):
                bbox_line = draw.textbbox((0, 0), line, font=font)
                line_w = bbox_line[2] - bbox_line[0]
                line_x = x + (w - line_w) // 2
                line_y = start_y + i * line_height
                draw.text((line_x, line_y), line, fill=(0, 0, 0), font=font)
        
        # 更新opencv图像
        img = cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)
        
        print(f"  ✓ 已渲染译文")
    
    # 保存结果
    cv2.imwrite(output_path, img)
    
    print(f"\n{'=' * 70}")
    print(f"✓ 翻译图片已生成: {output_path}")
    print(f"{'=' * 70}")
    
    return output_path

if __name__ == '__main__':
    import sys
    
    if len(sys.argv) < 3:
        print("用法: python render_translation.py <原图路径> <翻译JSON路径> [输出路径]")
        print("示例: python render_translation.py image.jpg translation_result.json translated.jpg")
        sys.exit(1)
    
    original_image = sys.argv[1]
    translation_json = sys.argv[2]
    output = sys.argv[3] if len(sys.argv) > 3 else "translated_image.jpg"
    
    if not os.path.exists(original_image):
        print(f"错误: 图像文件不存在: {original_image}")
        sys.exit(1)
    
    if not os.path.exists(translation_json):
        print(f"错误: JSON文件不存在: {translation_json}")
        sys.exit(1)
    
    render_translated_image(original_image, translation_json, output)
