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

import com.unionhole.zrag.service.RagService;
import com.unionhole.zrag.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web控制器
 * 提供前端页面和API接口
 */
@Slf4j
@Controller
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class WebController {

    private final RagService ragService;
    private final MinioStorageService minioStorageService;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
    
    /**
     * 直接访问index.html
     */
    @GetMapping("/index.html")
    public String indexHtml() {
        return "forward:/index.html";
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        status.put("version", "1.0-SNAPSHOT");
        status.put("javaVersion", System.getProperty("java.version"));
        status.put("springBootVersion", "2.7.18");
        status.put("langchain4jVersion", "0.29.1");
        
        return ResponseEntity.ok(status);
    }

    /**
     * 获取存储状态
     */
    @GetMapping("/storage/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStorageStatus() {
        try {
            String stats = minioStorageService.getStorageStats();
            Map<String, Object> status = new HashMap<>();
            status.put("status", "ok");
            status.put("stats", stats);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取存储状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/storage/files")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFiles() {
        try {
            List<String> files = minioStorageService.listFiles();
            List<Map<String, Object>> fileList = files.stream()
                    .map(fileName -> {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", fileName);
                        fileInfo.put("type", getFileType(fileName));
                        fileInfo.put("size", 0); // 实际应该获取文件大小
                        fileInfo.put("uploadTime", System.currentTimeMillis());
                        return fileInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(fileList);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/storage/file-info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFileInfo(@RequestParam String fileName) {
        try {
            String info = minioStorageService.getFileInfo(fileName);
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("info", info);
            return ResponseEntity.ok(fileInfo);
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/storage/file")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam String fileName) {
        try {
            minioStorageService.deleteFile(fileName);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文件删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "文件为空");
                return ResponseEntity.badRequest().body(error);
            }

            // 上传文件到MinIO
            String fileName = minioStorageService.uploadFile(file);
            
            // 处理文档（向量化等）
            // 这里需要先将InputStream转换为Document，暂时跳过
            // ragService.processDocument(document);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("message", "文件上传并处理成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 上传文本
     */
    @PostMapping("/upload-text")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadText(@RequestBody String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "内容为空");
                return ResponseEntity.badRequest().body(error);
            }

            // 上传文本到MinIO
            String fileName = minioStorageService.uploadText(content, "text_" + System.currentTimeMillis() + ".txt");
            
            // 处理文档
            ragService.processText(content);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("message", "文本上传并处理成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("上传文本失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 执行查询
     */
    @PostMapping("/query")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            if (query == null || query.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "查询内容为空");
                return ResponseEntity.badRequest().body(error);
            }

            // 执行RAG查询
            String answer = ragService.query(query);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("answer", answer);
            result.put("query", query);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("执行查询失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 检索文档
     */
    @GetMapping("/api/rag/retrieve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> retrieve(@RequestParam String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "查询内容为空");
                return ResponseEntity.badRequest().body(error);
            }

            // 执行检索
            List<String> results = ragService.retrieveDocuments(query);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("results", results);
            result.put("query", query);
            result.put("count", results.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("检索文档失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 根据文件名获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "PDF";
            case "txt":
                return "TXT";
            case "doc":
            case "docx":
                return "DOC";
            case "md":
                return "Markdown";
            default:
                return extension.toUpperCase();
        }
    }
}
