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

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        log.info("检索查询: {} (maxResults: {}, minScore: {})", query, maxResults, minScore);
        
        try {
            // 将查询文本转换为向量
            dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 在向量存储中搜索相似文档（获取更多结果用于重排）
            int searchResults = Math.max(maxResults * 2, 10); // 获取更多结果用于重排
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    queryEmbedding, 
                    searchResults, 
                    minScore
            );
            
            log.info("找到 {} 个相关文档片段，开始重排", matches.size());
            
            // 使用重排服务重新排序
            List<EmbeddingMatch<TextSegment>> rerankedMatches = rerankService.rerank(
                    query, matches, maxResults);
            
            log.info("重排完成，返回 {} 个文档片段", rerankedMatches.size());
            return rerankedMatches;
        } catch (Exception e) {
            log.error("检索失败", e);
            throw new RuntimeException("检索失败: " + e.getMessage(), e);
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
}