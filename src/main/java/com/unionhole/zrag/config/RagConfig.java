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


package com.unionhole.zrag.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import com.unionhole.zrag.store.InMemoryEmbeddingStore;
import com.unionhole.zrag.store.MilvusEmbeddingStore;
import com.unionhole.zrag.model.QwenChatModel;
import com.unionhole.zrag.model.QwenEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG配置类
 * 支持多种模型提供商：OpenAI、千问、Ollama
 */
@Configuration
public class RagConfig {

    // OpenAI配置
    @Value("${models.openai.api.key:}")
    private String openaiApiKey;

    @Value("${models.openai.base.url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${models.openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Value("${models.openai.embedding.model:text-embedding-ada-002}")
    private String openaiEmbeddingModel;

    // 千问配置
    @Value("${models.qwen.api.key:}")
    private String qwenApiKey;

    @Value("${models.qwen.base.url:https://dashscope.aliyuncs.com/api/v1}")
    private String qwenBaseUrl;

    @Value("${models.qwen.model:qwen-turbo}")
    private String qwenModel;

    @Value("${models.qwen.embedding.model:text-embedding-v1}")
    private String qwenEmbeddingModel;

    // Ollama配置
    @Value("${models.ollama.base.url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${models.ollama.model:qwen2.5:7b}")
    private String ollamaModel;

    @Value("${models.ollama.embedding.model:nomic-embed-text}")
    private String ollamaEmbeddingModel;

    // 默认提供商
    @Value("${default.provider:qwen}")
    private String defaultProvider;

    // 重排配置
    @Value("${default.rerank.provider:qwen}")
    private String defaultRerankProvider;

    // 向量存储配置
    @Value("${vector-store.type:milvus}")
    private String vectorStoreType;

    /**
     * 配置嵌入模型
     * 根据配置选择不同的嵌入模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        switch (defaultProvider.toLowerCase()) {
            case "openai":
                if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                    return OpenAiEmbeddingModel.builder()
                            .apiKey(openaiApiKey)
                            .baseUrl(openaiBaseUrl)
                            .modelName(openaiEmbeddingModel)
                            .build();
                }
                break;
            case "qwen":
                if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
                    return createQwenEmbeddingModel();
                }
                break;
            case "ollama":
                return OllamaEmbeddingModel.builder()
                        .baseUrl(ollamaBaseUrl)
                        .modelName(ollamaEmbeddingModel)
                        .build();
        }
        
        // 默认使用本地模型
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 配置聊天模型
     * 根据配置选择不同的聊天模型
     */
    @Bean
    public ChatLanguageModel chatModel() {
        switch (defaultProvider.toLowerCase()) {
            case "openai":
                if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                    return OpenAiChatModel.builder()
                            .apiKey(openaiApiKey)
                            .baseUrl(openaiBaseUrl)
                            .modelName(openaiModel)
                            .temperature(0.7)
                            .build();
                }
                break;
            case "qwen":
                if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
                    return createQwenChatModel();
                }
                break;
            case "ollama":
                return OllamaChatModel.builder()
                        .baseUrl(ollamaBaseUrl)
                        .modelName(ollamaModel)
                        .temperature(0.7)
                        .build();
        }
        
        // 默认使用千问模型（即使没有API Key也会返回，内部会使用模拟回答）
        return createQwenChatModel();
    }

    /**
     * 配置向量存储
     * 根据配置选择不同的向量存储
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(MilvusEmbeddingStore milvusEmbeddingStore) {
        switch (vectorStoreType.toLowerCase()) {
            case "milvus":
                return milvusEmbeddingStore;
            case "memory":
            default:
                return new InMemoryEmbeddingStore();
        }
    }

    /**
     * 配置内容检索器
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, 
                                           EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .build();
    }

    /**
     * 配置文档分割器
     */
    @Bean
    public DocumentSplitter documentSplitter() {
        return DocumentSplitters.recursive(300, 0);
    }

    /**
     * 创建千问嵌入模型
     */
    private EmbeddingModel createQwenEmbeddingModel() {
        return new QwenEmbeddingModel();
    }

    /**
     * 创建千问聊天模型
     */
    private ChatLanguageModel createQwenChatModel() {
        return new QwenChatModel();
    }

    // RerankService 已通过 @Service 注解自动注册，无需在此处重复定义
}