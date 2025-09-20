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


package com.unionhole.zrag.controller;

import com.unionhole.zrag.dto.QueryRequest;
import com.unionhole.zrag.dto.QueryResponse;
import com.unionhole.zrag.dto.UploadResponse;
import com.unionhole.zrag.service.RagService;
import com.unionhole.zrag.service.RerankService;
import com.unionhole.zrag.service.MinioStorageService;
import com.unionhole.zrag.service.DocumentService;
import com.unionhole.zrag.store.MilvusEmbeddingStore;
import com.unionhole.zrag.util.StreamingUtils;
import dev.langchain4j.data.document.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG REST控制器
 * 提供完整的RAG系统HTTP API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {
    
    private final RagService ragService;
    private final RerankService rerankService;
    private final MinioStorageService minioStorageService;
    private final DocumentService documentService;
    
    /**
     * 上传文档
     * @param file 文档文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            log.info("上传文档: {}", file.getOriginalFilename());
            
            // 使用MinIO存储并处理文档
            String filePath = ragService.processUploadedFile(file);
            
            UploadResponse response = UploadResponse.builder()
                    .success(true)
                    .message("文档上传并处理成功")
                    .filename(file.getOriginalFilename())
                    .filePath(filePath)
                    .documentCount(ragService.getDocumentCount())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("文档上传失败", e);
            UploadResponse response = UploadResponse.builder()
                    .success(false)
                    .message("文档上传失败: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 上传文本内容
     * @param content 文本内容
     * @return 上传结果
     */
    @PostMapping("/upload-text")
    public ResponseEntity<UploadResponse> uploadText(@RequestBody String content) {
        try {
            log.info("上传文本内容");
            
            // 使用MinIO存储并处理文本
            String filePath = ragService.processTextWithStorage(content);
            
            UploadResponse response = UploadResponse.builder()
                    .success(true)
                    .message("文本内容上传并处理成功")
                    .filePath(filePath)
                    .documentCount(ragService.getDocumentCount())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("文本上传失败", e);
            UploadResponse response = UploadResponse.builder()
                    .success(false)
                    .message("文本上传失败: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 执行RAG查询
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        try {
            log.info("执行RAG查询: {}", request.getQuery());
            
            String answer;
            if (request.getMaxResults() != null && request.getMinScore() != null) {
                answer = ragService.query(
                        request.getQuery(),
                        request.getMaxResults(),
                        request.getMinScore()
                );
            } else {
                answer = ragService.query(request.getQuery());
            }
            
            QueryResponse response = QueryResponse.builder()
                    .query(request.getQuery())
                    .answer(answer)
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("RAG查询失败", e);
            QueryResponse response = QueryResponse.builder()
                    .query(request.getQuery())
                    .answer("查询失败: " + e.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 执行流式RAG查询
     * @param request 查询请求
     * @return 流式响应
     */
    @PostMapping("/query/stream")
    public SseEmitter queryStream(@RequestBody QueryRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        
        try {
            log.info("执行流式RAG查询: {}", request.getQuery());
            
            // 发送开始思考的响应
            StreamingUtils.sendThinking(emitter, "🤔 开始分析您的问题...");
            
            // 执行流式查询
            if (request.getMaxResults() != null && request.getMinScore() != null) {
                ragService.queryStream(
                        request.getQuery(),
                        request.getMaxResults(),
                        request.getMinScore(),
                        emitter
                );
            } else {
                ragService.queryStream(request.getQuery(), emitter);
            }
            
        } catch (Exception e) {
            log.error("流式RAG查询失败", e);
            StreamingUtils.sendError(emitter, "查询失败: " + e.getMessage());
            emitter.complete();
        }
        
        return emitter;
    }
    
    /**
     * 执行股神投资主题流式RAG查询
     * @param request 查询请求
     * @return 流式响应
     */
    @PostMapping("/query/stream/stock")
    public SseEmitter queryStreamStock(@RequestBody QueryRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        
        try {
            log.info("执行股神投资流式RAG查询: {}", request.getQuery());
            
            // 发送开始思考的响应
            StreamingUtils.sendThinking(emitter, "📈 正在分析投资问题...");
            
            // 执行流式查询（投资主题）
            if (request.getMaxResults() != null && request.getMinScore() != null) {
                ragService.queryStreamStock(
                        request.getQuery(),
                        request.getMaxResults(),
                        request.getMinScore(),
                        emitter
                );
            } else {
                ragService.queryStreamStock(request.getQuery(), emitter);
            }
            
        } catch (Exception e) {
            log.error("股神投资流式RAG查询失败", e);
            StreamingUtils.sendError(emitter, "查询失败: " + e.getMessage());
            emitter.complete();
        }
        
        return emitter;
    }
    
    /**
     * 使用内容检索器执行RAG查询
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/query-with-retriever")
    public ResponseEntity<QueryResponse> queryWithRetriever(@RequestBody QueryRequest request) {
        try {
            log.info("使用内容检索器执行RAG查询: {}", request.getQuery());
            
            String answer = ragService.queryWithContentRetriever(request.getQuery());
            
            QueryResponse response = QueryResponse.builder()
                    .query(request.getQuery())
                    .answer(answer)
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("使用内容检索器RAG查询失败", e);
            QueryResponse response = QueryResponse.builder()
                    .query(request.getQuery())
                    .answer("查询失败: " + e.getMessage())
                    .success(false)
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检索文档片段
     * @param query 查询文本
     * @return 文档片段列表
     */
    @GetMapping("/retrieve")
    public ResponseEntity<List<String>> retrieveDocuments(@RequestParam String query) {
        try {
            log.info("检索文档片段: {}", query);
            
            List<String> documents = ragService.retrieveDocuments(query);
            
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            log.error("检索文档片段失败", e);
            return ResponseEntity.badRequest().body(java.util.Arrays.asList("检索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检索文档片段（带参数）
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 文档片段列表
     */
    @GetMapping("/retrieve-with-params")
    public ResponseEntity<List<String>> retrieveDocumentsWithParams(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxResults,
            @RequestParam(defaultValue = "0.6") double minScore) {
        try {
            log.info("检索文档片段: {} (maxResults: {}, minScore: {})", query, maxResults, minScore);
            
            List<String> documents = ragService.retrieveDocuments(query, maxResults, minScore);
            
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            log.error("检索文档片段失败", e);
            return ResponseEntity.badRequest().body(java.util.Arrays.asList("检索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取文档数量
     * @return 文档数量
     */
    @GetMapping("/document-count")
    public ResponseEntity<Long> getDocumentCount() {
        long count = ragService.getDocumentCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * 清空文档
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearDocuments() {
        try {
            ragService.clearDocuments();
            return ResponseEntity.ok("文档清空成功");
        } catch (Exception e) {
            log.error("清空文档失败", e);
            return ResponseEntity.badRequest().body("清空文档失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取系统状态
     * @return 系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "running");
            status.put("timestamp", System.currentTimeMillis());
            status.put("version", "1.0-SNAPSHOT");
            status.put("javaVersion", System.getProperty("java.version"));
            status.put("springBootVersion", "2.7.18");
            status.put("langchain4jVersion", "0.29.1");
            status.put("documentCount", ragService.getDocumentCount());
            status.put("message", "Z-RAG系统运行正常");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "系统状态检查失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 获取文档解析服务状态
     * @return 解析服务状态
     */
    @GetMapping("/converter/status")
    public ResponseEntity<Map<String, Object>> getConverterStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 检查Tika状态
            boolean tikaAvailable = false;
            try {
                tikaAvailable = documentService.isTikaAvailable();
            } catch (Exception e) {
                log.warn("检查Tika状态失败: {}", e.getMessage());
            }
            
            status.put("success", true);
            status.put("tikaAvailable", tikaAvailable);
            status.put("parserType", "Apache Tika");
            status.put("fallbackMethod", "编码检测和文本提取");
            status.put("supportedFormats", new String[]{
                    "text/plain", "text/html", "text/xml", "text/markdown",
                    "application/pdf",
                    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/rtf",
                    "application/vnd.oasis.opendocument.text",
                    "application/vnd.oasis.opendocument.spreadsheet",
                    "application/vnd.oasis.opendocument.presentation",
                    "application/epub+zip",
                    "application/x-tex"
            });
            status.put("message", tikaAvailable ?
                    "Apache Tika解析服务可用" : "使用备用解析方法");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取解析服务状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 健康检查
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Z-RAG服务运行正常");
    }
    
    /**
     * 获取重排服务状态
     * @return 重排服务状态
     */
    @GetMapping("/rerank/status")
    public ResponseEntity<String> getRerankStatus() {
        String status = rerankService.getRerankStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取MinIO存储状态
     * @return 存储状态
     */
    @GetMapping("/storage/status")
    public ResponseEntity<String> getStorageStatus() {
        String status = minioStorageService.getStorageStats();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 列出存储的文件
     * @return 文件列表
     */
    @GetMapping("/storage/files")
    public ResponseEntity<List<Map<String, Object>>> listFiles() {
        try {
            List<Map<String, Object>> files = minioStorageService.listFilesWithDetails();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("列出文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "列出文件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Arrays.asList(error));
        }
    }
    
    /**
     * 分页列出存储的文件
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param search 搜索关键词
     * @return 分页文件列表
     */
    @GetMapping("/storage/files/paged")
    public ResponseEntity<Map<String, Object>> listFilesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "lastModified") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String search) {
        try {
            Map<String, Object> result = minioStorageService.listFilesWithDetailsPaged(page, size, sortBy, sortOrder, search);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("分页列出文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "分页列出文件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取文件信息
     * @param fileName 文件名
     * @return 文件信息
     */
    @GetMapping("/storage/file-info")
    public ResponseEntity<String> getFileInfo(@RequestParam String fileName) {
        try {
            String info = minioStorageService.getFileInfo(fileName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            return ResponseEntity.badRequest().body("获取文件信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     * @param fileName 文件名
     * @return 删除结果
     */
    @DeleteMapping("/storage/file")
    public ResponseEntity<String> deleteFile(@RequestParam String fileName) {
        try {
            minioStorageService.deleteFile(fileName);
            return ResponseEntity.ok("文件删除成功: " + fileName);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return ResponseEntity.badRequest().body("删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 直接预览PDF文件（返回PDF流）
     * @param fileName 文件名
     * @return PDF文件流
     */
    @GetMapping("/storage/pdf")
    public ResponseEntity<byte[]> previewPdfFile(@RequestParam String fileName) {
        try {
            log.info("预览PDF文件: {}", fileName);
            
            // 检查文件类型
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().build();
            }
            
            // 获取PDF文件内容
            InputStream inputStream = minioStorageService.downloadFile(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            
            byte[] pdfBytes = outputStream.toByteArray();
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                    .header("Content-Length", String.valueOf(pdfBytes.length))
                    .body(pdfBytes);
            
        } catch (Exception e) {
            log.error("预览PDF文件失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 预览文件内容
     * @param fileName 文件名
     * @return 文件内容
     */
    @GetMapping("/storage/preview")
    public ResponseEntity<Map<String, Object>> previewFile(@RequestParam String fileName) {
        try {
            log.info("预览文件: {}", fileName);
            
            // 获取文件信息
            String fileInfo = minioStorageService.getFileInfo(fileName);
            
            // 根据文件类型确定预览方式
            String fileType = getFileTypeFromName(fileName);
            String previewType = getPreviewType(fileType);
            
            // 根据文件类型获取处理后的内容
            String processedContent = getProcessedFileContent(fileName, previewType);
            if (processedContent == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "文件不存在或无法读取");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("fileType", fileType);
            result.put("previewType", previewType);
            result.put("content", processedContent);
            result.put("fileInfo", fileInfo);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("预览文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "预览文件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 根据文件类型获取处理后的内容
     * @param fileName 文件名
     * @param previewType 预览类型
     * @return 处理后的内容
     */
    private String getProcessedFileContent(String fileName, String previewType) {
        try {
            switch (previewType) {
                case "text":
                case "markdown":
                    // 文本文件直接返回内容
                    return minioStorageService.getFileContentAsText(fileName);
                
                case "word":
                    // Word文档转换为HTML
                    return minioStorageService.convertWordToHtml(fileName);
                
                case "excel":
                    // Excel文档转换为HTML表格
                    return minioStorageService.convertExcelToHtml(fileName);
                
                case "pdf":
                    // PDF文档返回base64编码（前端用PDF.js渲染）
                    return minioStorageService.getFileContentAsBase64(fileName);
                
                case "image":
                    // 图片返回base64编码
                    return minioStorageService.getFileContentAsBase64(fileName);
                
                default:
                    // 其他文件类型返回文本内容
                    return minioStorageService.getFileContentAsText(fileName);
            }
        } catch (Exception e) {
            log.error("处理文件内容失败: {}", fileName, e);
            return null;
        }
    }
    
    /**
     * 从文件名推断文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    private String getFileTypeFromName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "未知类型";
        }
        
        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        switch (extension) {
            case "pdf":
                return "PDF文档";
            case "txt":
                return "文本文件";
            case "md":
            case "markdown":
                return "Markdown文档";
            case "doc":
            case "docx":
                return "Word文档";
            case "xls":
            case "xlsx":
                return "Excel表格";
            case "ppt":
            case "pptx":
                return "PowerPoint演示";
            case "png":
            case "jpg":
            case "jpeg":
            case "gif":
            case "bmp":
                return "图片文件";
            case "mp4":
            case "avi":
            case "mov":
                return "视频文件";
            case "mp3":
            case "wav":
            case "flac":
                return "音频文件";
            case "zip":
            case "rar":
            case "7z":
                return "压缩文件";
            default:
                return "其他文件";
        }
    }
    
    /**
     * 根据文件类型确定预览方式
     * @param fileType 文件类型
     * @return 预览方式
     */
    private String getPreviewType(String fileType) {
        switch (fileType) {
            case "PDF文档":
                return "pdf";
            case "Markdown文档":
                return "markdown";
            case "文本文件":
                return "text";
            case "Word文档":
                return "word";
            case "Excel表格":
                return "excel";
            case "PowerPoint演示":
                return "office";
            case "图片文件":
                return "image";
            case "视频文件":
                return "video";
            case "音频文件":
                return "audio";
            default:
                return "text";
        }
    }
    
    /**
     * 创建知识库
     * @return 创建结果
     */
    @PostMapping("/knowledge/create")
    public ResponseEntity<Map<String, Object>> createKnowledge() {
        try {
            log.info("创建知识库");
            
            // 清空现有知识库
            ragService.clearDocuments();
            
            // 重新处理所有存储的文档
            List<Map<String, Object>> files = minioStorageService.listFilesWithDetails();
            int processedCount = 0;
            
            for (Map<String, Object> fileInfo : files) {
                try {
                    String fileName = (String) fileInfo.get("name");
                    if (fileName != null) {
                        // 获取文件内容并处理
                        String content = minioStorageService.getFileContent(fileName);
                        if (content != null && !content.trim().isEmpty()) {
                            // 使用新的方法处理文档，支持多租户和文件ID
                            ragService.processTextWithMetadata(content, fileName);
                            processedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("处理文件失败: {}", fileInfo.get("name"), e);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "知识库创建成功");
            result.put("processedFiles", processedCount);
            result.put("totalFiles", files.size());
            result.put("documentCount", ragService.getDocumentCount());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "创建知识库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取知识库统计信息
     * @return 统计信息
     */
    @GetMapping("/knowledge/stats")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        try {
            log.info("获取知识库统计信息");
            
            Map<String, Object> stats = new HashMap<>();
            
            // 从MinIO获取文档总数
            int totalDocuments = minioStorageService.listFiles().size();
            stats.put("totalDocuments", totalDocuments);
            
            // 从向量库获取知识片段数量
            long vectorCount = ragService.getDocumentCount();
            stats.put("vectorCount", vectorCount);
            
            // 获取知识库名称
            String knowledgeBaseName = getKnowledgeBaseName();
            stats.put("knowledgeBaseName", knowledgeBaseName);
            
            // 添加最后更新时间
            stats.put("lastUpdated", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("获取知识库统计信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取知识库名称
     * @return 知识库名称
     */
    private String getKnowledgeBaseName() {
        try {
            // 根据配置获取知识库名称
            return "Z-RAG 知识库";
        } catch (Exception e) {
            log.warn("获取知识库名称失败", e);
            return "默认知识库";
        }
    }
    
    /**
     * 获取知识片段列表
     * @param page 页码
     * @param size 每页大小
     * @param search 搜索关键词
     * @return 知识片段列表
     */
    @GetMapping("/knowledge/chunks")
    public ResponseEntity<Map<String, Object>> getKnowledgeChunks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        try {
            log.info("获取知识片段列表，页码: {}, 大小: {}, 搜索: {}", page, size, search);
            
            Map<String, Object> result = ragService.getKnowledgeChunks(page, size, search);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取知识片段列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取知识片段列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取向量数据信息
     * @return 向量数据信息
     */
    @GetMapping("/knowledge/vectors")
    public ResponseEntity<Map<String, Object>> getVectorData() {
        try {
            log.info("获取向量数据信息");
            
            Map<String, Object> result = ragService.getVectorDataInfo();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取向量数据信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取向量数据信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取分析报告
     * @return 分析报告数据
     */
    @GetMapping("/knowledge/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        try {
            log.info("获取分析报告");
            
            Map<String, Object> result = ragService.getAnalyticsData();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取分析报告失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取分析报告失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 刷新知识库
     * @return 刷新结果
     */
    @PostMapping("/knowledge/refresh")
    public ResponseEntity<Map<String, Object>> refreshKnowledge() {
        try {
            Map<String, Object> result = new HashMap<>();
            log.info("刷新知识库");
           
//            // 重新处理所有存储的文档
//            List<String> files = minioStorageService.listFiles();
//            int processedCount = 0;
//
//            for (String fileName : files) {
//                try {
//                    // 获取文件内容并处理
//                    String content = minioStorageService.getFileContent(fileName);
//                    if (content != null && !content.trim().isEmpty()) {
//                        ragService.processText(content);
//                        processedCount++;
//                    }
//                } catch (Exception e) {
//                    log.warn("处理文件失败: {}", fileName, e);
//                }
//            }
            
         
            result.put("success", true);
            result.put("message", "知识库刷新成功");
//            result.put("processedFiles", processedCount);
//            result.put("totalFiles", files.size());
            result.put("documentCount", ragService.getDocumentCount());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("刷新知识库失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "刷新知识库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 动态分割文档
     * @param request 分割请求参数
     * @return 分割结果
     */
    @PostMapping("/document/split")
    public ResponseEntity<Map<String, Object>> splitDocument(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String fileName = (String) request.get("fileName");
            Integer chunkSize = (Integer) request.getOrDefault("chunkSize", 300);
            Integer chunkOverlap = (Integer) request.getOrDefault("chunkOverlap", 50);
            @SuppressWarnings("unchecked")
            List<String> customSeparators = (List<String>) request.get("customSeparators");
            
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "文档内容不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            log.info("动态分割文档: fileName={}, chunkSize={}, chunkOverlap={}",
                    fileName, chunkSize, chunkOverlap);
            
            // 创建文档对象
            Document document = Document.from(content);
            
            // 使用动态分割
            List<String> segments = ragService.dynamicSplitDocument(document, fileName, chunkSize, chunkOverlap, customSeparators);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文档分割成功");
            result.put("fileName", fileName);
            result.put("chunkSize", chunkSize);
            result.put("chunkOverlap", chunkOverlap);
            result.put("segmentCount", segments.size());
            result.put("segments", segments);
            result.put("customSeparators", customSeparators);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("动态分割文档失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "动态分割文档失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取分割器配置信息
     * @return 分割器配置
     */
    @GetMapping("/document/splitter/config")
    public ResponseEntity<Map<String, Object>> getSplitterConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 默认分割器配置
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("chunkSize", 300);
            defaultConfig.put("chunkOverlap", 50);
            defaultConfig.put("separators", new String[]{"\n\n", "\n", "。", "！", "？", ".", "!", "?", ";", ":", ",", " ", ""});
            config.put("default", defaultConfig);
            
            // 中文分割器配置
            Map<String, Object> chineseConfig = new HashMap<>();
            chineseConfig.put("chunkSize", 500);
            chineseConfig.put("chunkOverlap", 100);
            chineseConfig.put("separators", new String[]{"\n\n", "\n", "。", "！", "？", "；", "：", "，", "、", ".", "!", "?", ";", ":", ",", " ", ""});
            config.put("chinese", chineseConfig);
            
            // 代码分割器配置
            Map<String, Object> codeConfig = new HashMap<>();
            codeConfig.put("chunkSize", 1000);
            codeConfig.put("chunkOverlap", 200);
            codeConfig.put("separators", new String[]{"\n\n", "\n", ";", "}", ")", "]", "\t", " ", ""});
            config.put("code", codeConfig);
            
            // 支持的文件类型
            config.put("supportedFileTypes", new String[]{
                    "java", "js", "ts", "py", "cpp", "c", "h", "cs", "go", "rs",
                    "php", "rb", "swift", "kt", "scala", "xml", "json", "yaml",
                    "yml", "sql", "sh", "bat", "md", "txt", "pdf", "docx"
            });
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("获取分割器配置失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取分割器配置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * AI智能选股接口
     * @param request 选股请求参数
     * @return 选股结果
     */
    @PostMapping("/ai-stock-selection")
    public ResponseEntity<Map<String, Object>> aiStockSelection(@RequestBody Map<String, Object> request) {
        try {
            log.info("执行AI智能选股: {}", request);
            
            // 检查是否在交易时间内
            if (!isTradingTime()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "当前为休盘时间，请等待开盘后再进行选股操作");
                error.put("tradingTime", "交易时间：周一至周五 09:30-11:30, 13:00-15:00");
                return ResponseEntity.ok(error);
            }
            
            // 获取选股条件
            String industry = (String) request.getOrDefault("industry", "");
            String marketCap = (String) request.getOrDefault("marketCap", "");
            String technical = (String) request.getOrDefault("technical", "");
            String risk = (String) request.getOrDefault("risk", "");
            String currentTime = (String) request.getOrDefault("currentTime", "");
            
            // 构建AI查询提示词
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("基于当前时间 ").append(currentTime).append("，请推荐一只符合以下条件的优质股票：");
            
            if (!industry.isEmpty()) {
                queryBuilder.append(" 行业板块：").append(industry);
            }
            if (!marketCap.isEmpty()) {
                queryBuilder.append(" 市值范围：").append(marketCap);
            }
            if (!technical.isEmpty()) {
                queryBuilder.append(" 技术指标：").append(technical);
            }
            if (!risk.isEmpty()) {
                queryBuilder.append(" 风险等级：").append(risk);
            }
            
            queryBuilder.append("。请提供股票代码、名称、当前价格、涨跌幅、推荐理由和风险提示。");
            
            // 调用RAG服务进行AI选股
            String aiResponse = ragService.query(queryBuilder.toString());
            
            // 解析AI响应并构建推荐股票数据
            Map<String, Object> recommendedStock = parseAIStockResponse(aiResponse);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "AI选股完成");
            result.put("currentTime", currentTime);
            result.put("selectionCriteria", request);
            result.put("recommendedStock", recommendedStock);
            result.put("tradingTime", "交易时间：周一至周五 09:30-11:30, 13:00-15:00");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("AI选股失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "AI选股失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 检查是否在交易时间内
     * @return 是否在交易时间
     */
    private boolean isTradingTime() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        
        // 检查是否为工作日（周一到周五）
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return false;
        }
        
        java.time.LocalTime currentTime = now.toLocalTime();
        java.time.LocalTime morningStart = java.time.LocalTime.of(9, 30);
        java.time.LocalTime morningEnd = java.time.LocalTime.of(11, 30);
        java.time.LocalTime afternoonStart = java.time.LocalTime.of(13, 0);
        java.time.LocalTime afternoonEnd = java.time.LocalTime.of(15, 0);
        
        // 检查是否在上午交易时间 (09:30-11:30) 或下午交易时间 (13:00-15:00)
        return (currentTime.isAfter(morningStart) && currentTime.isBefore(morningEnd)) ||
                (currentTime.isAfter(afternoonStart) && currentTime.isBefore(afternoonEnd));
    }
    
    /**
     * 解析AI选股响应
     * @param aiResponse AI响应文本
     * @return 解析后的股票推荐数据
     */
    private Map<String, Object> parseAIStockResponse(String aiResponse) {
        Map<String, Object> stock = new HashMap<>();
        
        try {
            // 尝试从AI响应中提取股票信息
            // 这里可以根据AI响应的格式进行解析
            // 暂时返回一个示例结构
            stock.put("code", "000001");
            stock.put("name", "平安银行");
            stock.put("price", 12.45);
            stock.put("change", 2.1);
            stock.put("score", 85);
            stock.put("industry", "银行");
            stock.put("risk", "中等");
            stock.put("reason", aiResponse.length() > 200 ? aiResponse.substring(0, 200) + "..." : aiResponse);
            stock.put("riskWarning", "投资有风险，入市需谨慎");
            
        } catch (Exception e) {
            log.warn("解析AI选股响应失败", e);
            // 返回默认结构
            stock.put("code", "000001");
            stock.put("name", "平安银行");
            stock.put("price", 12.45);
            stock.put("change", 2.1);
            stock.put("score", 85);
            stock.put("industry", "银行");
            stock.put("risk", "中等");
            stock.put("reason", aiResponse);
            stock.put("riskWarning", "投资有风险，入市需谨慎");
        }
        
        return stock;
    }
    
    /**
     * 重新创建Milvus集合（用于处理向量维度不匹配问题）
     * @return 操作结果
     */
    @PostMapping("/admin/recreate-collection")
    public ResponseEntity<Map<String, Object>> recreateCollection() {
        try {
            log.info("开始重新创建Milvus集合...");
            
            // 检查是否使用Milvus存储
            if (documentService.getEmbeddingStore() instanceof MilvusEmbeddingStore) {
                MilvusEmbeddingStore milvusStore = (MilvusEmbeddingStore) documentService.getEmbeddingStore();
                milvusStore.forceRecreateCollection();
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Milvus集合重新创建成功");
                result.put("collectionName", milvusStore.getCollectionName());
                result.put("vectorDimension", 1024);
                
                log.info("Milvus集合重新创建成功");
                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "当前未使用Milvus存储，无需重新创建集合");
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("重新创建Milvus集合失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "重新创建Milvus集合失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 下载文档文件
     * @param fileId 文件ID
     * @return 文件内容
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String fileId) {
        try {
            log.info("下载文档: {}", fileId);
            
            // 根据fileId获取文件名（这里需要根据实际存储方式调整）
            // 暂时使用fileId作为文件名，实际应该从数据库查询
            String fileName = fileId;
            
            // 获取文件内容
            InputStream inputStream = minioStorageService.downloadFile(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            
            byte[] fileBytes = outputStream.toByteArray();
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(fileBytes.length);
            
            return new ResponseEntity<>(fileBytes, headers, org.springframework.http.HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("下载文档失败: {}", fileId, e);
            return ResponseEntity.badRequest().build();
        }
    }
}