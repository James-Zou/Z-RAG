# Z-RAG 技术实现细节

## 系统架构概览

Z-RAG系统基于Spring Boot和LangChain4j框架构建，采用分层架构设计，实现了完整的RAG（检索增强生成）流程。

## 核心组件实现

### 1. 配置层 (Configuration Layer)

#### RagConfig.java - 核心配置类
```java
@Configuration
public class RagConfig {
    
    // 嵌入模型配置
    @Bean
    public EmbeddingModel embeddingModel() {
        // 支持OpenAI和本地模型切换
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            return OpenAiEmbeddingModel.builder()
                    .apiKey(openaiApiKey)
                    .baseUrl(openaiBaseUrl)
                    .modelName(openaiEmbeddingModel)
                    .build();
        } else {
            return new AllMiniLmL6V2EmbeddingModel();
        }
    }
    
    // 聊天模型配置
    @Bean
    public ChatLanguageModel chatModel() {
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            return OpenAiChatModel.builder()
                    .apiKey(openaiApiKey)
                    .baseUrl(openaiBaseUrl)
                    .modelName(openaiModel)
                    .temperature(0.7)
                    .build();
        }
        return null; // 使用模拟模型
    }
    
    // 向量存储配置
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore();
    }
    
    // 内容检索器配置
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, 
                                           EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .build();
    }
}
```

### 2. 服务层 (Service Layer)

#### DocumentService.java - 文档处理服务

**核心功能**:
- 文档加载和解析
- 文档分割和预处理
- 向量化和存储

**实现细节**:
```java
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;
    
    public void processDocuments(List<Document> documents) {
        try {
            // 1. 创建嵌入存储摄取器
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            
            // 2. 批量处理文档
            ingestor.ingest(documents);
            
        } catch (Exception e) {
            log.error("文档处理失败", e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }
}
```

**文档分割策略**:
- 使用递归分割器，保持语义完整性
- 默认块大小300字符，重叠0字符
- 支持自定义分割参数

#### RetrievalService.java - 检索服务

**核心功能**:
- 查询向量化
- 相似性搜索
- 结果排序和过滤

**实现细节**:
```java
@Service
@RequiredArgsConstructor
public class RetrievalService {
    
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    
    public List<EmbeddingMatch<TextSegment>> retrieve(String query, int maxResults, double minScore) {
        try {
            // 1. 查询向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 2. 相似性搜索
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    queryEmbedding, maxResults, minScore);
            
            // 3. 结果排序
            return matches.stream()
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("检索失败", e);
            throw new RuntimeException("检索失败: " + e.getMessage(), e);
        }
    }
}
```

**检索优化**:
- 支持多种相似度计算方法
- 可配置的检索参数
- 结果缓存机制

#### GenerationService.java - 生成服务

**核心功能**:
- 上下文构建
- 提示词生成
- 回答生成

**实现细节**:
```java
@Service
@RequiredArgsConstructor
public class GenerationService {
    
    private final ChatLanguageModel chatModel;
    
    public String generateAnswer(String query, List<String> retrievedDocuments) {
        try {
            if (chatModel == null) {
                return generateAnswerWithMockModel(query, retrievedDocuments);
            }
            
            // 1. 构建上下文
            StringBuilder context = new StringBuilder();
            context.append("基于以下文档内容回答用户的问题：\n\n");
            
            for (int i = 0; i < retrievedDocuments.size(); i++) {
                context.append("文档片段 ").append(i + 1).append(":\n");
                context.append(retrievedDocuments.get(i)).append("\n\n");
            }
            
            // 2. 构建提示词
            String prompt = String.format(
                    "%s\n\n用户问题: %s\n\n请基于上述文档内容提供准确、详细的回答。",
                    context.toString(), query
            );
            
            // 3. 生成回答
            return chatModel.generate(prompt);
            
        } catch (Exception e) {
            log.error("生成回答失败", e);
            return generateAnswerWithMockModel(query, retrievedDocuments);
        }
    }
}
```

**生成优化**:
- 智能上下文压缩
- 多轮对话支持
- 流式输出支持

#### RagService.java - RAG核心服务

**核心功能**:
- 整合所有RAG组件
- 提供统一的RAG接口
- 流程控制和异常处理

**实现细节**:
```java
@Service
@RequiredArgsConstructor
public class RagService {
    
    private final DocumentService documentService;
    private final RetrievalService retrievalService;
    private final GenerationService generationService;
    private final ChatLanguageModel chatModel;
    private final ContentRetriever contentRetriever;
    
    public String query(String query) {
        try {
            // 1. 检索相关文档
            List<EmbeddingMatch<TextSegment>> matches = retrievalService.retrieve(query);
            
            if (matches.isEmpty()) {
                return "抱歉，没有找到与您查询相关的文档内容。";
            }
            
            // 2. 提取文档内容
            List<String> retrievedDocuments = matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.toList());
            
            // 3. 生成回答
            String answer = generationService.generateAnswer(query, retrievedDocuments);
            
            return answer;
            
        } catch (Exception e) {
            log.error("RAG查询失败", e);
            return "查询失败: " + e.getMessage();
        }
    }
}
```

