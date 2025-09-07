#!/bin/bash

# Z-RAG Weaviate启动脚本
# 启动Weaviate向量数据库和Z-RAG应用

echo "启动Z-RAG with Weaviate..."

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装，请先安装Docker"
    exit 1
fi

# 启动Weaviate服务
echo "启动Weaviate向量数据库..."
docker run -d \
  --name zrag-weaviate \
  -p 8080:8080 \
  -p 50051:50051 \
  -e QUERY_DEFAULTS_LIMIT=25 \
  -e AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED=true \
  -e PERSISTENCE_DATA_PATH='/var/lib/weaviate' \
  -e DEFAULT_VECTORIZER_MODULE='none' \
  -e ENABLE_MODULES='' \
  -e CLUSTER_HOSTNAME='node1' \
  semitechnologies/weaviate:latest

# 等待Weaviate启动
echo "等待Weaviate启动..."
sleep 10

# 检查Weaviate是否正常运行
if curl -s http://localhost:8080/v1/meta > /dev/null; then
    echo "Weaviate启动成功"
else
    echo "Weaviate启动失败"
    exit 1
fi

# 设置环境变量
export VECTOR_STORE_TYPE=weaviate
export WEAVIATE_HOST=localhost
export WEAVIATE_PORT=8080
export WEAVIATE_SCHEME=http

# 启动Z-RAG应用
echo "启动Z-RAG应用..."
java -jar target/zrag-1.0-SNAPSHOT.jar

echo "Z-RAG with Weaviate启动完成!"
echo "访问地址: http://localhost:8080"
echo "Weaviate管理界面: http://localhost:8080/v1/meta"
