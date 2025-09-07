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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<String> getSystemStatus() {
        String status = ragService.getSystemStatus();
        return ResponseEntity.ok(status);
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
    public ResponseEntity<List<String>> listFiles() {
        try {
            List<String> files = minioStorageService.listFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("列出文件失败", e);
            return ResponseEntity.badRequest().body(java.util.Arrays.asList("列出文件失败: " + e.getMessage()));
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
            List<String> files = minioStorageService.listFiles();
            int processedCount = 0;
            
            for (String fileName : files) {
                try {
                    // 获取文件内容并处理
                    String content = minioStorageService.getFileContent(fileName);
                    if (content != null && !content.trim().isEmpty()) {
                        ragService.processText(content);
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.warn("处理文件失败: {}", fileName, e);
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
            stats.put("totalDocuments", ragService.getDocumentCount());
            stats.put("totalFiles", minioStorageService.listFiles().size());
            stats.put("vectorCount", ragService.getDocumentCount()); // 假设每个文档块对应一个向量
            stats.put("totalQueries", 0); // 这里可以添加查询计数功能
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
     * 刷新知识库
     * @return 刷新结果
     */
    @PostMapping("/knowledge/refresh")
    public ResponseEntity<Map<String, Object>> refreshKnowledge() {
        try {
            log.info("刷新知识库");
            
            // 重新处理所有存储的文档
            List<String> files = minioStorageService.listFiles();
            int processedCount = 0;
            
            for (String fileName : files) {
                try {
                    // 获取文件内容并处理
                    String content = minioStorageService.getFileContent(fileName);
                    if (content != null && !content.trim().isEmpty()) {
                        ragService.processText(content);
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.warn("处理文件失败: {}", fileName, e);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "知识库刷新成功");
            result.put("processedFiles", processedCount);
            result.put("totalFiles", files.size());
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
}