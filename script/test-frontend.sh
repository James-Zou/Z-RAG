#!/bin/bash
# 测试前端功能

echo "=== Z-RAG 前端功能测试 ==="

# 检查服务是否运行
echo "检查服务状态..."
response=$(curl -s http://localhost:8080/api/rag/status 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✅ 服务正在运行"
    echo "响应: $response"
else
    echo "❌ 服务未运行，请先启动服务"
    echo "运行命令: ./start-frontend.sh"
    exit 1
fi

echo ""
echo "测试API接口..."

# 测试系统状态
echo "1. 测试系统状态接口..."
curl -s http://localhost:8080/api/rag/status | jq . 2>/dev/null || echo "响应: $(curl -s http://localhost:8080/api/rag/status)"

# 测试存储状态
echo ""
echo "2. 测试存储状态接口..."
curl -s http://localhost:8080/api/rag/storage/status | jq . 2>/dev/null || echo "响应: $(curl -s http://localhost:8080/api/rag/storage/status)"

# 测试文件列表
echo ""
echo "3. 测试文件列表接口..."
curl -s http://localhost:8080/api/rag/storage/files | jq . 2>/dev/null || echo "响应: $(curl -s http://localhost:8080/api/rag/storage/files)"

# 测试查询接口
echo ""
echo "4. 测试查询接口..."
curl -s -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "测试问题"}' | jq . 2>/dev/null || echo "响应: $(curl -s -X POST http://localhost:8080/api/rag/query -H "Content-Type: application/json" -d '{"query": "测试问题"}')"

# 测试检索接口
echo ""
echo "5. 测试检索接口..."
curl -s "http://localhost:8080/api/rag/retrieve?query=测试" | jq . 2>/dev/null || echo "响应: $(curl -s "http://localhost:8080/api/rag/retrieve?query=测试")"

echo ""
echo "=== 前端页面访问测试 ==="
echo "请在浏览器中访问以下地址："
echo "  主界面: http://localhost:8080"
echo "  演示页面: http://localhost:8080/demo.html"
echo "  API状态: http://localhost:8080/api/rag/status"

echo ""
echo "=== 测试完成 ==="
