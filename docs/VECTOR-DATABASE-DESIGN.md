# 向量数据库设计文档

## 🎯 设计目标

基于RAG系统的需求，设计一个支持多租户、多文件的向量数据库结构，包含主键ID、文件ID、租户ID和向量化数据。

## 📊 数据库结构设计

### 1. 核心字段定义

| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | INT64 | Primary Key, Auto ID | 主键，自动生成 |
| `file_id` | VARCHAR(64) | NOT NULL, Index | 文件唯一标识符 |
| `tenant_id` | VARCHAR(64) | NOT NULL, Index | 租户标识符 |
| `chunk_id` | VARCHAR(128) | NOT NULL, Index | 文档片段唯一标识 |
| `vector` | FLOAT_VECTOR | NOT NULL | 向量数据 |
| `content` | VARCHAR(65535) | NOT NULL | 原始文本内容 |
| `metadata` | JSON | NULL | 元数据信息 |
| `created_at` | INT64 | NOT NULL | 创建时间戳 |
| `updated_at` | INT64 | NOT NULL | 更新时间戳 |

### 2. 索引设计

#### 2.1 主键索引
- **字段**: `id`
- **类型**: Primary Key
- **特点**: 自动生成，唯一性保证

#### 2.2 业务索引
- **文件索引**: `file_id` - 支持按文件查询
- **租户索引**: `tenant_id` - 支持多租户隔离
- **片段索引**: `chunk_id` - 支持精确片段定位

#### 2.3 向量索引
- **字段**: `vector`
- **类型**: IVF_FLAT 或 IVF_PQ
- **距离度量**: COSINE
- **特点**: 支持高效相似性搜索

### 3. 分区策略

#### 3.1 按租户分区
```sql
-- 示例分区策略
PARTITION BY RANGE (tenant_id) (
    PARTITION p_tenant_1 VALUES LESS THAN ('tenant_1'),
    PARTITION p_tenant_2 VALUES LESS THAN ('tenant_2'),
    ...
);
```

#### 3.2 按文件分区（可选）
```sql
-- 按文件ID分区，提高查询效率
PARTITION BY HASH (file_id) PARTITIONS 16;
```

## 🔧 实现方案

### 1. Milvus集合创建

```java
public boolean createCollection(String collectionName, String description, int vectorDimension) {
    // 主键字段
    CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder()
            .autoID(true)
            .dataType(io.milvus.v2.common.DataType.Int64)
            .isPrimaryKey(true)
            .name("id")
            .build();

    // 文件ID字段
    CreateCollectionReq.FieldSchema fileIdField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.VarChar)
            .name("file_id")
            .isPrimaryKey(false)
            .maxLength(64)
            .build();

    // 租户ID字段
    CreateCollectionReq.FieldSchema tenantIdField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.VarChar)
            .name("tenant_id")
            .isPrimaryKey(false)
            .maxLength(64)
            .build();

    // 片段ID字段
    CreateCollectionReq.FieldSchema chunkIdField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.VarChar)
            .name("chunk_id")
            .isPrimaryKey(false)
            .maxLength(128)
            .build();

    // 向量字段
    CreateCollectionReq.FieldSchema vectorField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.FloatVector)
            .name("vector")
            .isPrimaryKey(false)
            .dimension(vectorDimension)
            .build();

    // 内容字段
    CreateCollectionReq.FieldSchema contentField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.VarChar)
            .name("content")
            .isPrimaryKey(false)
            .maxLength(65535)
            .build();

    // 元数据字段
    CreateCollectionReq.FieldSchema metadataField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.JSON)
            .name("metadata")
            .isPrimaryKey(false)
            .build();

    // 时间戳字段
    CreateCollectionReq.FieldSchema createdAtField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.Int64)
            .name("created_at")
            .isPrimaryKey(false)
            .build();

    CreateCollectionReq.FieldSchema updatedAtField = CreateCollectionReq.FieldSchema.builder()
            .dataType(io.milvus.v2.common.DataType.Int64)
            .name("updated_at")
            .isPrimaryKey(false)
            .build();

    // 字段列表
    List<CreateCollectionReq.FieldSchema> fieldSchemaList = Arrays.asList(
            idField, fileIdField, tenantIdField, chunkIdField, 
            vectorField, contentField, metadataField, 
            createdAtField, updatedAtField
    );

    // 创建集合
    CreateCollectionReq createCollectionReq = CreateCollectionReq.builder()
            .collectionName(collectionName)
            .description(description)
            .fieldSchemaList(fieldSchemaList)
            .build();

    return milvusClient.createCollection(createCollectionReq);
}
```

