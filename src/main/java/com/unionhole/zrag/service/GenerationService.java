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
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
            
            // 记录完整的prompt
            log.info("=== 发送给AI模型的完整Prompt ===");
            log.info("Prompt内容: {}", prompt);
            log.info("=== Prompt结束 ===");
            
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
    
    /**
     * 流式生成回答
     * @param query 查询文本
     * @param relevantSegments 相关文档片段
     * @param emitter 流式响应发射器
     * @return 生成的回答
     */
    public String generateStream(String query, List<TextSegment> relevantSegments, SseEmitter emitter) {
        try {
            log.info("生成回答，查询: {}", query);
            
            if (chatModel == null) {
                StreamingUtils.sendError(emitter, "聊天模型未配置");
                return "聊天模型未配置";
            }
            
            if (relevantSegments == null || relevantSegments.isEmpty()) {
                StreamingUtils.sendError(emitter, "没有相关文档片段");
                return "没有相关文档片段";
            }
            
            // 构建上下文
            StringBuilder context = new StringBuilder();
            context.append("基于以下文档内容回答问题：\n\n");
            
            for (int i = 0; i < relevantSegments.size(); i++) {
                TextSegment segment = relevantSegments.get(i);
                context.append("文档片段 ").append(i + 1).append(":\n");
                context.append(segment.text()).append("\n\n");
            }
            
            context.append("问题: ").append(query).append("\n\n");
            context.append("请基于上述文档内容，提供准确、详细的回答。如果文档中没有相关信息，请明确说明。");
            
            // 发送生成进度
            StreamingUtils.sendGeneration(emitter, "📝 正在构建上下文...");
            StreamingUtils.sendGeneration(emitter, String.format("📚 已加载 %d 个相关文档片段", relevantSegments.size()));
            StreamingUtils.sendGeneration(emitter, "🤖 正在调用AI模型生成回答...");
            
            // 记录完整的prompt
            log.info("=== 发送给AI模型的完整Prompt ===");
            log.info("Prompt内容: {}", context.toString());
            log.info("=== Prompt结束 ===");
            
            // 生成回答
            long startTime = System.currentTimeMillis();
            String answer = chatModel.generate(context.toString());
            long endTime = System.currentTimeMillis();
            
            log.info("回答生成完成，耗时: {} ms", endTime - startTime);
            
            // 发送生成完成信息
            StreamingUtils.sendGeneration(emitter, String.format("✅ 回答生成完成，耗时 %d ms", endTime - startTime));
            
            return answer;
            
        } catch (Exception e) {
            log.error("流式生成回答失败", e);
            StreamingUtils.sendError(emitter, "生成回答失败: " + e.getMessage());
            return "生成回答失败: " + e.getMessage();
        }
    }

    /**
     * 生成股神投资主题回答
     * @param query 用户查询
     * @param retrievedDocuments 检索到的文档内容
     * @return 生成的回答
     */
    public String generateStockAnswer(String query, List<String> retrievedDocuments) {
        log.info("生成股神投资主题回答，查询: {}", query);
        
        try {
            if (chatModel == null) {
                return generateStockAnswerWithMockModel(query, retrievedDocuments);
            }
            
            // 构建投资主题上下文
            StringBuilder context = new StringBuilder();
            context.append("你是一位专业的投资顾问，具有丰富的股票、基金、债券等投资经验。");
            context.append("请基于以下投资相关文档内容，以专业投资顾问的身份回答用户的问题：\n\n");
            
            for (int i = 0; i < retrievedDocuments.size(); i++) {
                context.append("投资文档片段 ").append(i + 1).append(":\n");
                context.append(retrievedDocuments.get(i)).append("\n\n");
            }
            
            // 构建投资主题提示词
            String prompt = String.format(
                    "%s\n\n用户投资问题: %s\n\n" +
                    "请作为专业投资顾问，基于上述文档内容提供详细、专业的投资建议和分析。\n" +
                    "回答要求：\n" +
                    "1. 使用专业的投资术语和概念\n" +
                    "2. 提供具体的分析和建议\n" +
                    "3. 提醒投资风险，强调风险控制\n" +
                    "4. 如果文档中没有相关信息，请基于投资常识提供建议\n" +
                    "5. 回答要专业、客观、负责任",
                    context.toString(),
                    query
            );
            
            // 记录完整的prompt
            log.info("=== 发送给AI模型的股神投资Prompt ===");
            log.info("Prompt内容: {}", prompt);
            log.info("=== Prompt结束 ===");
            
            // 生成回答
            String answer = chatModel.generate(prompt);
            
            log.info("股神投资主题回答生成完成");
            return answer;
        } catch (Exception e) {
            log.error("生成股神投资主题回答失败", e);
            return generateStockAnswerWithMockModel(query, retrievedDocuments);
        }
    }

    /**
     * 使用模拟模型生成股神投资主题回答
     * @param query 用户查询
     * @param retrievedDocuments 检索到的文档内容
     * @return 生成的回答
     */
    private String generateStockAnswerWithMockModel(String query, List<String> retrievedDocuments) {
        log.info("使用模拟模型生成股神投资主题回答");
        
        StringBuilder answer = new StringBuilder();
        answer.append("📈 **专业投资分析**\n\n");
        answer.append("基于您的问题：").append(query).append("\n\n");
        
        if (!retrievedDocuments.isEmpty()) {
            answer.append("**相关投资知识：**\n");
            for (int i = 0; i < Math.min(retrievedDocuments.size(), 3); i++) {
                answer.append("• ").append(retrievedDocuments.get(i).substring(0, Math.min(100, retrievedDocuments.get(i).length())));
                if (retrievedDocuments.get(i).length() > 100) {
                    answer.append("...");
                }
                answer.append("\n");
            }
            answer.append("\n");
        }
        
        answer.append("**投资建议：**\n");
        answer.append("• 建议进行充分的基本面分析\n");
        answer.append("• 关注技术指标和市场趋势\n");
        answer.append("• 合理控制仓位，分散投资风险\n");
        answer.append("• 定期关注市场动态和政策变化\n\n");
        
        answer.append("⚠️ **风险提示：**\n");
        answer.append("投资有风险，入市需谨慎。以上建议仅供参考，不构成投资建议。");
        
        return answer.toString();
    }

    /**
     * 无知识时的AI检索回答生成
     * @param query 用户查询
     * @param emitter 流式响应发射器
     * @return 生成的回答
     */
    public String generateAnswerWithoutKnowledge(String query, SseEmitter emitter) {
        log.info("无知识时的AI检索回答生成，查询: {}", query);
        
        try {
            if (chatModel == null) {
                StreamingUtils.sendError(emitter, "聊天模型未配置");
                return "聊天模型未配置";
            }
            
            // 构建无知识时的提示词
            String prompt = String.format(
                "用户问题: %s\n\n" +
                "请基于您的知识直接回答用户的问题。如果问题涉及专业领域，请提供一般性的建议和指导。\n" +
                "回答要求：\n" +
                "1. 提供准确、有用的信息\n" +
                "2. 如果涉及专业建议，请提醒用户咨询相关专业人士\n" +
                "3. 保持客观、负责任的回答态度\n" +
                "4. 如果无法确定答案，请诚实说明",
                query
            );
            
            // 发送生成进度
            StreamingUtils.sendGeneration(emitter, "🤖 正在使用AI进行检索分析...");
            
            // 记录完整的prompt
            log.info("=== 无知识时的AI检索Prompt ===");
            log.info("Prompt内容: {}", prompt);
            log.info("=== Prompt结束 ===");
            
            // 生成回答
            long startTime = System.currentTimeMillis();
            String answer = chatModel.generate(prompt);
            long endTime = System.currentTimeMillis();
            
            log.info("无知识AI检索回答生成完成，耗时: {} ms", endTime - startTime);
            
            // 发送生成完成信息
            StreamingUtils.sendGeneration(emitter, String.format("✅ AI检索完成，耗时 %d ms", endTime - startTime));
            
            return answer;
            
        } catch (Exception e) {
            log.error("无知识时的AI检索回答生成失败", e);
            StreamingUtils.sendError(emitter, "AI检索失败: " + e.getMessage());
            return "AI检索失败: " + e.getMessage();
        }
    }

    /**
     * 带引用信息的流式回答生成
     * @param query 查询文本
     * @param relevantSegments 相关文档片段
     * @param emitter 流式响应发射器
     * @return 生成的回答
     */
    public String generateStreamWithReferences(String query, List<TextSegment> relevantSegments, SseEmitter emitter) {
        try {
            log.info("生成带引用信息的回答，查询: {}", query);
            
            if (chatModel == null) {
                StreamingUtils.sendError(emitter, "聊天模型未配置");
                return "聊天模型未配置";
            }
            
            if (relevantSegments == null || relevantSegments.isEmpty()) {
                StreamingUtils.sendError(emitter, "没有相关文档片段");
                return "没有相关文档片段";
            }
            
            // 构建上下文和引用信息
            StringBuilder context = new StringBuilder();
            context.append("基于以下文档内容回答问题：\n\n");
            
            // 收集引用文件信息
            java.util.List<java.util.Map<String, String>> referencedDocuments = new java.util.ArrayList<>();
            
            for (int i = 0; i < relevantSegments.size(); i++) {
                TextSegment segment = relevantSegments.get(i);
                context.append("文档片段 ").append(i + 1).append(":\n");
                context.append(segment.text()).append("\n\n");
                
                // 提取详细文档信息
                java.util.Map<String, String> docInfo = extractDocumentInfo(segment);
                if (docInfo != null && !docInfo.isEmpty()) {
                    referencedDocuments.add(docInfo);
                }
            }
            
            context.append("问题: ").append(query).append("\n\n");
            context.append("请基于上述文档内容，提供准确、详细的回答。如果文档中没有相关信息，请明确说明。");
            
            // 发送生成进度
            StreamingUtils.sendGeneration(emitter, "📝 正在构建上下文...");
            StreamingUtils.sendGeneration(emitter, String.format("📚 已加载 %d 个相关文档片段", relevantSegments.size()));
            StreamingUtils.sendGeneration(emitter, "🤖 正在调用AI模型生成回答...");
            
            // 记录完整的prompt
            log.info("=== 发送给AI模型的完整Prompt ===");
            log.info("Prompt内容: {}", context.toString());
            log.info("=== Prompt结束 ===");
            
            // 生成回答
            long startTime = System.currentTimeMillis();
            String answer = chatModel.generate(context.toString());
            long endTime = System.currentTimeMillis();
            
            log.info("带引用信息的回答生成完成，耗时: {} ms", endTime - startTime);
            
            // 发送引用信息
            if (!referencedDocuments.isEmpty()) {
                StringBuilder references = new StringBuilder();
                references.append("**📚 引用来源：**\n");
                for (java.util.Map<String, String> docInfo : referencedDocuments) {
                    String fileName = docInfo.get("fileName");
                    String fileId = docInfo.get("fileId");
                    String chunkIndex = docInfo.get("chunkIndex");
                    references.append("• ").append(fileName);
                    if (chunkIndex != null) {
                        references.append(" (片段 ").append(chunkIndex).append(")");
                    }
                    references.append("\n");
                }
                StreamingUtils.sendReferences(emitter, references.toString());
                
                // 发送详细文档信息供前端使用
                log.info("发送文档详情信息，文档数量: {}", referencedDocuments.size());
                for (java.util.Map<String, String> doc : referencedDocuments) {
                    log.info("文档详情: fileName={}, fileId={}, chunkIndex={}", 
                        doc.get("fileName"), doc.get("fileId"), doc.get("chunkIndex"));
                }
                StreamingUtils.sendDocumentDetails(emitter, referencedDocuments);
            }
            
            // 发送生成完成信息
            StreamingUtils.sendGeneration(emitter, String.format("✅ 回答生成完成，耗时 %d ms", endTime - startTime));
            
            return answer;
            
        } catch (Exception e) {
            log.error("带引用信息的流式回答生成失败", e);
            StreamingUtils.sendError(emitter, "生成回答失败: " + e.getMessage());
            return "生成回答失败: " + e.getMessage();
        }
    }

    /**
     * 无知识时的股神投资AI检索回答生成
     * @param query 用户查询
     * @param emitter 流式响应发射器
     * @return 生成的回答
     */
    public String generateStockAnswerWithoutKnowledge(String query, SseEmitter emitter) {
        log.info("无知识时的股神投资AI检索回答生成，查询: {}", query);
        
        try {
            if (chatModel == null) {
                StreamingUtils.sendError(emitter, "聊天模型未配置");
                return "聊天模型未配置";
            }
            
            // 构建投资主题的无知识提示词
            String prompt = String.format(
                "你是一位专业的投资顾问，具有丰富的股票、基金、债券等投资经验。\n\n" +
                "用户投资问题: %s\n\n" +
                "请作为专业投资顾问，基于你的专业知识提供详细、专业的投资建议和分析。\n" +
                "回答要求：\n" +
                "1. 使用专业的投资术语和概念\n" +
                "2. 提供具体的分析和建议\n" +
                "3. 提醒投资风险，强调风险控制\n" +
                "4. 基于投资常识提供建议\n" +
                "5. 回答要专业、客观、负责任\n" +
                "6. 如果涉及具体股票或投资产品，请提醒用户进行进一步研究",
                query
            );
            
            // 发送生成进度
            StreamingUtils.sendGeneration(emitter, "🤖 正在使用AI进行专业投资分析...");
            
            // 记录完整的prompt
            log.info("=== 无知识时的股神投资AI检索Prompt ===");
            log.info("Prompt内容: {}", prompt);
            log.info("=== Prompt结束 ===");
            
            // 生成回答
            long startTime = System.currentTimeMillis();
            String answer = chatModel.generate(prompt);
            long endTime = System.currentTimeMillis();
            
            log.info("无知识股神投资AI检索回答生成完成，耗时: {} ms", endTime - startTime);
            
            // 发送生成完成信息
            StreamingUtils.sendGeneration(emitter, String.format("✅ AI投资分析完成，耗时 %d ms", endTime - startTime));
            
            return answer;
            
        } catch (Exception e) {
            log.error("无知识时的股神投资AI检索回答生成失败", e);
            StreamingUtils.sendError(emitter, "AI投资分析失败: " + e.getMessage());
            return "AI投资分析失败: " + e.getMessage();
        }
    }

    /**
     * 带引用信息的股神投资主题回答生成
     * @param query 用户查询
     * @param relevantSegments 相关文档片段
     * @param emitter 流式响应发射器
     * @return 生成的回答
     */
    public String generateStockAnswerWithReferences(String query, List<TextSegment> relevantSegments, SseEmitter emitter) {
        try {
            log.info("生成带引用信息的股神投资回答，查询: {}", query);
            
            if (chatModel == null) {
                StreamingUtils.sendError(emitter, "聊天模型未配置");
                return "聊天模型未配置";
            }
            
            if (relevantSegments == null || relevantSegments.isEmpty()) {
                StreamingUtils.sendError(emitter, "没有相关文档片段");
                return "没有相关文档片段";
            }
            
            // 构建投资主题上下文和引用信息
            StringBuilder context = new StringBuilder();
            context.append("你是一位专业的投资顾问，具有丰富的股票、基金、债券等投资经验。");
            context.append("请基于以下投资相关文档内容，以专业投资顾问的身份回答用户的问题：\n\n");
            
            // 收集引用文件信息
            java.util.List<java.util.Map<String, String>> referencedDocuments = new java.util.ArrayList<>();
            
            for (int i = 0; i < relevantSegments.size(); i++) {
                TextSegment segment = relevantSegments.get(i);
                context.append("投资文档片段 ").append(i + 1).append(":\n");
                context.append(segment.text()).append("\n\n");
                
                // 提取详细文档信息
                java.util.Map<String, String> docInfo = extractDocumentInfo(segment);
                if (docInfo != null && !docInfo.isEmpty()) {
                    referencedDocuments.add(docInfo);
                }
            }
            
            context.append("用户投资问题: ").append(query).append("\n\n");
            context.append("请作为专业投资顾问，基于上述文档内容提供详细、专业的投资建议和分析。\n");
            context.append("回答要求：\n");
            context.append("1. 使用专业的投资术语和概念\n");
            context.append("2. 提供具体的分析和建议\n");
            context.append("3. 提醒投资风险，强调风险控制\n");
            context.append("4. 如果文档中没有相关信息，请基于投资常识提供建议\n");
            context.append("5. 回答要专业、客观、负责任");
            
            // 发送生成进度
            StreamingUtils.sendGeneration(emitter, "📝 正在构建投资分析上下文...");
            StreamingUtils.sendGeneration(emitter, String.format("📚 已加载 %d 个相关投资文档片段", relevantSegments.size()));
            StreamingUtils.sendGeneration(emitter, "🤖 正在调用AI模型生成专业投资建议...");
            
            // 记录完整的prompt
            log.info("=== 发送给AI模型的股神投资Prompt ===");
            log.info("Prompt内容: {}", context.toString());
            log.info("=== Prompt结束 ===");
            
            // 生成回答
            long startTime = System.currentTimeMillis();
            String answer = chatModel.generate(context.toString());
            long endTime = System.currentTimeMillis();
            
            log.info("带引用信息的股神投资回答生成完成，耗时: {} ms", endTime - startTime);
            
            // 发送引用信息
            if (!referencedDocuments.isEmpty()) {
                StringBuilder references = new StringBuilder();
                references.append("**📚 引用来源：**\n");
                for (java.util.Map<String, String> docInfo : referencedDocuments) {
                    String fileName = docInfo.get("fileName");
                    String fileId = docInfo.get("fileId");
                    String chunkIndex = docInfo.get("chunkIndex");
                    references.append("• ").append(fileName);
                    if (chunkIndex != null) {
                        references.append(" (片段 ").append(chunkIndex).append(")");
                    }
                    references.append("\n");
                }
                StreamingUtils.sendReferences(emitter, references.toString());
                
                // 发送详细文档信息供前端使用
                StreamingUtils.sendDocumentDetails(emitter, referencedDocuments);
            }
            
            // 发送生成完成信息
            StreamingUtils.sendGeneration(emitter, String.format("✅ 专业投资建议生成完成，耗时 %d ms", endTime - startTime));
            
            return answer;
            
        } catch (Exception e) {
            log.error("带引用信息的股神投资回答生成失败", e);
            StreamingUtils.sendError(emitter, "生成投资建议失败: " + e.getMessage());
            return "生成投资建议失败: " + e.getMessage();
        }
    }

    /**
     * 从TextSegment中提取详细文档信息
     * @param segment 文本片段
     * @return 文档信息Map
     */
    private java.util.Map<String, String> extractDocumentInfo(TextSegment segment) {
        java.util.Map<String, String> docInfo = new java.util.HashMap<>();
        try {
            log.info("开始提取文档信息，元数据: {}", segment.metadata());
            if (segment.metadata() != null) {
                String fileName = null;
                String fileId = null;
                String chunkIndex = null;
                
                // 直接从metadata中获取source字段
                String source = segment.metadata().get("source");
                log.info("source字段内容: {}", source);
                
                if (source != null && !source.isEmpty()) {
                    // source是转义的JSON字符串，需要先去掉外层引号
                    String jsonString = source;
                    if (source.startsWith("\"") && source.endsWith("\"")) {
                        // 去掉外层引号并处理转义字符
                        jsonString = source.substring(1, source.length() - 1);
                        // 处理转义的双引号
                        jsonString = jsonString.replace("\\\"", "\"");
                        log.info("处理后的JSON字符串: {}", jsonString);
                    }
                    
                    // 解析JSON
                    if (jsonString.startsWith("{")) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.JsonNode sourceNode = mapper.readTree(jsonString);
                            log.info("解析source JSON成功: {}", sourceNode);
                            
                            // 从source JSON中提取字段
                            if (sourceNode.has("file_name")) {
                                fileName = sourceNode.get("file_name").asText();
                                log.info("从source解析到fileName: {}", fileName);
                            }
                            if (sourceNode.has("file_id")) {
                                fileId = sourceNode.get("file_id").asText();
                                log.info("从source解析到fileId: {}", fileId);
                            }
                            if (sourceNode.has("chunk_index")) {
                                chunkIndex = String.valueOf(sourceNode.get("chunk_index").asInt());
                                log.info("从source解析到chunkIndex: {}", chunkIndex);
                            }
                        } catch (Exception e) {
                            log.error("解析source JSON失败: {}", e.getMessage());
                        }
                    }
                }
                
                // 如果上述方法没有获取到，尝试直接从segment.metadata()获取（兼容其他存储格式）
                if (fileName == null || fileName.isEmpty()) {
                    fileName = segment.metadata().get("file_name");
                    fileId = segment.metadata().get("file_id");
                    chunkIndex = segment.metadata().get("chunk_index");
                    log.info("直接获取的字段 - fileName: {}, fileId: {}, chunkIndex: {}", fileName, fileId, chunkIndex);
                }
                
                if (fileName != null && !fileName.isEmpty()) {
                    docInfo.put("fileName", fileName);
                }
                if (fileId != null && !fileId.isEmpty()) {
                    docInfo.put("fileId", fileId);
                }
                if (chunkIndex != null && !chunkIndex.isEmpty()) {
                    docInfo.put("chunkIndex", chunkIndex);
                }
                
                // 添加文本内容预览
                String textPreview = segment.text();
                if (textPreview != null && textPreview.length() > 100) {
                    textPreview = textPreview.substring(0, 100) + "...";
                }
                docInfo.put("textPreview", textPreview);
            }
        } catch (Exception e) {
            log.warn("提取文档信息失败", e);
        }
        log.info("提取的文档信息: {}", docInfo);
        return docInfo;
    }

    /**
     * 从TextSegment中提取文件名
     * @param segment 文本片段
     * @return 文件名
     */
    private String extractFileName(TextSegment segment) {
        try {
            // 尝试从元数据中获取文件名
            if (segment.metadata() != null) {
                // 首先尝试直接获取file_name字段
                String fileName = segment.metadata().get("file_name");
                if (fileName != null && !fileName.isEmpty()) {
                    return fileName;
                }
                
                // 尝试从metadata字段获取（Milvus存储的格式）
                String metadata = segment.metadata().get("metadata");
                if (metadata != null && !metadata.isEmpty()) {
                    // 如果metadata是JSON格式，尝试解析
                    if (metadata.startsWith("{")) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(metadata);
                            if (jsonNode.has("file_name")) {
                                return jsonNode.get("file_name").asText();
                            }
                        } catch (Exception e) {
                            log.debug("解析metadata JSON失败: {}", e.getMessage());
                        }
                    }
                }
                
                // 尝试从source字段获取
                String source = segment.metadata().get("source");
                if (source != null && !source.isEmpty()) {
                    // 如果source是JSON格式，尝试解析
                    if (source.startsWith("{")) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(source);
                            if (jsonNode.has("file_name")) {
                                return jsonNode.get("file_name").asText();
                            }
                        } catch (Exception e) {
                            log.debug("解析source JSON失败: {}", e.getMessage());
                        }
                    }
                    return source;
                }
            }
            
            // 如果没有找到文件名，返回默认值
            return "未知文档";
        } catch (Exception e) {
            log.warn("提取文件名失败", e);
            return "未知文档";
        }
    }
}