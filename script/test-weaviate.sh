#!/bin/bash

# Weaviate集成测试脚本

echo "开始测试Weaviate集成..."

# 检查Weaviate服务是否运行
echo "检查Weaviate服务状态..."
if curl -s http://localhost:8080/v1/meta > /dev/null; then
    echo "✅ Weaviate服务正在运行"
else
    echo "❌ Weaviate服务未运行，请先启动Weaviate"
    echo "运行命令: docker run -p 8080:8080 -p 50051:50051 -e AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED=true semitechnologies/weaviate:latest"
    exit 1
fi

# 检查Z-RAG应用是否运行
echo "检查Z-RAG应用状态..."
if curl -s http://localhost:8080/api/rag/health > /dev/null; then
    echo "✅ Z-RAG应用正在运行"
else
    echo "❌ Z-RAG应用未运行，请先启动应用"
    echo "运行命令: ./start-weaviate.sh"
    exit 1
fi

# 测试上传文档
echo "测试上传文档..."
response=$(curl -s -X POST -F "file=@src/main/resources/sample-documents/ai-knowledge.txt" http://localhost:8080/api/rag/upload)
if echo "$response" | grep -q "success.*true"; then
    echo "✅ 文档上传成功"
else
    echo "❌ 文档上传失败: $response"
fi

# 测试创建知识库
echo "测试创建知识库..."
response=$(curl -s -X POST http://localhost:8080/api/rag/knowledge/create)
if echo "$response" | grep -q "success.*true"; then
    echo "✅ 知识库创建成功"
else
    echo "❌ 知识库创建失败: $response"
fi

# 测试查询
echo "测试RAG查询..."
response=$(curl -s -X POST -H "Content-Type: application/json" -d '{"query":"什么是人工智能？"}' http://localhost:8080/api/rag/query)
if echo "$response" | grep -q "success.*true"; then
    echo "✅ RAG查询成功"
    echo "查询结果: $response"
else
    echo "❌ RAG查询失败: $response"
fi

# 测试获取知识库统计
echo "测试获取知识库统计..."
response=$(curl -s http://localhost:8080/api/rag/knowledge/stats)
if echo "$response" | grep -q "totalDocuments"; then
    echo "✅ 知识库统计获取成功"
    echo "统计信息: $response"
else
    echo "❌ 知识库统计获取失败: $response"
fi

echo "Weaviate集成测试完成！"