### 2. 数据插入

```java
public void addDocument(String fileId, String tenantId, String chunkId, 
                       List<Float> vector, String content, Map<String, Object> metadata) {
    
    long currentTime = System.currentTimeMillis();
    
    Map<String, Object> data = new HashMap<>();
    data.put("file_id", fileId);
    data.put("tenant_id", tenantId);
    data.put("chunk_id", chunkId);
    data.put("vector", vector);
    data.put("content", content);
    data.put("metadata", metadata);
    data.put("created_at", currentTime);
    data.put("updated_at", currentTime);
    
    // 插入数据到Milvus
    insertData(collectionName, Arrays.asList(data));
}
```

### 3. 多租户查询

```java
public List<Map<String, Object>> searchByTenant(String tenantId, List<Float> queryVector, 
                                               int maxResults, double minScore) {
    
    // 构建查询条件
    String filter = String.format("tenant_id == '%s'", tenantId);
    
    // 执行向量搜索
    return searchSimilar(collectionName, queryVector, maxResults, minScore, filter);
}
```

### 4. 按文件查询

```java
public List<Map<String, Object>> searchByFile(String fileId, List<Float> queryVector, 
                                             int maxResults, double minScore) {
    
    // 构建查询条件
    String filter = String.format("file_id == '%s'", fileId);
    
    // 执行向量搜索
    return searchSimilar(collectionName, queryVector, maxResults, minScore, filter);
}
```

## 🚀 性能优化建议

### 1. 索引优化
- 为`file_id`和`tenant_id`创建标量索引
- 使用合适的向量索引类型（IVF_FLAT用于小规模，IVF_PQ用于大规模）
- 定期重建索引以保持性能

### 2. 查询优化
- 使用过滤条件减少搜索范围
- 合理设置`nprobe`参数平衡精度和性能
- 使用批量查询减少网络开销

### 3. 存储优化
- 按租户分区减少单次查询数据量
- 使用压缩存储减少存储空间
- 定期清理过期数据

## 📈 监控指标

### 1. 性能指标
- 查询延迟（P50, P95, P99）
- 吞吐量（QPS）
- 索引构建时间

### 2. 存储指标
- 集合大小
- 向量数量
- 存储使用率

### 3. 业务指标
- 按租户的查询分布
- 按文件的查询分布
- 查询成功率

## 🔒 安全考虑

### 1. 数据隔离
- 租户间数据完全隔离
- 使用租户ID作为查询过滤条件
- 实现租户级别的权限控制

### 2. 访问控制
- API级别的认证和授权
- 查询日志记录
- 敏感数据脱敏

## 📝 使用示例

### 1. 创建文档向量
```java
// 为租户A的文件B创建向量
String fileId = "file_12345";
String tenantId = "tenant_A";
String chunkId = "chunk_001";
List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f, ...);
String content = "这是文档片段的内容";
Map<String, Object> metadata = Map.of(
    "page", 1,
    "section", "introduction",
    "author", "张三"
);

addDocument(fileId, tenantId, chunkId, vector, content, metadata);
```

### 2. 多租户查询
```java
// 在租户A中搜索相关文档
String tenantId = "tenant_A";
List<Float> queryVector = Arrays.asList(0.15f, 0.25f, 0.35f, ...);
List<Map<String, Object>> results = searchByTenant(tenantId, queryVector, 10, 0.7);
```

### 3. 按文件查询
```java
// 在特定文件中搜索
String fileId = "file_12345";
List<Float> queryVector = Arrays.asList(0.15f, 0.25f, 0.35f, ...);
List<Map<String, Object>> results = searchByFile(fileId, queryVector, 5, 0.8);
```

## 🎯 总结

这个设计提供了：

1. **完整的数据结构**：包含所有必需字段
2. **多租户支持**：通过租户ID实现数据隔离
3. **高效查询**：支持多种查询模式
4. **可扩展性**：支持大规模数据存储
5. **性能优化**：合理的索引和分区策略
6. **安全性**：数据隔离和访问控制

通过这个设计，您可以构建一个功能完整、性能优异的RAG系统向量数据库。
