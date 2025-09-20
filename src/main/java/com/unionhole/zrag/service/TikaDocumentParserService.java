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

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Apache Tika文档解析服务
 * 使用Tika统一处理各种文档格式的文本提取和编码问题
 */
@Slf4j
@Service
public class TikaDocumentParserService {

    private final Tika tika;
    private final AutoDetectParser parser;

    public TikaDocumentParserService() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
    }

    /**
     * 解析文档并提取文本内容
     * 
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    public String parseDocument(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        log.info("开始使用Tika解析文档: {}, 大小: {} bytes", fileName, file.getSize());
        
        try {
            // 方法1: 使用Tika的简单API
            String text = parseWithTikaSimple(file);
            if (text != null && !text.trim().isEmpty()) {
                log.info("Tika简单解析成功，文本长度: {} 字符", text.length());
                // 输出完整解析原文
                log.info("=== Tika解析原文 ===");
                log.info("{}", text);
                log.info("=== 解析原文结束 ===");
                return cleanText(text);
            }
            
            // 方法2: 使用Tika的完整解析器
            text = parseWithTikaParser(file);
            if (text != null && !text.trim().isEmpty()) {
                log.info("Tika完整解析成功，文本长度: {} 字符", text.length());
                // 输出完整解析原文
                log.info("=== Tika解析原文 ===");
                log.info("{}", text);
                log.info("=== 解析原文结束 ===");
                return cleanText(text);
            }
            
            log.warn("Tika解析失败，返回空文本");
            log.info("=== Tika解析失败，无内容输出 ===");
            return "[文档解析失败: 无法提取文本内容]";
            
        } catch (Exception e) {
            log.error("Tika解析文档异常: {}", e.getMessage(), e);
            log.info("=== Tika解析异常，无内容输出 ===");
            return "[文档解析异常: " + e.getMessage() + "]";
        }
    }
    
    /**
     * 使用Tika简单API解析文档
     */
    private String parseWithTikaSimple(MultipartFile file) {
        try {
            // 检测文档类型
            String detectedType = tika.detect(file.getBytes());
            log.debug("检测到文档类型: {}", detectedType);
            
            // 创建Tika实例并设置编码
            Tika tikaWithEncoding = new Tika();
            tikaWithEncoding.setMaxStringLength(-1); // 无限制长度
            
            // 解析文档内容
            String text = tikaWithEncoding.parseToString(file.getInputStream());
            
            // 检查是否有乱码
            if (containsGarbledText(text)) {
                log.warn("检测到可能的乱码，尝试重新解析");
                // 重新解析，使用不同的编码策略
                text = parseWithDifferentEncoding(file);
            }
            
            return text;
            
        } catch (Exception e) {
            log.debug("Tika简单解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 使用Tika完整解析器解析文档
     */
    private String parseWithTikaParser(MultipartFile file) {
        try {
            // 创建解析上下文
            ParseContext parseContext = new ParseContext();
            parseContext.set(Parser.class, parser);
            
            // 创建元数据对象
            Metadata metadata = new Metadata();
            metadata.set("resourceName", file.getOriginalFilename());
            
            // 创建内容处理器
            BodyContentHandler handler = new BodyContentHandler(-1); // -1表示无限制
            
            // 解析文档
            try (InputStream inputStream = file.getInputStream()) {
                parser.parse(inputStream, handler, metadata, parseContext);
            }
            
            // 获取解析结果
            String text = handler.toString();
            
            // 记录元数据信息
            log.debug("解析元数据: {}", metadata);
            log.debug("解析的文本长度: {} 字符", text.length());
            
            return text;
            
        } catch (Exception e) {
            log.debug("Tika完整解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 清理提取的文本
     */
    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // 移除多余的空白字符
            text = text.replaceAll("\\s+", " ").trim();
            
            // 移除控制字符（保留换行符和制表符）
            text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
            
            // 移除XML标签（如果有）
            text = text.replaceAll("<[^>]+>", " ");
            
            // 移除多余的空白字符
            text = text.replaceAll("\\s+", " ").trim();
            
            log.debug("文本清理完成，清理后长度: {} 字符", text.length());
            return text;
            
        } catch (Exception e) {
            log.warn("文本清理失败: {}", e.getMessage());
            return text;
        }
    }
    
    /**
     * 检测文档类型
     */
    public String detectDocumentType(MultipartFile file) {
        try {
            String detectedType = tika.detect(file.getBytes());
            log.info("检测到文档类型: {}", detectedType);
            return detectedType;
        } catch (Exception e) {
            log.warn("文档类型检测失败: {}", e.getMessage());
            return "application/octet-stream";
        }
    }
    
    /**
     * 检查是否支持该文档类型
     */
    public boolean isSupportedDocumentType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        
        // 支持的主要文档类型
        String[] supportedTypes = {
            "text/plain",
            "text/html",
            "text/xml",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/rtf",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/epub+zip",
            "application/x-tex",
            "text/markdown"
        };
        
        for (String supportedType : supportedTypes) {
            if (mimeType.toLowerCase().contains(supportedType.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取支持的文档类型列表
     */
    public String[] getSupportedDocumentTypes() {
        return new String[]{
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
        };
    }
    
    /**
     * 检测文本是否包含乱码
     */
    private boolean containsGarbledText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 检查是否包含大量非ASCII字符（可能是乱码）
        int nonAsciiCount = 0;
        int totalChars = text.length();
        
        for (char c : text.toCharArray()) {
            if (c > 127) {
                nonAsciiCount++;
            }
        }
        
        // 如果非ASCII字符超过50%，可能是乱码
        double nonAsciiRatio = (double) nonAsciiCount / totalChars;
        boolean isGarbled = nonAsciiRatio > 0.5;
        
        if (isGarbled) {
            log.warn("检测到可能的乱码: 非ASCII字符比例: {:.2f}%", nonAsciiRatio * 100);
        }
        
        return isGarbled;
    }
    
    /**
     * 使用不同编码策略重新解析文档
     */
    private String parseWithDifferentEncoding(MultipartFile file) {
        try {
            // 使用完整解析器重新解析
            ParseContext parseContext = new ParseContext();
            parseContext.set(Parser.class, parser);
            
            Metadata metadata = new Metadata();
            metadata.set("resourceName", file.getOriginalFilename());
            // 设置编码提示
            metadata.set("Content-Encoding", "UTF-8");
            
            BodyContentHandler handler = new BodyContentHandler(-1);
            
            try (InputStream inputStream = file.getInputStream()) {
                parser.parse(inputStream, handler, metadata, parseContext);
            }
            
            String text = handler.toString();
            log.info("使用不同编码策略重新解析完成，文本长度: {} 字符", text.length());
            return text;
            
        } catch (Exception e) {
            log.warn("使用不同编码策略重新解析失败: {}", e.getMessage());
            return null;
        }
    }
}
