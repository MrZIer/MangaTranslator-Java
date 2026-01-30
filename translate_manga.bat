@echo off
chcp 65001 >nul
echo ============================================================
echo 漫画翻译一键工具
echo 检测 + OCR + 翻译 + 渲染
echo ============================================================
echo.

if "%1"=="" (
    echo 错误: 请提供图像路径
    echo.
    echo 用法: translate_manga.bat "图像路径" [输出路径]
    echo 示例: translate_manga.bat "F:\Manga\chapter-132\002.jpg"
    echo.
    pause
    exit /b 1
)

set INPUT_IMAGE=%1
set OUTPUT_IMAGE=%2

if "%OUTPUT_IMAGE%"=="" (
    set OUTPUT_IMAGE=translated_manga.jpg
)

echo 输入图片: %INPUT_IMAGE%
echo 输出图片: %OUTPUT_IMAGE%
echo.
echo 正在处理，请稍候...
echo.

python translate_manga.py %INPUT_IMAGE% %OUTPUT_IMAGE%

echo.
if exist %OUTPUT_IMAGE% (
    echo ============================================================
    echo 成功! 翻译图片已生成
    echo ============================================================
    echo.
    echo 是否打开查看结果? (Y/N)
    set /p OPEN_FILE=
    if /i "%OPEN_FILE%"=="Y" (
        start %OUTPUT_IMAGE%
    )
) else (
    echo 错误: 生成失败
)

echo.
pause
