#!/bin/bash
# Z-RAG测试脚本

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 设置环境变量
export PROJECT_HOME="$PROJECT_DIR"

# 进入项目目录
cd "$PROJECT_DIR"

echo "=== Z-RAG系统测试 ==="
echo "项目目录: $PROJECT_DIR"

# 检查服务是否运行
echo "检查Z-RAG服务状态..."
if ! curl -s http://localhost:8080/api/rag/health > /dev/null; then
    echo "错误: Z-RAG服务未运行，请先启动服务"
    echo "启动命令: ./bin/start.sh"
    exit 1
fi

echo "Z-RAG服务运行正常"

# 测试API接口
echo ""
echo "=== 测试API接口 ==="

# 测试健康检查
echo "1. 测试健康检查..."
curl -s http://localhost:8080/api/rag/health
echo ""

# 测试系统状态
echo "2. 测试系统状态..."
curl -s http://localhost:8080/api/rag/status
echo ""

# 测试文档上传
echo "3. 测试文档上传..."
if [ -f "config/sample-documents/ai-knowledge.txt" ]; then
    curl -X POST http://localhost:8080/api/rag/upload-text \
        -H "Content-Type: text/plain" \
        -d "人工智能（AI）是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。"
    echo ""
else
    echo "示例文档不存在，跳过上传测试"
fi

# 测试查询
echo "4. 测试查询..."
curl -X POST http://localhost:8080/api/rag/query \
    -H "Content-Type: application/json" \
    -d '{"query": "什么是人工智能？", "maxResults": 3, "minScore": 0.7}'
echo ""

echo "=== 测试完成 ==="
