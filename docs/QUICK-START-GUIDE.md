# Z-RAG 快速开始指南

## 🚀 5分钟快速体验

### 1. 环境准备
```bash
# 确保已安装Java 8+
java -version

# 确保已安装Maven 3.6+
mvn -version
```

### 2. 一键启动
```bash
# 克隆项目
git clone https://github.com/your-org/z-rag.git
cd z-rag

# 一键启动（使用内存存储，无需额外服务）
./start.sh

# 访问应用
open http://localhost:8080
```

### 3. 快速测试
1. **上传文档**：在界面上传一个PDF或TXT文档
2. **智能问答**：在问答界面输入问题
3. **查看结果**：系统会基于文档内容生成回答

## 🎯 核心优势体验

### 轻量级部署
```bash
# 查看资源占用
ps aux | grep java
# 内存占用通常 < 1GB

# 查看启动时间
time ./start.sh
# 启动时间通常 < 10秒
```

### 专业开发友好
```bash
# 查看源码结构
tree src/main/java/com/unionhole/zrag/

# 查看配置
cat src/main/resources/application.yml

# 查看日志
tail -f logs/zrag.log
```

### 链路透明可控
```bash
# 查看处理流程日志
grep "文档处理" logs/zrag.log
grep "向量检索" logs/zrag.log
grep "智能问答" logs/zrag.log
```

## 🔧 进阶配置

### 使用千问模型（推荐国内用户）
```bash
# 设置API Key
export QWEN_API_KEY="your-qwen-api-key"

# 启动应用
./start-qwen.sh
```

### 使用Milvus向量数据库
```bash
# 启动Milvus服务
docker run -d --name milvus \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:latest

# 启动Z-RAG应用
./start.sh
```

### 使用MinIO文件存储
```bash
# 启动MinIO服务
./start-minio.sh &

# 启动Z-RAG应用
./start-with-minio.sh
```

## 📊 性能测试

### 基准测试
```bash
# 运行性能测试
./test-rag.sh

# 运行Milvus测试
./test-milvus.sh
```

### 压力测试
```bash
# 使用Apache Bench进行压力测试
ab -n 1000 -c 10 http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "测试问题", "maxResults": 3}'
```

## 🛠️ 开发调试

### 查看系统状态
```bash
# 查看API状态
curl http://localhost:8080/api/rag/status

# 查看知识库统计
curl http://localhost:8080/api/rag/knowledge/stats
```

### 调试模式
```bash
# 启用调试日志
export LOG_LEVEL=DEBUG
./start.sh

# 查看详细日志
tail -f logs/zrag.log | grep DEBUG
```

### 性能监控
```bash
# 查看JVM状态
jps -l
jstat -gc <pid>

# 查看内存使用
jmap -histo <pid>
```

## 🎯 常见问题

### Q: 启动失败怎么办？
A: 检查Java版本和端口占用
```bash
# 检查Java版本
java -version

# 检查端口占用
netstat -tlnp | grep 8080
```

### Q: 如何更换模型？
A: 修改配置文件
```yaml
# application.yml
default:
  provider: qwen  # 或 openai, ollama
```

### Q: 如何调整性能？
A: 修改JVM参数
```bash
# 调整内存
export JAVA_OPTS="-Xms512m -Xmx2g"

# 启动应用
./start.sh
```

## 📚 下一步

1. **阅读文档**：查看 [PRODUCT-OVERVIEW.md](PRODUCT-OVERVIEW.md)
2. **了解特性**：查看 [FEATURES.md](FEATURES.md)
3. **对比分析**：查看 [COMPARISON.md](COMPARISON.md)
4. **深入配置**：查看 [INSTALLATION.md](INSTALLATION.md)
5. **API参考**：查看 [API-GUIDE.md](API-GUIDE.md)

## 🎉 开始使用

现在您已经了解了Z-RAG的基本使用方法，可以开始构建您的RAG应用了！

**立即开始使用 Z-RAG，体验专业级RAG系统的强大功能！**
