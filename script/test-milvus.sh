#!/bin/bash

# Milvus向量数据库测试脚本
# 测试使用Milvus Java客户端进行向量存储和检索

echo "🚀 开始测试Milvus向量数据库集成..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 启动Milvus服务
echo "📦 启动Milvus服务..."
docker run -d --name milvus-test \
  -p 19530:19530 \
  -p 9091:9091 \
  -e ETCD_USE_EMBED=true \
  -e ETCD_DATA_DIR=/opt/milvus/etcd \
  -e COMMON_STORAGETYPE=local \
  -e COMMON_WALDIR=/opt/milvus/wal \
  -e COMMON_LOGDIR=/opt/milvus/logs \
  -e COMMON_LOG_LEVEL=debug \
  milvusdb/milvus:latest

# 等待Milvus启动
echo "⏳ 等待Milvus启动..."
sleep 30

# 检查Milvus是否启动成功
echo "🔍 检查Milvus服务状态..."
if docker logs milvus-test 2>&1 | grep -q "Milvus is ready"; then
    echo "✅ Milvus服务启动成功"
else
    echo "❌ Milvus服务启动失败"
    docker logs milvus-test
    exit 1
fi

# 编译项目
echo "🔨 编译项目..."
./mvnw clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ 项目编译成功"
else
    echo "❌ 项目编译失败"
    exit 1
fi

# 启动应用
echo "🚀 启动Z-RAG应用..."
./mvnw spring-boot:run &
APP_PID=$!

# 等待应用启动
echo "⏳ 等待应用启动..."
sleep 20

# 测试RAG功能
echo "🧪 测试RAG功能..."

# 测试上传文档
echo "1. 测试上传文档..."
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@src/main/resources/sample-documents/ai-knowledge.txt" \
  -H "Content-Type: multipart/form-data" \
  -s | jq '.'

# 等待一下
sleep 2

# 测试查询
echo "2. 测试RAG查询..."
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是人工智能？", "maxResults": 3}' \
  -s | jq '.'

# 等待一下
sleep 2

# 测试知识库统计
echo "3. 测试知识库统计..."
curl -X GET http://localhost:8080/api/rag/knowledge/stats \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 等待一下
sleep 2

# 测试刷新知识库
echo "4. 测试刷新知识库..."
curl -X POST http://localhost:8080/api/rag/knowledge/refresh \
  -H "Content-Type: application/json" \
  -s | jq '.'

echo ""
echo "🎉 Milvus向量数据库测试完成！"

# 停止应用
echo "🛑 停止应用..."
kill $APP_PID 2>/dev/null

# 清理Docker容器
echo "🧹 清理Docker容器..."
docker stop milvus-test > /dev/null 2>&1
docker rm milvus-test > /dev/null 2>&1

echo "✅ 测试完成，所有资源已清理"
