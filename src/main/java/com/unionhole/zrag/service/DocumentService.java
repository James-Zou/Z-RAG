/*
 * Copyright 2025 james-zou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.unionhole.zrag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import com.unionhole.zrag.store.InMemoryEmbeddingStore;
import com.unionhole.zrag.store.WeaviateEmbeddingStore;
import com.unionhole.zrag.store.MilvusEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 文档处理服务
 * 使用LangChain4j的完整文档处理功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;
    private final SmartDocumentSplitterService smartSplitterService;
    private final MinioStorageService minioStorageService;
    private final TikaDocumentParserService tikaParser;

    @Value("${storage.type:minio}")
    private String storageType;
    
    /**
     * 获取嵌入存储实例
     */
    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    /**
     * 处理文档并存储到向量数据库
     * @param documents 文档列表
     */
    public void processDocuments(List<Document> documents) {
        log.info("开始处理 {} 个文档", documents.size());
        
        try {
            // 使用注入的文档分割器
            DocumentSplitter splitter = documentSplitter;
            
            // 创建嵌入存储摄取器
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            
            // 处理并存储文档
            ingestor.ingest(documents);
            
            log.info("文档处理完成，已存储到向量数据库");
        } catch (Exception e) {
            log.error("文档处理失败", e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理单个文档
     * @param document 文档
     */
    public void processDocument(Document document) {
        processDocuments(java.util.Arrays.asList(document));
    }

    /**
     * 处理文本内容
     * @param content 文本内容
     */
    public void processText(String content) {
        Document document = Document.from(content);
        processDocument(document);
    }

    /**
     * 处理上传的文件
     * @param file 上传的文件
     * @return 文件存储路径
     */
    public String processUploadedFile(MultipartFile file) {
        try {
            log.info("处理上传文件: {}", file.getOriginalFilename());
            
            // 存储文件到MinIO
            String filePath = minioStorageService.uploadFile(file);
            
            // 生成文件ID和租户ID
            String fileId = generateFileId(file.getOriginalFilename());
            String tenantId = getCurrentTenantId(); // 可以从安全上下文或配置中获取
            
            // 处理文档并存储到向量数据库
            processDocumentWithMetadata(file, fileId, tenantId);
            
            log.info("文件处理完成: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("处理上传文件失败", e);
            throw new RuntimeException("处理上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理文本内容并存储
     * @param content 文本内容
     * @return 文件存储路径
     */
    public String processTextWithStorage(String content) {
        try {
            log.info("处理文本内容并存储");
            
            // 存储文本到MinIO
            String filePath = minioStorageService.uploadText(content, "text_content.txt");
            
            // 处理文档
            Document document = Document.from(content);
            processDocument(document);
            
            log.info("文本内容处理完成: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("处理文本内容失败", e);
            throw new RuntimeException("处理文本内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取向量数据库中的文档数量
     * @return 文档数量
     */
    public long getDocumentCount() {
        try {
            // 根据不同的EmbeddingStore实现获取文档数量
            if (embeddingStore instanceof InMemoryEmbeddingStore) {
                InMemoryEmbeddingStore store = (InMemoryEmbeddingStore) embeddingStore;
                return store.findAll().size();
            } else if (embeddingStore instanceof WeaviateEmbeddingStore) {
                WeaviateEmbeddingStore store = (WeaviateEmbeddingStore) embeddingStore;
                return store.findAll().size();
            }
            return 0;
        } catch (Exception e) {
            log.warn("获取文档数量失败", e);
            return 0;
        }
    }
    
    /**
     * 从向量库中获取文件名
     * @param objectName MinIO存储的对象名
     * @return 原始文件名，如果不存在则返回null
     */
    public String getFileNameFromVectorStore(String objectName) {
        try {
            // 简化实现：直接返回null，让前端使用默认的文件名提取逻辑
            // 这样可以避免复杂的向量库查询，提高性能
            log.debug("从向量库获取文件名: {}", objectName);
            return null;
        } catch (Exception e) {
            log.warn("从向量库获取文件名失败: {}", objectName, e);
            return null;
        }
    }

    /**
     * 清空向量数据库
     */
    public void clearDocuments() {
        try {
            if (embeddingStore instanceof InMemoryEmbeddingStore) {
                InMemoryEmbeddingStore store = (InMemoryEmbeddingStore) embeddingStore;
                store.clear();
                log.info("内存向量数据库已清空");
            } else if (embeddingStore instanceof WeaviateEmbeddingStore) {
                WeaviateEmbeddingStore store = (WeaviateEmbeddingStore) embeddingStore;
                store.clear();
                log.info("Weaviate向量数据库已清空");
            } else if (embeddingStore instanceof MilvusEmbeddingStore) {
                MilvusEmbeddingStore store = (MilvusEmbeddingStore) embeddingStore;
                store.clear();
                log.info("Milvus向量数据库已清空");
            } else {
                log.warn("当前EmbeddingStore不支持清空操作");
            }
        } catch (Exception e) {
            log.error("清空向量数据库失败", e);
            throw new RuntimeException("清空向量数据库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理带元数据的文档（支持多租户和文件ID）
     * @param document 文档对象
     * @param fileId 文件ID
     * @param tenantId 租户ID
     * @param fileName 文件名
     */
    public void processDocumentWithMetadata(Document document, String fileId, String tenantId, String fileName) {
        try {
            log.info("处理带元数据的文档: fileId={}, tenantId={}, fileName={}", fileId, tenantId, fileName);
            
            // 使用智能分割器分割文档
            List<TextSegment> segments = smartSplitterService.smartSplit(document, fileName);
            
            log.info("文档分割完成，共 {} 个片段", segments.size());
            log.info("分割统计: {}", smartSplitterService.getSplitterStats(segments));
            
            // 处理每个片段
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                String chunkId = generateChunkId(fileId, i);
                
                // 生成嵌入向量
                log.info("开始生成嵌入向量: chunkId={}, textLength={}", chunkId, segment.text().length());
                long startTime = System.currentTimeMillis();
                Embedding embedding = embeddingModel.embed(segment).content();
                long endTime = System.currentTimeMillis();
                log.info("嵌入向量生成完成: chunkId={}, vectorSize={}, 耗时={}ms", 
                        chunkId, embedding.vector().length, (endTime - startTime));
                
                // 创建元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("file_id", fileId);
                metadata.put("tenant_id", tenantId);
                metadata.put("chunk_id", chunkId);
                metadata.put("file_name", fileName);
                metadata.put("file_type", getFileType(fileName));
                metadata.put("chunk_index", i);
                metadata.put("total_chunks", segments.size());
                metadata.put("created_at", System.currentTimeMillis());
                
                // 存储到向量数据库
                if (embeddingStore instanceof MilvusEmbeddingStore) {
                    MilvusEmbeddingStore milvusStore = (MilvusEmbeddingStore) embeddingStore;
                    // 使用新的方法存储带元数据的向量
                    storeDocumentVector(milvusStore, fileId, tenantId, chunkId, 
                                     embedding, segment.text(), metadata);
                } else {
                    // 对于其他类型的存储，使用原有方法
                    embeddingStore.add(embedding, segment);
                }
            }
            
            log.info("文档处理完成: fileId={}, 处理了 {} 个片段", fileId, segments.size());
            
        } catch (Exception e) {
            log.error("处理带元数据的文档失败", e);
            throw new RuntimeException("处理带元数据的文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理带元数据的文档（支持多租户和文件ID）
     * @param file 上传的文件
     * @param fileId 文件ID
     * @param tenantId 租户ID
     */
    public void processDocumentWithMetadata(MultipartFile file, String fileId, String tenantId) {
        try {
            log.info("=== RAG 索引阶段开始 ===");
            log.info("文件信息: fileId={}, tenantId={}, fileName={}, fileSize={} bytes", 
                    fileId, tenantId, file.getOriginalFilename(), file.getSize());
            
            // 1. 文档读取阶段
            log.info("--- 步骤1: 文档读取 ---");
            long startTime = System.currentTimeMillis();
            String content = readFileWithCorrectEncoding(file);
            Document document = Document.from(content);
            long endTime = System.currentTimeMillis();
            log.info("文档读取完成，内容长度: {} 字符，耗时: {} ms", 
                    content.length(), (endTime - startTime));
            
            // 2. 文档分割阶段
            log.info("--- 步骤2: 文档分割 ---");
            log.info("开始使用智能分割器分割文档...");
            startTime = System.currentTimeMillis();
            List<TextSegment> segments = smartSplitterService.smartSplit(document, file.getOriginalFilename());
            endTime = System.currentTimeMillis();
            
            log.info("文档分割完成，共 {} 个片段，耗时: {} ms", 
                    segments.size(), (endTime - startTime));
            log.info("分割统计: {}", smartSplitterService.getSplitterStats(segments));
            
            // 3. 向量化和存储阶段
            log.info("--- 步骤3: 向量化和存储 ---");
            log.info("开始处理 {} 个文档片段...", segments.size());
            
            int successCount = 0;
            int failCount = 0;
            long totalVectorizationTime = 0;
            long totalStorageTime = 0;
            
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                String chunkId = generateChunkId(fileId, i);
                
                try {
                    // 3.1 生成嵌入向量
                    log.info("处理片段 {}/{}: chunkId={}, textLength={}", 
                            i + 1, segments.size(), chunkId, segment.text().length());
                    
                    long vectorStartTime = System.currentTimeMillis();
                    Embedding embedding = embeddingModel.embed(segment).content();
                    long vectorEndTime = System.currentTimeMillis();
                    long vectorTime = vectorEndTime - vectorStartTime;
                    totalVectorizationTime += vectorTime;
                    
                    log.info("向量化完成: chunkId={}, vectorSize={}, 耗时={}ms", 
                            chunkId, embedding.vector().length, vectorTime);
                    
                    // 3.2 创建元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("file_id", fileId);
                    metadata.put("tenant_id", tenantId);
                    metadata.put("chunk_id", chunkId);
                    metadata.put("file_name", file.getOriginalFilename());
                    metadata.put("file_type", getFileType(file.getOriginalFilename()));
                    metadata.put("chunk_index", i);
                    metadata.put("total_chunks", segments.size());
                    metadata.put("created_at", System.currentTimeMillis());
                    
                    log.debug("元数据: {}", metadata);
                    
                    // 3.3 存储到向量数据库
                    long storageStartTime = System.currentTimeMillis();
                    if (embeddingStore instanceof MilvusEmbeddingStore) {
                        MilvusEmbeddingStore milvusStore = (MilvusEmbeddingStore) embeddingStore;
                        // 使用新的方法存储带元数据的向量
                        storeDocumentVector(milvusStore, fileId, tenantId, chunkId, 
                                         embedding, segment.text(), metadata);
                    } else {
                        // 对于其他类型的存储，使用原有方法
                        embeddingStore.add(embedding, segment);
                    }
                    long storageEndTime = System.currentTimeMillis();
                    long storageTime = storageEndTime - storageStartTime;
                    totalStorageTime += storageTime;
                    
                    log.info("存储完成: chunkId={}, 耗时={}ms", chunkId, storageTime);
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("处理片段失败: chunkId={}", chunkId, e);
                    failCount++;
                }
            }
            
            // 4. 索引阶段总结
            log.info("--- 索引阶段总结 ---");
            log.info("处理统计: 总片段数={}, 成功={}, 失败={}", 
                    segments.size(), successCount, failCount);
            log.info("性能统计: 总向量化耗时={}ms, 总存储耗时={}ms, 平均向量化耗时={}ms, 平均存储耗时={}ms", 
                    totalVectorizationTime, totalStorageTime, 
                    successCount > 0 ? totalVectorizationTime / successCount : 0,
                    successCount > 0 ? totalStorageTime / successCount : 0);
            
            log.info("=== RAG 索引阶段完成 ===");
            log.info("文档处理完成: fileId={}, 成功处理了 {} 个片段", fileId, successCount);
            
        } catch (Exception e) {
            log.error("处理带元数据的文档失败", e);
            throw new RuntimeException("处理带元数据的文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 存储文档向量到Milvus
     */
    private void storeDocumentVector(MilvusEmbeddingStore milvusStore, String fileId, String tenantId, 
                                   String chunkId, Embedding embedding, String content, 
                                   Map<String, Object> metadata) {
        try {
            // 转换float[]为List<Float>
            List<Float> vector = new ArrayList<>();
            for (float f : embedding.vector()) {
                vector.add(f);
            }
            
            // 使用MilvusLLMUtils存储带元数据的向量
            boolean success = milvusStore.getMilvusUtils().addDocumentVector(
                milvusStore.getCollectionName(), fileId, tenantId, chunkId, 
                vector, content, metadata
            );
            
            if (!success) {
                throw new RuntimeException("存储文档向量失败");
            }
            
        } catch (Exception e) {
            // 检查是否是维度不匹配的错误
            if (e.getMessage() != null && e.getMessage().contains("dimension")) {
                log.error("向量维度不匹配，需要重新创建集合: {}", e.getMessage());
                try {
                    log.info("尝试重新创建Milvus集合...");
                    milvusStore.forceRecreateCollection();
                    
                    // 重新尝试存储
                    log.info("重新尝试存储文档向量...");
                    List<Float> retryVector = new ArrayList<>();
                    for (float f : embedding.vector()) {
                        retryVector.add(f);
                    }
                    boolean success = milvusStore.getMilvusUtils().addDocumentVector(
                        milvusStore.getCollectionName(), fileId, tenantId, chunkId, 
                        retryVector, content, metadata
                    );
                    
                    if (!success) {
                        throw new RuntimeException("重新存储文档向量失败");
                    }
                    
                    log.info("重新存储文档向量成功");
                } catch (Exception retryException) {
                    log.error("重新创建集合和存储向量失败", retryException);
                    throw new RuntimeException("存储文档向量失败: " + retryException.getMessage(), retryException);
                }
            } else {
                log.error("存储文档向量失败", e);
                throw new RuntimeException("存储文档向量失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 生成文件ID
     */
    private String generateFileId(String fileName) {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * 生成片段ID
     */
    private String generateChunkId(String fileId, int chunkIndex) {
        return fileId + "_chunk_" + chunkIndex;
    }
    
    /**
     * 获取当前租户ID（可以从安全上下文或配置中获取）
     */
    private String getCurrentTenantId() {
        // 这里可以从Spring Security上下文或配置中获取
        // 暂时使用默认值
        return "default_tenant";
    }
    
    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 使用Apache Tika统一解析文档为文本格式
     * 
     * @param file 上传的文件
     * @return 解析后的文本内容
     */
    private String readFileWithCorrectEncoding(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            byte[] fileBytes = file.getBytes();
            
            log.info("=== 文档解析处理开始 ===");
            log.info("文件信息: fileName={}, contentType={}, size={} bytes", 
                    fileName, contentType, fileBytes.length);
            
            // 检测文档类型
            String detectedType = tikaParser.detectDocumentType(file);
            log.info("检测到文档类型: {}", detectedType);
            
            // 优先使用Tika解析，支持更多文档类型
            log.info("尝试使用Tika解析文档: {}", fileName);
            try {
                String parsedText = tikaParser.parseDocument(file);
                if (parsedText != null && !parsedText.trim().isEmpty() && 
                    !parsedText.startsWith("[文档解析失败") && 
                    !parsedText.startsWith("[文档解析异常")) {
                    log.info("Tika解析成功，文本长度: {} 字符", parsedText.length());
                    return parsedText;
                } else {
                    log.warn("Tika解析返回空结果或失败信息，使用备用方法");
                }
            } catch (Exception e) {
                log.warn("Tika解析过程中发生异常，使用备用方法: {}", e.getMessage());
            }
            
            // 备用方法：使用原有的编码检测和文本提取逻辑
            log.info("使用备用方法处理文档: {}", fileName);
            
            // 检查文件类型
            if (isBinaryFile(fileName, contentType, fileBytes)) {
                log.warn("检测到二进制文件，尝试提取文本内容: {}", fileName);
                
                // 对于DOCX文件，使用特殊的处理方式
                if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
                    log.info("检测到DOCX文件，使用特殊处理方式");
                    String extractedText = extractTextFromDocxSpecial(fileBytes, fileName);
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        log.info("DOCX文件特殊处理成功，长度: {} 字符", extractedText.length());
                        log.debug("提取的文本预览: {}", extractedText.substring(0, Math.min(200, extractedText.length())));
                        return extractedText;
                    }
                }
                
                String extractedText = extractTextFromBinaryFile(fileBytes, fileName);
                log.info("二进制文件文本提取完成，长度: {} 字符", extractedText.length());
                log.debug("提取的文本预览: {}", extractedText.substring(0, Math.min(200, extractedText.length())));
                return extractedText;
            }
            
            // 对于文本文件，智能检测编码
            String content = detectAndReadTextFile(fileBytes, fileName);
            log.info("文本文件读取完成，原始长度: {} 字符", content.length());
            log.debug("原始文本预览: {}", content.substring(0, Math.min(200, content.length())));
            
            // 清理文本，确保UTF-8有效性
            String cleanedContent = cleanTextForUTF8(content);
            log.info("文本清理完成，清理后长度: {} 字符", cleanedContent.length());
            log.debug("清理后文本预览: {}", cleanedContent.substring(0, Math.min(200, cleanedContent.length())));
            
            log.info("=== 文档解析处理完成 ===");
            return cleanedContent;
            
        } catch (Exception e) {
            log.error("读取文件内容失败", e);
            throw new RuntimeException("无法读取文件内容: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理文本以确保UTF-8有效性
     * 移除或替换无效的UTF-8字符
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanTextForUTF8(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            log.debug("开始清理文本，原始长度: {} 字符", text.length());
            
            // 1. 移除BOM标记
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
                log.debug("移除了UTF-8 BOM标记");
            }
            
            // 2. 替换替换字符（）
            int replacementCount = 0;
            text = text.replace("", "");
            if (text.length() != text.replace("", "").length()) {
                replacementCount = text.length() - text.replace("", "").length();
                text = text.replace("", "");
                log.debug("替换了 {} 个替换字符", replacementCount);
            }
            
            // 3. 移除或替换无效的控制字符和二进制数据
            StringBuilder cleaned = new StringBuilder();
            int controlCharCount = 0;
            int binaryCharCount = 0;
            
            for (char c : text.toCharArray()) {
                if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                    // 替换无效控制字符为空格
                    cleaned.append(' ');
                    controlCharCount++;
                } else if (c == 0xFFFD) {
                    // 替换替换字符
                    cleaned.append('?');
                    controlCharCount++;
                } else if (c < 32 || c > 126) {
                    // 检查是否为非ASCII字符，但保留有效的Unicode字符
                    if (Character.isValidCodePoint(c) && Character.isDefined(c)) {
                        cleaned.append(c);
                    } else {
                        // 无效的Unicode字符，替换为问号
                        cleaned.append('?');
                        binaryCharCount++;
                    }
                } else {
                    cleaned.append(c);
                }
            }
            
            String result = cleaned.toString();
            log.debug("文本清理统计: 控制字符={}, 二进制字符={}, 清理后长度={}", 
                    controlCharCount, binaryCharCount, result.length());
            
            // 4. 验证UTF-8有效性
            if (!isValidUTF8(result)) {
                log.warn("文本仍包含无效UTF-8字符，尝试GBK编码转换");
                String gbkResult = tryConvertWithGBK(result);
                if (gbkResult != null && !containsGarbledText(gbkResult)) {
                    result = gbkResult;
                    log.info("成功使用GBK编码转换文本");
                } else {
                    log.warn("GBK转换失败，进行强制UTF-8转换");
                    result = forceUTF8Conversion(result);
                }
            }
            
            // 5. 清理多余的空白字符
            result = result.replaceAll("\\s+", " ").trim();
            
            log.debug("文本清理完成，最终长度: {} 字符", result.length());
            return result;
            
        } catch (Exception e) {
            log.error("文本清理失败，返回原始文本", e);
            return text;
        }
    }
    
    /**
     * 强制转换为有效的UTF-8
     * 
     * @param text 原始文本
     * @return 有效的UTF-8文本
     */
    private String forceUTF8Conversion(String text) {
        try {
            // 将文本转换为字节数组，然后重新解码为UTF-8
            byte[] bytes = text.getBytes("UTF-8");
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            log.warn("强制UTF-8转换失败，使用ASCII清理", e);
            // 如果UTF-8转换失败，使用ASCII清理
            return text.replaceAll("[^\\x00-\\x7F]", "?");
        }
    }
    
    /**
     * 验证字符串是否为有效的UTF-8
     * 
     * @param text 待验证的文本
     * @return 是否为有效的UTF-8
     */
    private boolean isValidUTF8(String text) {
        try {
            if (text == null || text.isEmpty()) {
                return true;
            }
            
            // 1. 检查是否包含替换字符
            if (text.contains("")) {
                log.debug("文本包含替换字符，UTF-8无效");
                return false;
            }
            
            // 2. 检查是否包含无效的Unicode字符
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == 0xFFFD) { // 替换字符
                    log.debug("文本包含替换字符，UTF-8无效");
                    return false;
                }
                if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                    // 检查代理对是否有效
                    if (i + 1 >= text.length() || !Character.isSurrogatePair(c, text.charAt(i + 1))) {
                        log.debug("文本包含无效的代理对，UTF-8无效");
                        return false;
                    }
                    i++; // 跳过下一个字符
                }
            }
            
            // 3. 尝试重新编码验证
            byte[] bytes = text.getBytes("UTF-8");
            String reencoded = new String(bytes, "UTF-8");
            boolean isValid = text.equals(reencoded);
            
            if (!isValid) {
                log.debug("文本重新编码后不匹配，UTF-8无效");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.debug("UTF-8验证异常: {}", e.getMessage());
            return false;
        }
    }
    
    
    /**
     * 检查文件是否为二进制文件
     * 
     * @param fileName 文件名
     * @param contentType MIME类型
     * @param fileBytes 文件字节数组
     * @return 是否为二进制文件
     */
    private boolean isBinaryFile(String fileName, String contentType, byte[] fileBytes) {
        // 根据文件扩展名判断
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".pdf") || lowerFileName.endsWith(".doc") || 
                lowerFileName.endsWith(".docx") || lowerFileName.endsWith(".xls") || 
                lowerFileName.endsWith(".xlsx") || lowerFileName.endsWith(".ppt") || 
                lowerFileName.endsWith(".pptx") || lowerFileName.endsWith(".zip") || 
                lowerFileName.endsWith(".rar") || lowerFileName.endsWith(".7z") ||
                lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") || 
                lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") ||
                lowerFileName.endsWith(".bmp") || lowerFileName.endsWith(".tiff")) {
                return true;
            }
        }
        
        // 根据MIME类型判断
        if (contentType != null) {
            if (contentType.startsWith("application/") && 
                !contentType.equals("application/json") && 
                !contentType.equals("application/xml") &&
                !contentType.equals("application/javascript")) {
                return true;
            }
            if (contentType.startsWith("image/") || contentType.startsWith("video/") || 
                contentType.startsWith("audio/")) {
                return true;
            }
        }
        
        // 根据文件内容判断（检查是否包含大量非文本字符）
        if (fileBytes.length > 0) {
            int nonTextCount = 0;
            int sampleSize = Math.min(fileBytes.length, 1024); // 只检查前1KB
            
            for (int i = 0; i < sampleSize; i++) {
                byte b = fileBytes[i];
                // 检查是否为可打印ASCII字符或常见控制字符
                if (b < 32 && b != 9 && b != 10 && b != 13) { // 非制表符、换行符、回车符的控制字符
                    nonTextCount++;
                }
            }
            
            // 如果超过30%的字符是非文本字符，认为是二进制文件
            return (double) nonTextCount / sampleSize > 0.3;
        }
        
        return false;
    }
    
    /**
     * 从二进制文件中提取文本内容
     * 
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return 提取的文本内容
     */
    private String extractTextFromBinaryFile(byte[] fileBytes, String fileName) {
        try {
            // 对于Word文档，尝试简单的文本提取
            if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
                return extractTextFromDocx(fileBytes);
            }
            
            // 对于PDF文件，尝试简单的文本提取
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                return extractTextFromPdf(fileBytes);
            }
            
            // 对于其他二进制文件，尝试提取可读文本
            return extractReadableText(fileBytes);
            
        } catch (Exception e) {
            log.warn("从二进制文件提取文本失败: {}", e.getMessage());
            return "[无法提取文本内容: " + fileName + "]";
        }
    }
    
    /**
     * 特殊的DOCX文件处理方法
     * 直接提取可读文本，避免复杂的ZIP解析
     */
    private String extractTextFromDocxSpecial(byte[] fileBytes, String fileName) {
        try {
            log.info("开始特殊处理DOCX文件: {}", fileName);
            
            // 直接提取可读文本，不进行复杂的XML解析
            String extractedText = extractReadableText(fileBytes);
            
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                // 进一步清理文本，移除XML标签和特殊字符
                String cleanedText = cleanDocxText(extractedText);
                log.info("DOCX特殊处理成功，原始长度: {}, 清理后长度: {}", 
                        extractedText.length(), cleanedText.length());
                return cleanedText;
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("DOCX特殊处理失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 清理DOCX文本，移除XML标签和特殊字符
     */
    private String cleanDocxText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // 移除XML标签
            String cleaned = text.replaceAll("<[^>]+>", " ");
            
            // 移除XML实体
            cleaned = cleaned.replace("&lt;", "<")
                           .replace("&gt;", ">")
                           .replace("&amp;", "&")
                           .replace("&quot;", "\"")
                           .replace("&apos;", "'");
            
            // 移除多余的空格和换行
            cleaned = cleaned.replaceAll("\\s+", " ").trim();
            
            // 移除常见的DOCX特殊字符
            cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F-\\x9F]", " ");
            
            return cleaned;
            
        } catch (Exception e) {
            log.debug("DOCX文本清理失败: {}", e.getMessage());
            return text;
        }
    }
    
    /**
     * 从DOCX文件中提取文本
     */
    private String extractTextFromDocx(byte[] fileBytes) {
        try {
            log.info("开始解析DOCX文件，大小: {} bytes", fileBytes.length);
            
            // DOCX文件是ZIP格式，包含document.xml文件
            // 这里实现一个简单的DOCX文本提取
            String extractedText = extractTextFromDocxSimple(fileBytes);
            
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                log.info("DOCX文本提取成功，长度: {} 字符", extractedText.length());
                log.debug("提取的DOCX文本预览: {}", extractedText.substring(0, Math.min(200, extractedText.length())));
                return extractedText;
            } else {
                log.warn("DOCX文本提取结果为空，尝试通用文本提取");
                return extractReadableText(fileBytes);
            }
            
        } catch (Exception e) {
            log.warn("DOCX文本提取失败: {}", e.getMessage());
            return "[DOCX文件文本提取失败]";
        }
    }
    
    /**
     * 简单的DOCX文本提取实现
     */
    private String extractTextFromDocxSimple(byte[] fileBytes) {
        try {
            // 尝试不同的编码来读取DOCX文件内容
            String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
            String bestContent = null;
            int maxTextLength = 0;
            
            for (String encoding : encodings) {
                try {
                    String content = new String(fileBytes, encoding);
                    log.debug("使用编码 {} 读取DOCX，长度: {} 字符", encoding, content.length());
                    
                    // 查找document.xml中的文本内容
                    StringBuilder text = new StringBuilder();
                    
                    // 查找<w:t>标签中的文本内容
                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        if (line.contains("<w:t>") && line.contains("</w:t>")) {
                            // 提取<w:t>和</w:t>之间的文本
                            int start = line.indexOf("<w:t>") + 5;
                            int end = line.indexOf("</w:t>", start);
                            if (start < end) {
                                String textContent = line.substring(start, end);
                                // 解码XML实体
                                textContent = textContent.replace("&lt;", "<")
                                                       .replace("&gt;", ">")
                                                       .replace("&amp;", "&")
                                                       .replace("&quot;", "\"")
                                                       .replace("&apos;", "'");
                                text.append(textContent).append(" ");
                            }
                        }
                    }
                    
                    String result = text.toString().trim();
                    if (result.length() > maxTextLength) {
                        maxTextLength = result.length();
                        bestContent = result;
                        log.debug("编码 {} 提取到 {} 字符文本", encoding, result.length());
                    }
                    
                } catch (Exception e) {
                    log.debug("使用编码 {} 解析DOCX失败: {}", encoding, e.getMessage());
                }
            }
            
            if (bestContent != null && !bestContent.isEmpty()) {
                return bestContent;
            }
            
            // 如果所有编码都失败，尝试查找其他可能的文本标签
            for (String encoding : encodings) {
                try {
                    String content = new String(fileBytes, encoding);
                    String result = extractTextFromXmlTags(content);
                    if (result != null && !result.trim().isEmpty()) {
                        log.debug("使用编码 {} 通过XML标签提取到文本", encoding);
                        return result;
                    }
                } catch (Exception e) {
                    // 继续尝试下一个编码
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.debug("简单DOCX解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从XML标签中提取文本
     */
    private String extractTextFromXmlTags(String content) {
        try {
            StringBuilder text = new StringBuilder();
            
            // 查找各种可能的文本标签
            String[] textTags = {"<w:t>", "<t>", "<text>", "<p>", "<span>"};
            String[] endTags = {"</w:t>", "</t>", "</text>", "</p>", "</span>"};
            
            for (int i = 0; i < textTags.length; i++) {
                String startTag = textTags[i];
                String endTag = endTags[i];
                
                int start = 0;
                while ((start = content.indexOf(startTag, start)) != -1) {
                    int tagStart = start + startTag.length();
                    int end = content.indexOf(endTag, tagStart);
                    if (end != -1) {
                        String textContent = content.substring(tagStart, end);
                        // 解码XML实体
                        textContent = textContent.replace("&lt;", "<")
                                               .replace("&gt;", ">")
                                               .replace("&amp;", "&")
                                               .replace("&quot;", "\"")
                                               .replace("&apos;", "'");
                        text.append(textContent).append(" ");
                    }
                    start = end + endTag.length();
                }
            }
            
            return text.toString().trim();
            
        } catch (Exception e) {
            log.debug("XML标签文本提取失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从PDF文件中提取文本
     */
    private String extractTextFromPdf(byte[] fileBytes) {
        try {
            // 简单的PDF文本提取
            // 这里可以实现更复杂的PDF解析逻辑
            return extractReadableText(fileBytes);
        } catch (Exception e) {
            log.warn("PDF文本提取失败: {}", e.getMessage());
            return "[PDF文件文本提取失败]";
        }
    }
    
    /**
     * 从字节数组中提取可读文本
     */
    private String extractReadableText(byte[] fileBytes) {
        StringBuilder text = new StringBuilder();
        
        for (byte b : fileBytes) {
            // 只保留可打印的ASCII字符和常见控制字符
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                text.append((char) b);
            } else if (b == 0) {
                // 遇到null字符时添加空格
                text.append(' ');
            }
        }
        
        String result = text.toString();
        // 清理多余的空白字符
        result = result.replaceAll("\\s+", " ").trim();
        
        if (result.length() < 10) {
            return "[文件内容无法解析为文本]";
        }
        
        return result;
    }
    
    /**
     * 智能检测文本文件编码并读取
     * 
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return 读取的文本内容
     */
    private String detectAndReadTextFile(byte[] fileBytes, String fileName) {
        try {
            // 尝试检测BOM
            if (fileBytes.length >= 3) {
                // UTF-8 BOM
                if (fileBytes[0] == (byte) 0xEF && fileBytes[1] == (byte) 0xBB && fileBytes[2] == (byte) 0xBF) {
                    return new String(fileBytes, 3, fileBytes.length - 3, "UTF-8");
                }
            }
            
            if (fileBytes.length >= 2) {
                // UTF-16 LE BOM
                if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xFE) {
                    return new String(fileBytes, 2, fileBytes.length - 2, "UTF-16LE");
                }
                // UTF-16 BE BOM
                if (fileBytes[0] == (byte) 0xFE && fileBytes[1] == (byte) 0xFF) {
                    return new String(fileBytes, 2, fileBytes.length - 2, "UTF-16BE");
                }
            }
            
            // 尝试不同的编码，优先使用中文编码
            String[] encodings = {"UTF-8", "GBK", "GB2312", "GB18030", "ISO-8859-1", "Windows-1252"};
            
            for (String encoding : encodings) {
                try {
                    String content = new String(fileBytes, encoding);
                    // 检查是否包含替换字符，如果有则说明编码不正确
                    if (!content.contains("")) {
                        // 对于中文编码，额外检查是否包含中文字符
                        if (encoding.startsWith("GB") || encoding.equals("UTF-8")) {
                            if (containsChineseCharacters(content)) {
                                log.info("成功使用编码 {} 读取文件，检测到中文字符: {}", encoding, fileName);
                                return content;
                            }
                        } else {
                            log.info("成功使用编码 {} 读取文件: {}", encoding, fileName);
                            return content;
                        }
                    }
                } catch (Exception e) {
                    // 继续尝试下一个编码
                }
            }
            
            // 如果所有编码都失败，使用UTF-8作为默认
            log.warn("无法确定文件编码，使用UTF-8作为默认: {}", fileName);
            try {
                return new String(fileBytes, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                // UTF-8应该总是支持的，如果失败则使用系统默认编码
                log.warn("UTF-8编码不可用，使用系统默认编码: {}", e.getMessage());
                return new String(fileBytes);
            }
            
        } catch (Exception e) {
            log.error("检测文件编码失败: {}", e.getMessage());
            try {
                return new String(fileBytes, "UTF-8");
            } catch (java.io.UnsupportedEncodingException ex) {
                log.warn("UTF-8编码不可用，使用系统默认编码: {}", ex.getMessage());
                return new String(fileBytes);
            }
        }
    }
    
    /**
     * 检查文本是否包含中文字符
     * 
     * @param text 待检查的文本
     * @return 是否包含中文字符
     */
    private boolean containsChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            // 检查是否为中文字符
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 尝试使用GBK编码转换文本
     * 
     * @param text 原始文本
     * @return 转换后的文本，如果失败返回null
     */
    private String tryConvertWithGBK(String text) {
        try {
            // 将文本转换为字节数组，然后使用GBK解码
            byte[] bytes = text.getBytes("ISO-8859-1");
            String gbkText = new String(bytes, "GBK");
            
            // 检查转换结果是否包含中文字符且没有乱码
            if (containsChineseCharacters(gbkText) && !containsGarbledText(gbkText)) {
                return gbkText;
            }
            
            return null;
        } catch (Exception e) {
            log.debug("GBK转换失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查文本是否包含乱码
     * 
     * @param text 待检查的文本
     * @return 是否包含乱码
     */
    private boolean containsGarbledText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 检查常见的乱码模式
        // 1. 包含替换字符
        if (text.contains("")) {
            return true;
        }
        
        // 2. 检查是否有异常的控制字符
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        
        // 3. 检查UTF-8有效性
        try {
            byte[] bytes = text.getBytes("UTF-8");
            String reencoded = new String(bytes, "UTF-8");
            return !text.equals(reencoded);
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * 
     * @param bytes 字节数组
     * @param maxLength 最大长度
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes, int maxLength) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        int length = Math.min(bytes.length, maxLength);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
            if ((i + 1) % 16 == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * 检查Tika是否可用
     * @return 是否可用
     */
    public boolean isTikaAvailable() {
        try {
            // Tika是Java库，总是可用的
            return true;
        } catch (Exception e) {
            log.warn("检查Tika可用性失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 动态分割文档
     * @param document 文档对象
     * @param fileName 文件名
     * @param chunkSize 块大小
     * @param chunkOverlap 重叠大小
     * @param customSeparators 自定义分隔符
     * @return 分割后的文本片段列表
     */
    public List<TextSegment> dynamicSplitDocument(Document document, String fileName, int chunkSize, int chunkOverlap, List<String> customSeparators) {
        try {
            log.info("动态分割文档: fileName={}, chunkSize={}, chunkOverlap={}, customSeparators={}", 
                    fileName, chunkSize, chunkOverlap, customSeparators);
            
            // 使用智能分割器服务的动态分割功能
            if (customSeparators != null && !customSeparators.isEmpty()) {
                // 使用自定义分隔符
                String[] separators = customSeparators.toArray(new String[0]);
                return smartSplitterService.dynamicSplit(document, chunkSize, chunkOverlap, separators);
            } else {
                // 使用默认分隔符
                return smartSplitterService.dynamicSplit(document, chunkSize, chunkOverlap);
            }
            
        } catch (Exception e) {
            log.error("动态分割文档失败", e);
            throw new RuntimeException("动态分割文档失败: " + e.getMessage(), e);
        }
    }
}