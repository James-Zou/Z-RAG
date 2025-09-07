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

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生成服务
 * 使用LangChain4j的完整生成功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final ChatLanguageModel chatModel;

    /**
     * 基于检索到的文档生成回答
     * @param query 用户查询
     * @param retrievedDocuments 检索到的文档内容
     * @return 生成的回答
     */
    public String generateAnswer(String query, List<String> retrievedDocuments) {
        log.info("生成回答，查询: {}", query);
        
        try {
            if (chatModel == null) {
                return generateAnswerWithMockModel(query, retrievedDocuments);
            }
            
            // 构建上下文
            StringBuilder context = new StringBuilder();
            context.append("基于以下文档内容回答用户的问题：\n\n");
            
            for (int i = 0; i < retrievedDocuments.size(); i++) {
                context.append("文档片段 ").append(i + 1).append(":\n");
                context.append(retrievedDocuments.get(i)).append("\n\n");
            }
            
            // 构建提示词
            String prompt = String.format(
                    "%s\n\n用户问题: %s\n\n请基于上述文档内容提供准确、详细的回答。如果文档中没有相关信息，请明确说明。",
                    context.toString(),
                    query
            );
            
            // 生成回答
            String answer = chatModel.generate(prompt);
            
            log.info("回答生成完成");
            return answer;
        } catch (Exception e) {
            log.error("生成回答失败", e);
            return generateAnswerWithMockModel(query, retrievedDocuments);
        }
    }

    /**
     * 使用内容检索器生成回答
     * @param query 用户查询
     * @param contentRetriever 内容检索器
     * @return 生成的回答
     */
    public String generateAnswer(String query, ContentRetriever contentRetriever) {
        log.info("使用内容检索器生成回答，查询: {}", query);
        
        try {
            if (chatModel == null) {
                return generateAnswerWithMockModel(query, java.util.Arrays.asList("无检索内容"));
            }
            
            // 检索相关内容
            List<String> retrievedContent = contentRetriever.retrieve(Query.from(query))
                    .stream()
                    .map(content -> content.textSegment().text())
                    .collect(java.util.stream.Collectors.toList());
            
            return generateAnswer(query, retrievedContent);
        } catch (Exception e) {
            log.error("使用内容检索器生成回答失败", e);
            return generateAnswerWithMockModel(query, java.util.Arrays.asList("检索失败"));
        }
    }

    /**
     * 生成回答（使用模拟模型）
     * @param query 用户查询
     * @param retrievedDocuments 检索到的文档内容
     * @return 生成的回答
     */
    public String generateAnswerWithMockModel(String query, List<String> retrievedDocuments) {
        log.info("使用模拟模型生成回答，查询: {}", query);
        
        StringBuilder answer = new StringBuilder();
        answer.append("基于检索到的文档，我为您提供以下回答：\n\n");
        answer.append("查询: ").append(query).append("\n\n");
        answer.append("相关文档内容:\n");
        
        for (int i = 0; i < retrievedDocuments.size(); i++) {
            answer.append(i + 1).append(". ").append(retrievedDocuments.get(i)).append("\n");
        }
        
        answer.append("\n注意：这是使用模拟模型生成的回答，实际应用中请配置OpenAI API Key。");
        
        return answer.toString();
    }

    /**
     * 创建AI服务接口
     * @param contentRetriever 内容检索器
     * @return AI服务接口
     */
    public Object createAiService(ContentRetriever contentRetriever) {
        if (chatModel == null) {
            return null;
        }
        
        try {
            return AiServices.builder(Object.class)
                    .chatLanguageModel(chatModel)
                    .contentRetriever(contentRetriever)
                    .build();
        } catch (Exception e) {
            log.error("创建AI服务失败", e);
            return null;
        }
    }
}