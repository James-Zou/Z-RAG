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

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
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

/**
 * 千问聊天模型实现
 * 基于阿里云千问API
 */
@Slf4j
@Component
public class QwenChatModel implements ChatLanguageModel {

    @Value("${models.qwen.api.key:}")
    private String apiKey;

    @Value("${models.qwen.base.url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${models.qwen.model:qwen-turbo}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        // 提取最后一条用户消息
        String userMessage = messages.get(messages.size() - 1).text();
        Response<String> response = generateSingleMessage(userMessage);
        AiMessage aiMessage = AiMessage.from(response.content());
        return Response.from(aiMessage);
    }

    public Response<String> generateSingleMessage(String userMessage) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("千问API Key未配置，使用模拟回答");
                return Response.from("基于千问模型的回答：这是一个模拟回答，请配置正确的API Key。");
            }

            // 构建请求
            String url = baseUrl + "/chat/completions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 使用OpenAI兼容的请求格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", userMessage);
            messages.add(message);
            requestBody.put("messages", messages);
            
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> responseMessage = (Map<String, Object>) firstChoice.get("message");
                    
                    if (responseMessage != null && responseMessage.containsKey("content")) {
                        String answer = (String) responseMessage.get("content");
                        return Response.from(answer);
                    }
                }
            }

            log.error("千问API调用失败: {}", response.getStatusCode());
            return Response.from("千问API调用失败，请检查配置。");

        } catch (Exception e) {
            log.error("千问模型调用异常", e);
            return Response.from("千问模型调用异常: " + e.getMessage());
        }
    }

    public Response<String> generate(String userMessage, String systemMessage) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("千问API Key未配置，使用模拟回答");
                return Response.from("基于千问模型的回答：这是一个模拟回答，请配置正确的API Key。");
            }

            // 构建请求
            String url = baseUrl + "/chat/completions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            input.put("messages", new Object[]{systemMsg, userMsg});
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.7);
            parameters.put("max_tokens", 2000);
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> responseMessage = (Map<String, Object>) firstChoice.get("message");
                    
                    if (responseMessage != null && responseMessage.containsKey("content")) {
                        String answer = (String) responseMessage.get("content");
                        return Response.from(answer);
                    }
                }
            }

            log.error("千问API调用失败: {}", response.getStatusCode());
            return Response.from("千问API调用失败，请检查配置。");

        } catch (Exception e) {
            log.error("千问模型调用异常", e);
            return Response.from("千问模型调用异常: " + e.getMessage());
        }
    }
}
