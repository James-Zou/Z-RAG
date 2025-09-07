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


package com.unionhole.zrag.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 千问嵌入模型实现
 * 基于阿里云千问API
 */
@Slf4j
@Component
public class QwenEmbeddingModel implements EmbeddingModel {

    @Value("${models.qwen.api.key:}")
    private String apiKey;

    @Value("${models.qwen.base.url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${models.qwen.embedding.model:text-embedding-v1}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Response<Embedding> embed(String text) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("千问API Key未配置，使用本地嵌入模型");
                // 使用本地模型作为备选
                dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel localModel = 
                    new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
                return localModel.embed(text);
            }

            // 构建请求
            String url = baseUrl + "/services/embeddings/text-embedding/text-embedding";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            Map<String, Object> input = new HashMap<>();
            input.put("texts", new String[]{text});
            requestBody.put("input", input);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                
                if (output != null && output.containsKey("embeddings")) {
                    List<Map<String, Object>> embeddings = (List<Map<String, Object>>) output.get("embeddings");
                    if (!embeddings.isEmpty()) {
                        List<Double> embeddingValues = (List<Double>) embeddings.get(0).get("embedding");
                        float[] embeddingArray = new float[embeddingValues.size()];
                        for (int i = 0; i < embeddingValues.size(); i++) {
                            embeddingArray[i] = embeddingValues.get(i).floatValue();
                        }
                        
                        Embedding embedding = Embedding.from(embeddingArray);
                        return Response.from(embedding);
                    }
                }
            }

            log.error("千问嵌入API调用失败: {}", response.getStatusCode());
            // 使用本地模型作为备选
            dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel localModel = 
                new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
            return localModel.embed(text);

        } catch (Exception e) {
            log.error("千问嵌入模型调用异常", e);
            // 使用本地模型作为备选
            dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel localModel = 
                new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
            return localModel.embed(text);
        }
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("千问API Key未配置，使用本地嵌入模型");
                // 使用本地模型作为备选
                dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel localModel = 
                    new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
                return localModel.embedAll(textSegments);
            }

            // 构建请求
            String url = baseUrl + "/services/embeddings/text-embedding/text-embedding";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            // 提取文本内容
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .collect(Collectors.toList());
            
            Map<String, Object> input = new HashMap<>();
            input.put("texts", texts.toArray());
            requestBody.put("input", input);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                
                if (output != null && output.containsKey("embeddings")) {
                    List<Map<String, Object>> embeddings = (List<Map<String, Object>>) output.get("embeddings");
                    List<Embedding> embeddingList = embeddings.stream()
                            .map(emb -> {
                                List<Double> embeddingValues = (List<Double>) emb.get("embedding");
                                float[] embeddingArray = new float[embeddingValues.size()];
                                for (int i = 0; i < embeddingValues.size(); i++) {
                                    embeddingArray[i] = embeddingValues.get(i).floatValue();
                                }
                                return Embedding.from(embeddingArray);
                            })
                            .collect(Collectors.toList());
                    
                    return Response.from(embeddingList);
                }
            }

            log.error("千问嵌入API调用失败: {}", response.getStatusCode());
            // 使用本地模型作为备选
            dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel localModel = 
                new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
            return localModel.embedAll(textSegments);

        } catch (Exception e) {
            log.error("千问嵌入模型调用异常", e);
            // 使用本地模型作为备选
            dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel localModel = 
                new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
            return localModel.embedAll(textSegments);
        }
    }
}
