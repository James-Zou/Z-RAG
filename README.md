# Z-RAG - 基于LangChain4j的检索增强生成系统

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.29.1-red.svg)](https://github.com/langchain4j/langchain4j)

## 项目简介

It is a lightweight and high-performance Retrieval-Augmented Generation (RAG) system specifically designed for professional developers. 
Built on the Java ecosystem, it provides a complete solution for document processing, vector retrieval, and intelligent question answering, 
making it particularly suitable for enterprise-level applications and customized development needs.

- 📖 **产品说明文档**: 查看 [docs/PRODUCT-OVERVIEW.md](docs/PRODUCT-OVERVIEW.md) 了解详细的产品介绍和竞品对比
- 📊 **功能特性详解**: 查看 [docs/FEATURES.md](docs/FEATURES.md) 了解完整的功能特性
- 🆚 **竞品对比分析**: 查看 [docs/COMPARISON.md](docs/COMPARISON.md) 了解与Dify、RAGFlow的详细对比
- 🚀 **快速开始指南**: 查看 [docs/QUICK-START-GUIDE.md](docs/QUICK-START-GUIDE.md) 5分钟快速体验
- 📋 **产品总结**: 查看 [docs/PRODUCT-SUMMARY.md](docs/PRODUCT-SUMMARY.md) 了解产品核心优势和技术特性
- 📚 **文档索引**: 查看 [docs/DOCS-INDEX.md](docs/DOCS-INDEX.md) 了解完整文档体系
- 📁 **目录结构**: 查看 [docs/DIRECTORY-STRUCTURE.md](docs/DIRECTORY-STRUCTURE.md) 了解项目目录组织

## 作者信息

- **作者**: james-zou
- **邮箱**: 18301545237@163.com
- **组织**: UnionHole
- **许可证**: Apache License 2.0

## 技术栈

- **Java 8** - 编程语言
- **Spring Boot 2.7.18** - 应用框架
- **LangChain4j 0.29.1** - AI框架（稳定版本）
- **Maven** - 构建工具
- **多模型支持**:
  - **阿里云千问** - 国内大语言模型（推荐）
  - **OpenAI GPT** - 国际大语言模型（需翻墙）
  - **Ollama** - 本地大语言模型（完全离线）
- **重排模型**:
  - **千问重排** - 国内重排模型（推荐）
  - **OpenAI重排** - 国际重排模型
  - **Ollama重排** - 本地重排模型
- **AllMiniLmL6V2** - 本地嵌入模型
- **Milvus** - 向量数据库（支持持久化向量存储）
- **MinIO** - 对象存储服务（支持文件持久化）

## 核心功能

### 1. 文档处理
- 支持多种文档格式的加载和解析
- 智能文档分割，将长文档切分为合适的片段
- 文档向量化和存储

### 2. 向量检索
- 基于语义相似性的文档检索
- 可配置的检索参数（最大结果数、相似度阈值）
- 支持多种检索策略
- **智能重排**：对检索结果进行重新排序，提升相关性
- **持久化存储**：支持Milvus向量数据库，数据持久化保存

### 3. 智能问答
- 基于检索到的文档生成准确回答
- 支持OpenAI GPT模型和本地模型
- 可配置的生成参数

### 4. REST API
- 完整的HTTP API接口
- 支持文档上传、查询、检索等操作
- 系统状态监控

### 5. 现代化Web界面
- **智能问答界面**: 实时对话、消息历史、多轮对话
- **文档管理界面**: 拖拽上传、文件列表、批量操作
- **知识管理界面**: 统计概览、知识片段、向量数据
- **系统设置界面**: 模型配置、参数调整、状态监控
- **响应式设计**: 支持桌面和移动设备

### 6. 文件存储
- **MinIO对象存储**：高性能文件存储
- **文件管理**：上传、下载、删除、列表
- **存储统计**：使用量监控
- **持久化存储**：文件永久保存

## 项目结构

```
src/main/java/com/unionhole/zrag/
├── config/
│   └── RagConfig.java              # RAG配置类
├── controller/
│   └── RagController.java          # REST控制器
├── service/
│   ├── DocumentService.java        # 文档处理服务
│   ├── RetrievalService.java       # 检索服务
│   ├── GenerationService.java      # 生成服务
│   └── RagService.java             # RAG核心服务
├── store/
│   ├── InMemoryEmbeddingStore.java # 内存向量存储
│   └── MilvusEmbeddingStore.java   # Milvus向量存储
├── dto/
│   ├── QueryRequest.java           # 查询请求DTO
│   ├── QueryResponse.java          # 查询响应DTO
│   └── UploadResponse.java         # 上传响应DTO
├── demo/
│   └── RagDemo.java                # 演示程序
├── ApplicationRunner.java          # 应用启动器
└── ZRagApplication.java            # 主应用类
```

## 快速开始

### 1. 环境要求

- Java 8+
- Maven 3.6+

> 📖 **详细安装指南**: 请查看 [docs/INSTALLATION.md](docs/INSTALLATION.md) 获取完整的安装步骤和故障排除信息。

### 2. 打包部署

#### 2.1 自动打包

```bash
# 执行打包脚本
./script/package.sh

# 解压发布包
tar -xzf target/zrag-1.0-SNAPSHOT.tar.gz
cd zrag-1.0-SNAPSHOT

# 启动服务
./script/start.sh
```

#### 2.2 手动打包

```bash
# 编译项目
mvn clean compile

# 打包JAR
mvn package

# 创建发布包
mvn assembly:single
```

### 3. 配置

#### 3.1 千问模型配置（推荐国内使用）

```yaml
# application.yml
models:
  qwen:
    api:
      key: your-qwen-api-key  # 从阿里云控制台获取
    base:
      url: https://dashscope.aliyuncs.com/api/v1
    model: qwen-turbo
    embedding:
      model: text-embedding-v1

default:
  provider: qwen
```

#### 2.2 Ollama本地模型配置（完全离线）

```yaml
# application.yml
models:
  ollama:
    base:
      url: http://localhost:11434
    model: qwen2.5:7b
    embedding:
      model: nomic-embed-text

default:
  provider: ollama
```

#### 2.3 OpenAI配置（需要翻墙）

```yaml
# application.yml
models:
  openai:
    api:
      key: your-openai-api-key
    base:
      url: https://api.openai.com/v1
    model: gpt-3.5-turbo
    embedding:
      model: text-embedding-ada-002

default:
  provider: openai
```

#### 2.4 Milvus向量数据库配置

```yaml
# application.yml
# Milvus向量数据库配置
milvus:
  host: localhost
  port: 19530
  database: zrag
  collection: documents
  # 向量配置
  vector-dimension: 384  # AllMiniLmL6V2的向量维度
  index-type: IVF_FLAT
  metric-type: COSINE

# 向量存储配置
vector-store:
  type: milvus  # 可选: milvus, weaviate, memory

default:
  provider: qwen
```

### 3. 运行应用

#### 3.1 使用前端界面（推荐）

```bash
# 启动前端演示
./script/start-frontend.sh

# 访问地址
# 主界面: http://localhost:8080
# 演示页面: http://localhost:8080/demo.html
```

#### 3.2 使用千问模型

```bash
# 设置API Key
export QWEN_API_KEY="your-qwen-api-key"

# 启动应用
./script/start-qwen.sh
```

#### 3.3 使用Ollama本地模型

```bash
# 安装Ollama
brew install ollama  # macOS
# 或 curl -fsSL https://ollama.ai/install.sh | sh  # Linux

# 启动Ollama服务
ollama serve

# 下载模型
ollama pull qwen2.5:7b
ollama pull nomic-embed-text

# 启动应用
./script/start-ollama.sh
```

#### 3.4 使用OpenAI模型

```bash
# 设置API Key
export OPENAI_API_KEY="your-openai-api-key"

# 启动应用
./script/start.sh
```

#### 3.5 使用Milvus向量数据库

```bash
# 启动Milvus服务（使用Docker）
docker run -d --name milvus \
  -p 19530:19530 \
  -p 9091:9091 \
  -e ETCD_USE_EMBED=true \
  -e ETCD_DATA_DIR=/opt/milvus/etcd \
  -e COMMON_STORAGETYPE=local \
  -e COMMON_WALDIR=/opt/milvus/wal \
  -e COMMON_LOGDIR=/opt/milvus/logs \
  milvusdb/milvus:latest

# 启动Z-RAG应用（带Milvus存储）
./script/start.sh

# 运行Milvus测试
./script/test-milvus.sh
```

#### 3.6 使用MinIO存储

```bash
# 启动MinIO服务
./script/start-minio.sh &

# 启动Z-RAG应用（带MinIO存储）
./script/start-with-minio.sh
```

### 4. 使用API

#### 上传文档
```bash
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@your-document.txt"
```

#### 上传文本
```bash
curl -X POST http://localhost:8080/api/rag/upload-text \
  -H "Content-Type: text/plain" \
  -d "你的文本内容"
```

#### 执行查询
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "你的问题"}'
```

#### 检索文档
```bash
curl "http://localhost:8080/api/rag/retrieve?query=你的查询"
```

#### 获取系统状态
```bash
curl http://localhost:8080/api/rag/status
```

#### 文件存储管理
```bash
# 获取存储状态
curl http://localhost:8080/api/rag/storage/status

