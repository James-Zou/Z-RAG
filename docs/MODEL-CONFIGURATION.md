# Z-RAG 模型配置指南

## 概述

Z-RAG系统支持多种AI模型提供商，包括OpenAI、阿里云千问（Qwen）和Ollama本地模型。本文档详细说明如何配置和使用这些模型。

## 支持的模型提供商

### 1. 阿里云千问（推荐国内使用）

千问是阿里云推出的大语言模型，在国内使用无需翻墙，性能优秀。

#### 1.1 获取API Key

1. 访问 [阿里云控制台](https://dashscope.console.aliyun.com/)
2. 开通DashScope服务
3. 创建API Key
4. 获取API Key

#### 1.2 配置参数

```yaml
# application.yml
models:
  qwen:
    api:
      key: "your-qwen-api-key"  # 从阿里云控制台获取
    base:
      url: "https://dashscope.aliyuncs.com/api/v1"
    model: "qwen-turbo"  # 可选: qwen-turbo, qwen-plus, qwen-max
    embedding:
      model: "text-embedding-v1"

default:
  provider: "qwen"  # 设置为千问
```

#### 1.3 环境变量配置

```bash
export QWEN_API_KEY="your-qwen-api-key"
export DEFAULT_PROVIDER="qwen"
```

### 2. OpenAI（需要翻墙）

OpenAI提供强大的GPT模型，但在国内需要翻墙使用。

#### 2.1 配置参数

```yaml
# application.yml
models:
  openai:
    api:
      key: "your-openai-api-key"
    base:
      url: "https://api.openai.com/v1"
    model: "gpt-3.5-turbo"  # 可选: gpt-3.5-turbo, gpt-4
    embedding:
      model: "text-embedding-ada-002"

default:
  provider: "openai"
```

#### 2.2 环境变量配置

```bash
export OPENAI_API_KEY="your-openai-api-key"
export DEFAULT_PROVIDER="openai"
```

### 3. Ollama本地模型（推荐）

Ollama支持在本地运行大语言模型，无需API Key，完全离线使用。

#### 3.1 安装Ollama

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# 下载安装包从 https://ollama.ai/download
```

#### 3.2 下载模型

```bash
# 下载千问模型
ollama pull qwen2.5:7b

# 下载嵌入模型
ollama pull nomic-embed-text
```

#### 3.3 配置参数

```yaml
# application.yml
models:
  ollama:
    base:
      url: "http://localhost:11434"
    model: "qwen2.5:7b"
    embedding:
      model: "nomic-embed-text"

default:
  provider: "ollama"
```

#### 3.4 环境变量配置

```bash
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="qwen2.5:7b"
export DEFAULT_PROVIDER="ollama"
```

## 配置示例

### 1. 千问配置（推荐）

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: z-rag

# 模型配置
models:
  # 千问模型配置（国内可用）
  qwen:
    api:
      key: ${QWEN_API_KEY:}
    base:
      url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
    model: ${QWEN_MODEL:qwen-turbo}
    embedding:
      model: ${QWEN_EMBEDDING_MODEL:text-embedding-v1}

# 默认使用千问
default:
  provider: ${DEFAULT_PROVIDER:qwen}

# 日志配置
logging:
  level:
    com.unionhole.zrag: INFO
    dev.langchain4j: DEBUG
```

### 2. Ollama配置

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: z-rag

# 模型配置
models:
  # Ollama本地模型配置
  ollama:
    base:
      url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:qwen2.5:7b}
    embedding:
      model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

# 默认使用Ollama
default:
  provider: ${DEFAULT_PROVIDER:ollama}

# 日志配置
logging:
  level:
    com.unionhole.zrag: INFO
    dev.langchain4j: DEBUG
