#!/bin/bash
# Z-RAG打包脚本

echo "=== Z-RAG项目打包 ==="

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

# 设置Java环境变量
export JAVA_HOME=${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home}

# 清理之前的构建
echo "清理之前的构建..."
mvn clean

# 编译项目
echo "编译项目..."
mvn compile
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 运行测试
echo "运行测试..."
mvn test
if [ $? -ne 0 ]; then
    echo "警告: 测试失败，继续打包..."
fi

# 打包项目
echo "打包项目..."
mvn package
if [ $? -ne 0 ]; then
    echo "错误: 项目打包失败"
    exit 1
fi

# 创建发布包
echo "创建发布包..."
mvn assembly:single
if [ $? -ne 0 ]; then
    echo "错误: 发布包创建失败"
    exit 1
fi

# 显示结果
echo ""
echo "=== 打包完成 ==="
echo "发布包位置: target/zrag-1.0-SNAPSHOT.tar.gz"
echo "文件大小: $(du -h target/zrag-1.0-SNAPSHOT.tar.gz | cut -f1)"
echo ""
echo "解压命令: tar -xzf target/zrag-1.0-SNAPSHOT.tar.gz"
echo "启动命令: cd zrag-1.0-SNAPSHOT && ./bin/start.sh"
