@echo off
chcp 65001 >nul
echo ============================================================
echo 漫画翻译完整流程测试
echo ============================================================
echo.

set ZHIPU_API_KEY=af2e42eada4b428b952079ebfb4ba2a9.K2kNodV0dMq04rit

if "%1"=="" (
    echo 错误: 请提供图像路径
    echo.
    echo 用法: test_full_workflow.bat "图像路径"
    echo 示例: test_full_workflow.bat "F:\Manga\chapter-132\002.jpg"
    pause
    exit /b 1
)

echo 测试图像: %1
echo.
echo [选项] 选择测试模式:
echo   1. 快速测试 (前3个区域 - 推荐首次测试)
echo   2. 完整测试 (所有区域 + 翻译)
echo   3. 仅检测和OCR (不翻译)
echo.

set /p choice="请选择 (1-3): "

if "%choice%"=="1" (
    echo.
    echo 运行快速测试...
    python quick_test.py %1
) else if "%choice%"=="2" (
    echo.
    echo 运行完整测试 (包含翻译)...
    python python_ocr_detector.py %1 --translate
) else if "%choice%"=="3" (
    echo.
    echo 运行OCR测试 (不翻译)...
    python python_ocr_detector.py %1
) else (
    echo 无效选择
)

echo.
pause