```

### 3. 混合配置

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: z-rag

# 模型配置
models:
  # OpenAI配置（需要翻墙）
  openai:
    api:
      key: ${OPENAI_API_KEY:}
    base:
      url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    model: ${OPENAI_MODEL:gpt-3.5-turbo}
    embedding:
      model: ${OPENAI_EMBEDDING_MODEL:text-embedding-ada-002}
  
  # 千问模型配置（国内可用）
  qwen:
    api:
      key: ${QWEN_API_KEY:}
    base:
      url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
    model: ${QWEN_MODEL:qwen-turbo}
    embedding:
      model: ${QWEN_EMBEDDING_MODEL:text-embedding-v1}
  
  # Ollama本地模型配置
  ollama:
    base:
      url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:qwen2.5:7b}
    embedding:
      model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

# 默认使用千问
default:
  provider: ${DEFAULT_PROVIDER:qwen}

# 日志配置
logging:
  level:
    com.unionhole.zrag: INFO
    dev.langchain4j: DEBUG
```

## 启动脚本

### 1. 千问启动脚本

```bash
#!/bin/bash
# start-qwen.sh

echo "=== 启动Z-RAG系统（千问模型） ==="

# 设置环境变量
export DEFAULT_PROVIDER="qwen"
export QWEN_API_KEY="your-qwen-api-key"

# 检查Java环境
java -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Java环境，请先安装Java 8+"
    exit 1
fi

# 检查Maven环境
mvn -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

# 设置Java环境变量
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_341.jdk/Contents/Home

# 编译项目
echo "编译项目..."
mvn clean compile -s maven-settings.xml
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 运行应用
echo "启动Z-RAG应用（千问模型）..."
mvn spring-boot:run -s maven-settings.xml
```

### 2. Ollama启动脚本

```bash
#!/bin/bash
# start-ollama.sh

echo "=== 启动Z-RAG系统（Ollama本地模型） ==="

# 设置环境变量
export DEFAULT_PROVIDER="ollama"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="qwen2.5:7b"

# 检查Ollama是否运行
curl -s http://localhost:11434/api/tags > /dev/null
if [ $? -ne 0 ]; then
    echo "错误: Ollama未运行，请先启动Ollama"
    echo "运行命令: ollama serve"
    exit 1
fi

# 检查模型是否已下载
ollama list | grep -q "qwen2.5:7b"
if [ $? -ne 0 ]; then
    echo "下载千问模型..."
    ollama pull qwen2.5:7b
fi

ollama list | grep -q "nomic-embed-text"
if [ $? -ne 0 ]; then
    echo "下载嵌入模型..."
    ollama pull nomic-embed-text
fi

# 检查Java环境
java -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Java环境，请先安装Java 8+"
    exit 1
fi

# 检查Maven环境
mvn -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

# 设置Java环境变量
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_341.jdk/Contents/Home

# 编译项目
echo "编译项目..."
mvn clean compile -s maven-settings.xml
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 运行应用
echo "启动Z-RAG应用（Ollama本地模型）..."
mvn spring-boot:run -s maven-settings.xml
```

## 性能对比

| 模型提供商 | 网络要求 | 成本 | 性能 | 隐私 | 推荐场景 |
|------------|----------|------|------|------|----------|
| 千问 | 国内直连 | 中等 | 高 | 中等 | 国内生产环境 |
| OpenAI | 需要翻墙 | 高 | 很高 | 低 | 国际项目 |
| Ollama | 本地运行 | 低 | 中等 | 高 | 开发测试 |

## 故障排除

### 1. 千问API调用失败

```bash
# 检查API Key是否正确
echo $QWEN_API_KEY

# 检查网络连接
curl -H "Authorization: Bearer $QWEN_API_KEY" \
     https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
```

### 2. Ollama连接失败

```bash
# 检查Ollama是否运行
curl http://localhost:11434/api/tags

# 启动Ollama
ollama serve

# 检查模型是否已下载
ollama list
```

### 3. 模型切换

```bash
# 切换到千问
export DEFAULT_PROVIDER="qwen"

# 切换到Ollama
export DEFAULT_PROVIDER="ollama"

# 切换到OpenAI
export DEFAULT_PROVIDER="openai"
```

## 总结

Z-RAG系统支持多种模型提供商，可以根据实际需求选择合适的模型：

- **国内生产环境**: 推荐使用千问模型
- **开发测试**: 推荐使用Ollama本地模型
- **国际项目**: 可以使用OpenAI模型

通过灵活的配置，系统可以适应不同的使用场景和网络环境。