# 列出所有文件
curl http://localhost:8080/api/rag/storage/files

# 获取文件信息
curl "http://localhost:8080/api/rag/storage/file-info?fileName=documents/file.pdf"

# 删除文件
curl -X DELETE "http://localhost:8080/api/rag/storage/file?fileName=documents/file.pdf"
```

## 核心组件说明

### RagConfig
配置LangChain4j的核心组件：
- 嵌入模型（OpenAI、千问、Ollama或本地模型）
- 聊天模型（OpenAI GPT、千问、Ollama）
- 向量存储（Milvus、内存存储）
- 内容检索器
- 文档分割器

### DocumentService
负责文档的完整处理流程：
- 文档加载和解析
- 文档分割
- 向量化
- 存储到向量数据库

### RetrievalService
提供基于语义相似性的文档检索：
- 查询向量化
- 相似性搜索
- 结果排序和过滤

### GenerationService
基于检索到的文档生成回答：
- 上下文构建
- 提示词生成
- 回答生成

### RagService
整合所有组件，提供完整的RAG功能：
- 文档处理
- 查询处理
- 结果生成

## 配置说明

### 嵌入模型配置
- **OpenAI**: 需要API Key，性能更好
- **AllMiniLmL6V2**: 本地模型，无需API Key

### 检索参数
- `maxResults`: 最大返回结果数（默认5）
- `minScore`: 最小相似度分数（默认0.6）

### 文档分割参数
- `chunkSize`: 文档块大小（默认300）
- `chunkOverlap`: 文档块重叠（默认0）

## 演示功能

应用启动后会自动运行演示程序，展示：
1. 文档上传和处理
2. 多种查询类型
3. 检索功能
4. 系统状态

## 扩展功能

### 向量存储支持
系统支持多种向量存储方案：

#### 内置支持
- **Milvus** - 生产级向量数据库（推荐）
- **内存存储** - 开发测试使用

#### Milvus最佳实践
参考[CSDN文章：手把手教你用Java实现RAG向量库Milvus的增删改查](https://blog.csdn.net/qq_38196449/article/details/148061140)，我们实现了以下最佳实践：

1. **工具类封装**：创建了`MilvusLLMUtils`工具类，封装所有Milvus操作
2. **原生客户端**：使用Milvus官方Java客户端进行高效通信
3. **错误处理**：完善的异常处理和日志记录
4. **类型安全**：使用泛型和强类型确保代码安全
5. **配置管理**：通过Spring配置管理Milvus连接参数

#### 可扩展支持
可以实现 `EmbeddingStore` 接口来支持其他向量数据库：
- Pinecone
- Chroma
- Qdrant
- Milvus

### 自定义文档分割器
可以实现 `DocumentSplitter` 接口来支持不同的分割策略。

### 自定义生成模型
可以集成其他大语言模型：
- 本地模型（Ollama）
- 其他云服务模型

## 注意事项

1. 确保Java环境正确配置
2. 如果使用OpenAI API，请确保网络连接正常
3. 本地模型首次运行可能需要下载模型文件
4. **Milvus存储**：数据持久化保存，重启后数据不丢失
5. **内存存储**：仅用于开发测试，重启后数据丢失
6. 使用Milvus时，确保Milvus服务正常运行

## 故障排除

### 常见问题

1. **编译错误**: 检查Java版本和Maven配置
2. **依赖下载失败**: 检查网络连接和Maven仓库配置
3. **API调用失败**: 检查OpenAI API Key和网络连接
4. **内存不足**: 调整JVM堆内存设置
5. **Milvus连接失败**: 
   - 检查Milvus服务是否正常运行
   - 验证连接配置（host、port、认证信息）
   - 查看Milvus服务日志
6. **向量存储错误**: 
   - 检查向量维度是否匹配（默认384）
   - 验证Milvus集合是否创建成功

### 日志查看

应用日志会显示详细的执行信息，包括：
- 文档处理进度
- 查询执行过程
- 错误信息

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

```
Copyright 2025 james-zou

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 相关文档

- 📖 [安装指南](INSTALLATION.md) - 详细的安装步骤和故障排除
- 🎨 [前端界面说明](FRONTEND-README.md) - 前端功能详细说明
- 🔧 [重排模型配置](RERANK-CONFIGURATION.md) - 重排模型配置指南
- 📋 [版本信息](VERSION-INFO.md) - 版本选择和升级说明

## 联系方式

- **作者**: james-zou
- **邮箱**: 18301545237@163.com
- **GitHub**: [https://github.com/james-zou/zrag](https://github.com/james-zou/zrag)
