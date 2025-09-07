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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义内存向量存储实现
 * 实现基本的向量存储和检索功能
 */
public class InMemoryEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final Map<String, EmbeddingData> embeddings = new ConcurrentHashMap<>();

    @Override
    public String add(Embedding embedding) {
        String id = generateId();
        embeddings.put(id, new EmbeddingData(embedding, null));
        return id;
    }

    public void add(String id, Embedding embedding) {
        embeddings.put(id, new EmbeddingData(embedding, null));
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = generateId();
        embeddings.put(id, new EmbeddingData(embedding, textSegment));
        return id;
    }

    public void add(String id, Embedding embedding, TextSegment textSegment) {
        embeddings.put(id, new EmbeddingData(embedding, textSegment));
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            ids.add(add(embedding));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        if (embeddings.size() != textSegments.size()) {
            throw new IllegalArgumentException("Embeddings and text segments must have the same size");
        }
        
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            ids.add(add(embeddings.get(i), textSegments.get(i)));
        }
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults) {
        return findRelevant(referenceEmbedding, maxResults, 0.0);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        return embeddings.values().stream()
                .map(data -> new EmbeddingMatch<>(
                        calculateSimilarity(referenceEmbedding, data.embedding),
                        data.id,
                        data.embedding,
                        data.textSegment
                ))
                .filter(match -> match.score() >= minScore)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<TextSegment> findAll() {
        return embeddings.values().stream()
                .map(data -> data.textSegment)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    public void clear() {
        embeddings.clear();
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    private double calculateSimilarity(Embedding embedding1, Embedding embedding2) {
        // 简单的余弦相似度计算
        float[] vector1 = embedding1.vector();
        float[] vector2 = embedding2.vector();
        
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
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

    private static class EmbeddingData {
        final String id;
        final Embedding embedding;
        final TextSegment textSegment;

        EmbeddingData(Embedding embedding, TextSegment textSegment) {
            this.id = UUID.randomUUID().toString();
            this.embedding = embedding;
            this.textSegment = textSegment;
        }
    }
}
