# 文档分割器规则说明文档

## 概述

Z-RAG系统提供了强大的文档分割功能，支持多种分割策略和自定义参数。本文档详细说明了各种分割器的规则、配置和使用方法。

## 目录

1. [分割器类型](#分割器类型)
2. [分割规则详解](#分割规则详解)
3. [动态分割功能](#动态分割功能)
4. [API接口说明](#api接口说明)
5. [使用示例](#使用示例)
6. [最佳实践](#最佳实践)
7. [故障排除](#故障排除)

## 分割器类型

### 1. 默认分割器 (Default Splitter)

**配置参数:**
- 块大小 (chunkSize): 300字符
- 重叠大小 (chunkOverlap): 50字符
- 适用场景: 通用文档处理

**分隔符优先级:**
```
1. "\n\n"     - 段落分隔符（最高优先级）
2. "\n"       - 换行符
3. "。"       - 中文句号
4. "！"       - 中文感叹号
5. "？"       - 中文问号
6. "."        - 英文句号
7. "!"        - 英文感叹号
8. "?"        - 英文问号
9. ";"        - 分号
10. ":"       - 冒号
11. ","       - 逗号
12. " "       - 空格（最低优先级）
13. ""        - 字符级别分割（最后选择）
```

### 2. 中文文档分割器 (Chinese Splitter)

**配置参数:**
- 块大小 (chunkSize): 500字符
- 重叠大小 (chunkOverlap): 100字符
- 适用场景: 中文文档优化

**分隔符优先级:**
```
1. "\n\n"     - 段落分隔符
2. "\n"       - 换行符
3. "。"       - 中文句号
4. "！"       - 中文感叹号
5. "？"       - 中文问号
6. "；"       - 中文分号
7. "："       - 中文冒号
8. "，"       - 中文逗号
9. "、"       - 中文顿号
10. "."       - 英文句号
11. "!"       - 英文感叹号
12. "?"       - 英文问号
13. ";"       - 分号
14. ":"       - 冒号
15. ","       - 逗号
16. " "       - 空格
17. ""        - 字符级别分割
```

### 3. 代码文档分割器 (Code Splitter)

**配置参数:**
- 块大小 (chunkSize): 1000字符
- 重叠大小 (chunkOverlap): 200字符
- 适用场景: 代码文件处理

**分隔符优先级:**
```
1. "\n\n"     - 空行分隔符
2. "\n"       - 换行符
3. ";"        - 分号
4. "}"        - 右大括号
5. ")"        - 右括号
6. "]"        - 右方括号
7. "\t"       - 制表符
8. " "        - 空格
9. ""         - 字符级别分割
```

## 分割规则详解

### 递归分割算法

Z-RAG使用递归字符分割算法，工作原理如下：

1. **优先级分割**: 按照分隔符优先级列表，从高到低尝试分割
2. **块大小控制**: 确保每个分割块不超过指定的chunkSize
3. **重叠处理**: 在相邻块之间保持chunkOverlap个字符的重叠
4. **递归处理**: 如果使用高优先级分隔符无法满足块大小要求，则使用下一优先级分隔符

### 分割流程

```
原始文档
    ↓
尝试段落分割 (\n\n)
    ↓
检查块大小是否合适
    ↓
如果太大 → 尝试换行分割 (\n)
    ↓
检查块大小是否合适
    ↓
如果太大 → 尝试句子分割 (。!?)
    ↓
...继续递归...
    ↓
最终分割结果
```

### 智能分割器选择

系统会根据以下规则自动选择分割器：

1. **文件类型检测**: 根据文件扩展名判断
   - 代码文件: `.java`, `.js`, `.py`, `.cpp`, `.md` 等
   - 使用代码分割器

2. **内容语言检测**: 根据中文字符占比判断
   - 中文字符占比 > 30%: 使用中文分割器
   - 否则使用默认分割器

3. **文档长度检测**: 根据文档长度判断
   - 文档长度 > 5000字符: 使用中文分割器
   - 否则使用默认分割器

## 动态分割功能

### 支持的参数

- **chunkSize**: 块大小（字符数）
- **chunkOverlap**: 重叠大小（字符数）
- **customSeparators**: 自定义分隔符列表（可选）

### 使用方式

#### 1. 基本动态分割
```java
// 使用默认分隔符
List<TextSegment> segments = smartSplitterService.dynamicSplit(
    document, 
    400,  // 块大小
    80    // 重叠大小
);
```

#### 2. 自定义分隔符分割
```java
// 使用自定义分隔符
List<TextSegment> segments = smartSplitterService.dynamicSplit(
    document, 
    400,  // 块大小
    80,   // 重叠大小
    "###", "##", "#", "\n\n", "\n"  // 自定义分隔符
);
```

#### 3. 按文件类型分割
```java
// 根据文件类型选择分割器
List<TextSegment> segments = smartSplitterService.splitByFileType(
    document, 
    "document.md", 
    500,  // 块大小
    100   // 重叠大小
);
```

## API接口说明

### 1. 动态分割文档

**接口**: `POST /api/rag/document/split`

**请求参数**:
```json
{
    "content": "文档内容",
    "fileName": "document.txt",
    "chunkSize": 300,
    "chunkOverlap": 50,
    "customSeparators": ["\n\n", "\n", "。", ".", " "]
}
```

**响应格式**:
```json
{
    "success": true,
    "message": "文档分割成功",
    "fileName": "document.txt",
    "chunkSize": 300,
    "chunkOverlap": 50,
    "segmentCount": 15,
    "segments": [
        "第一个文档片段...",
        "第二个文档片段...",
        "..."
    ],
    "customSeparators": ["\n\n", "\n", "。", ".", " "]
}
```

### 2. 获取分割器配置

**接口**: `GET /api/rag/document/splitter/config`

**响应格式**:
```json
{
    "default": {
        "chunkSize": 300,
        "chunkOverlap": 50,
        "separators": ["\n\n", "\n", "。", "！", "？", ".", "!", "?", ";", ":", ",", " ", ""]
    },
    "chinese": {
        "chunkSize": 500,
        "chunkOverlap": 100,
        "separators": ["\n\n", "\n", "。", "！", "？", "；", "：", "，", "、", ".", "!", "?", ";", ":", ",", " ", ""]
    },
    "code": {
        "chunkSize": 1000,
        "chunkOverlap": 200,
        "separators": ["\n\n", "\n", ";", "}", ")", "]", "\t", " ", ""]
    },
    "supportedFileTypes": ["java", "js", "ts", "py", "cpp", "c", "h", "cs", "go", "rs", "php", "rb", "swift", "kt", "scala", "xml", "json", "yaml", "yml", "sql", "sh", "bat", "md", "txt", "pdf", "docx"]
}
```

## 使用示例

### 1. 基本使用

```java
@Service
public class DocumentProcessingService {
    
    @Autowired
    private SmartDocumentSplitterService smartSplitterService;
    
    public void processDocument(String content, String fileName) {
        Document document = Document.from(content);
        
        // 智能分割
        List<TextSegment> segments = smartSplitterService.smartSplit(document, fileName);
        
        // 处理分割结果
        for (TextSegment segment : segments) {
            System.out.println("片段: " + segment.text());
        }
    }
}
```

### 2. 自定义分割参数

```java
public void customSplit(String content, String fileName) {
    Document document = Document.from(content);
    
    // 自定义分割参数
    List<TextSegment> segments = smartSplitterService.dynamicSplit(
        document, 
        500,  // 更大的块大小
        100,  // 更多的重叠
        "###", "##", "#", "\n\n"  // 自定义分隔符
    );
    
    System.out.println("分割完成，共 " + segments.size() + " 个片段");
}
```

### 3. REST API调用

```bash
# 动态分割文档
curl -X POST http://localhost:8080/api/rag/document/split \
  -H "Content-Type: application/json" \
  -d '{
    "content": "这是一个测试文档。\n\n包含多个段落。\n\n每个段落都有不同的内容。",
    "fileName": "test.txt",
    "chunkSize": 200,
    "chunkOverlap": 50,
    "customSeparators": ["\n\n", "\n", "。"]
  }'

# 获取分割器配置
curl -X GET http://localhost:8080/api/rag/document/splitter/config
```

## 最佳实践

### 1. 块大小选择

- **小文档 (< 1000字符)**: 使用较小的块大小 (200-300字符)
- **中等文档 (1000-5000字符)**: 使用中等块大小 (300-500字符)
- **大文档 (> 5000字符)**: 使用较大的块大小 (500-1000字符)

### 2. 重叠大小设置

- **一般文档**: 重叠大小为块大小的 10-20%
- **技术文档**: 重叠大小为块大小的 20-30%
- **代码文件**: 重叠大小为块大小的 20-25%

### 3. 分隔符选择

- **中文文档**: 优先使用中文标点符号
- **英文文档**: 优先使用英文标点符号
- **代码文档**: 使用代码结构相关的分隔符
- **混合文档**: 同时包含中英文分隔符

### 4. 性能优化

- **批量处理**: 对于大量文档，使用批量处理API
- **缓存结果**: 对于相同参数的文档，可以缓存分割结果
- **异步处理**: 对于大文档，使用异步处理避免阻塞

## 故障排除

### 1. 常见问题

**问题**: 分割结果不理想
**解决方案**: 
- 调整块大小和重叠大小
- 检查分隔符优先级设置
- 使用自定义分隔符

**问题**: 分割速度慢
**解决方案**:
- 减少重叠大小
- 使用更简单的分隔符
- 考虑使用异步处理

**问题**: 内存占用过高
**解决方案**:
- 减少块大小
- 分批处理大文档
- 优化分隔符列表

### 2. 调试技巧

1. **启用调试日志**: 查看分割过程的详细信息
2. **检查分割统计**: 使用 `getSplitterStats()` 方法
3. **测试不同参数**: 尝试不同的块大小和重叠设置
4. **验证分隔符**: 确保分隔符设置正确

### 3. 性能监控

```java
// 获取分割统计信息
String stats = smartSplitterService.getSplitterStats(segments);
log.info("分割统计: {}", stats);
```

## 总结

Z-RAG的文档分割器提供了灵活、强大的文档处理能力。通过合理配置分割参数和选择合适的分割策略，可以获得最佳的文档处理效果。建议根据具体的文档类型和业务需求，选择合适的分割器配置。

---

**版本**: 1.0  
**更新时间**: 2025-09-16  
**维护者**: Z-RAG开发团队
