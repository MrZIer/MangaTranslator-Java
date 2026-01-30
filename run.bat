@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   MangaTrans 一键启动脚本 (Windows)
echo ========================================
echo.

REM 设置颜色
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "NC=[0m"

REM ============ 1. 检查Java环境 ============
echo [1/7] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%❌ 未检测到Java环境%NC%
    echo.
    echo 请安装Java 17或更高版本：
    echo 下载地址: https://adoptium.net/temurin/releases/
    echo.
    pause
    exit /b 1
) else (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
    )
    echo %GREEN%✓ Java已安装: !JAVA_VERSION!%NC%
)

REM ============ 2. 检查Python环境 ============
echo.
echo [2/7] 检查Python环境...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%❌ 未检测到Python环境%NC%
    echo.
    echo 请安装Python 3.8或更高版本：
    echo 下载地址: https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
) else (
    for /f "tokens=2" %%g in ('python --version 2^>^&1') do (
        set PYTHON_VERSION=%%g
    )
    echo %GREEN%✓ Python已安装: !PYTHON_VERSION!%NC%
)

REM ============ 3. 检查MongoDB ============
echo.
echo [3/7] 检查MongoDB...
sc query MongoDB >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%✓ MongoDB服务已安装%NC%
    sc query MongoDB | find "RUNNING" >nul
    if %errorlevel% neq 0 (
        echo %YELLOW%⚠ MongoDB未运行，正在启动...%NC%
        net start MongoDB >nul 2>&1
        if %errorlevel% equ 0 (
            echo %GREEN%✓ MongoDB已启动%NC%
        ) else (
            echo %YELLOW%⚠ 无法启动MongoDB服务，请手动启动%NC%
        )
    ) else (
        echo %GREEN%✓ MongoDB正在运行%NC%
    )
) else (
    echo %YELLOW%⚠ MongoDB未安装，将使用Docker启动（如果可用）%NC%
    docker --version >nul 2>&1
    if %errorlevel% equ 0 (
        echo 正在启动MongoDB Docker容器...
        docker ps | find "mongodb-mangatrans" >nul
        if %errorlevel% neq 0 (
            docker run -d --name mongodb-mangatrans -p 27017:27017 mongo:4.4
            echo %GREEN%✓ MongoDB容器已启动%NC%
        ) else (
            echo %GREEN%✓ MongoDB容器已运行%NC%
        )
    ) else (
        echo %RED%❌ MongoDB和Docker都未安装%NC%
        echo 请安装MongoDB或Docker：
        echo MongoDB: https://www.mongodb.com/try/download/community
        echo Docker: https://www.docker.com/products/docker-desktop
        echo.
        pause
        exit /b 1
    )
)

REM ============ 4. 创建Python虚拟环境 ============
echo.
echo [4/7] 配置Python环境...
if not exist "venv" (
    echo 创建Python虚拟环境...
    python -m venv venv
    if %errorlevel% neq 0 (
        echo %RED%❌ 虚拟环境创建失败%NC%
        pause
        exit /b 1
    )
    echo %GREEN%✓ 虚拟环境创建成功%NC%
) else (
    echo %GREEN%✓ 虚拟环境已存在%NC%
)

REM 激活虚拟环境
call venv\Scripts\activate.bat

REM ============ 5. 安装Python依赖 ============
echo.
echo [5/7] 安装Python依赖...
if not exist "venv\Lib\site-packages\manga_ocr" (
    echo 安装依赖包（首次运行可能需要几分钟）...
    pip install --quiet --upgrade pip
    pip install --quiet torch torchvision --index-url https://download.pytorch.org/whl/cu118
    pip install --quiet manga-ocr Pillow requests opencv-python numpy
    if %errorlevel% neq 0 (
        echo %RED%❌ 依赖安装失败%NC%
        pause
        exit /b 1
    )
    echo %GREEN%✓ 依赖安装完成%NC%
) else (
    echo %GREEN%✓ 依赖已安装%NC%
)

REM ============ 6. 下载AI模型 ============
echo.
echo [6/7] 检查AI模型...
if not exist "comictextdetector.pt" (
    echo %YELLOW%⚠ 检测模型未找到，正在下载（约900MB，请耐心等待）...%NC%
    echo 下载地址: https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt
    echo.
    echo 提示: 如果下载失败，请手动下载并放置到项目根目录
    echo.
    
    REM 尝试使用curl下载
    curl --version >nul 2>&1
    if %errorlevel% equ 0 (
        curl -L -o comictextdetector.pt https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt
        if %errorlevel% equ 0 (
            echo %GREEN%✓ 模型下载完成%NC%
        ) else (
            echo %RED%❌ 模型下载失败，请手动下载%NC%
        )
    ) else (
        echo %YELLOW%⚠ 请手动下载模型文件%NC%
    )
) else (
    echo %GREEN%✓ 检测模型已存在%NC%
)

REM ============ 7. 启动应用 ============
echo.
echo [7/7] 启动MangaTrans应用...
echo.
echo %GREEN%========================================%NC%
echo %GREEN%  应用启动中，请稍候...%NC%
echo %GREEN%========================================%NC%
echo.
echo 访问地址: http://localhost:8080
echo 按 Ctrl+C 停止应用
echo.

REM 检查端口占用
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo %YELLOW%⚠ 端口8080已被占用，正在尝试停止...%NC%
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 >nul
)

REM 启动Spring Boot应用
if exist "gradlew.bat" (
    call gradlew.bat bootRun
) else (
    echo %RED%❌ gradlew.bat 未找到%NC%
    pause
    exit /b 1
)

REM 保持窗口打开
pause
