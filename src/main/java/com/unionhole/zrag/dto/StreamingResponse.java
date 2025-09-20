package com.unionhole.zrag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {
    
    /**
     * 响应类型
     */
    private String type;
    
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 是否完成
     */
    private boolean finished;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    /**
     * 创建思考步骤响应
     */
    public static StreamingResponse thinking(String content) {
        return StreamingResponse.builder()
                .type("thinking")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建检索步骤响应
     */
    public static StreamingResponse retrieval(String content) {
        return StreamingResponse.builder()
                .type("retrieval")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建重排步骤响应
     */
    public static StreamingResponse rerank(String content) {
        return StreamingResponse.builder()
                .type("rerank")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建生成步骤响应
     */
    public static StreamingResponse generation(String content) {
        return StreamingResponse.builder()
                .type("generation")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建最终答案响应
     */
    public static StreamingResponse answer(String content) {
        return StreamingResponse.builder()
                .type("answer")
                .content(content)
                .finished(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static StreamingResponse error(String error) {
        return StreamingResponse.builder()
                .type("error")
                .content("")
                .error(error)
                .finished(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建引用信息响应
     */
    public static StreamingResponse references(String content) {
        return StreamingResponse.builder()
                .type("references")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建文档详情响应
     */
    public static StreamingResponse documentDetails(String content) {
        return StreamingResponse.builder()
                .type("documentDetails")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建RAG过程日志响应
     */
    public static StreamingResponse ragLog(String content) {
        return StreamingResponse.builder()
                .type("ragLog")
                .content(content)
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建RAG过程步骤日志响应
     */
    public static StreamingResponse ragStep(String step, String content) {
        return StreamingResponse.builder()
                .type("ragStep")
                .content(String.format("[%s] %s", step, content))
                .finished(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
