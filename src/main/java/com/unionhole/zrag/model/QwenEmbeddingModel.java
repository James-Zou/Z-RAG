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
            String url = baseUrl + "/embeddings";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 打印模型信息和请求参数
            log.info("=== 千问Embedding模型调用 ===");
            log.info("模型名称: {}", model);
            log.info("API地址: {}", url);
            log.info("输入文本长度: {} 字符", text.length());
            
            // 安全地预览文本内容，避免乱码显示
            String safePreview = getSafeTextPreview(text, 100);
            log.info("输入文本预览: {}", safePreview);
            
            // 不直接打印requestBody，避免打印大量二进制内容
            log.debug("请求参数详情: {}", requestBody);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long endTime = System.currentTimeMillis();
            
            // 打印响应结果
            log.info("响应状态码: {}", response.getStatusCode());
            log.info("响应耗时: {} ms", (endTime - startTime));
            log.info("响应体: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 千问API的响应格式
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    if (!data.isEmpty()) {
                        List<Double> embeddingValues = (List<Double>) data.get(0).get("embedding");
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
            String url = baseUrl + "/embeddings";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            // 提取文本内容
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .collect(Collectors.toList());
            
            requestBody.put("input", texts);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 打印模型信息和请求参数
            log.info("=== 千问Embedding模型批量调用 ===");
            log.info("模型名称: {}", model);
            log.info("API地址: {}", url);
            log.info("文本片段数量: {}", textSegments.size());
            log.info("总文本长度: {} 字符", texts.stream().mapToInt(String::length).sum());
            
         
            
            // 不直接打印requestBody，避免打印大量二进制内容
            log.debug("请求参数详情: {}", requestBody);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long endTime = System.currentTimeMillis();
            
            // 打印响应结果
            log.info("响应状态码: {}", response.getStatusCode());
            log.info("响应耗时: {} ms", (endTime - startTime));
            log.info("响应体: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 千问API的响应格式
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    List<Embedding> embeddingList = data.stream()
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
    
    /**
     * 安全地预览文本内容，避免乱码显示
     * 
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 安全的预览文本
     */
    private String getSafeTextPreview(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "[空文本]";
        }
        
        try {
            // 先尝试清理文本
            String cleanedText = cleanTextForDisplay(text);
            
            // 检查是否仍然包含乱码
            if (containsGarbledText(cleanedText)) {
                // 尝试使用GBK编码重新处理
                String gbkText = tryConvertWithGBK(text);
                if (gbkText != null && !containsGarbledText(gbkText)) {
                    cleanedText = gbkText;
                } else {
                    return "[检测到乱码，长度: " + text.length() + " 字符]";
                }
            }
            
            // 如果文本长度超过最大长度，截取并添加省略号
            if (cleanedText.length() > maxLength) {
                return cleanedText.substring(0, maxLength) + "...";
            }
            
            return cleanedText;
            
        } catch (Exception e) {
            log.debug("文本预览处理失败: {}", e.getMessage());
            return "[文本预览失败，长度: " + text.length() + " 字符]";
        }
    }
    
    /**
     * 检查文本是否包含乱码
     * 
     * @param text 待检查的文本
     * @return 是否包含乱码
     */
    private boolean containsGarbledText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 检查常见的乱码模式
        // 1. 包含替换字符
        if (text.contains("")) {
            return true;
        }
        
        // 2. 检查是否有异常的控制字符
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        
        // 3. 检查UTF-8有效性
        try {
            byte[] bytes = text.getBytes("UTF-8");
            String reencoded = new String(bytes, "UTF-8");
            return !text.equals(reencoded);
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 清理文本用于显示
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanTextForDisplay(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // 移除BOM标记
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            
            // 替换替换字符
            text = text.replace("", "");
            
            // 移除无效的控制字符
            StringBuilder cleaned = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                    cleaned.append(' ');
                } else if (c == 0xFFFD) {
                    cleaned.append('?');
                } else {
                    cleaned.append(c);
                }
            }
            
            return cleaned.toString().trim();
            
        } catch (Exception e) {
            log.debug("文本清理失败: {}", e.getMessage());
            return text;
        }
    }
    
    /**
     * 尝试使用GBK编码转换文本
     * 
     * @param text 原始文本
     * @return 转换后的文本，如果失败返回null
     */
    private String tryConvertWithGBK(String text) {
        try {
            // 将文本转换为字节数组，然后使用GBK解码
            byte[] bytes = text.getBytes("ISO-8859-1");
            String gbkText = new String(bytes, "GBK");
            
            // 检查转换结果是否包含中文字符
            if (containsChineseCharacters(gbkText)) {
                return gbkText;
            }
            
            return null;
        } catch (Exception e) {
            log.debug("GBK转换失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查文本是否包含中文字符
     * 
     * @param text 待检查的文本
     * @return 是否包含中文字符
     */
    private boolean containsChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            // 检查是否为中文字符
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        
        return false;
    }
}
