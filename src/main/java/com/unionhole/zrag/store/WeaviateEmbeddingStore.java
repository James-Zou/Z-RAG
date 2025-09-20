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

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import com.unionhole.zrag.util.WeaviateLLMUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Weaviate向量存储实现
 * 基于Weaviate向量数据库的持久化存储
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "vector-store.type", havingValue = "weaviate")
public class WeaviateEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final WeaviateLLMUtils weaviateUtils;
    
    @Value("${weaviate.class-name:ZRAGDocument}")
    private String className;
    
    @Value("${weaviate.vector-dimension:384}")
    private Integer vectorDimension;
    
    @Value("${weaviate.host:localhost}")
    private String host;
    
    @Value("${weaviate.port:8080}")
    private Integer port;
    
    @Value("${weaviate.scheme:http}")
    private String scheme;

    public WeaviateEmbeddingStore(@Autowired(required = false) WeaviateLLMUtils weaviateUtils) {
        this.weaviateUtils = weaviateUtils;
    }

    @PostConstruct
    public void initialize() {
        if (weaviateUtils != null) {
            createClassIfNotExists();
        } else {
            log.warn("WeaviateLLMUtils 不可用，跳过 Weaviate 初始化");
        }
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding, null);
        return id;
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        add(id, embedding, textSegment);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        add(id, embedding, null);
    }

    public void add(String id, Embedding embedding, TextSegment textSegment) {
        if (weaviateUtils == null) {
            log.warn("WeaviateLLMUtils 不可用，无法添加向量");
            return;
        }
        
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("text", textSegment != null ? textSegment.text() : "");
            properties.put("metadata", textSegment != null ? textSegment.metadata().asMap() : new HashMap<>());
            
            // 转换float[]为Float[]
            Float[] vector = new Float[embedding.vector().length];
            for (int i = 0; i < embedding.vector().length; i++) {
                vector[i] = embedding.vector()[i];
            }
            
            // 使用WeaviateUtils添加对象
            boolean success = weaviateUtils.addObject(className, id, properties, vector);
            if (success) {
                log.debug("成功添加向量到Weaviate: {}", id);
            } else {
                throw new RuntimeException("添加向量失败");
            }
        } catch (Exception e) {
            log.error("添加向量到Weaviate失败", e);
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
            TextSegment textSegment = i < textSegments.size() ? textSegments.get(i) : null;
            ids.add(add(embeddings.get(i), textSegment));
        }
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults) {
        return findRelevant(referenceEmbedding, maxResults, 0.0);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        if (weaviateUtils == null) {
            log.warn("WeaviateLLMUtils 不可用，无法查询向量");
            return Collections.emptyList();
        }
        
        try {
            log.info("执行Weaviate向量查询，maxResults: {}, minScore: {}", maxResults, minScore);
            
            // 使用WeaviateLLMUtils查询对象
            List<io.weaviate.client.v1.data.model.WeaviateObject> objects = weaviateUtils.queryObjects(className, maxResults);
            
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            
            if (objects != null) {
                for (io.weaviate.client.v1.data.model.WeaviateObject obj : objects) {
                    try {
                        Map<String, Object> properties = obj.getProperties();
                        Float[] objVector = obj.getVector();
                        
                        if (objVector != null && properties != null) {
                            // 计算相似度分数
                            double similarity = calculateCosineSimilarity(referenceEmbedding.vector(), objVector);
                            
                            if (similarity >= minScore) {
                                String text = (String) properties.get("text");
                                Map<String, Object> metadata = (Map<String, Object>) properties.get("metadata");
                                
                                TextSegment textSegment = TextSegment.from(text, 
                                    dev.langchain4j.data.document.Metadata.from(metadata != null ? metadata : new HashMap<>()));
                                
                                Embedding embedding = new Embedding(new float[vectorDimension]);
                                EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(similarity, obj.getId(), embedding, textSegment);
                                matches.add(match);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("处理Weaviate对象失败", e);
                    }
                }
            }
            
            // 按相似度排序
            matches.sort((a, b) -> Double.compare(b.score(), a.score()));
            
            return matches.stream().limit(maxResults).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Weaviate查询失败", e);
            return Collections.emptyList();
        }
    }

    public List<TextSegment> findAll() {
        if (weaviateUtils == null) {
            log.warn("WeaviateLLMUtils 不可用，无法查询所有数据");
            return Collections.emptyList();
        }
        
        try {
            log.info("执行Weaviate findAll查询");
            
            // 使用WeaviateLLMUtils查询所有对象
            List<io.weaviate.client.v1.data.model.WeaviateObject> objects = weaviateUtils.queryObjects(className, 10000);
            
            List<TextSegment> textSegments = new ArrayList<>();
            
            if (objects != null) {
                for (io.weaviate.client.v1.data.model.WeaviateObject obj : objects) {
                    try {
                        Map<String, Object> properties = obj.getProperties();
                        if (properties != null) {
                            String text = (String) properties.get("text");
                            Map<String, Object> metadata = (Map<String, Object>) properties.get("metadata");
                            
                            TextSegment textSegment = TextSegment.from(text, 
                                dev.langchain4j.data.document.Metadata.from(metadata != null ? metadata : new HashMap<>()));
                            textSegments.add(textSegment);
                        }
                    } catch (Exception e) {
                        log.warn("处理Weaviate对象失败", e);
                    }
                }
            }
            
            return textSegments;

        } catch (Exception e) {
            log.error("Weaviate查询所有数据失败", e);
            return Collections.emptyList();
        }
    }

    public void clear() {
        if (weaviateUtils == null) {
            log.warn("WeaviateLLMUtils 不可用，无法清空数据");
            return;
        }
        
        try {
            log.info("开始清空Weaviate数据");
            
            // 先获取所有对象
            List<io.weaviate.client.v1.data.model.WeaviateObject> objects = weaviateUtils.queryObjects(className, 10000);
            
            if (objects != null) {
                // 删除每个对象
                for (io.weaviate.client.v1.data.model.WeaviateObject obj : objects) {
                    String objectId = obj.getId();
                    if (objectId != null) {
                        weaviateUtils.deleteObject(className, objectId);
                    }
                }
            }
            
            log.info("成功清空Weaviate数据");
        } catch (Exception e) {
            log.error("清空Weaviate数据失败", e);
            throw new RuntimeException("清空数据失败: " + e.getMessage(), e);
        }
    }

    private void createClassIfNotExists() {
        try {
            // 检查类是否存在
            boolean classExists = weaviateUtils.classExists(className);
            
            if (!classExists) {
                createClass();
            } else {
                log.info("Weaviate类已存在: {}", className);
            }
        } catch (Exception e) {
            log.error("检查Weaviate类存在性失败", e);
        }
    }

    private void createClass() {
        try {
            boolean success = weaviateUtils.createClass(className, "Z-RAG文档向量存储", vectorDimension);
            if (success) {
                log.info("成功创建Weaviate类: {}", className);
            } else {
                throw new RuntimeException("创建Weaviate类失败");
            }
        } catch (Exception e) {
            log.error("创建Weaviate类失败", e);
            throw new RuntimeException("创建Weaviate类失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(float[] vector1, Float[] vector2) {
        if (vector1.length != vector2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}
