import argparse
import json
from manga_ocr import MangaOcr
from PIL import Image

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', required=True)
    parser.add_argument('--model', required=True)
    args = parser.parse_args()

    # 加载模型
    mocr = MangaOcr()
    
    # 处理图像
    image = Image.open(args.image)
    text = mocr(image)

    # 返回JSON结果
    result = [{
        'text': text,
        'language': 'ja',
        'confidence': 0.95,
        'boundingBox': {
            'x': 0,
            'y': 0,
            'width': image.width,
            'height': image.height
        }
    }]

    print(json.dumps(result))

if __name__ == '__main__':
    main()