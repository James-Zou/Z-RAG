package com.unionhole.zrag.util;

import com.unionhole.zrag.dto.StreamingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * 流式响应工具类
 */
public class StreamingUtils {
    
    private static final Logger log = LoggerFactory.getLogger(StreamingUtils.class);
    
    /**
     * 发送流式响应
     */
    public static void sendStreamingResponse(SseEmitter emitter, StreamingResponse response) {
        try {
            emitter.send(response);
            log.debug("发送流式响应: type={}, content={}", response.getType(), response.getContent());
        } catch (IOException e) {
            log.error("发送流式响应失败", e);
            emitter.completeWithError(e);
        }
    }
    
    /**
     * 发送思考步骤
     */
    public static void sendThinking(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.thinking(content));
    }
    
    /**
     * 发送检索步骤
     */
    public static void sendRetrieval(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.retrieval(content));
    }
    
    /**
     * 发送重排步骤
     */
    public static void sendRerank(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.rerank(content));
    }
    
    /**
     * 发送生成步骤
     */
    public static void sendGeneration(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.generation(content));
    }
    
    /**
     * 发送最终答案
     */
    public static void sendAnswer(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.answer(content));
    }
    
    /**
     * 发送错误
     */
    public static void sendError(SseEmitter emitter, String error) {
        sendStreamingResponse(emitter, StreamingResponse.error(error));
    }
    
    /**
     * 发送引用信息
     */
    public static void sendReferences(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.references(content));
    }
    
    /**
     * 发送详细文档信息
     */
    public static void sendDocumentDetails(SseEmitter emitter, java.util.List<java.util.Map<String, String>> documents) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String content = mapper.writeValueAsString(documents);
            sendStreamingResponse(emitter, StreamingResponse.documentDetails(content));
        } catch (Exception e) {
            log.error("发送文档详情失败", e);
        }
    }
    
    /**
     * 发送RAG过程日志
     */
    public static void sendRagLog(SseEmitter emitter, String content) {
        sendStreamingResponse(emitter, StreamingResponse.ragLog(content));
    }
    
    /**
     * 发送RAG过程步骤日志
     */
    public static void sendRagStep(SseEmitter emitter, String step, String content) {
        sendStreamingResponse(emitter, StreamingResponse.ragStep(step, content));
    }
    
    /**
     * 发送详细的RAG过程日志（带时间戳）
     */
    public static void sendDetailedRagLog(SseEmitter emitter, String step, String content) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String detailedContent = String.format("[%s] [%s] %s", timestamp, step, content);
        sendStreamingResponse(emitter, StreamingResponse.ragLog(detailedContent));
    }
    
    /**
     * 创建流式响应处理器
     */
    public static Consumer<String> createStreamingHandler(SseEmitter emitter, String type) {
        return content -> {
            StreamingResponse response = StreamingResponse.builder()
                    .type(type)
                    .content(content)
                    .finished(false)
                    .timestamp(System.currentTimeMillis())
                    .build();
            sendStreamingResponse(emitter, response);
        };
    }
}
