#!/bin/bash

# Weaviate原生客户端测试脚本
# 测试使用Weaviate Java客户端而不是RestTemplate

echo "🚀 开始测试Weaviate原生客户端集成..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 启动Weaviate服务
echo "📦 启动Weaviate服务..."
docker run -d --name weaviate-test \
  -p 8080:8080 -p 50051:50051 \
  -e QUERY_DEFAULTS_LIMIT=25 \
  -e AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED=true \
  -e PERSISTENCE_DATA_PATH='/var/lib/weaviate' \
  -e DEFAULT_VECTORIZER_MODULE='none' \
  -e ENABLE_MODULES='' \
  -e CLUSTER_HOSTNAME='node1' \
  semitechnologies/weaviate:latest

# 等待Weaviate启动
echo "⏳ 等待Weaviate启动..."
sleep 15

# 检查Weaviate是否启动成功
echo "🔍 检查Weaviate服务状态..."
if curl -s http://localhost:8080/v1/meta > /dev/null; then
    echo "✅ Weaviate服务启动成功"
else
    echo "❌ Weaviate服务启动失败"
    docker logs weaviate-test
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

# 测试Weaviate功能
echo "🧪 测试Weaviate功能..."

# 测试创建类
echo "1. 测试创建Weaviate类..."
curl -X POST http://localhost:8080/api/weaviate/example/create-class \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 等待一下
sleep 2

# 测试添加文档
echo "2. 测试添加文档..."
curl -X POST http://localhost:8080/api/weaviate/example/add-documents \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 等待一下
sleep 2

# 测试搜索
echo "3. 测试搜索相似文档..."
curl -X POST http://localhost:8080/api/weaviate/example/search \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 等待一下
sleep 2

# 测试获取所有文档
echo "4. 测试获取所有文档..."
curl -X GET http://localhost:8080/api/weaviate/example/get-all \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 等待一下
sleep 2

# 测试清空数据
echo "5. 测试清空数据..."
curl -X POST http://localhost:8080/api/weaviate/example/clear \
  -H "Content-Type: application/json" \
  -s | jq '.'

echo ""
echo "🎉 Weaviate原生客户端测试完成！"

# 停止应用
echo "🛑 停止应用..."
kill $APP_PID 2>/dev/null

# 清理Docker容器
echo "🧹 清理Docker容器..."
docker stop weaviate-test > /dev/null 2>&1
docker rm weaviate-test > /dev/null 2>&1

echo "✅ 测试完成，所有资源已清理"