### 3. 存储层 (Storage Layer)

#### InMemoryEmbeddingStore.java - 内存向量存储

**核心功能**:
- 向量存储和管理
- 相似性搜索
- 数据持久化

**实现细节**:
```java
public class InMemoryEmbeddingStore implements EmbeddingStore<TextSegment> {
    
    private final Map<String, EmbeddingData> embeddings = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = generateId();
        embeddings.put(id, new EmbeddingData(embedding, textSegment));
        return id;
    }
    
    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = add(embeddings.get(i), textSegments.get(i));
            ids.add(id);
        }
        return ids;
    }
    
    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, 
                                                         int maxResults, 
                                                         double minScore) {
        return embeddings.values().stream()
                .map(data -> new EmbeddingMatch<>(
                        calculateSimilarity(referenceEmbedding, data.embedding),
                        data.embedding,
                        data.textSegment
                ))
                .filter(match -> match.score() >= minScore)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    private double calculateSimilarity(Embedding embedding1, Embedding embedding2) {
        float[] vector1 = embedding1.vector();
        float[] vector2 = embedding2.vector();
        
        if (vector1.length != vector2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
```

### 4. 控制层 (Controller Layer)

#### RagController.java - REST API控制器

**核心功能**:
- 提供HTTP API接口
- 请求参数验证
- 响应格式化

**API接口**:
```java
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {
    
    private final RagService ragService;
    
    // 文档上传
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(@RequestParam("file") MultipartFile file);
    
    // 文本上传
    @PostMapping("/upload-text")
    public ResponseEntity<UploadResponse> uploadText(@RequestBody String content);
    
    // RAG查询
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request);
    
    // 文档检索
    @GetMapping("/retrieve")
    public ResponseEntity<List<String>> retrieveDocuments(@RequestParam String query);
    
    // 系统状态
    @GetMapping("/status")
    public ResponseEntity<String> getSystemStatus();
}
```

## 数据流设计

### 1. 文档处理流程

```
原始文档 → 文档解析 → 文档分割 → 向量化 → 向量存储
    ↓
[Document] → [TextSegment[]] → [Embedding[]] → [VectorStore]
```

### 2. 查询处理流程

```
用户查询 → 查询向量化 → 相似性搜索 → 结果排序 → 上下文构建 → 回答生成
    ↓
[String] → [Embedding] → [EmbeddingMatch[]] → [String[]] → [String] → [String]
```

## 性能优化策略

### 1. 内存优化
- 使用ConcurrentHashMap提高并发性能
- 实现LRU缓存机制
- 定期清理过期数据

### 2. 计算优化
- 向量相似度计算优化
- 批量处理减少I/O开销
- 异步处理提升响应速度

### 3. 存储优化
- 支持多种向量存储后端
- 数据压缩减少存储空间
- 索引优化提升查询速度

## 错误处理机制

### 1. 异常分类
- **配置异常**: 模型配置错误
- **网络异常**: API调用失败
- **数据异常**: 文档处理失败
- **业务异常**: 查询处理失败

### 2. 异常处理策略
```java
try {
    // 业务逻辑
} catch (ConfigurationException e) {
    log.error("配置错误", e);
    return errorResponse("系统配置错误");
} catch (NetworkException e) {
    log.error("网络错误", e);
    return errorResponse("网络连接失败");
} catch (Exception e) {
    log.error("未知错误", e);
    return errorResponse("系统内部错误");
}
```

## 监控和日志

### 1. 关键指标监控
- 文档处理数量
- 查询响应时间
- 检索准确率
- 系统资源使用率

### 2. 日志记录
```java
@Slf4j
public class RagService {
    
    public String query(String query) {
        log.info("执行RAG查询: {}", query);
        
        try {
            // 业务逻辑
            log.info("RAG查询完成");
            return answer;
        } catch (Exception e) {
            log.error("RAG查询失败", e);
            return "查询失败: " + e.getMessage();
        }
    }
}
```

## 扩展性设计

### 1. 插件化架构
- 支持自定义嵌入模型
- 支持自定义向量存储
- 支持自定义生成模型

### 2. 配置化支持
- 通过配置文件调整参数
- 支持环境变量配置
- 支持动态配置更新

### 3. 微服务化
- 服务拆分和独立部署
- 服务间通信和协调
- 负载均衡和容错

## 总结

Z-RAG系统通过精心设计的架构和实现，提供了完整的RAG功能。系统具有良好的可扩展性、可维护性和性能，能够满足不同场景的需求。通过模块化设计和清晰的接口定义，系统为构建高质量的RAG应用提供了坚实的基础。
