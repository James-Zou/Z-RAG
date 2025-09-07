#!/bin/bash
# Z-RAG Ollama模型启动脚本

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 设置环境变量
export DEFAULT_PROVIDER="ollama"
export JAVA_HOME=${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home}
export PROJECT_HOME="$PROJECT_DIR"

# 进入项目目录
cd "$PROJECT_DIR"

echo "=== Z-RAG系统启动（Ollama模型） ==="
echo "项目目录: $PROJECT_DIR"

# 检查Ollama服务
echo "检查Ollama服务..."
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "警告: Ollama服务未运行，请先启动Ollama"
    echo "安装: https://ollama.ai/"
    echo "启动: ollama serve"
fi

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请先安装Java 17+"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

# 编译项目
echo "编译项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 运行应用
echo "启动Z-RAG应用（Ollama模型）..."
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止应用"
echo ""

mvn spring-boot:run
