# 🚀 MangaTrans 安装指南

本文档提供详细的安装和运行说明。

---

## 方式一：一键启动（推荐）⚡

### Windows

```bash
# 1. 克隆项目
git clone https://github.com/MrZIer/MangaTranslator-Java.git
cd MangaTranslator-Java

# 2. 运行启动脚本
run.bat
```

### Linux/Mac

```bash
# 1. 克隆项目
git clone https://github.com/MrZIer/MangaTranslator-Java.git
cd MangaTranslator-Java

# 2. 给予执行权限
chmod +x run.sh

# 3. 运行启动脚本
./run.sh
```

### 启动脚本功能

启动脚本会自动完成以下任务：

1. ✅ **环境检测**
   - 检查Java 17+是否安装
   - 检查Python 3.8+是否安装
   - 检查MongoDB是否安装/运行

2. ✅ **MongoDB启动**
   - 如已安装：自动启动MongoDB服务
   - 如未安装：尝试使用Docker启动MongoDB容器

3. ✅ **Python环境配置**
   - 创建虚拟环境（venv）
   - 安装所有Python依赖
   - 配置PyTorch（自动选择CPU/CUDA版本）

4. ✅ **AI模型下载**
   - 自动下载comic-text-detector模型（约900MB）
   - manga-ocr模型首次运行时自动下载

5. ✅ **应用启动**
   - 检查并释放8080端口
   - 启动Spring Boot应用
   - 打开浏览器访问界面

### 首次启动预计时间

- **网络良好**: 5-10分钟（主要是下载模型）
- **网络较慢**: 15-30分钟
- **后续启动**: 30-60秒

---

## 方式二：手动安装

如果一键脚本遇到问题，可以按以下步骤手动安装。

### 1. 安装Java

#### Windows

1. 下载Java 17+ JDK：https://adoptium.net/temurin/releases/
2. 运行安装程序
3. 配置环境变量（通常自动完成）
4. 验证安装：
   ```bash
   java -version
   ```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

#### Mac

```bash
brew install openjdk@17
java -version
```

### 2. 安装Python

#### Windows

1. 下载Python 3.11：https://www.python.org/downloads/
2. 运行安装程序（勾选"Add Python to PATH"）
3. 验证安装：
   ```bash
   python --version
   ```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install python3 python3-pip python3-venv
python3 --version
```

#### Mac

```bash
brew install python@3.11
python3 --version
```

### 3. 安装MongoDB

#### Windows

1. 下载MongoDB：https://www.mongodb.com/try/download/community
2. 运行安装程序（选择"Complete"安装类型）
3. 安装为Windows服务（自动启动）
4. 验证安装：
   ```bash
   mongod --version
   ```

#### Linux (Ubuntu)

```bash
# 导入MongoDB公钥
wget -qO - https://www.mongodb.org/static/pgp/server-4.4.asc | sudo apt-key add -

# 添加MongoDB源
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/4.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.4.list

# 安装MongoDB
sudo apt update
sudo apt install -y mongodb-org

# 启动MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod
```

#### Mac

```bash
# 安装MongoDB
brew tap mongodb/brew
brew install mongodb-community@4.4

# 启动MongoDB
brew services start mongodb-community@4.4
```

#### 使用Docker（推荐）

如果不想安装MongoDB，可以使用Docker：

```bash
# 安装Docker Desktop
# Windows/Mac: https://www.docker.com/products/docker-desktop
# Linux: https://docs.docker.com/engine/install/

# 启动MongoDB容器
docker run -d --name mongodb-mangatrans -p 27017:27017 mongo:4.4
```

### 4. 配置Python环境

```bash
# 进入项目目录
cd MangaTranslator-Java

# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# 升级pip
pip install --upgrade pip

# 安装PyTorch (根据系统选择)
# CPU版本（适用于Mac或无NVIDIA GPU的Linux/Windows）:
pip install torch torchvision

# CUDA版本（适用于有NVIDIA GPU的Windows/Linux）:
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118

# 安装其他依赖
pip install manga-ocr Pillow requests opencv-python numpy
```

### 5. 下载AI模型

```bash
# 下载comic-text-detector模型（约900MB）
# Windows (使用PowerShell):
Invoke-WebRequest -Uri "https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt" -OutFile "comictextdetector.pt"

# Linux/Mac:
curl -L -o comictextdetector.pt https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt

# 或使用wget:
wget -O comictextdetector.pt https://github.com/dmMaze/comic-text-detector/releases/download/v1.0/comictextdetector.pt
```

**注意**: manga-ocr模型（约400MB）会在首次运行时自动下载到：
- Windows: `C:\Users\你的用户名\.cache\huggingface\hub`
- Linux/Mac: `~/.cache/huggingface/hub`

### 6. 配置应用

编辑 `src/main/resources/application.properties`：

```properties
# 服务器端口
server.port=8080

