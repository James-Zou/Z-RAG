# Z-RAG API 使用指南

## 概述

Z-RAG提供了完整的REST API接口，支持文档上传、向量检索、智能问答等功能。本文档详细介绍了所有API接口的使用方法。

## 基础信息

- **基础URL**: `http://localhost:8080/api/rag`
- **内容类型**: `application/json` (除文件上传外)
- **字符编码**: `UTF-8`

## API接口列表

### 1. 文档管理接口

#### 1.1 上传文档文件

**接口**: `POST /api/rag/upload`

**描述**: 上传文档文件并进行RAG处理

**请求参数**:
- `file` (MultipartFile): 文档文件

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@document.txt" \
  -H "Content-Type: multipart/form-data"
```

**响应示例**:
```json
{
  "success": true,
  "message": "文档上传并处理成功",
  "filename": "document.txt",
  "documentCount": 5
}
```

#### 1.2 上传文本内容

**接口**: `POST /api/rag/upload-text`

**描述**: 直接上传文本内容并进行RAG处理

**请求参数**:
- `content` (String): 文本内容

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/rag/upload-text \
  -H "Content-Type: text/plain" \
  -d "人工智能是计算机科学的一个分支，它企图了解智能的实质。"
```

**响应示例**:
```json
{
  "success": true,
  "message": "文本内容上传并处理成功",
  "documentCount": 6
}
```

### 2. 查询接口

#### 2.1 执行RAG查询

**接口**: `POST /api/rag/query`

**描述**: 执行RAG查询，基于检索到的文档生成回答

**请求参数**:
```json
{
  "query": "用户查询问题",
  "maxResults": 5,        // 可选，最大检索结果数
  "minScore": 0.6         // 可选，最小相似度分数
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是人工智能？",
    "maxResults": 3,
    "minScore": 0.7
  }'
```

**响应示例**:
```json
{
  "query": "什么是人工智能？",
  "answer": "基于检索到的文档，人工智能是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。",
  "success": true
}
```

#### 2.2 使用内容检索器查询

**接口**: `POST /api/rag/query-with-retriever`

**描述**: 使用预配置的内容检索器执行查询

**请求参数**:
```json
{
  "query": "用户查询问题"
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/rag/query-with-retriever \
  -H "Content-Type: application/json" \
  -d '{"query": "机器学习的应用场景有哪些？"}'
```

**响应示例**:
```json
{
  "query": "机器学习的应用场景有哪些？",
  "answer": "机器学习在图像识别、语音识别、自然语言处理、推荐系统、金融风控等领域有广泛应用。",
  "success": true
}
```

### 3. 检索接口

#### 3.1 检索文档片段

**接口**: `GET /api/rag/retrieve`

**描述**: 检索与查询相关的文档片段

**请求参数**:
- `query` (String): 查询文本

**请求示例**:
```bash
curl "http://localhost:8080/api/rag/retrieve?query=深度学习"
```

**响应示例**:
```json
[
  "深度学习是机器学习的一个子集，它使用多层神经网络来模拟人脑的工作方式。",
  "深度学习在图像识别、语音识别和自然语言处理等领域取得了突破性进展。"
]
```

#### 3.2 带参数检索文档片段

**接口**: `GET /api/rag/retrieve-with-params`

**描述**: 使用自定义参数检索文档片段

**请求参数**:
- `query` (String): 查询文本
- `maxResults` (int): 最大返回结果数，默认5
- `minScore` (double): 最小相似度分数，默认0.6

**请求示例**:
```bash
curl "http://localhost:8080/api/rag/retrieve-with-params?query=自然语言处理&maxResults=3&minScore=0.8"
```

**响应示例**:
```json
[
  "自然语言处理（NLP）是人工智能和语言学的交叉领域，它研究如何让计算机理解、解释和生成人类语言。",
  "NLP技术广泛应用于机器翻译、聊天机器人和文本分析等场景。"
]
```

### 4. 系统管理接口

#### 4.1 获取文档数量

**接口**: `GET /api/rag/document-count`

**描述**: 获取当前向量数据库中的文档数量

**请求示例**:
```bash
curl http://localhost:8080/api/rag/document-count
```

**响应示例**:
```json
5
```

#### 4.2 清空文档

**接口**: `DELETE /api/rag/clear`

**描述**: 清空向量数据库中的所有文档

**请求示例**:
```bash
curl -X DELETE http://localhost:8080/api/rag/clear
```

**响应示例**:
```json
"文档清空成功"
```

#### 4.3 获取系统状态

**接口**: `GET /api/rag/status`

