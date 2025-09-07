# Z-RAG 部署指南

## 打包说明

Z-RAG项目支持多种打包方式，可以生成完整的发布包。

### 1. 自动打包

使用提供的打包脚本：

```bash
# 执行打包
./package.sh
```

这将生成 `target/zrag-1.0-SNAPSHOT.tar.gz` 发布包。

### 2. 手动打包

```bash
# 清理和编译
mvn clean compile

# 运行测试
mvn test

# 打包JAR
mvn package

# 创建发布包
mvn assembly:single
```

## 发布包结构

```
zrag-1.0-SNAPSHOT/
├── bin/                    # 启动脚本
│   ├── start.sh           # 统一启动脚本
│   ├── start-qwen.sh      # 千问模型启动脚本
│   ├── start-ollama.sh    # Ollama模型启动脚本
│   ├── start-minio.sh     # MinIO存储启动脚本
│   └── test-rag.sh        # 测试脚本
├── config/                 # 配置文件
│   ├── application.yml    # 主配置文件
│   ├── sample-documents/  # 示例文档
│   └── maven-settings.xml # Maven配置
├── docs/                  # 文档
│   ├── README.md
│   ├── LICENSE
│   ├── NOTICE
│   ├── CONTRIBUTING.md
│   ├── CHANGELOG.md
│   └── *.md
├── lib/                   # 依赖库
│   └── *.jar
└── logs/                  # 日志目录（运行时创建）
```

## 部署步骤

### 1. 解压发布包

```bash
# 解压到目标目录
tar -xzf zrag-1.0-SNAPSHOT.tar.gz
cd zrag-1.0-SNAPSHOT
```

### 2. 配置环境

#### 2.1 设置Java环境

```bash
# 设置JAVA_HOME
export JAVA_HOME=/path/to/java17

# 或者编辑启动脚本
vim bin/start.sh
```

#### 2.2 配置应用参数

```bash
# 编辑配置文件
vim config/application.yml

# 设置环境变量
export QWEN_API_KEY="your-qwen-api-key"
export OPENAI_API_KEY="your-openai-api-key"
```

### 3. 启动服务

#### 3.1 基础启动

```bash
# 使用默认配置启动
./bin/start.sh
```

#### 3.2 千问模型启动

```bash
# 使用千问模型启动
export QWEN_API_KEY="your-qwen-api-key"
./bin/start-qwen.sh
```

#### 3.3 Ollama模型启动

```bash
# 先启动Ollama服务
ollama serve

# 使用Ollama模型启动
./bin/start-ollama.sh
```

#### 3.4 MinIO存储启动

```bash
# 先启动MinIO服务
minio server ./minio-data --console-address ":9001"

# 使用MinIO存储启动
./bin/start-minio.sh
```

### 4. 验证部署

```bash
# 运行测试脚本
./bin/test-rag.sh
```

## 环境要求

### 系统要求

- **操作系统**: Linux, macOS, Windows
- **Java版本**: 17+
- **内存**: 最少2GB，推荐4GB+
- **磁盘空间**: 最少1GB

### 依赖服务

#### 可选依赖

1. **Ollama** (本地模型)
   - 安装: https://ollama.ai/
   - 启动: `ollama serve`

2. **MinIO** (对象存储)
   - 安装: `brew install minio/stable/minio`
   - 启动: `minio server ./minio-data --console-address ":9001"`

3. **Milvus** (向量数据库)
   - 安装: https://milvus.io/docs/install_standalone-docker.md
   - 启动: `docker run -d --name milvus -p 19530:19530 milvusdb/milvus:latest`

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JAVA_HOME` | Java安装路径 | 自动检测 |
| `QWEN_API_KEY` | 千问API密钥 | 无 |
| `OPENAI_API_KEY` | OpenAI API密钥 | 无 |
| `MINIO_ENDPOINT` | MinIO服务地址 | http://localhost:9000 |
| `MINIO_ACCESS_KEY` | MinIO访问密钥 | minioadmin |
| `MINIO_SECRET_KEY` | MinIO秘密密钥 | minioadmin |

### 配置文件

主要配置文件位于 `config/application.yml`：

```yaml
# 服务器配置
server:
  port: 8080

# 模型配置
models:
  qwen:
    api:
      key: ${QWEN_API_KEY:}
    base:
      url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
    model: ${QWEN_MODEL:qwen-turbo}

# 存储配置
storage:
  type: ${STORAGE_TYPE:minio}
  minio:
    bucket: ${MINIO_BUCKET_NAME:zrag-documents}
    prefix: ${MINIO_PREFIX:documents/}
```

## 故障排除

### 常见问题

1. **Java版本错误**
   ```
   错误: 需要Java 17或更高版本
   解决: 安装Java 17并设置JAVA_HOME
   ```

2. **端口占用**
   ```
   错误: Port 8080 was already in use
   解决: 修改config/application.yml中的server.port
   ```

3. **API密钥未配置**
   ```
   警告: 未设置QWEN_API_KEY环境变量
   解决: 设置相应的API密钥环境变量
   ```

4. **依赖服务未启动**
   ```
   警告: Ollama服务未运行
   解决: 启动相应的依赖服务
   ```

### 日志查看

```bash
# 查看应用日志
tail -f logs/application.log

# 查看错误日志
grep ERROR logs/application.log
```

## 性能优化

### 1. JVM参数优化

```bash
# 在启动脚本中添加JVM参数
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
java $JAVA_OPTS -jar lib/zrag-1.0-SNAPSHOT.jar
```

### 2. 数据库优化

- 调整向量数据库连接池大小
- 优化索引配置
- 调整缓存设置

### 3. 网络优化

- 调整HTTP连接超时
- 优化文件上传大小限制
- 配置CDN加速

## 监控和维护

### 1. 健康检查

```bash
# 检查服务状态
curl http://localhost:8080/api/rag/health

# 检查系统状态
curl http://localhost:8080/api/rag/status
```

### 2. 性能监控

- 监控CPU和内存使用率
- 监控磁盘I/O
- 监控网络连接数

### 3. 日志管理

- 定期清理日志文件
- 配置日志轮转
- 设置日志级别

## 升级指南

### 1. 备份数据

```bash
# 备份配置文件
cp -r config config.backup

# 备份数据目录
cp -r storage storage.backup
```

### 2. 停止服务

```bash
# 停止应用
pkill -f "zrag"

# 停止依赖服务
docker stop milvus
minio server stop
```

### 3. 更新应用

```bash
# 解压新版本
tar -xzf zrag-new-version.tar.gz

# 恢复配置
cp config.backup/* zrag-new-version/config/

# 启动服务
cd zrag-new-version
./bin/start.sh
```

## 支持

如有问题，请参考：

- [项目文档](docs/)
- [问题报告](https://github.com/james-zou/zrag/issues)
- [讨论区](https://github.com/james-zou/zrag/discussions)

或联系：18301545237@163.com
