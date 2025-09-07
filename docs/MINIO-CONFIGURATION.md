# Z-RAG MinIO存储配置指南

## 概述

Z-RAG系统已集成MinIO对象存储，提供高性能、可扩展的文件存储解决方案。MinIO是一个兼容Amazon S3 API的对象存储服务，适合存储文档、图片、视频等各种类型的文件。

## MinIO特性

### 1. 高性能
- 分布式架构，支持水平扩展
- 优化的网络和存储性能
- 支持并发访问

### 2. 兼容性
- 完全兼容Amazon S3 API
- 支持多种客户端和SDK
- 易于迁移和集成

### 3. 安全性
- 支持访问控制和权限管理
- 支持SSL/TLS加密
- 支持数据加密

## 安装和配置

### 1. 安装MinIO

#### 方式一：使用脚本安装（推荐）

```bash
# 运行MinIO安装脚本
./start-minio.sh
```

#### 方式二：手动安装

```bash
# macOS
brew install minio/stable/minio

# Linux
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio
sudo mv minio /usr/local/bin/

# Windows
# 下载安装包从 https://min.io/download
```

### 2. 启动MinIO服务

```bash
# 创建数据目录
mkdir -p ./minio-data

# 设置环境变量
export MINIO_ROOT_USER=minioadmin
export MINIO_ROOT_PASSWORD=minioadmin

# 启动MinIO
minio server ./minio-data --console-address ":9001"
```

### 3. 访问MinIO

- **API地址**: http://localhost:9000
- **控制台地址**: http://localhost:9001
- **默认用户名**: minioadmin
- **默认密码**: minioadmin

## Z-RAG配置

### 1. 应用配置

```yaml
# application.yml
# MinIO对象存储配置
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket-name: ${MINIO_BUCKET_NAME:zrag-documents}
  auto-create-bucket: true

# 文件存储配置
storage:
  type: ${STORAGE_TYPE:minio}  # 可选: minio, local, memory
  local:
    path: ${LOCAL_STORAGE_PATH:./storage}
  minio:
    bucket: ${MINIO_BUCKET_NAME:zrag-documents}
    prefix: ${MINIO_PREFIX:documents/}
```

### 2. 环境变量配置

```bash
# MinIO配置
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_ACCESS_KEY="minioadmin"
export MINIO_SECRET_KEY="minioadmin"
export MINIO_BUCKET_NAME="zrag-documents"

# 存储类型
export STORAGE_TYPE="minio"

# 启动应用
./start-with-minio.sh
```

## 功能特性

### 1. 文件上传

```bash
# 上传文档文件
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@document.pdf"

# 上传文本内容
curl -X POST http://localhost:8080/api/rag/upload-text \
  -H "Content-Type: text/plain" \
  -d "文档内容"
```

### 2. 文件管理

```bash
# 列出所有文件
curl http://localhost:8080/api/rag/storage/files

# 获取文件信息
curl "http://localhost:8080/api/rag/storage/file-info?fileName=documents/20250907_120000_abc123.pdf"

# 删除文件
curl -X DELETE "http://localhost:8080/api/rag/storage/file?fileName=documents/20250907_120000_abc123.pdf"
```

### 3. 存储统计

```bash
# 获取存储状态
curl http://localhost:8080/api/rag/storage/status
```

## API接口

### 1. 文件上传接口

#### 上传文档文件
- **接口**: `POST /api/rag/upload`
- **参数**: `file` (MultipartFile)
- **返回**: 上传结果，包含文件路径

#### 上传文本内容
- **接口**: `POST /api/rag/upload-text`
- **参数**: `content` (String)
- **返回**: 上传结果，包含文件路径

### 2. 文件管理接口

#### 列出文件
- **接口**: `GET /api/rag/storage/files`
- **返回**: 文件列表

#### 获取文件信息
- **接口**: `GET /api/rag/storage/file-info`
- **参数**: `fileName` (String)
- **返回**: 文件详细信息

#### 删除文件
- **接口**: `DELETE /api/rag/storage/file`
- **参数**: `fileName` (String)
- **返回**: 删除结果

#### 获取存储状态
- **接口**: `GET /api/rag/storage/status`
- **返回**: 存储统计信息

## 使用示例

### 1. 完整工作流程

```bash
# 1. 启动MinIO
./start-minio.sh &

# 2. 启动Z-RAG应用
./start-with-minio.sh

# 3. 上传文档
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@ai-knowledge.pdf"

# 4. 查看存储状态
curl http://localhost:8080/api/rag/storage/status

# 5. 列出文件
curl http://localhost:8080/api/rag/storage/files

# 6. 执行查询
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是人工智能？"}'
```

