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
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import com.unionhole.zrag.store.InMemoryEmbeddingStore;
import com.unionhole.zrag.store.WeaviateEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    private final MinioStorageService minioStorageService;

    @Value("${storage.type:minio}")
    private String storageType;

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
            
            // 读取文件内容并处理
            String content = new String(file.getBytes(), "UTF-8");
            Document document = Document.from(content);
            processDocument(document);
            
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
            } else {
                log.warn("当前EmbeddingStore不支持清空操作");
            }
        } catch (Exception e) {
            log.error("清空向量数据库失败", e);
            throw new RuntimeException("清空向量数据库失败: " + e.getMessage(), e);
        }
    }
}