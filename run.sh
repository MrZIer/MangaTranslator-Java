#!/bin/bash

# 设置颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================"
echo "  MangaTrans 一键启动脚本 (Linux/Mac)"
echo "========================================"
echo ""

# 检测操作系统
OS="$(uname -s)"
case "${OS}" in
    Linux*)     MACHINE=Linux;;
    Darwin*)    MACHINE=Mac;;
    *)          MACHINE="UNKNOWN:${OS}"
esac
echo "检测到操作系统: ${MACHINE}"
echo ""

# ============ 1. 检查Java环境 ============
echo "[1/7] 检查Java环境..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✓ Java已安装: ${JAVA_VERSION}${NC}"
    
    # 检查Java版本是否>=17
    JAVA_MAJOR_VERSION=$(echo "${JAVA_VERSION}" | cut -d'.' -f1)
    if [ "${JAVA_MAJOR_VERSION}" -lt 17 ]; then
        echo -e "${YELLOW}⚠ Java版本过低，需要17或更高版本${NC}"
    fi
else
    echo -e "${RED}❌ 未检测到Java环境${NC}"
    echo ""
    echo "请安装Java 17或更高版本："
    if [ "${MACHINE}" = "Mac" ]; then
        echo "  brew install openjdk@17"
    else
        echo "  sudo apt-get install openjdk-17-jdk  # Ubuntu/Debian"
        echo "  sudo yum install java-17-openjdk     # CentOS/RHEL"
    fi
    echo ""
    echo "或从以下地址下载:"
    echo "  https://adoptium.net/temurin/releases/"
    exit 1
fi

# ============ 2. 检查Python环境 ============
echo ""
echo "[2/7] 检查Python环境..."
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version | awk '{print $2}')
    echo -e "${GREEN}✓ Python已安装: ${PYTHON_VERSION}${NC}"
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_VERSION=$(python --version | awk '{print $2}')
    echo -e "${GREEN}✓ Python已安装: ${PYTHON_VERSION}${NC}"
    PYTHON_CMD="python"
else
    echo -e "${RED}❌ 未检测到Python环境${NC}"
    echo ""
    echo "请安装Python 3.8或更高版本："
    if [ "${MACHINE}" = "Mac" ]; then
        echo "  brew install python@3.11"
    else
        echo "  sudo apt-get install python3 python3-pip python3-venv  # Ubuntu/Debian"
        echo "  sudo yum install python3 python3-pip                   # CentOS/RHEL"
    fi
    echo ""
    echo "或从以下地址下载:"
    echo "  https://www.python.org/downloads/"
    exit 1
fi

# ============ 3. 检查MongoDB ============
echo ""
echo "[3/7] 检查MongoDB..."
if command -v mongod &> /dev/null; then
    echo -e "${GREEN}✓ MongoDB已安装${NC}"
    
    # 检查MongoDB是否运行
    if pgrep -x "mongod" > /dev/null; then
        echo -e "${GREEN}✓ MongoDB正在运行${NC}"
    else
        echo -e "${YELLOW}⚠ MongoDB未运行，正在启动...${NC}"
        if [ "${MACHINE}" = "Mac" ]; then
            brew services start mongodb-community &> /dev/null || mongod --fork --logpath /usr/local/var/log/mongodb/mongo.log --dbpath /usr/local/var/mongodb &> /dev/null
        else
            sudo systemctl start mongod &> /dev/null || sudo service mongod start &> /dev/null
        fi
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ MongoDB已启动${NC}"
        else
            echo -e "${YELLOW}⚠ 无法启动MongoDB，请手动启动${NC}"
        fi
    fi