**描述**: 获取系统运行状态信息

**请求示例**:
```bash
curl http://localhost:8080/api/rag/status
```

**响应示例**:
```json
"Z-RAG系统状态:\n- 文档数量: 5\n- 聊天模型: 已配置\n- 内容检索器: 已配置\n"
```

#### 4.4 健康检查

**接口**: `GET /api/rag/health`

**描述**: 检查服务健康状态

**请求示例**:
```bash
curl http://localhost:8080/api/rag/health
```

**响应示例**:
```json
"Z-RAG服务运行正常"
```

## 错误处理

### 错误响应格式

```json
{
  "success": false,
  "message": "错误描述信息",
  "errorCode": "ERROR_CODE",
  "timestamp": "2025-09-07T00:00:00Z"
}
```

### 常见错误码

| 错误码 | 描述 | 解决方案 |
|--------|------|----------|
| `INVALID_REQUEST` | 请求参数无效 | 检查请求参数格式 |
| `FILE_TOO_LARGE` | 文件过大 | 减小文件大小 |
| `UNSUPPORTED_FORMAT` | 不支持的文件格式 | 使用支持的文件格式 |
| `MODEL_NOT_AVAILABLE` | 模型不可用 | 检查模型配置 |
| `QUERY_FAILED` | 查询失败 | 检查查询参数 |
| `SYSTEM_ERROR` | 系统内部错误 | 联系技术支持 |

### 错误处理示例

```bash
# 错误请求示例
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"invalid": "request"}'

# 错误响应示例
{
  "success": false,
  "message": "请求参数无效",
  "errorCode": "INVALID_REQUEST",
  "timestamp": "2025-09-07T00:00:00Z"
}
```

## 使用示例

### 1. 完整的RAG流程示例

```bash
# 1. 上传文档
curl -X POST http://localhost:8080/api/rag/upload-text \
  -H "Content-Type: text/plain" \
  -d "人工智能是计算机科学的一个分支，它企图了解智能的实质。"

# 2. 执行查询
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是人工智能？"}'

# 3. 检索相关文档
curl "http://localhost:8080/api/rag/retrieve?query=人工智能"

# 4. 查看系统状态
curl http://localhost:8080/api/rag/status
```

### 2. 批量文档处理示例

```bash
# 批量上传多个文档
for file in *.txt; do
  curl -X POST http://localhost:8080/api/rag/upload \
    -F "file=@$file"
done

# 执行多个查询
queries=("什么是机器学习？" "深度学习的优势是什么？" "自然语言处理的应用场景？")
for query in "${queries[@]}"; do
  curl -X POST http://localhost:8080/api/rag/query \
    -H "Content-Type: application/json" \
    -d "{\"query\": \"$query\"}"
done
```

### 3. 高级查询示例

```bash
# 使用自定义参数查询
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "机器学习和深度学习的区别",
    "maxResults": 5,
    "minScore": 0.8
  }'

# 检索高相似度文档
curl "http://localhost:8080/api/rag/retrieve-with-params?query=神经网络&maxResults=3&minScore=0.9"
```

## 性能优化建议

### 1. 批量操作
- 使用批量上传接口处理多个文档
- 避免频繁的单个文档上传

### 2. 参数调优
- 根据数据特点调整`maxResults`和`minScore`参数
- 平衡检索质量和响应速度

### 3. 缓存策略
- 对相同查询使用缓存结果
- 避免重复的文档处理

### 4. 异步处理
- 对于大量文档处理，考虑异步处理
- 使用回调或轮询机制获取处理结果

## 安全注意事项

### 1. 输入验证
- 验证上传文件的大小和格式
- 过滤恶意查询内容

### 2. 访问控制
- 实现API访问认证
- 限制API调用频率

### 3. 数据保护
- 加密敏感数据
- 定期清理临时数据

## 监控和调试

### 1. 日志查看
```bash
# 查看应用日志
tail -f logs/application.log

# 查看错误日志
grep "ERROR" logs/application.log
```

### 2. 性能监控
```bash
# 查看系统状态
curl http://localhost:8080/api/rag/status

# 查看文档数量
curl http://localhost:8080/api/rag/document-count
```

### 3. 调试技巧
- 使用详细的错误信息定位问题
- 检查网络连接和API配置
- 验证输入数据的格式和内容

## 总结

Z-RAG API提供了完整的RAG功能接口，支持文档管理、智能查询、向量检索等功能。通过合理使用这些接口，可以构建强大的RAG应用系统。建议根据实际需求选择合适的接口和参数，并注意错误处理和性能优化。
