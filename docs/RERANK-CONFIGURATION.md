# Z-RAG 重排模型配置指南

## 概述

重排（Rerank）是RAG系统中的重要环节，它能够对检索到的文档片段进行重新排序，提升结果的相关性和准确性。Z-RAG系统基于 LangChain4j 0.29.1 构建，支持多种重排模型，包括千问、OpenAI和Ollama。

## 系统环境

- **Java版本**: 8
- **Spring Boot版本**: 2.7.18
- **LangChain4j版本**: 0.29.1

## 重排的作用

### 1. 提升检索质量
- 基于语义相似性重新排序
- 过滤低质量结果
- 提升最终答案的准确性

### 2. 优化用户体验
- 返回最相关的文档片段
- 减少无关信息的干扰
- 提升问答质量

## 支持的重排模型

### 1. 千问重排模型（推荐国内使用）

#### 1.1 配置参数

```yaml
# application.yml
rerank:
  qwen:
    api:
      key: "your-qwen-api-key"  # 从阿里云控制台获取
    base:
      url: "https://dashscope.aliyuncs.com/api/v1"
    model: "qwen-reranker"  # 千问重排模型

default:
  rerank:
    provider: "qwen"  # 使用千问重排
```

#### 1.2 环境变量配置

```bash
export QWEN_API_KEY="your-qwen-api-key"
export DEFAULT_RERANK_PROVIDER="qwen"
```

#### 1.3 特点
- 国内直连，无需翻墙
- 支持中文优化
- 性能优秀，成本合理

### 2. OpenAI重排模型

#### 2.1 配置参数

```yaml
# application.yml
rerank:
  openai:
    api:
      key: "your-openai-api-key"
    base:
      url: "https://api.openai.com/v1"
    model: "text-embedding-3-large"  # 使用嵌入模型进行重排

default:
  rerank:
    provider: "openai"
```

#### 2.2 环境变量配置

```bash
export OPENAI_API_KEY="your-openai-api-key"
export DEFAULT_RERANK_PROVIDER="openai"
```

#### 2.3 特点
- 性能最强
- 需要翻墙
- 成本较高

### 3. Ollama本地重排模型

#### 3.1 配置参数

```yaml
# application.yml
rerank:
  ollama:
    base:
      url: "http://localhost:11434"
    model: "qwen2.5:7b"  # 使用本地模型进行重排

default:
  rerank:
    provider: "ollama"
```

#### 3.2 环境变量配置

```bash
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="qwen2.5:7b"
export DEFAULT_RERANK_PROVIDER="ollama"
```

#### 3.3 特点
- 完全离线
- 数据隐私保护
- 免费使用

## 重排流程

### 1. 检索阶段
```
用户查询 → 向量化 → 向量搜索 → 获取候选文档
```

### 2. 重排阶段
```
候选文档 → 重排模型 → 重新排序 → 返回最相关文档
```

### 3. 生成阶段
```
重排后文档 → 上下文构建 → 生成回答
```

## 配置示例

### 1. 千问重排配置（推荐）

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: z-rag

# 模型配置
models:
  qwen:
    api:
      key: ${QWEN_API_KEY:}
    base:
      url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
    model: ${QWEN_MODEL:qwen-turbo}
    embedding:
      model: ${QWEN_EMBEDDING_MODEL:text-embedding-v1}

# 重排模型配置
rerank:
  qwen:
    api:
      key: ${QWEN_API_KEY:}
    base:
      url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
    model: ${QWEN_RERANK_MODEL:qwen-reranker}

# 默认配置
default:
  provider: ${DEFAULT_PROVIDER:qwen}
  rerank:
    provider: ${DEFAULT_RERANK_PROVIDER:qwen}

# 日志配置
logging:
  level:
    com.unionhole.zrag: INFO
    dev.langchain4j: DEBUG
