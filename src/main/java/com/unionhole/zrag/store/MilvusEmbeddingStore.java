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
    
    @Value("${milvus.collection:km}")
    private String collectionName;
    
    @Value("${milvus.vector-dimension:1536}")
    private Integer vectorDimension;

    public MilvusEmbeddingStore(MilvusLLMUtils milvusUtils) {
        this.milvusUtils = milvusUtils;
    }
    
    public MilvusLLMUtils getMilvusUtils() {
        return milvusUtils;
    }
    
    public String getCollectionName() {
        return collectionName;
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
            log.info("=== Milvus 向量搜索开始 ===");
            log.info("集合名称: {}", collectionName);
            log.info("搜索参数: maxResults={}, minScore={}", maxResults, minScore);
            log.info("查询向量维度: {}", referenceEmbedding.vector().length);
            
            // 转换float[]为List<Float>
            List<Float> queryVector = new ArrayList<>();
            for (float f : referenceEmbedding.vector()) {
                queryVector.add(f);
            }
            log.info("查询向量预览: [{}...{}]", 
                    queryVector.get(0), queryVector.get(queryVector.size() - 1));

            long startTime = System.currentTimeMillis();
            // 使用MilvusLLMUtils搜索相似向量
            List<Map<String, Object>> searchResults = milvusUtils.searchSimilar(collectionName, queryVector, maxResults);
            long endTime = System.currentTimeMillis();
            
            log.info("Milvus搜索完成，原始结果数量: {}, 耗时: {} ms", 
                    searchResults.size(), (endTime - startTime));
            
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            int processedCount = 0;
            int filteredCount = 0;
            
            log.info("开始处理搜索结果...");
            for (Map<String, Object> result : searchResults) {
                try {
                    processedCount++;
                    // 直接使用MilvusLLMUtils已经计算好的相似度分数
                    Object scoreObj = result.get("score");
                    log.info("原始score对象类型: {}, 值: {}", scoreObj.getClass().getName(), scoreObj);
                    
                    double similarity;
                    if (scoreObj instanceof Number) {
                        similarity = ((Number) scoreObj).doubleValue();
                    } else {
                        similarity = Double.parseDouble(scoreObj.toString());
                    }
                    
                    log.info("处理结果 {}/{}: 相似度={:.6f}, 最小分数={:.6f}", 
                            processedCount, searchResults.size(), similarity, minScore);
                    
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
                        
                        log.info("添加匹配结果: ID={}, 相似度={:.6f}, 文本长度={}", 
                                result.get("id"), similarity, text.length());
                    } else {
                        filteredCount++;
                        log.info("过滤低分结果: 相似度={:.6f} < 最小分数={:.6f}", similarity, minScore);
                    }
                } catch (Exception e) {
                    log.error("处理Milvus搜索结果失败: {}", result, e);
                    filteredCount++;
                }
            }
            
            log.info("搜索结果处理完成: 处理总数={}, 过滤数量={}, 有效结果={}", 
                    processedCount, filteredCount, matches.size());
            
            // 按相似度排序
            matches.sort((a, b) -> Double.compare(b.score(), a.score()));
            
            List<EmbeddingMatch<TextSegment>> finalResults = matches.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());
            
            log.info("最终返回结果数量: {}", finalResults.size());
            if (!finalResults.isEmpty()) {
                log.info("最高相似度: {:.6f}, 最低相似度: {:.6f}", 
                        finalResults.get(0).score(), 
                        finalResults.get(finalResults.size() - 1).score());
            }
            
            log.info("=== Milvus 向量搜索完成 ===");
            return finalResults;

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
                log.info("Milvus集合不存在，开始创建: {}", collectionName);
                createCollection();
            } else {
                log.info("Milvus集合已存在: {}", collectionName);
                
                // 检查集合的向量维度是否匹配
                if (!isVectorDimensionMatched()) {
                    log.warn("集合向量维度不匹配，需要重新创建集合");
                    log.info("删除现有集合: {}", collectionName);
                    milvusUtils.dropCollection(collectionName);
                    log.info("重新创建集合: {}", collectionName);
                    createCollection();
                } else {
                    log.info("集合向量维度匹配，继续使用现有集合");
                    
                    // 确保集合已加载到内存
                    boolean isLoaded = milvusUtils.isCollectionLoaded(collectionName);
                    if (!isLoaded) {
                        log.info("集合未加载，正在加载到内存: {}", collectionName);
                        boolean loadSuccess = milvusUtils.loadCollection(collectionName);
                        if (loadSuccess) {
                            log.info("成功加载Milvus集合到内存: {}", collectionName);
                        } else {
                            log.warn("加载Milvus集合到内存失败: {}", collectionName);
                        }
                    } else {
                        log.info("Milvus集合已加载到内存: {}", collectionName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查Milvus集合存在性失败", e);
        }
    }
    
    /**
     * 检查集合的向量维度是否匹配
     * 通过查询集合信息来验证维度，避免插入测试数据
     */
    private boolean isVectorDimensionMatched() {
        try {
            // 通过查询集合信息来获取向量维度，避免插入测试数据
            String collectionInfo = milvusUtils.describeCollection(collectionName);
            if (collectionInfo != null && collectionInfo.contains("\"dimension\":")) {
                // 从集合信息中提取维度信息
                // 假设集合信息格式包含 "dimension": 1024 这样的信息
                String dimensionPattern = "\"dimension\"\\s*:\\s*(\\d+)";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(dimensionPattern);
                java.util.regex.Matcher matcher = pattern.matcher(collectionInfo);
                
                if (matcher.find()) {
                    int collectionDimension = Integer.parseInt(matcher.group(1));
                    boolean matched = collectionDimension == vectorDimension;
                    if (matched) {
                        log.info("向量维度匹配验证成功: 集合维度={}, 配置维度={}", collectionDimension, vectorDimension);
                    } else {
                        log.warn("向量维度不匹配: 集合维度={}, 配置维度={}", collectionDimension, vectorDimension);
                    }
                    return matched;
                }
            }
            
            // 如果无法从集合信息中获取维度，假设匹配（避免误删集合）
            log.warn("无法从集合信息中获取维度信息，假设维度匹配");
            return true;
        } catch (Exception e) {
            // 如果查询集合信息失败，假设维度匹配（避免误删集合）
            log.warn("查询集合信息时出现异常，假设维度匹配: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 强制重新创建集合（用于处理维度不匹配的情况）
     */
    public void forceRecreateCollection() {
        try {
            log.info("强制重新创建Milvus集合: {}", collectionName);
            
            // 删除现有集合
            if (milvusUtils.hasCollection(collectionName)) {
                log.info("删除现有集合: {}", collectionName);
                milvusUtils.dropCollection(collectionName);
            }
            
            // 重新创建集合
            createCollection();
            
            log.info("集合重新创建完成: {}", collectionName);
        } catch (Exception e) {
            log.error("强制重新创建集合失败", e);
            throw new RuntimeException("强制重新创建集合失败: " + e.getMessage(), e);
        }
    }

    private void createCollection() {
        try {
            boolean success = milvusUtils.createCollection(collectionName, "Z-RAG文档向量存储", vectorDimension);
            if (success) {
                log.info("成功创建Milvus集合: {}", collectionName);
                
                // 创建集合后自动加载到内存
                boolean loadSuccess = milvusUtils.loadCollection(collectionName);
                if (loadSuccess) {
                    log.info("成功加载Milvus集合到内存: {}", collectionName);
                } else {
                    log.warn("加载Milvus集合到内存失败: {}", collectionName);
                }
            } else {
                throw new RuntimeException("创建Milvus集合失败");
            }
        } catch (Exception e) {
            log.error("创建Milvus集合失败", e);
            throw new RuntimeException("创建Milvus集合失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理test测试数据
     * @return 清理的数据条数
     */
    public int cleanTestData() {
        try {
            log.info("开始清理test测试数据");
            int cleanedCount = milvusUtils.cleanTestData(collectionName);
            log.info("test测试数据清理完成，清理条数: {}", cleanedCount);
            return cleanedCount;
        } catch (Exception e) {
            log.error("清理test测试数据失败", e);
            return 0;
        }
    }
}
