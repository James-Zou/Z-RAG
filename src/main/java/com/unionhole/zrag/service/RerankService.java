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

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重排服务
 * 对检索结果进行重新排序，提升相关性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RerankService {

    @Value("${rerank.qwen.api.key:}")
    private String qwenApiKey;

    @Value("${rerank.qwen.base.url:https://dashscope.aliyuncs.com/api/v1}")
    private String qwenBaseUrl;

    @Value("${rerank.qwen.model:qwen-reranker}")
    private String qwenModel;

    @Value("${rerank.openai.api.key:}")
    private String openaiApiKey;

    @Value("${rerank.openai.base.url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${rerank.openai.model:text-embedding-3-large}")
    private String openaiModel;

    @Value("${rerank.ollama.base.url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${rerank.ollama.model:qwen2.5:7b}")
    private String ollamaModel;

    @Value("${default.rerank.provider:qwen}")
    private String defaultRerankProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 重排检索结果
     * @param query 查询文本
     * @param matches 检索到的匹配结果
     * @param maxResults 最大返回结果数
     * @return 重排后的结果
     */
    public List<EmbeddingMatch<TextSegment>> rerank(String query, 
                                                   List<EmbeddingMatch<TextSegment>> matches, 
                                                   int maxResults) {
        if (matches == null || matches.isEmpty()) {
            return matches;
        }

        try {
            switch (defaultRerankProvider.toLowerCase()) {
                case "qwen":
                    return rerankWithQwen(query, matches, maxResults);
                case "openai":
                    return rerankWithOpenAI(query, matches, maxResults);
                case "ollama":
                    return rerankWithOllama(query, matches, maxResults);
                default:
                    return rerankWithDefault(query, matches, maxResults);
            }
        } catch (Exception e) {
            log.error("重排失败，使用默认排序", e);
            return rerankWithDefault(query, matches, maxResults);
        }
    }

    /**
     * 使用千问重排模型
     */
    private List<EmbeddingMatch<TextSegment>> rerankWithQwen(String query, 
                                                            List<EmbeddingMatch<TextSegment>> matches, 
                                                            int maxResults) {
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            log.warn("千问API Key未配置，使用默认排序");
            return rerankWithDefault(query, matches, maxResults);
        }

        try {
            // 构建重排请求
            String url = qwenBaseUrl + "/services/aigc/text-generation/generation";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenApiKey);

            // 构建重排提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据查询内容对以下文档片段进行相关性排序，返回最相关的文档。\n\n");
            prompt.append("查询: ").append(query).append("\n\n");
            prompt.append("文档片段:\n");
            
            for (int i = 0; i < matches.size(); i++) {
                prompt.append(i + 1).append(". ").append(matches.get(i).embedded().text()).append("\n");
            }
            
            prompt.append("\n请返回最相关的文档编号，用逗号分隔，按相关性从高到低排序。");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qwenModel);
            
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt.toString());
            input.put("messages", new Object[]{message});
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.1);
            parameters.put("max_tokens", 100);
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                
                if (output != null && output.containsKey("text")) {
                    String rankedIndices = (String) output.get("text");
                    return rerankByIndices(matches, rankedIndices, maxResults);
                }
            }

            log.error("千问重排API调用失败: {}", response.getStatusCode());
            return rerankWithDefault(query, matches, maxResults);

        } catch (Exception e) {
            log.error("千问重排模型调用异常", e);
            return rerankWithDefault(query, matches, maxResults);
        }
    }

    /**
     * 使用OpenAI重排模型
     */
    private List<EmbeddingMatch<TextSegment>> rerankWithOpenAI(String query, 
                                                              List<EmbeddingMatch<TextSegment>> matches, 
                                                              int maxResults) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.warn("OpenAI API Key未配置，使用默认排序");
            return rerankWithDefault(query, matches, maxResults);
        }

        // 使用OpenAI的嵌入模型进行重排
        try {
            // 这里可以实现基于OpenAI嵌入模型的重排逻辑
            // 暂时使用默认排序
            return rerankWithDefault(query, matches, maxResults);
        } catch (Exception e) {
            log.error("OpenAI重排模型调用异常", e);
            return rerankWithDefault(query, matches, maxResults);
        }
    }

    /**
     * 使用Ollama重排模型
     */
    private List<EmbeddingMatch<TextSegment>> rerankWithOllama(String query, 
                                                              List<EmbeddingMatch<TextSegment>> matches, 
                                                              int maxResults) {
        try {
            // 使用Ollama本地模型进行重排
            String url = ollamaBaseUrl + "/api/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建重排提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据查询内容对以下文档片段进行相关性排序，返回最相关的文档。\n\n");
            prompt.append("查询: ").append(query).append("\n\n");
            prompt.append("文档片段:\n");
            
            for (int i = 0; i < matches.size(); i++) {
                prompt.append(i + 1).append(". ").append(matches.get(i).embedded().text()).append("\n");
            }
            
            prompt.append("\n请返回最相关的文档编号，用逗号分隔，按相关性从高到低排序。");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt.toString());
            requestBody.put("stream", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String rankedIndices = (String) responseBody.get("response");
                return rerankByIndices(matches, rankedIndices, maxResults);
            }

            log.error("Ollama重排API调用失败: {}", response.getStatusCode());
            return rerankWithDefault(query, matches, maxResults);

        } catch (Exception e) {
            log.error("Ollama重排模型调用异常", e);
            return rerankWithDefault(query, matches, maxResults);
        }
    }

    /**
     * 默认重排（基于相似度分数）
     */
    private List<EmbeddingMatch<TextSegment>> rerankWithDefault(String query, 
                                                               List<EmbeddingMatch<TextSegment>> matches, 
                                                               int maxResults) {
        return matches.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 根据模型返回的索引进行重排
     */
    private List<EmbeddingMatch<TextSegment>> rerankByIndices(List<EmbeddingMatch<TextSegment>> matches, 
                                                             String rankedIndices, 
                                                             int maxResults) {
        try {
            // 解析模型返回的索引
            String[] indices = rankedIndices.trim().split("[,\\s]+");
            List<EmbeddingMatch<TextSegment>> rerankedMatches = new ArrayList<>();
            
            for (String indexStr : indices) {
                try {
                    int index = Integer.parseInt(indexStr.trim()) - 1; // 转换为0基索引
                    if (index >= 0 && index < matches.size()) {
                        rerankedMatches.add(matches.get(index));
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析索引: {}", indexStr);
                }
            }
            
            // 如果解析失败，使用默认排序
            if (rerankedMatches.isEmpty()) {
                return rerankWithDefault("", matches, maxResults);
            }
            
            return rerankedMatches.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("解析重排结果失败", e);
            return rerankWithDefault("", matches, maxResults);
        }
    }

    /**
     * 获取重排服务状态
     */
    public String getRerankStatus() {
        StringBuilder status = new StringBuilder();
        status.append("重排服务状态:\n");
        status.append("- 默认提供商: ").append(defaultRerankProvider).append("\n");
        status.append("- 千问配置: ").append(qwenApiKey != null && !qwenApiKey.isEmpty() ? "已配置" : "未配置").append("\n");
        status.append("- OpenAI配置: ").append(openaiApiKey != null && !openaiApiKey.isEmpty() ? "已配置" : "未配置").append("\n");
        status.append("- Ollama配置: ").append("已配置").append("\n");
        return status.toString();
    }
}
