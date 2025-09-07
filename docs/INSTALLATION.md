# Z-RAG 安装指南

## 系统要求

### 最低要求
- **Java**: 8 或更高版本
- **Maven**: 3.6 或更高版本
- **内存**: 2GB 以上
- **磁盘**: 1GB 以上可用空间

### 推荐配置
- **Java**: 8
- **Maven**: 3.8 或更高版本
- **内存**: 4GB 以上
- **磁盘**: 5GB 以上可用空间

## 快速安装

### 1. 检查环境

```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version
```

### 2. 下载项目

```bash
# 克隆项目
git clone https://github.com/james-zou/zrag.git
cd zrag
```

### 3. 编译项目

```bash
# 编译项目
mvn clean compile

# 打包项目
mvn package -DskipTests
```

### 4. 运行项目

```bash
# 使用千问模型（推荐）
./start-qwen.sh

# 使用Ollama本地模型
./start-ollama.sh

# 使用OpenAI模型
./start.sh
```

## 详细安装步骤

### 步骤1: 安装Java 8

#### macOS
```bash
# 使用Homebrew安装
brew install openjdk@8

# 设置环境变量
export JAVA_HOME=/opt/homebrew/opt/openjdk@8
export PATH=$JAVA_HOME/bin:$PATH
```

#### Linux (Ubuntu/Debian)
```bash
# 安装OpenJDK 8
sudo apt update
sudo apt install openjdk-8-jdk

# 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

#### Windows
1. 从 [Oracle官网](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) 下载Java 8
2. 安装并设置环境变量

### 步骤2: 安装Maven

#### macOS
```bash
# 使用Homebrew安装
brew install maven
```

#### Linux
```bash
# Ubuntu/Debian
sudo apt install maven

# CentOS/RHEL
sudo yum install maven
```

#### Windows
1. 从 [Maven官网](https://maven.apache.org/download.cgi) 下载Maven
2. 解压并设置环境变量

### 步骤3: 配置项目

#### 3.1 千问模型配置（推荐）

```bash
# 设置API Key
export QWEN_API_KEY="your-qwen-api-key"

# 编辑配置文件
vim src/main/resources/application.yml
```

```yaml
models:
  qwen:
    api:
      key: your-qwen-api-key
    base:
      url: https://dashscope.aliyuncs.com/api/v1
    model: qwen-turbo
    embedding:
      model: text-embedding-v1

default:
  provider: qwen
```

#### 3.2 Ollama本地模型配置

```bash
# 安装Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# 启动Ollama服务
ollama serve

# 下载模型
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

#### 3.3 OpenAI模型配置

```bash
# 设置API Key
export OPENAI_API_KEY="your-openai-api-key"
```

### 步骤4: 启动服务

#### 使用千问模型
```bash
./start-qwen.sh
```

#### 使用Ollama本地模型
```bash
./start-ollama.sh
```

#### 使用OpenAI模型
```bash
./start.sh
```

## 验证安装

### 1. 检查服务状态

```bash
curl http://localhost:8080/api/rag/status
```

### 2. 测试API

```bash
# 上传文档
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@sample.txt"

# 执行查询
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "测试问题"}'
```

## 常见问题

### Q1: Java版本不匹配
**问题**: `Fatal error compiling: invalid target release: 8`

**解决方案**: 确保使用Java 8或更高版本
```bash
java -version
# 应该显示 1.8.x 或更高版本
```

### Q2: Maven依赖下载失败
**问题**: 依赖下载超时或失败

**解决方案**: 配置Maven镜像
```bash
# 编辑Maven设置
vim ~/.m2/settings.xml
```

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/central</url>
  </mirror>
</mirrors>
```

### Q3: 端口被占用
**问题**: `Port 8080 was already in use`

**解决方案**: 修改端口或停止占用进程
```bash
# 查看端口占用
lsof -i :8080

# 停止进程
kill -9 <PID>

# 或修改端口
vim src/main/resources/application.yml
```

```yaml
server:
  port: 8081
```

### Q4: 内存不足
**问题**: `OutOfMemoryError`

**解决方案**: 增加JVM内存
```bash
export MAVEN_OPTS="-Xmx2g -Xms1g"
mvn spring-boot:run
```

## 获取帮助

如果遇到其他问题，请：

1. 查看项目文档
2. 检查GitHub Issues
3. 联系作者: 18301545237@163.com

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。
