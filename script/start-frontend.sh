#!/bin/bash
# 启动Z-RAG前端演示

echo "=== 启动Z-RAG前端演示 ==="

# 检查Java环境
echo "检查Java环境..."
if ! java -version 2>&1 | grep -q "version \"1.8"; then
    echo "错误: 需要Java 8或更高版本"
    echo "当前Java版本:"
    java -version
    exit 1
fi

# 检查Maven环境
echo "检查Maven环境..."
mvn -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

# 设置Java环境变量（Java 8）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_341.jdk/Contents/Home

echo "使用JAVA_HOME: $JAVA_HOME"

# 编译项目
echo "编译项目..."
mvn clean compile
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 运行应用
echo "启动Z-RAG应用（前端演示）..."
echo "访问地址:"
echo "  主界面: http://localhost:8080"
echo "  演示页面: http://localhost:8080/demo.html"
echo "  API状态: http://localhost:8080/api/rag/status"
echo ""
echo "按 Ctrl+C 停止应用"
echo ""

mvn spring-boot:run
