#!/bin/bash
# Z-RAG MinIO存储启动脚本

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 设置环境变量
export STORAGE_TYPE="minio"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_ACCESS_KEY="minioadmin"
export MINIO_SECRET_KEY="minioadmin"
export MINIO_BUCKET_NAME="zrag-documents"
export JAVA_HOME=${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home}
export PROJECT_HOME="$PROJECT_DIR"

# 进入项目目录
cd "$PROJECT_DIR"

echo "=== Z-RAG系统启动（MinIO存储） ==="
echo "项目目录: $PROJECT_DIR"

# 检查MinIO服务
echo "检查MinIO服务..."
if ! curl -s http://localhost:9000/minio/health/live > /dev/null; then
    echo "警告: MinIO服务未运行，请先启动MinIO"
    echo "启动命令: minio server ./minio-data --console-address \":9001\""
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
echo "启动Z-RAG应用（MinIO存储）..."
echo "访问地址: http://localhost:8080"
echo "MinIO控制台: http://localhost:9001"
echo "按 Ctrl+C 停止应用"
echo ""

mvn spring-boot:run
