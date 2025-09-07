#!/bin/bash
# 带MinIO的Z-RAG启动脚本

echo "=== 启动Z-RAG系统（带MinIO存储） ==="

# 检查MinIO是否运行
echo "检查MinIO服务..."
curl -s http://localhost:9000/minio/health/live > /dev/null
if [ $? -ne 0 ]; then
    echo "MinIO服务未运行，正在启动..."
    ./start-minio.sh &
    sleep 10
fi

# 设置环境变量
export DEFAULT_PROVIDER="qwen"
export STORAGE_TYPE="minio"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_ACCESS_KEY="minioadmin"
export MINIO_SECRET_KEY="minioadmin"
export MINIO_BUCKET_NAME="zrag-documents"

# 检查Java环境
echo "检查Java环境..."
java -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Java环境，请先安装Java 17+"
    exit 1
fi

# 检查Maven环境
echo "检查Maven环境..."
mvn -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

# 设置Java环境变量（Java 17）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 编译项目
echo "编译项目..."
mvn clean compile -s maven-settings.xml
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 运行应用
echo "启动Z-RAG应用（带MinIO存储）..."
echo "访问地址: http://localhost:8080"
echo "MinIO控制台: http://localhost:9001"
echo "MinIO API: http://localhost:9000"
echo "按 Ctrl+C 停止应用"
echo ""

mvn spring-boot:run -s maven-settings.xml
