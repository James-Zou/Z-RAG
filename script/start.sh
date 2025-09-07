#!/bin/bash
# Z-RAG统一启动脚本

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 设置环境变量
export JAVA_HOME=${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home}
export PROJECT_HOME="$PROJECT_DIR"

# 进入项目目录
cd "$PROJECT_DIR"

echo "=== Z-RAG系统启动 ==="
echo "项目目录: $PROJECT_DIR"
echo "Java版本: $(java -version 2>&1 | head -n 1)"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请先安装Java 17+"
    exit 1
fi

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "错误: 需要Java 17或更高版本，当前版本: $JAVA_VERSION"
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
echo "启动Z-RAG应用..."
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止应用"
echo ""

mvn spring-boot:run