# MongoDB连接
spring.data.mongodb.uri=mongodb://localhost:27017/manga_trans

# 文件存储路径（修改为你的路径）
file.storage.base-path=./storage

# 智谱AI配置（必须填写）
zhipu.api.key=你的智谱AI API密钥
zhipu.api.url=https://open.bigmodel.cn/api/paas/v4/chat/completions

# 文件上传限制
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### 7. 启动应用

```bash
# Windows:
gradlew.bat bootRun

# Linux/Mac:
./gradlew bootRun
```

启动成功后，访问：**http://localhost:8080**

---

## 常见问题排查

### Java相关

**Q: 提示"java不是内部或外部命令"**

A: Java未正确安装或环境变量未配置。重新安装Java并确保勾选"设置环境变量"。

**Q: Java版本过低**

A: 项目需要Java 17+，请升级Java版本。

### Python相关

**Q: pip安装依赖失败**

A: 尝试以下方法：
```bash
# 使用国内镜像源
pip install -i https://pypi.tuna.tsinghua.edu.cn/simple torch torchvision manga-ocr Pillow requests opencv-python numpy

# 或清华源
pip install -i https://mirrors.aliyun.com/pypi/simple/ [包名]
```

**Q: PyTorch安装失败**

A: 根据系统选择合适的版本：
- **CPU版本**（兼容性最好）：
  ```bash
  pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
  ```
- **CUDA 11.8版本**（需要NVIDIA GPU）：
  ```bash
  pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
  ```

### MongoDB相关

**Q: MongoDB无法启动**

A: 
```bash
# Windows - 检查服务
sc query MongoDB

# 手动启动
net start MongoDB

# Linux/Mac - 检查状态
sudo systemctl status mongod

# 手动启动
sudo systemctl start mongod
```

**Q: 使用Docker替代MongoDB**

A:
```bash
# 启动MongoDB容器
docker run -d --name mongodb-mangatrans -p 27017:27017 mongo:4.4

# 检查容器状态
docker ps

# 查看日志
docker logs mongodb-mangatrans
```

### 模型下载相关

**Q: 模型下载失败或速度慢**

A: 
1. 手动下载模型文件
2. 使用下载工具（IDM、迅雷等）
3. 从镜像站下载（如果有）
4. 放置到项目根目录

**Q: manga-ocr模型下载卡住**

A:
```bash
# 设置Hugging Face镜像
export HF_ENDPOINT=https://hf-mirror.com  # Linux/Mac
set HF_ENDPOINT=https://hf-mirror.com     # Windows CMD
$env:HF_ENDPOINT="https://hf-mirror.com"  # Windows PowerShell
```

### 应用启动相关

**Q: 端口8080已被占用**

A:
```bash
# Windows - 查找并结束进程
netstat -ano | findstr :8080
taskkill /F /PID [进程ID]

# Linux/Mac - 查找并结束进程
lsof -ti:8080 | xargs kill -9
```

**Q: 应用启动后无法访问**

A: 检查防火墙设置，确保8080端口未被阻止。

---

## 获取API密钥

### 智谱AI（推荐）

1. 访问：https://open.bigmodel.cn/
2. 注册/登录账号
3. 进入"API密钥管理"
4. 创建新密钥
5. 复制密钥到配置文件

**新用户福利**: 注册即送免费额度！

### 其他引擎

- **OpenAI**: https://platform.openai.com/api-keys
- **Claude (Anthropic)**: https://console.anthropic.com/
- **DeepSeek**: https://platform.deepseek.com/

---

## 验证安装

启动成功后，进行以下测试：

1. **访问主界面**: http://localhost:8080
2. **上传测试图片**: 选择一张日语漫画图片
3. **检查翻译流程**: 观察进度显示
4. **下载结果**: 验证翻译效果

如果所有步骤正常，安装成功！🎉

---

## 更新项目

```bash
# 进入项目目录
cd MangaTranslator-Java

# 拉取最新代码
git pull

# 重新运行启动脚本
# Windows:
run.bat

# Linux/Mac:
./run.sh
```

---

## 卸载

### 完全删除

```bash
# 1. 停止应用和MongoDB
# Windows:
net stop MongoDB

# Linux/Mac:
sudo systemctl stop mongod

# 2. 删除项目目录
cd ..
rm -rf MangaTranslator-Java

# 3. 删除Python缓存（可选）
rm -rf ~/.cache/huggingface
rm -rf ~/.cache/torch

# 4. 删除MongoDB数据（可选）
# 根据实际安装位置删除
```

---

## 技术支持

如遇到问题，请：

1. 查看 [FAQ文档](#常见问题排查)
2. 搜索 [GitHub Issues](https://github.com/MrZIer/MangaTranslator-Java/issues)
3. 提交新的 Issue（附上错误日志）
4. 加入交流群获取帮助

---

**祝你使用愉快！** 🎉