### 2. 编程接口示例

```java
@Autowired
private MinioStorageService minioStorageService;

// 上传文件
String filePath = minioStorageService.uploadFile(multipartFile);

// 上传文本
String filePath = minioStorageService.uploadText("文档内容", "document.txt");

// 列出文件
List<String> files = minioStorageService.listFiles();

// 获取文件信息
String info = minioStorageService.getFileInfo("documents/file.pdf");

// 删除文件
minioStorageService.deleteFile("documents/file.pdf");
```

## 性能优化

### 1. MinIO配置优化

```bash
# 设置环境变量
export MINIO_CACHE_DRIVES="/tmp/cache"
export MINIO_CACHE_EXCLUDE="*.pdf,*.mp4"
export MINIO_CACHE_QUOTA=80
export MINIO_CACHE_AFTER=3
export MINIO_CACHE_WATERMARK_LOW=70
export MINIO_CACHE_WATERMARK_HIGH=90
```

### 2. 网络优化

```yaml
# application.yml
minio:
  endpoint: "https://minio.example.com"  # 使用HTTPS
  access-key: "your-access-key"
  secret-key: "your-secret-key"
  bucket-name: "zrag-documents"
  auto-create-bucket: true
  # 连接池配置
  connection-timeout: 10000
  read-timeout: 10000
  write-timeout: 10000
```

### 3. 存储策略

```yaml
# 文件分类存储
storage:
  minio:
    bucket: "zrag-documents"
    prefix: "documents/"
    # 按类型分类
    types:
      pdf: "documents/pdf/"
      txt: "documents/text/"
      images: "documents/images/"
```

## 监控和维护

### 1. 健康检查

```bash
# 检查MinIO服务状态
curl http://localhost:9000/minio/health/live

# 检查Z-RAG存储状态
curl http://localhost:8080/api/rag/storage/status
```

### 2. 日志监控

```yaml
# 日志配置
logging:
  level:
    com.unionhole.zrag.service.MinioStorageService: DEBUG
    io.minio: DEBUG
```

### 3. 性能监控

- 监控存储使用量
- 监控上传/下载速度
- 监控错误率
- 监控响应时间

## 故障排除

### 1. 连接问题

**问题**: 无法连接到MinIO服务

**解决方案**:
```bash
# 检查MinIO是否运行
curl http://localhost:9000/minio/health/live

# 检查网络连接
telnet localhost 9000

# 检查防火墙设置
sudo ufw status
```

### 2. 认证问题

**问题**: 认证失败

**解决方案**:
```bash
# 检查访问密钥
echo $MINIO_ACCESS_KEY
echo $MINIO_SECRET_KEY

# 测试连接
mc alias set myminio http://localhost:9000 minioadmin minioadmin
mc ls myminio
```

### 3. 存储桶问题

**问题**: 存储桶不存在

**解决方案**:
```bash
# 创建存储桶
mc mb myminio/zrag-documents

# 设置权限
mc policy set public myminio/zrag-documents
```

## 安全配置

### 1. 访问控制

```bash
# 创建用户
mc admin user add myminio newuser newpassword

# 创建策略
mc admin policy add myminio readwrite-policy policy.json

# 分配策略
mc admin policy set myminio readwrite-policy user=newuser
```

### 2. SSL/TLS配置

```bash
# 生成证书
openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 \
  -keyout private.key -out public.crt

# 启动MinIO with SSL
minio server ./minio-data --certs-dir /path/to/certs
```

## 扩展配置

### 1. 分布式部署

```bash
# 启动分布式MinIO
export MINIO_ROOT_USER=minioadmin
export MINIO_ROOT_PASSWORD=minioadmin
minio server http://node{1...4}/export{1...4}
```

### 2. 备份策略

```bash
# 设置备份
mc mirror myminio/zrag-documents backup/zrag-documents

# 定时备份
crontab -e
# 添加: 0 2 * * * mc mirror myminio/zrag-documents backup/zrag-documents
```

## 总结

MinIO为Z-RAG系统提供了强大的对象存储能力，支持：

- **高性能文件存储**
- **完整的API接口**
- **灵活的配置选项**
- **强大的监控功能**
- **企业级安全特性**

通过合理的配置和优化，可以构建稳定、高效的文档存储系统。
