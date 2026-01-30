@echo off
echo 安装comic-text-detector依赖...
echo.

pip install wandb -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install onnx onnx-simplifier -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install tqdm torchsummary -i https://pypi.tuna.tsinghua.edu.cn/simple

echo.
echo 安装完成！
pause
