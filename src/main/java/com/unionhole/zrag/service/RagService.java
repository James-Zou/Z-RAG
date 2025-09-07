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
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG核心服务
 * 整合LangChain4j的所有组件，提供完整的RAG功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocumentService documentService;
    private final RetrievalService retrievalService;
    private final GenerationService generationService;
    private final ChatLanguageModel chatModel;
    private final ContentRetriever contentRetriever;

    /**
     * 处理文档并存储到向量数据库
     * @param documents 文档列表
     */
    public void processDocuments(List<Document> documents) {
        documentService.processDocuments(documents);
    }

    /**
     * 处理单个文档
     * @param document 文档
     */
    public void processDocument(Document document) {
        documentService.processDocument(document);
    }

    /**
     * 处理文本内容
     * @param content 文本内容
     */
    public void processText(String content) {
        documentService.processText(content);
    }

    /**
     * 执行RAG查询
     * @param query 用户查询
     * @return RAG回答
     */
    public String query(String query) {
        log.info("执行RAG查询: {}", query);
        
        try {
            // 1. 检索相关文档
            List<EmbeddingMatch<TextSegment>> matches = retrievalService.retrieve(query);
            
            if (matches.isEmpty()) {
                return "抱歉，没有找到与您查询相关的文档内容。";
            }
            
            // 2. 提取文档内容
            List<String> retrievedDocuments = matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(java.util.stream.Collectors.toList());
            
            // 3. 生成回答
            String answer = generationService.generateAnswer(query, retrievedDocuments);
            
            log.info("RAG查询完成");
            return answer;
        } catch (Exception e) {
            log.error("RAG查询失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 执行RAG查询（带参数）
     * @param query 用户查询
     * @param maxResults 最大检索结果数
     * @param minScore 最小相似度分数
     * @return RAG回答
     */
    public String query(String query, int maxResults, double minScore) {
        log.info("执行RAG查询: {} (maxResults: {}, minScore: {})", query, maxResults, minScore);
        
        try {
            // 1. 检索相关文档
            List<EmbeddingMatch<TextSegment>> matches = retrievalService.retrieve(query, maxResults, minScore);
            
            if (matches.isEmpty()) {
                return "抱歉，没有找到与您查询相关的文档内容。";
            }
            
            // 2. 提取文档内容
            List<String> retrievedDocuments = matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(java.util.stream.Collectors.toList());
            
            // 3. 生成回答
            String answer = generationService.generateAnswer(query, retrievedDocuments);
            
            log.info("RAG查询完成");
            return answer;
        } catch (Exception e) {
            log.error("RAG查询失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 使用内容检索器执行RAG查询
     * @param query 用户查询
     * @return RAG回答
     */
    public String queryWithContentRetriever(String query) {
        log.info("使用内容检索器执行RAG查询: {}", query);
        
        try {
            return generationService.generateAnswer(query, contentRetriever);
        } catch (Exception e) {
            log.error("使用内容检索器RAG查询失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 获取向量数据库中的文档数量
     * @return 文档数量
     */
    public long getDocumentCount() {
        return documentService.getDocumentCount();
    }

    /**
     * 清空向量数据库
     */
    public void clearDocuments() {
        documentService.clearDocuments();
    }

    /**
     * 获取检索到的文档片段
     * @param query 查询文本
     * @return 文档片段列表
     */
    public List<String> retrieveDocuments(String query) {
        return retrievalService.retrieveContent(query);
    }

    /**
     * 获取检索到的文档片段（带参数）
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 文档片段列表
     */
    public List<String> retrieveDocuments(String query, int maxResults, double minScore) {
        return retrievalService.retrieveContent(query, maxResults, minScore);
    }

    /**
     * 处理上传的文件
     * @param file 上传的文件
     * @return 文件存储路径
     */
    public String processUploadedFile(MultipartFile file) {
        return documentService.processUploadedFile(file);
    }

    /**
     * 处理文本内容并存储
     * @param content 文本内容
     * @return 文件存储路径
     */
    public String processTextWithStorage(String content) {
        return documentService.processTextWithStorage(content);
    }

    /**
     * 检查系统状态
     * @return 系统状态信息
     */
    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Z-RAG系统状态:\n");
        status.append("- 文档数量: ").append(getDocumentCount()).append("\n");
        status.append("- 聊天模型: ").append(chatModel != null ? "已配置" : "未配置").append("\n");
        status.append("- 内容检索器: ").append(contentRetriever != null ? "已配置" : "未配置").append("\n");
        return status.toString();
    }
}