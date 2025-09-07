# Z-RAG 快速开始指南

## 概述

本指南将帮助您快速启动Z-RAG系统，支持多种AI模型提供商。

## 选择模型提供商

### 1. 阿里云千问（推荐国内用户）

**优势**：
- 国内直连，无需翻墙
- 性能优秀，成本合理
- 支持中文优化

**步骤**：
1. 注册阿里云账号
2. 开通DashScope服务
3. 获取API Key
4. 配置环境变量

### 2. Ollama本地模型（推荐开发测试）

**优势**：
- 完全离线，无需API Key
- 数据隐私保护
- 免费使用

**步骤**：
1. 安装Ollama
2. 下载模型
3. 启动服务

### 3. OpenAI（需要翻墙）

**优势**：
- 性能最强
- 功能最全

**步骤**：
1. 注册OpenAI账号
2. 获取API Key
3. 配置翻墙

## 快速启动

### 方式一：千问模型（推荐）

```bash
# 1. 克隆项目
git clone <repository-url>
cd Z-RAG

# 2. 设置API Key
export QWEN_API_KEY="your-qwen-api-key"

# 3. 启动应用
./start-qwen.sh
```

### 方式二：Ollama本地模型

```bash
# 1. 安装Ollama
brew install ollama  # macOS
# 或 curl -fsSL https://ollama.ai/install.sh | sh  # Linux

# 2. 启动Ollama服务
ollama serve

# 3. 下载模型
ollama pull qwen2.5:7b
ollama pull nomic-embed-text

# 4. 启动应用
./start-ollama.sh
```

### 方式三：OpenAI模型

```bash
# 1. 设置API Key
export OPENAI_API_KEY="your-openai-api-key"

# 2. 启动应用
./start.sh
```

## 验证安装

### 1. 检查服务状态

```bash
curl http://localhost:8080/api/rag/health
```

预期输出：
```json
"Z-RAG服务运行正常"
```

### 2. 测试RAG功能

```bash
# 上传文档
curl -X POST http://localhost:8080/api/rag/upload-text \
  -H "Content-Type: text/plain" \
  -d "人工智能是计算机科学的一个分支，它企图了解智能的实质。"

# 执行查询
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是人工智能？"}'
```

### 3. 查看系统状态

```bash
curl http://localhost:8080/api/rag/status
```

## 常见问题

### 1. 千问API调用失败

**问题**：API Key无效或网络问题

**解决方案**：
```bash
# 检查API Key
echo $QWEN_API_KEY

# 测试API连接
curl -H "Authorization: Bearer $QWEN_API_KEY" \
     https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
```

### 2. Ollama连接失败

**问题**：Ollama服务未启动或模型未下载

**解决方案**：
```bash
# 检查Ollama状态
curl http://localhost:11434/api/tags

# 启动Ollama
ollama serve

# 检查模型
ollama list
```

### 3. 编译错误

**问题**：Java版本或Maven配置问题

**解决方案**：
```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version

# 设置Java环境
export JAVA_HOME=/path/to/jdk
```

## 配置说明

### 1. 环境变量配置

```bash
# 千问配置
export QWEN_API_KEY="your-qwen-api-key"
export DEFAULT_PROVIDER="qwen"

# Ollama配置
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="qwen2.5:7b"
export DEFAULT_PROVIDER="ollama"

# OpenAI配置
export OPENAI_API_KEY="your-openai-api-key"
export DEFAULT_PROVIDER="openai"
```

### 2. 配置文件修改

编辑 `src/main/resources/application.yml`：

```yaml
# 千问配置
models:
  qwen:
    api:
      key: "your-qwen-api-key"
    model: "qwen-turbo"

default:
  provider: "qwen"
```

## 性能优化

### 1. 内存配置

```bash
# 设置JVM参数
export JAVA_OPTS="-Xms2g -Xmx4g"

# 启动应用
java $JAVA_OPTS -jar target/ZRAG-1.0-SNAPSHOT.jar
```

### 2. 模型选择

| 模型 | 内存需求 | 性能 | 推荐场景 |
|------|----------|------|----------|
| qwen-turbo | 低 | 高 | 生产环境 |
| qwen-plus | 中 | 很高 | 高质量需求 |
| qwen-max | 高 | 最高 | 复杂任务 |
| qwen2.5:7b | 8GB | 中 | 本地开发 |

## 下一步

1. **阅读API文档**：查看 `API-GUIDE.md`
2. **了解架构**：查看 `RAG-ARCHITECTURE.md`
3. **技术实现**：查看 `TECHNICAL-IMPLEMENTATION.md`
4. **模型配置**：查看 `MODEL-CONFIGURATION.md`

## 获取帮助

- **GitHub Issues**：报告问题和建议
- **文档**：查看项目文档
- **社区**：加入讨论群组

## 总结

Z-RAG系统支持多种模型提供商，可以根据您的需求选择合适的模型：

- **国内用户**：推荐使用千问模型
- **开发测试**：推荐使用Ollama本地模型
- **国际项目**：可以使用OpenAI模型

通过简单的配置和启动脚本，您可以快速体验RAG系统的强大功能。
