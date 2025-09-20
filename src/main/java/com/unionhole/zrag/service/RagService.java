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

import com.unionhole.zrag.util.StreamingUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
     * 执行股神投资主题流式RAG查询
     * @param query 用户查询
     * @param emitter SSE发射器
     */
    public void queryStreamStock(String query, SseEmitter emitter) {
        log.info("执行股神投资主题流式RAG查询: {}", query);
        
        try {
            // 发送RAG过程开始日志
            StreamingUtils.sendDetailedRagLog(emitter, "RAG_START", "开始执行股神投资主题RAG查询");
            StreamingUtils.sendDetailedRagLog(emitter, "QUERY", "用户查询: " + query);
            
            // 1. 检索相关文档
            StreamingUtils.sendRetrieval(emitter, "🔍 正在检索投资相关文档...");
            StreamingUtils.sendDetailedRagLog(emitter, "RETRIEVAL", "开始检索相关文档，查询向量化中...");
            
            List<EmbeddingMatch<TextSegment>> matches = retrievalService.retrieve(query);
            StreamingUtils.sendDetailedRagLog(emitter, "RETRIEVAL", 
                String.format("检索完成，找到 %d 个相关文档片段", matches.size()));
            
            if (matches.isEmpty()) {
                // 没有命中知识，使用AI进行投资分析
                StreamingUtils.sendThinking(emitter, "📚 未找到相关投资知识，正在使用AI进行专业分析...");
                StreamingUtils.sendDetailedRagLog(emitter, "FALLBACK", "未找到相关知识，切换到AI直接分析模式");
                String answer = generationService.generateStockAnswerWithoutKnowledge(query, emitter);
                StreamingUtils.sendAnswer(emitter, answer);
                StreamingUtils.sendDetailedRagLog(emitter, "RAG_END", "RAG查询完成（AI直接分析模式）");
                return;
            }
            
            // 2. 提取文档内容和元数据
            StreamingUtils.sendDetailedRagLog(emitter, "EXTRACT", "提取文档内容和元数据...");
            List<TextSegment> retrievedSegments = matches.stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(java.util.stream.Collectors.toList());
            
            // 记录检索到的文档信息
            for (int i = 0; i < retrievedSegments.size(); i++) {
                TextSegment segment = retrievedSegments.get(i);
                String docInfo = String.format("文档片段 %d: 长度 %d 字符", i + 1, segment.text().length());
                if (segment.metadata().containsKey("source")) {
                    docInfo += ", 来源: " + segment.metadata().get("source");
                }
                StreamingUtils.sendDetailedRagLog(emitter, "DOCUMENT", docInfo);
            }
            
            // 3. 重新排序（如果有重排序服务）
            StreamingUtils.sendRerank(emitter, "📊 正在重新排序相关文档...");
            StreamingUtils.sendDetailedRagLog(emitter, "RERANK", "重新排序相关文档，提升相关性...");
            
            // 4. 生成带引用信息的投资主题回答
            StreamingUtils.sendGeneration(emitter, "💡 正在基于知识库生成专业投资建议...");
            StreamingUtils.sendDetailedRagLog(emitter, "GENERATION", "开始生成回答，构建上下文...");
            String answer = generationService.generateStockAnswerWithReferences(query, retrievedSegments, emitter);
            
            // 5. 发送最终回答
            StreamingUtils.sendAnswer(emitter, answer);
            StreamingUtils.sendDetailedRagLog(emitter, "RAG_END", "RAG查询完成，回答已生成");
            
            log.info("股神投资主题流式RAG查询完成");
        } catch (Exception e) {
            log.error("股神投资主题流式RAG查询失败", e);
            StreamingUtils.sendDetailedRagLog(emitter, "ERROR", "RAG查询失败: " + e.getMessage());
            StreamingUtils.sendError(emitter, "投资分析失败: " + e.getMessage());
        }
    }

    /**
     * 执行股神投资主题流式RAG查询（带参数）
     * @param query 用户查询
     * @param maxResults 最大结果数
     * @param minScore 最小相似度
     * @param emitter SSE发射器
     */
    public void queryStreamStock(String query, int maxResults, double minScore, SseEmitter emitter) {
        log.info("执行股神投资主题流式RAG查询: {} (maxResults: {}, minScore: {})", query, maxResults, minScore);
        
        try {
            // 发送RAG过程开始日志
            StreamingUtils.sendDetailedRagLog(emitter, "RAG_START", "开始执行股神投资主题RAG查询（带参数）");
            StreamingUtils.sendDetailedRagLog(emitter, "QUERY", String.format("用户查询: %s, 最大结果数: %d, 最小相似度: %.2f", query, maxResults, minScore));
            
            // 1. 检索相关文档
            StreamingUtils.sendRetrieval(emitter, "🔍 正在检索投资相关文档...");
            StreamingUtils.sendDetailedRagLog(emitter, "RETRIEVAL", "开始检索相关文档，查询向量化中...");
            
            List<EmbeddingMatch<TextSegment>> matches = retrievalService.retrieve(query, maxResults, minScore);
            StreamingUtils.sendDetailedRagLog(emitter, "RETRIEVAL", 
                String.format("检索完成，找到 %d 个相关文档片段（相似度 >= %.2f）", matches.size(), minScore));
            
            if (matches.isEmpty()) {
                // 没有命中知识，使用AI进行投资分析
                StreamingUtils.sendThinking(emitter, "📚 未找到相关投资知识，正在使用AI进行专业分析...");
                StreamingUtils.sendDetailedRagLog(emitter, "FALLBACK", "未找到相关知识，切换到AI直接分析模式");
                String answer = generationService.generateStockAnswerWithoutKnowledge(query, emitter);
                StreamingUtils.sendAnswer(emitter, answer);
                StreamingUtils.sendDetailedRagLog(emitter, "RAG_END", "RAG查询完成（AI直接分析模式）");
                return;
            }
            
            // 2. 提取文档内容和元数据
            StreamingUtils.sendDetailedRagLog(emitter, "EXTRACT", "提取文档内容和元数据...");
            List<TextSegment> retrievedSegments = matches.stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(java.util.stream.Collectors.toList());
            
            // 记录检索到的文档信息
            for (int i = 0; i < retrievedSegments.size(); i++) {
                TextSegment segment = retrievedSegments.get(i);
                String docInfo = String.format("文档片段 %d: 长度 %d 字符", i + 1, segment.text().length());
                if (segment.metadata().containsKey("source")) {
                    docInfo += ", 来源: " + segment.metadata().get("source");
                }
                StreamingUtils.sendDetailedRagLog(emitter, "DOCUMENT", docInfo);
            }
            
            // 3. 重新排序（如果有重排序服务）
            StreamingUtils.sendRerank(emitter, "📊 正在重新排序相关文档...");
            StreamingUtils.sendDetailedRagLog(emitter, "RERANK", "重新排序相关文档，提升相关性...");
            
            // 4. 生成带引用信息的投资主题回答
            StreamingUtils.sendGeneration(emitter, "💡 正在基于知识库生成专业投资建议...");
            StreamingUtils.sendDetailedRagLog(emitter, "GENERATION", "开始生成回答，构建上下文...");
            String answer = generationService.generateStockAnswerWithReferences(query, retrievedSegments, emitter);
            
            // 5. 发送最终回答
            StreamingUtils.sendAnswer(emitter, answer);
            StreamingUtils.sendDetailedRagLog(emitter, "RAG_END", "RAG查询完成，回答已生成");
            
            log.info("股神投资主题流式RAG查询完成");
        } catch (Exception e) {
            log.error("股神投资主题流式RAG查询失败", e);
            StreamingUtils.sendDetailedRagLog(emitter, "ERROR", "RAG查询失败: " + e.getMessage());
            StreamingUtils.sendError(emitter, "投资分析失败: " + e.getMessage());
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
     * 获取知识片段列表
     * @param page 页码
     * @param size 每页大小
     * @param search 搜索关键词
     * @return 知识片段列表
     */
    public Map<String, Object> getKnowledgeChunks(int page, int size, String search) {
        try {
            log.info("获取知识片段列表，页码: {}, 大小: {}, 搜索: {}", page, size, search);
            
            // 这里应该从向量数据库中获取实际的片段数据
            // 目前返回模拟数据
            List<Map<String, Object>> chunks = new ArrayList<>();
            
            // 模拟数据
            for (int i = 0; i < Math.min(size, 20); i++) {
                Map<String, Object> chunk = new HashMap<>();
                chunk.put("id", "chunk_" + (page * size + i + 1));
                chunk.put("content", "这是第 " + (page * size + i + 1) + " 个知识片段的内容。包含相关的信息和数据，用于RAG系统的检索和问答。");
                chunk.put("source", "文档_" + (i % 5 + 1) + ".pdf");
                chunk.put("chunkIndex", i + 1);
                chunk.put("totalChunks", 10);
                chunk.put("createdAt", System.currentTimeMillis() - (i * 1000 * 60 * 60)); // 模拟时间
                chunk.put("similarity", 0.85 + (Math.random() * 0.1)); // 模拟相似度
                chunks.add(chunk);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", chunks);
            result.put("totalElements", 100); // 模拟总数
            result.put("totalPages", 5);
            result.put("currentPage", page);
            result.put("size", size);
            result.put("first", page == 0);
            result.put("last", page >= 4);
            result.put("numberOfElements", chunks.size());
            
            return result;
        } catch (Exception e) {
            log.error("获取知识片段列表失败", e);
            throw new RuntimeException("获取知识片段列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取向量数据信息
     * @return 向量数据信息
     */
    public Map<String, Object> getVectorDataInfo() {
        try {
            log.info("获取向量数据信息");
            
            Map<String, Object> result = new HashMap<>();
            result.put("vectorDimension", 1024); // 千问embedding的维度
            result.put("vectorCount", getDocumentCount());
            result.put("storageType", "Milvus"); // 或者 "Weaviate", "Memory"
            result.put("indexType", "IVF_FLAT");
            result.put("metricType", "COSINE");
            result.put("memoryUsage", "约 " + (getDocumentCount() * 1024 * 4 / 1024 / 1024) + " MB");
            result.put("lastUpdated", System.currentTimeMillis());
            
            return result;
        } catch (Exception e) {
            log.error("获取向量数据信息失败", e);
            throw new RuntimeException("获取向量数据信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取分析报告数据
     * @return 分析报告数据
     */
    public Map<String, Object> getAnalyticsData() {
        try {
            log.info("获取分析报告数据");
            
            Map<String, Object> result = new HashMap<>();
            
            // 模拟查询统计
            Map<String, Object> queryStats = new HashMap<>();
            queryStats.put("totalQueries", 156);
            queryStats.put("successfulQueries", 142);
            queryStats.put("failedQueries", 14);
            queryStats.put("averageResponseTime", 1.2); // 秒
            
            // 模拟文档统计
            Map<String, Object> documentStats = new HashMap<>();
            documentStats.put("totalDocuments", getDocumentCount());
            documentStats.put("processedDocuments", getDocumentCount());
            documentStats.put("totalChunks", getDocumentCount() * 10); // 假设每个文档平均10个片段
            documentStats.put("averageChunkSize", 300); // 字符
            
            // 模拟使用趋势（最近7天）
            List<Map<String, Object>> dailyStats = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                Map<String, Object> dayStat = new HashMap<>();
                dayStat.put("date", java.time.LocalDate.now().minusDays(i).toString());
                dayStat.put("queries", 20 + (int)(Math.random() * 10));
                dayStat.put("documents", 2 + (int)(Math.random() * 3));
                dailyStats.add(dayStat);
            }
            
            result.put("queryStats", queryStats);
            result.put("documentStats", documentStats);
            result.put("dailyStats", dailyStats);
            result.put("lastUpdated", System.currentTimeMillis());
            
            return result;
        } catch (Exception e) {
            log.error("获取分析报告数据失败", e);
            throw new RuntimeException("获取分析报告数据失败: " + e.getMessage(), e);
        }
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
     * 处理带元数据的文本内容
     * @param content 文本内容
     * @param fileName 文件名
     */
    public void processTextWithMetadata(String content, String fileName) {
        try {
            log.info("处理带元数据的文本内容: {}", fileName);
            
            // 生成文件ID和租户ID
            String fileId = generateFileId(fileName);
            String tenantId = getCurrentTenantId();
            
            // 创建文档
            Document document = Document.from(content);
            
            // 使用DocumentService处理带元数据的文档
            documentService.processDocumentWithMetadata(document, fileId, tenantId, fileName);
            
            log.info("文本内容处理完成: {}", fileName);
            
        } catch (Exception e) {
            log.error("处理带元数据的文本内容失败", e);
            throw new RuntimeException("处理文本内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成文件ID
     */
    private String generateFileId(String fileName) {
        return "file_" + System.currentTimeMillis() + "_" + 
               fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * 获取当前租户ID
     */
    private String getCurrentTenantId() {
        // 这里可以从Spring Security上下文或配置中获取
        return "default_tenant";
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
    public List<String> dynamicSplitDocument(Document document, String fileName, int chunkSize, int chunkOverlap, List<String> customSeparators) {
        try {
            log.info("动态分割文档: fileName={}, chunkSize={}, chunkOverlap={}", fileName, chunkSize, chunkOverlap);
            
            // 使用DocumentService的动态分割功能
            List<TextSegment> segments = documentService.dynamicSplitDocument(document, fileName, chunkSize, chunkOverlap, customSeparators);
            
            // 转换为字符串列表
            return segments.stream()
                    .map(TextSegment::text)
                    .collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            log.error("动态分割文档失败", e);
            throw new RuntimeException("动态分割文档失败: " + e.getMessage(), e);
        }
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
    
    /**
     * 流式RAG查询
     * @param query 查询文本
     * @param emitter 流式响应发射器
     */
    public void queryStream(String query, SseEmitter emitter) {
        try {
            log.info("执行流式RAG查询: {}", query);
            
            // 发送思考步骤
            StreamingUtils.sendThinking(emitter, "🔍 正在检索相关文档...");
            
            // 执行检索
            List<TextSegment> relevantSegments = retrievalService.retrieveStream(query, emitter);
            
            if (relevantSegments.isEmpty()) {
                // 没有命中知识，使用AI进行检索
                StreamingUtils.sendThinking(emitter, "📚 未找到相关知识，正在使用AI进行检索...");
                String answer = generationService.generateAnswerWithoutKnowledge(query, emitter);
                StreamingUtils.sendAnswer(emitter, answer);
                emitter.complete();
                return;
            }
            
            // 发送生成步骤
            StreamingUtils.sendGeneration(emitter, "🤖 正在基于知识库生成回答...");
            
            // 生成带引用信息的回答
            String answer = generationService.generateStreamWithReferences(query, relevantSegments, emitter);
            
            // 发送最终答案
            StreamingUtils.sendAnswer(emitter, answer);
            emitter.complete();
            
        } catch (Exception e) {
            log.error("流式RAG查询失败", e);
            StreamingUtils.sendError(emitter, "查询失败: " + e.getMessage());
            emitter.completeWithError(e);
        }
    }
    
    /**
     * 流式RAG查询（带参数）
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param minScore 最小相似度分数
     * @param emitter 流式响应发射器
     */
    public void queryStream(String query, Integer maxResults, Double minScore, SseEmitter emitter) {
        try {
            log.info("执行流式RAG查询: {}", query);
            
            // 发送思考步骤
            StreamingUtils.sendThinking(emitter, "🔍 正在检索相关文档...");
            
            // 执行检索
            List<TextSegment> relevantSegments = retrievalService.retrieveStream(query, maxResults, minScore, emitter);
            
            if (relevantSegments.isEmpty()) {
                // 没有命中知识，使用AI进行检索
                StreamingUtils.sendThinking(emitter, "📚 未找到相关知识，正在使用AI进行检索...");
                String answer = generationService.generateAnswerWithoutKnowledge(query, emitter);
                StreamingUtils.sendAnswer(emitter, answer);
                emitter.complete();
                return;
            }
            
            // 发送生成步骤
            StreamingUtils.sendGeneration(emitter, "🤖 正在基于知识库生成回答...");
            
            // 生成带引用信息的回答
            String answer = generationService.generateStreamWithReferences(query, relevantSegments, emitter);
            
            // 发送最终答案
            StreamingUtils.sendAnswer(emitter, answer);
            emitter.complete();
            
        } catch (Exception e) {
            log.error("流式RAG查询失败", e);
            StreamingUtils.sendError(emitter, "查询失败: " + e.getMessage());
            emitter.completeWithError(e);
        }
    }
    
    /**
     * 获取EmbeddingStore实例
     * @return EmbeddingStore实例
     */
    public dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> getEmbeddingStore() {
        return retrievalService.getEmbeddingStore();
    }
}