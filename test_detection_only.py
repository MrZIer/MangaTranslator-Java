#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试comic-text-detector文本检测功能
只做文本检测，不做OCR
"""

import sys
import os
import json
import cv2

# 添加comic-text-detector到Python路径
CTD_PATH = os.path.join(os.path.dirname(__file__), 'comic-text-detector')
if os.path.exists(CTD_PATH):
    sys.path.insert(0, CTD_PATH)
else:
    print(f"错误: 找不到comic-text-detector目录: {CTD_PATH}")
    sys.exit(1)

def test_detection(image_path):
    """测试文本检测"""
    try:
        import torch
        
        # 切换到comic-text-detector目录执行导入
        original_dir = os.getcwd()
        os.chdir(CTD_PATH)
        
        from inference import TextDetector
        from utils.textmask import REFINEMASK_INPAINT
        
        # 切换回原目录
        os.chdir(original_dir)
        
        print("=" * 60)
        print("文本检测测试")
        print("=" * 60)
        
        # 检查模型文件
        model_path = os.path.join(os.path.dirname(__file__), 'comictextdetector.pt')
        if not os.path.exists(model_path):
            print(f"错误: 模型文件不存在: {model_path}")
            return
        
        print(f"✓ 模型文件: {model_path}")
        print(f"✓ 模型大小: {os.path.getsize(model_path) / 1024 / 1024:.1f} MB")
        
        # 检查CUDA
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        print(f"✓ 使用设备: {device}")
        if device == 'cuda':
            print(f"  GPU: {torch.cuda.get_device_name(0)}")
        
        # 加载检测器
        print("\n加载文本检测器...")
        detector = TextDetector(
            model_path=model_path,
            input_size=1024,
            device=device,
            conf_thresh=0.4,
            nms_thresh=0.35
        )
        print("✓ 检测器加载成功")
        
        # 读取图像
        print(f"\n读取图像: {image_path}")
        img = cv2.imread(image_path)
        if img is None:
            print(f"错误: 无法读取图像")
            return
        
        img_h, img_w = img.shape[:2]
        print(f"✓ 图像尺寸: {img_w} x {img_h}")
        
        # 执行检测
        print("\n开始文本检测...")
        mask, mask_refined, blk_list = detector(
            img, 
            refine_mode=REFINEMASK_INPAINT, 
            keep_undetected_mask=False
        )
        
        print(f"\n{'=' * 60}")
        print(f"检测结果: 发现 {len(blk_list)} 个文本区域")
        print(f"{'=' * 60}")
        
        # 显示每个区域的详细信息
        regions = []
        for idx, blk in enumerate(blk_list):
            x1, y1, x2, y2 = blk.xyxy
            width = x2 - x1
            height = y2 - y1
            area = width * height
            
            region_info = {
                "id": idx + 1,
                "boundingBox": {
                    "x": int(x1),
                    "y": int(y1),
                    "width": int(width),
                    "height": int(height)
                },
                "area": int(area),
                "vertical": bool(blk.vertical) if hasattr(blk, 'vertical') else False,
                "fontSize": int(blk.font_size) if hasattr(blk, 'font_size') and blk.font_size > 0 else None
            }
            
            print(f"\n区域 {idx + 1}:")
            print(f"  位置: ({int(x1)}, {int(y1)}) -> ({int(x2)}, {int(y2)})")
            print(f"  尺寸: {int(width)} x {int(height)} (面积: {int(area)} 像素)")
            print(f"  方向: {'竖排' if region_info['vertical'] else '横排'}")
            if region_info['fontSize']:
                print(f"  字体大小: {region_info['fontSize']}")
            
            regions.append(region_info)
        
        # 保存检测结果为JSON
        result = {
            "imageWidth": img_w,
            "imageHeight": img_h,
            "regionsCount": len(regions),
            "regions": regions
        }
        
        output_file = "detection_result.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        
        print(f"\n✓ 检测结果已保存到: {output_file}")
        
        # 可视化检测结果（可选）
        try:
            output_img = img.copy()
            for idx, blk in enumerate(blk_list):
                x1, y1, x2, y2 = [int(v) for v in blk.xyxy]
                color = (0, 255, 0)  # 绿色
                cv2.rectangle(output_img, (x1, y1), (x2, y2), color, 2)
                cv2.putText(output_img, str(idx + 1), (x1, y1 - 10), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.9, color, 2)
            
            output_img_path = "detection_visualization.jpg"
            cv2.imwrite(output_img_path, output_img)
            print(f"✓ 可视化结果已保存到: {output_img_path}")
        except Exception as e:
            print(f"警告: 可视化保存失败: {e}")
        
        print(f"\n{'=' * 60}")
        print("测试完成！")
        print(f"{'=' * 60}")
        
    except ImportError as e:
        print(f"\n错误: 导入失败")
        print(f"详细信息: {e}")
        print(f"\n请确保已安装所有依赖:")
        print("  pip install torch torchvision opencv-python")
    except Exception as e:
        import traceback
        print(f"\n错误: {e}")
        print("\n详细错误信息:")
        traceback.print_exc()

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python test_detection_only.py <图像路径>")
        print("示例: python test_detection_only.py F:\\Manga\\chapter-132\\002.jpg")
        sys.exit(1)
    
    image_path = sys.argv[1]
    if not os.path.exists(image_path):
        print(f"错误: 图像文件不存在: {image_path}")
        sys.exit(1)
    
    test_detection(image_path)
