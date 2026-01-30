@echo off
REM 一键安装 comic-text-detector 和所需依赖

echo ========================================
echo  漫画文本检测器安装脚本
echo ========================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Python，请先安装Python 3.8+
    pause
    exit /b 1
)

echo [1/4] 克隆 comic-text-detector 仓库...
if exist "comic-text-detector" (
    echo    - 仓库已存在，跳过克隆
) else (
    git clone https://github.com/dmMaze/comic-text-detector.git
    if errorlevel 1 (
        echo [错误] 克隆失败，请检查网络连接
        pause
        exit /b 1
    )
    echo    - 克隆成功
)
echo.

echo [2/4] 安装Python依赖...
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
pip install manga-ocr opencv-python pillow numpy scipy shapely pyclipper
if errorlevel 1 (
    echo [警告] 部分依赖安装失败，请手动检查
)
echo    - 依赖安装完成
echo.

echo [3/4] 检查模型文件...
if exist "comictextdetector.pt" (
    echo    - 模型文件已存在
) else (
    echo [警告] 未找到模型文件 comictextdetector.pt
    echo.
    echo 请手动下载模型文件（约200MB）：
    echo https://github.com/zyddnys/manga-image-translator/releases/tag/beta-0.2.1
    echo.
    echo 下载后放到项目根目录：
    echo %CD%\comictextdetector.pt
    echo.
    pause
)
echo.

echo [4/4] 测试安装...
python -c "import torch; print('PyTorch:', torch.__version__)"
python -c "from manga_ocr import MangaOcr; print('manga-ocr: OK')"
python -c "import cv2; print('OpenCV:', cv2.__version__)"
echo    - 安装验证完成
echo.

echo ========================================
echo  安装完成！
echo ========================================
echo.
echo 使用方法：
echo   python python_ocr_detector.py "图像路径.jpg"
echo.
echo 测试命令：
echo   python python_ocr_detector.py "F:\Manga\chapter-132\002.jpg"
echo.
pause
