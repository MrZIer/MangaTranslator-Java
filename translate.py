#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
漫画翻译主入口
自动判断输入是单张图片还是文件夹，并调用相应的处理逻辑
"""

import os
import sys
from pathlib import Path
from translate_manga import complete_translation_pipeline

def main():
    """主函数：智能判断输入类型并处理"""
    
    if len(sys.argv) < 2:
        print("=" * 70)
        print("漫画翻译工具")
        print("=" * 70)
        print("\n使用方法:")
        print("  python translate.py <输入路径...> [--output <输出路径>]")
        print("\n输入路径可以是:")
        print("  1. 单张图片文件")
        print("  2. 多张图片文件")
        print("  3. 包含多张图片的文件夹")
        print("\n示例:")
        print("  # 单张图片")
        print("  python translate.py \"F:\\Manga\\001.jpg\"")
        print("  python translate.py \"F:\\Manga\\001.jpg\" --output \"F:\\Manga\\001_cn.jpg\"")
        print("\n  # 多张图片")
        print("  python translate.py \"F:\\Manga\\001.jpg\" \"F:\\Manga\\002.jpg\" \"F:\\Manga\\003.jpg\"")
        print("  python translate.py \"F:\\Manga\\001.jpg\" \"F:\\Manga\\002.jpg\" --output \"F:\\Manga\\translated\"")
        print("\n  # 整个文件夹")
        print("  python translate.py \"F:\\Manga\\chapter-132\"")
        print("  python translate.py \"F:\\Manga\\chapter-132\" --output \"F:\\Manga\\chapter-132-cn\"")
        print("=" * 70)
        sys.exit(1)
    
    # 解析参数
    args = sys.argv[1:]
    output_path = None
    input_paths = []
    
    # 检查是否有 --output 参数
    if '--output' in args:
        output_idx = args.index('--output')
        if output_idx + 1 < len(args):
            output_path = args[output_idx + 1]
            input_paths = args[:output_idx]
        else:
            print("错误: --output 参数需要指定输出路径")
            sys.exit(1)
    else:
        input_paths = args
    
    if not input_paths:
        print("错误: 未指定输入文件")
        sys.exit(1)
    
    # 获取API密钥
    api_key = os.environ.get('ZHIPU_API_KEY', 'af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit')
    
    # 验证所有输入路径
    valid_paths = []
    for input_path in input_paths:
        path = Path(input_path)
        if not path.exists():
            print(f"警告: 路径不存在，跳过: {input_path}")
            continue
        valid_paths.append(path)
    
    if not valid_paths:
        print("错误: 没有有效的输入路径")
        sys.exit(1)
    
    # 判断处理模式
    if len(valid_paths) == 1 and valid_paths[0].is_dir():
        # 单个文件夹 - 批量处理
        print("=" * 70)
        print("检测到: 文件夹")
        print("=" * 70)
        batch_translate_folder(str(valid_paths[0]), output_path)
    
    elif len(valid_paths) == 1 and valid_paths[0].is_file():
        # 单张图片
        print("=" * 70)
        print("检测到: 单张图片")
        print("=" * 70)
        
        path = valid_paths[0]
        if output_path is None:
            output_file = path.parent / f"{path.stem}_translated{path.suffix}"
        else:
            output_file = Path(output_path)
        
        print(f"输入: {path}")
        print(f"输出: {output_file}")
        print("=" * 70)
        
        try:
            complete_translation_pipeline(str(path), api_key, str(output_file))
            print("\n✓ 翻译完成!")
        except Exception as e:
            print(f"\n✗ 翻译失败: {str(e)}")
            sys.exit(1)
    
    else:
        # 多张图片
        print("=" * 70)
        print("检测到: 多张图片")
        print("=" * 70)
        
        # 过滤出图片文件
        image_files = [p for p in valid_paths if p.is_file()]
        if not image_files:
            print("错误: 没有找到图片文件")
            sys.exit(1)
        
        batch_translate_files(image_files, output_path, api_key)


def batch_translate_files(image_files, output_folder=None, api_key=None):
    """
    批量翻译指定的多个图片文件
    
    Args:
        image_files: 图片文件路径列表（Path对象）
        output_folder: 输出文件夹路径，如果为None则与第一个图片同目录
        api_key: API密钥
    """
    if not image_files:
        print("错误: 没有图片文件")
        return
    
    # 确定输出文件夹
    if output_folder is None:
        output_path = image_files[0].parent / "translated"
    else:
        output_path = Path(output_folder)
    
    # 创建输出文件夹
    output_path.mkdir(parents=True, exist_ok=True)
    
    # 按文件名排序
    image_files = sorted(image_files, key=lambda x: x.name)
    
    print(f"找到 {len(image_files)} 个图片文件")
    print(f"输出文件夹: {output_path}")
    print("=" * 70)
    
    # 处理每个图片
    success_count = 0
    failed_files = []
    
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
    
    print(f"\n所有翻译结果已保存到: {output_path}")
    print("=" * 70)


def batch_translate_folder(input_folder, output_folder=None):
    """
    批量翻译文件夹内的所有图片
    
    Args:
        input_folder: 输入文件夹路径
        output_folder: 输出文件夹路径，如果为None则在输入文件夹下创建translated子文件夹
    """
    input_path = Path(input_folder)
    
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
    
    print(f"\n所有翻译结果已保存到: {output_path}")
    print("=" * 70)


if __name__ == "__main__":
    main()
