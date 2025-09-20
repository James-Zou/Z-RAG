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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @Value("${rerank.qwen.base.url:https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank}")
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
            log.info("=== 重排服务 ===");
            log.info("输入匹配结果为空，跳过重排");
            return matches;
        }

        log.info("=== 重排服务开始 ===");
        log.info("查询文本: {}", query);
        log.info("输入匹配结果数量: {}", matches.size());
        log.info("最大返回结果数: {}", maxResults);
        log.info("使用重排提供商: {}", defaultRerankProvider);

        try {
            List<EmbeddingMatch<TextSegment>> result;
            switch (defaultRerankProvider.toLowerCase()) {
                case "qwen":
                    result = rerankWithQwen(query, matches, maxResults);
                    break;
                case "openai":
                    result = rerankWithOpenAI(query, matches, maxResults);
                    break;
                case "ollama":
                    result = rerankWithOllama(query, matches, maxResults);
                    break;
                default:
                    result = rerankWithDefault(query, matches, maxResults);
                    break;
            }
            
            log.info("重排完成，返回结果数量: {}", result.size());
            log.info("=== 重排服务结束 ===");
            return result;
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
            // 构建重排请求 - 使用正确的千问重排API端点
            String url = qwenBaseUrl;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenApiKey);

            // 构建千问重排API请求格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qwenModel);
            
            // 构建input对象
            Map<String, Object> input = new HashMap<>();
            input.put("query", query);
            
            // 构建文档列表 - 根据官方文档，documents应该是字符串数组
            List<String> documents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                documents.add(match.embedded().text());
            }
            input.put("documents", documents);
            requestBody.put("input", input);
            
            // 重排参数
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("top_n", Math.min(maxResults, matches.size()));
            parameters.put("return_documents", true);
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 打印千问重排模型信息
            log.info("=== 千问重排模型调用 ===");
            log.info("模型名称: {}", qwenModel);
            log.info("API地址: {}", url);
            log.info("请求参数: {}", requestBody);
            log.info("查询文本: {}", query);
            log.info("文档数量: {}", documents.size());
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long endTime = System.currentTimeMillis();
            
            // 打印响应结果
            log.info("响应状态码: {}", response.getStatusCode());
            log.info("响应耗时: {} ms", (endTime - startTime));
            log.info("响应体: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                
                if (output != null) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) output.get("results");
                    
                    if (results != null && !results.isEmpty()) {
                        // 解析重排结果
                        List<EmbeddingMatch<TextSegment>> rerankedMatches = new ArrayList<>();
                        
                        for (Map<String, Object> result : results) {
                            Integer index = (Integer) result.get("index");
                            Double score = (Double) result.get("relevance_score");
                            
                            if (index != null && index >= 0 && index < matches.size()) {
                                // 更新相似度分数
                                EmbeddingMatch<TextSegment> originalMatch = matches.get(index);
                                EmbeddingMatch<TextSegment> rerankedMatch = new EmbeddingMatch<>(
                                    score != null ? score : originalMatch.score(),
                                    originalMatch.embeddingId(),
                                    originalMatch.embedding(),
                                    originalMatch.embedded()
                                );
                                rerankedMatches.add(rerankedMatch);
                            }
                        }
                        
                        // 如果重排结果为空，返回原始顺序
                        if (rerankedMatches.isEmpty()) {
                            log.warn("重排结果为空，返回原始顺序");
                            return matches.stream().limit(maxResults).collect(Collectors.toList());
                        }
                        
                        return rerankedMatches.stream().limit(maxResults).collect(Collectors.toList());
                    }
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

            // 打印Ollama重排模型信息
            log.info("=== Ollama重排模型调用 ===");
            log.info("模型名称: {}", ollamaModel);
            log.info("API地址: {}", url);
            log.info("请求参数: {}", requestBody);
            log.info("提示词长度: {} 字符", prompt.length());
            log.info("提示词预览: {}", prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt.toString());
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long endTime = System.currentTimeMillis();
            
            // 打印响应结果
            log.info("响应状态码: {}", response.getStatusCode());
            log.info("响应耗时: {} ms", (endTime - startTime));
            log.info("响应体: {}", response.getBody());

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
    
    /**
     * 流式重排
     * @param query 查询文本
     * @param matches 匹配结果
     * @param maxResults 最大结果数
     * @param emitter 流式响应发射器
     * @return 重排后的结果
     */
    public List<TextSegment> rerankStream(String query, List<EmbeddingMatch<TextSegment>> matches, int maxResults, SseEmitter emitter) {
        try {
            log.info("=== 重排服务开始 ===");
            log.info("查询文本: {}", query);
            log.info("输入匹配结果数量: {}", matches.size());
            log.info("最大返回结果数: {}", maxResults);
            log.info("使用重排提供商: {}", defaultRerankProvider);
            
            if (matches.isEmpty()) {
                StreamingUtils.sendRerank(emitter, "❌ 没有需要重排的结果");
                log.info("没有需要重排的结果");
                return new ArrayList<>();
            }
            
            if (matches.size() <= maxResults) {
                StreamingUtils.sendRerank(emitter, String.format("✅ 结果数量(%d)不超过最大限制(%d)，无需重排", matches.size(), maxResults));
                log.info("结果数量不超过最大限制，无需重排");
                return matches.stream()
                        .map(EmbeddingMatch::embedded)
                        .collect(Collectors.toList());
            }
            
            // 发送重排进度
            StreamingUtils.sendRerank(emitter, String.format("🔄 正在对 %d 个结果进行重排...", matches.size()));
            
            List<TextSegment> rerankedSegments;
            
            switch (defaultRerankProvider.toLowerCase()) {
                case "qwen":
                    rerankedSegments = rerankWithQwenStream(query, matches, maxResults, emitter);
                    break;
                case "openai":
                    rerankedSegments = rerankWithOpenAIStream(query, matches, maxResults, emitter);
                    break;
                case "ollama":
                    rerankedSegments = rerankWithOllamaStream(query, matches, maxResults, emitter);
                    break;
                default:
                    log.warn("未知的重排提供商: {}，使用默认重排", defaultRerankProvider);
                    rerankedSegments = rerankWithDefault("", matches, maxResults).stream()
                            .map(EmbeddingMatch::embedded)
                            .collect(Collectors.toList());
                    break;
            }
            
            log.info("重排完成，返回结果数量: {}", rerankedSegments.size());
            log.info("=== 重排服务结束 ===");
            
            return rerankedSegments;
            
        } catch (Exception e) {
            log.error("流式重排失败", e);
            StreamingUtils.sendError(emitter, "重排失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 使用千问进行流式重排
     */
    private List<TextSegment> rerankWithQwenStream(String query, List<EmbeddingMatch<TextSegment>> matches, int maxResults, SseEmitter emitter) {
        try {
            StreamingUtils.sendRerank(emitter, "🤖 使用千问模型进行重排...");
            
            // 构建重排请求
            String url = qwenBaseUrl;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qwenModel);
            
            // 构建input对象
            Map<String, Object> input = new HashMap<>();
            input.put("query", query);
            
            // 构建文档列表 - 根据官方文档，documents应该是字符串数组
            List<String> documents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                documents.add(match.embedded().text());
            }
            input.put("documents", documents);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("top_n", maxResults);
            parameters.put("return_documents", true);
            requestBody.put("parameters", parameters);
            
            log.info("=== 千问重排模型调用 ===");
            log.info("模型名称: {}", qwenModel);
            log.info("API地址: {}", url);
            log.info("请求参数: {}", requestBody);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenApiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long endTime = System.currentTimeMillis();
            
            log.info("千问重排API调用完成，耗时: {} ms", endTime - startTime);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                
                if (output != null) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) output.get("results");
                    
                    if (results != null && !results.isEmpty()) {
                        List<TextSegment> rerankedSegments = new ArrayList<>();
                        for (Map<String, Object> result : results) {
                            Integer index = (Integer) result.get("index");
                            if (index != null && index < matches.size()) {
                                rerankedSegments.add(matches.get(index).embedded());
                            }
                        }
                        
                        StreamingUtils.sendRerank(emitter, String.format("✅ 千问重排完成，返回 %d 个结果", rerankedSegments.size()));
                        return rerankedSegments;
                    }
                }
            }
            
            // 如果API调用失败，使用默认重排
            StreamingUtils.sendRerank(emitter, "⚠️ 千问重排失败，使用默认重排");
            return rerankWithDefault("", matches, maxResults).stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("千问重排模型调用异常", e);
            StreamingUtils.sendRerank(emitter, "⚠️ 千问重排异常，使用默认重排");
            return rerankWithDefault("", matches, maxResults).stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 使用OpenAI进行流式重排
     */
    private List<TextSegment> rerankWithOpenAIStream(String query, List<EmbeddingMatch<TextSegment>> matches, int maxResults, SseEmitter emitter) {
        try {
            StreamingUtils.sendRerank(emitter, "🤖 使用OpenAI模型进行重排...");
            
            // 这里可以实现OpenAI的重排逻辑
            // 暂时使用默认重排
            StreamingUtils.sendRerank(emitter, "⚠️ OpenAI重排暂未实现，使用默认重排");
            return rerankWithDefault("", matches, maxResults).stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("OpenAI重排失败", e);
            StreamingUtils.sendRerank(emitter, "⚠️ OpenAI重排失败，使用默认重排");
            return rerankWithDefault("", matches, maxResults).stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 使用Ollama进行流式重排
     */
    private List<TextSegment> rerankWithOllamaStream(String query, List<EmbeddingMatch<TextSegment>> matches, int maxResults, SseEmitter emitter) {
        try {
            StreamingUtils.sendRerank(emitter, "🤖 使用Ollama模型进行重排...");
            
            // 这里可以实现Ollama的重排逻辑
            // 暂时使用默认重排
            StreamingUtils.sendRerank(emitter, "⚠️ Ollama重排暂未实现，使用默认重排");
            return rerankWithDefault("", matches, maxResults).stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Ollama重排失败", e);
            StreamingUtils.sendRerank(emitter, "⚠️ Ollama重排失败，使用默认重排");
            return rerankWithDefault("", matches, maxResults).stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
        }
    }
}