```

### 2. Ollama重排配置

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: z-rag

# 模型配置
models:
  ollama:
    base:
      url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:qwen2.5:7b}
    embedding:
      model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

# 重排模型配置
rerank:
  ollama:
    base:
      url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_RERANK_MODEL:qwen2.5:7b}

# 默认配置
default:
  provider: ${DEFAULT_PROVIDER:ollama}
  rerank:
    provider: ${DEFAULT_RERANK_PROVIDER:ollama}

# 日志配置
logging:
  level:
    com.unionhole.zrag: INFO
    dev.langchain4j: DEBUG
```

## API接口

### 1. 获取重排服务状态

```bash
curl http://localhost:8080/api/rag/rerank/status
```

响应示例：
```json
"重排服务状态:\n- 默认提供商: qwen\n- 千问配置: 已配置\n- OpenAI配置: 未配置\n- Ollama配置: 已配置\n"
```

### 2. 执行带重排的查询

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是人工智能？",
    "maxResults": 5,
    "minScore": 0.6
  }'
```

## 性能优化

### 1. 重排参数调优

```yaml
# 检索更多结果用于重排
retrieval:
  search_multiplier: 2  # 检索结果数 = maxResults * 2
  min_score: 0.5        # 降低最小相似度分数

# 重排参数
rerank:
  max_results: 5        # 重排后返回的最大结果数
  temperature: 0.1      # 重排模型的温度参数
```

### 2. 缓存策略

```java
// 缓存重排结果
@Cacheable(value = "rerank", key = "#query + #matches.hashCode()")
public List<EmbeddingMatch<TextSegment>> rerank(String query, List<EmbeddingMatch<TextSegment>> matches) {
    // 重排逻辑
}
```

### 3. 异步处理

```java
// 异步重排
@Async
public CompletableFuture<List<EmbeddingMatch<TextSegment>>> rerankAsync(String query, List<EmbeddingMatch<TextSegment>> matches) {
    return CompletableFuture.completedFuture(rerank(query, matches));
}
```

## 监控和调试

### 1. 重排效果监控

```java
// 记录重排前后的结果
log.info("重排前: {}", matches.stream().map(m -> m.score()).collect(Collectors.toList()));
List<EmbeddingMatch<TextSegment>> reranked = rerankService.rerank(query, matches, maxResults);
log.info("重排后: {}", reranked.stream().map(m -> m.score()).collect(Collectors.toList()));
```

### 2. 性能指标

- 重排延迟时间
- 重排准确率
- 重排成功率
- 用户满意度

### 3. 日志配置

```yaml
logging:
  level:
    com.unionhole.zrag.service.RerankService: DEBUG
    com.unionhole.zrag.service.RetrievalService: DEBUG
```

## 故障排除

### 1. 重排模型调用失败

**问题**：重排API调用失败

**解决方案**：
```bash
# 检查API Key
echo $QWEN_API_KEY

# 检查网络连接
curl -H "Authorization: Bearer $QWEN_API_KEY" \
     https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
```

### 2. 重排结果不准确

**问题**：重排后的结果质量不高

**解决方案**：
- 调整重排模型的温度参数
- 优化重排提示词
- 增加检索结果数量

### 3. 重排延迟过高

**问题**：重排处理时间过长

**解决方案**：
- 使用更快的重排模型
- 实现结果缓存
- 优化重排算法

## 最佳实践

### 1. 模型选择

| 场景 | 推荐模型 | 原因 |
|------|----------|------|
| 生产环境 | 千问重排 | 性能稳定，成本合理 |
| 开发测试 | Ollama重排 | 免费，完全离线 |
| 高质量需求 | OpenAI重排 | 性能最强 |

### 2. 参数调优

- **检索倍数**：建议设置为2-3倍
- **最小相似度**：建议设置为0.5-0.6
- **重排温度**：建议设置为0.1-0.3

### 3. 监控建议

- 定期检查重排服务状态
- 监控重排延迟和准确率
- 收集用户反馈优化重排效果

## 总结

重排模型是RAG系统的重要组成部分，能够显著提升检索结果的质量。Z-RAG系统支持多种重排模型，可以根据实际需求选择合适的模型和配置。通过合理的配置和优化，可以构建高质量的RAG系统。
