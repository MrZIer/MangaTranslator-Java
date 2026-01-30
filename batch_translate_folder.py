#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量翻译文件夹内的所有漫画图片
"""

import os
import sys
from pathlib import Path
from translate_manga import complete_translation_pipeline

def batch_translate_folder(input_folder, output_folder=None):
    """
    批量翻译文件夹内的所有图片
    
    Args:
        input_folder: 输入文件夹路径
        output_folder: 输出文件夹路径，如果为None则在输入文件夹下创建translated子文件夹
    """
    input_path = Path(input_folder)
    
    if not input_path.exists() or not input_path.is_dir():
        print(f"错误: 输入文件夹不存在: {input_folder}")
        return
    
    # 确定输出文件夹
    if output_folder is None:
        output_path = input_path / "translated"
    else:
        output_path = Path(output_folder)
    
    # 创建输出文件夹
    output_path.mkdir(parents=True, exist_ok=True)
    
    # 支持的图片格式
    image_extensions = {'.jpg', '.jpeg', '.png', '.bmp', '.webp'}
    
    # 获取所有图片文件
    image_files = []
    for ext in image_extensions:
        image_files.extend(input_path.glob(f"*{ext}"))
        image_files.extend(input_path.glob(f"*{ext.upper()}"))
    
    # 按文件名排序
    image_files = sorted(image_files, key=lambda x: x.name)
    
    if not image_files:
        print(f"错误: 在 {input_folder} 中没有找到图片文件")
        return
    
    print("=" * 70)
    print("批量漫画翻译")
    print("=" * 70)
    print(f"输入文件夹: {input_path}")
    print(f"输出文件夹: {output_path}")
    print(f"找到 {len(image_files)} 个图片文件")
    print("=" * 70)
    
    # 处理每个图片
    success_count = 0
    failed_files = []
    
    # 获取API密钥
    api_key = os.environ.get('ZHIPU_API_KEY', 'af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit')
    
    for idx, image_file in enumerate(image_files, 1):
        print(f"\n[{idx}/{len(image_files)}] 处理: {image_file.name}")
        print("-" * 70)
        
        # 生成输出文件名
        output_file = output_path / image_file.name
        
        try:
            # 调用翻译函数
            complete_translation_pipeline(str(image_file), api_key, str(output_file))
            success_count += 1
            print(f"✓ 成功: {output_file.name}")
        except Exception as e:
            print(f"✗ 失败: {image_file.name}")
            print(f"  错误: {str(e)}")
            failed_files.append(image_file.name)
    
    # 打印总结
    print("\n" + "=" * 70)
    print("批量处理完成")
    print("=" * 70)
    print(f"总计: {len(image_files)} 个文件")
    print(f"成功: {success_count} 个")
    print(f"失败: {len(failed_files)} 个")
    
    if failed_files:
        print("\n失败的文件:")
        for filename in failed_files:
            print(f"  - {filename}")
    
    print(f"\n翻译结果已保存到: {output_path}")
    print("=" * 70)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("使用方法:")
        print("  python batch_translate_folder.py <输入文件夹> [输出文件夹]")
        print("\n示例:")
        print("  python batch_translate_folder.py \"F:\\Manga\\chapter-132\"")
        print("  python batch_translate_folder.py \"F:\\Manga\\chapter-132\" \"F:\\Manga\\chapter-132-cn\"")
        sys.exit(1)
    
    input_folder = sys.argv[1]
    output_folder = sys.argv[2] if len(sys.argv) > 2 else None
    
    batch_translate_folder(input_folder, output_folder)
