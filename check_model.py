import torch

model_path = "comictextdetector.pt"
checkpoint = torch.load(model_path, map_location='cpu', weights_only=False)

print("模型文件键值:")
if isinstance(checkpoint, dict):
    for key in checkpoint.keys():
        print(f"  - {key}: {type(checkpoint[key])}")
else:
    print(f"  类型: {type(checkpoint)}")
