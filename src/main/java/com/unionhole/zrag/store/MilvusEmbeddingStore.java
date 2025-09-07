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

package com.unionhole.zrag.store;

import com.unionhole.zrag.util.MilvusLLMUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus向量存储实现
 * 基于Milvus向量数据库的持久化存储
 */
@Slf4j
@Component
public class MilvusEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final MilvusLLMUtils milvusUtils;
    
    @Value("${milvus.collection:zrag_documents}")
    private String collectionName;
    
    @Value("${milvus.vector-dimension:384}")
    private Integer vectorDimension;

    public MilvusEmbeddingStore(MilvusLLMUtils milvusUtils) {
        this.milvusUtils = milvusUtils;
    }

    @PostConstruct
    public void initialize() {
        createCollectionIfNotExists();
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding, null);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        add(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        add(id, embedding, textSegment);
        return id;
    }

    public void add(String id, Embedding embedding, TextSegment textSegment) {
        try {
            // 转换float[]为List<Float>
            List<Float> vector = new ArrayList<>();
            for (float f : embedding.vector()) {
                vector.add(f);
            }

            String text = textSegment != null ? textSegment.text() : "";
            String metadata = textSegment != null ? textSegment.metadata().asMap().toString() : "{}";

            // 添加向量到Milvus
            boolean success = milvusUtils.addVectors(collectionName, 
                    Arrays.asList(vector), 
                    Arrays.asList(text), 
                    Arrays.asList(metadata));
            
            if (success) {
                log.debug("成功添加向量到Milvus: {}", id);
            } else {
                throw new RuntimeException("添加向量失败");
            }
        } catch (Exception e) {
            log.error("添加向量到Milvus失败", e);
            throw new RuntimeException("添加向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return addAll(embeddings, Collections.emptyList());
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            TextSegment textSegment = i < textSegments.size() ? textSegments.get(i) : null;
            add(id, embeddings.get(i), textSegment);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults) {
        return findRelevant(referenceEmbedding, maxResults, 0.0);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        try {
            log.info("执行Milvus向量查询，maxResults: {}, minScore: {}", maxResults, minScore);
            
            // 转换float[]为List<Float>
            List<Float> queryVector = new ArrayList<>();
            for (float f : referenceEmbedding.vector()) {
                queryVector.add(f);
            }

            // 使用MilvusLLMUtils搜索相似向量
            List<Map<String, Object>> searchResults = milvusUtils.searchSimilar(collectionName, queryVector, maxResults);
            
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            
            for (Map<String, Object> result : searchResults) {
                try {
                    // 计算相似度分数（Milvus返回的是距离，需要转换为相似度）
                    double distance = (Double) result.get("score");
                    double similarity = 1.0 / (1.0 + distance); // 将距离转换为相似度
                    
                    if (similarity >= minScore) {
                        String text = (String) result.get("text");
                        String metadataStr = (String) result.get("metadata");
                        
                        // 解析元数据
                        Map<String, Object> metadata = new HashMap<>();
                        if (metadataStr != null && !metadataStr.equals("{}")) {
                            // 简单的元数据解析，实际项目中可能需要更复杂的解析
                            metadata.put("source", metadataStr);
                        }
                        
                        TextSegment textSegment = TextSegment.from(text, 
                            dev.langchain4j.data.document.Metadata.from(metadata));
                        
                        Embedding embedding = new Embedding(new float[vectorDimension]);
                        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(similarity, 
                            String.valueOf(result.get("id")), embedding, textSegment);
                        matches.add(match);
                    }
                } catch (Exception e) {
                    log.warn("处理Milvus搜索结果失败", e);
                }
            }
            
            // 按相似度排序
            matches.sort((a, b) -> Double.compare(b.score(), a.score()));
            
            return matches.stream().limit(maxResults).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Milvus查询失败", e);
            return Collections.emptyList();
        }
    }

    public List<TextSegment> findAll() {
        try {
            log.info("执行Milvus findAll查询");
            
            // 使用MilvusLLMUtils查询所有数据
            List<Map<String, Object>> results = milvusUtils.queryAll(collectionName, 10000);
            
            List<TextSegment> textSegments = new ArrayList<>();
            
            for (Map<String, Object> result : results) {
                try {
                    String text = (String) result.get("text");
                    String metadataStr = (String) result.get("metadata");
                    
                    // 解析元数据
                    Map<String, Object> metadata = new HashMap<>();
                    if (metadataStr != null && !metadataStr.equals("{}")) {
                        metadata.put("source", metadataStr);
                    }
                    
                    TextSegment textSegment = TextSegment.from(text, 
                        dev.langchain4j.data.document.Metadata.from(metadata));
                    textSegments.add(textSegment);
                } catch (Exception e) {
                    log.warn("处理Milvus查询结果失败", e);
                }
            }
            
            return textSegments;

        } catch (Exception e) {
            log.error("Milvus查询所有数据失败", e);
            return Collections.emptyList();
        }
    }

    public void clear() {
        try {
            log.info("开始清空Milvus数据");
            
            // 使用MilvusLLMUtils清空集合
            boolean success = milvusUtils.clearCollection(collectionName);
            
            if (success) {
                log.info("成功清空Milvus数据");
            } else {
                throw new RuntimeException("清空数据失败");
            }
        } catch (Exception e) {
            log.error("清空Milvus数据失败", e);
            throw new RuntimeException("清空数据失败: " + e.getMessage(), e);
        }
    }

    private void createCollectionIfNotExists() {
        try {
            // 检查集合是否存在
            boolean collectionExists = milvusUtils.hasCollection(collectionName);
            
            if (!collectionExists) {
                createCollection();
            } else {
                log.info("Milvus集合已存在: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("检查Milvus集合存在性失败", e);
        }
    }

    private void createCollection() {
        try {
            boolean success = milvusUtils.createCollection(collectionName, "Z-RAG文档向量存储", vectorDimension);
            if (success) {
                log.info("成功创建Milvus集合: {}", collectionName);
            } else {
                throw new RuntimeException("创建Milvus集合失败");
            }
        } catch (Exception e) {
            log.error("创建Milvus集合失败", e);
            throw new RuntimeException("创建Milvus集合失败: " + e.getMessage(), e);
        }
    }
}