else
    echo -e "${YELLOW}⚠ MongoDB未安装，尝试使用Docker启动...${NC}"
    
    if command -v docker &> /dev/null; then
        # 检查容器是否已存在
        if docker ps -a | grep -q mongodb-mangatrans; then
            docker start mongodb-mangatrans &> /dev/null
            echo -e "${GREEN}✓ MongoDB容器已启动${NC}"
        else
            docker run -d --name mongodb-mangatrans -p 27017:27017 mongo:4.4
            echo -e "${GREEN}✓ MongoDB容器已创建并启动${NC}"
        fi
    else
        echo -e "${RED}❌ MongoDB和Docker都未安装${NC}"
        echo ""
        echo "请安装MongoDB或Docker："
        if [ "${MACHINE}" = "Mac" ]; then
            echo "  brew tap mongodb/brew"
            echo "  brew install mongodb-community@4.4"
            echo "或："
            echo "  brew install --cask docker"
        else
            echo "MongoDB: https://www.mongodb.com/docs/manual/installation/"
            echo "Docker: https://docs.docker.com/engine/install/"
        fi
        exit 1
    fi
fi

# ============ 4. 创建Python虚拟环境 ============
echo ""
echo "[4/7] 配置Python环境..."
if [ ! -d "venv" ]; then
    echo "创建Python虚拟环境..."
    ${PYTHON_CMD} -m venv venv
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ 虚拟环境创建失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 虚拟环境创建成功${NC}"
else
    echo -e "${GREEN}✓ 虚拟环境已存在${NC}"
fi

# 激活虚拟环境
source venv/bin/activate

# ============ 5. 安装Python依赖 ============
echo ""
echo "[5/7] 安装Python依赖..."
if [ ! -f "venv/lib/python*/site-packages/manga_ocr/__init__.py" ] && \
   [ ! -f "venv/lib/python*/site-packages/manga_ocr.py" ]; then
    echo "安装依赖包（首次运行可能需要几分钟）..."
    
    pip install --quiet --upgrade pip
    
    # 根据系统选择合适的PyTorch版本
    if [ "${MACHINE}" = "Mac" ]; then
        # Mac使用CPU版本
        pip install --quiet torch torchvision
    else
        # Linux尝试使用CUDA版本
        if command -v nvidia-smi &> /dev/null; then
            echo "检测到NVIDIA GPU，安装CUDA版本..."
            pip install --quiet torch torchvision --index-url https://download.pytorch.org/whl/cu118
        else
            echo "安装CPU版本..."
            pip install --quiet torch torchvision --index-url https://download.pytorch.org/whl/cpu
        fi
    fi
    
    pip install --quiet manga-ocr Pillow requests opencv-python numpy
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ 依赖安装失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 依赖安装完成${NC}"
else
    echo -e "${GREEN}✓ 依赖已安装${NC}"
fi

# ============ 6. 下载AI模型 ============
echo ""
echo "[6/7] 检查AI模型..."
if [ ! -f "comictextdetector.pt" ]; then
    echo -e "${YELLOW}⚠ 检测模型未找到，正在下载（约900MB，请耐心等待）...${NC}"
    echo "下载地址: https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt"
    echo ""
    
    if command -v curl &> /dev/null; then
        curl -L -o comictextdetector.pt https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 模型下载完成${NC}"
        else
            echo -e "${RED}❌ 模型下载失败${NC}"
            echo "请手动下载模型文件并放置到项目根目录"
            echo ""
        fi
    elif command -v wget &> /dev/null; then
        wget -O comictextdetector.pt https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 模型下载完成${NC}"
        else
            echo -e "${RED}❌ 模型下载失败${NC}"
            echo "请手动下载模型文件并放置到项目根目录"
            echo ""
        fi
    else
        echo -e "${YELLOW}⚠ 未找到curl或wget，请手动下载模型文件${NC}"
    fi
else
    echo -e "${GREEN}✓ 检测模型已存在${NC}"
fi

# ============ 7. 启动应用 ============
echo ""
echo "[7/7] 启动MangaTrans应用..."
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  应用启动中，请稍候...${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止应用"
echo ""

# 检查端口占用
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ 端口8080已被占用，正在尝试停止...${NC}"
    lsof -ti:8080 | xargs kill -9 2>/dev/null
    sleep 2
fi

# 给予gradlew执行权限
chmod +x gradlew

# 启动Spring Boot应用
if [ -f "gradlew" ]; then
    ./gradlew bootRun
else
    echo -e "${RED}❌ gradlew 未找到${NC}"
    exit 1
fi
