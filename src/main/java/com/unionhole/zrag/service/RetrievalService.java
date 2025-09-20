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
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 检索服务
 * 使用LangChain4j的完整检索功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RerankService rerankService;

    /**
     * 根据查询检索相关文档片段
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 相关文档片段列表
     */
    public List<EmbeddingMatch<TextSegment>> retrieve(String query, int maxResults, double minScore) {
        log.info("=== RAG 召回阶段开始 ===");
        log.info("查询文本: {}", query);
        log.info("最大返回结果数: {}", maxResults);
        log.info("最小相似度分数: {}", minScore);
        
        try {
            // 1. 查询向量化阶段
            log.info("--- 步骤1: 查询向量化 ---");
            log.info("开始将查询文本转换为向量...");
            long startTime = System.currentTimeMillis();
            
            dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            long endTime = System.currentTimeMillis();
            log.info("查询向量化完成，向量维度: {}, 耗时: {} ms", 
                    queryEmbedding.vector().length, (endTime - startTime));
            log.info("查询向量预览: [{}...{}]", 
                    queryEmbedding.vector()[0], 
                    queryEmbedding.vector()[queryEmbedding.vector().length - 1]);
            
            // 2. 向量搜索阶段
            log.info("--- 步骤2: 向量数据库搜索 ---");
            int searchResults = Math.max(maxResults * 2, 10); // 获取更多结果用于重排
            log.info("在向量数据库中搜索相似文档，搜索数量: {}", searchResults);
            
            startTime = System.currentTimeMillis();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    queryEmbedding, 
                    searchResults, 
                    minScore
            );
            endTime = System.currentTimeMillis();
            
            log.info("向量搜索完成，找到 {} 个候选文档片段，耗时: {} ms", 
                    matches.size(), (endTime - startTime));
            
            if (!matches.isEmpty()) {
                log.info("最高相似度分数: {}", matches.get(0).score());
                log.info("最低相似度分数: {}", matches.get(matches.size() - 1).score());
                log.info("候选文档片段预览:");
                for (int i = 0; i < Math.min(3, matches.size()); i++) {
                    String text = matches.get(i).embedded().text();
                    log.info("  {}. 相似度: {:.4f}, 内容: {}", 
                            i + 1, matches.get(i).score(), 
                            text.length() > 100 ? text.substring(0, 100) + "..." : text);
                }
            }
            
            // 3. 结果重排阶段
            log.info("--- 步骤3: 结果重排 ---");
            log.info("开始对 {} 个候选结果进行重排...", matches.size());
            
            startTime = System.currentTimeMillis();
            List<EmbeddingMatch<TextSegment>> rerankedMatches = rerankService.rerank(
                    query, matches, maxResults);
            endTime = System.currentTimeMillis();
            
            log.info("重排完成，返回 {} 个最终结果，耗时: {} ms", 
                    rerankedMatches.size(), (endTime - startTime));
            
            if (!rerankedMatches.isEmpty()) {
                log.info("重排后最高相似度分数: {}", rerankedMatches.get(0).score());
                log.info("重排后最低相似度分数: {}", rerankedMatches.get(rerankedMatches.size() - 1).score());
                log.info("最终返回的文档片段:");
                for (int i = 0; i < rerankedMatches.size(); i++) {
                    String text = rerankedMatches.get(i).embedded().text();
                    log.info("  {}. 相似度: {:.4f}, 内容: {}", 
                            i + 1, rerankedMatches.get(i).score(), 
                            text.length() > 100 ? text.substring(0, 100) + "..." : text);
                }
            }
            
            log.info("=== RAG 召回阶段完成 ===");
            return rerankedMatches;
        } catch (Exception e) {
            log.error("召回阶段失败", e);
            throw new RuntimeException("召回失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据查询检索相关文档片段（使用默认参数）
     * @param query 查询文本
     * @return 相关文档片段列表
     */
    public List<EmbeddingMatch<TextSegment>> retrieve(String query) {
        return retrieve(query, 5, 0.6);
    }

    /**
     * 获取检索到的文档内容
     * @param query 查询文本
     * @return 文档内容列表
     */
    public List<String> retrieveContent(String query) {
        List<EmbeddingMatch<TextSegment>> matches = retrieve(query);
        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取检索到的文档内容（带参数）
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 文档内容列表
     */
    public List<String> retrieveContent(String query, int maxResults, double minScore) {
        List<EmbeddingMatch<TextSegment>> matches = retrieve(query, maxResults, minScore);
        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 创建内容检索器
     * @return 内容检索器
     */
    public ContentRetriever createContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .build();
    }

    /**
     * 创建内容检索器（带参数）
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 内容检索器
     */
    public ContentRetriever createContentRetriever(int maxResults, double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }
    
    /**
     * 流式检索
     * @param query 查询文本
     * @param emitter 流式响应发射器
     * @return 检索结果
     */
    public List<TextSegment> retrieveStream(String query, SseEmitter emitter) {
        return retrieveStream(query, 5, 0.6, emitter);
    }
    
    /**
     * 流式检索（带参数）
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param minScore 最小相似度分数
     * @param emitter 流式响应发射器
     * @return 检索结果
     */
    public List<TextSegment> retrieveStream(String query, Integer maxResults, Double minScore, SseEmitter emitter) {
        try {
            log.info("=== RAG 召回阶段开始 ===");
            log.info("查询文本: {}", query);
            log.info("最大返回结果数: {}", maxResults);
            log.info("最小相似度分数: {}", minScore);
            
            // 发送步骤1：查询向量化
            StreamingUtils.sendRetrieval(emitter, "📝 步骤1: 将查询转换为向量...");
            log.info("--- 步骤1: 查询向量化 ---");
            log.info("开始将查询文本转换为向量...");
            
            long startTime = System.currentTimeMillis();
            dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query).content();
            float[] queryVectorArray = queryEmbedding.vector();
            List<Float> queryVector = new ArrayList<>();
            for (float f : queryVectorArray) {
                queryVector.add(f);
            }
            long embeddingTime = System.currentTimeMillis() - startTime;
            
            log.info("查询向量化完成，向量维度: {}, 耗时: {} ms", queryVector.size(), embeddingTime);
            log.info("查询向量预览: [{}...{}]", 
                    queryVector.subList(0, Math.min(5, queryVector.size())),
                    queryVector.subList(Math.max(0, queryVector.size() - 5), queryVector.size()));
            
            // 发送步骤2：向量数据库搜索
            StreamingUtils.sendRetrieval(emitter, "🔍 步骤2: 在向量数据库中搜索相似文档...");
            log.info("--- 步骤2: 向量数据库搜索 ---");
            log.info("在向量数据库中搜索相似文档，搜索数量: {}", maxResults * 2);
            
            startTime = System.currentTimeMillis();
            // 降低过滤阈值，让更多候选进入重排序阶段
            double relaxedMinScore = Math.max(0.0, minScore - 0.2);
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults * 3, relaxedMinScore);
            long searchTime = System.currentTimeMillis() - startTime;
            
            log.info("向量搜索完成，找到 {} 个候选文档片段，耗时: {} ms", matches.size(), searchTime);
            
            if (matches.isEmpty()) {
                StreamingUtils.sendRetrieval(emitter, "❌ 未找到相关文档");
                log.info("未找到相关文档");
                return new ArrayList<>();
            }
            
            // 计算相似度分数统计
            double maxScore = matches.stream().mapToDouble(EmbeddingMatch::score).max().orElse(0.0);
            double minScoreActual = matches.stream().mapToDouble(EmbeddingMatch::score).min().orElse(0.0);
            
            log.info("最高相似度分数: {}", maxScore);
            log.info("最低相似度分数: {}", minScoreActual);
            
            // 发送步骤3：结果重排
            StreamingUtils.sendRetrieval(emitter, "🔄 步骤3: 对搜索结果进行重排...");
            log.info("--- 步骤3: 结果重排 ---");
            log.info("开始对 {} 个候选结果进行重排...", matches.size());
            
            startTime = System.currentTimeMillis();
            List<TextSegment> rerankedSegments = rerankService.rerankStream(query, matches, maxResults, emitter);
            long rerankTime = System.currentTimeMillis() - startTime;
            
            log.info("重排完成，返回 {} 个最终结果，耗时: {} ms", rerankedSegments.size(), rerankTime);
            
            if (rerankedSegments.isEmpty()) {
                StreamingUtils.sendRetrieval(emitter, "❌ 重排后无有效结果");
                log.info("重排后无有效结果");
                return new ArrayList<>();
            }
            
            // 发送完成信息
            StreamingUtils.sendRetrieval(emitter, String.format("✅ 检索完成，找到 %d 个相关文档片段", rerankedSegments.size()));
            log.info("=== RAG 召回阶段完成 ===");
            
            return rerankedSegments;
            
        } catch (Exception e) {
            log.error("流式检索失败", e);
            StreamingUtils.sendError(emitter, "检索失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取EmbeddingStore实例
     * @return EmbeddingStore实例
     */
    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }
}