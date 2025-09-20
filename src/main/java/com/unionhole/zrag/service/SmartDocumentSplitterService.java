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

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能文档分割器服务
 * 根据文档类型和内容自动选择最合适的分割策略
 */
@Slf4j
@Service
public class  SmartDocumentSplitterService {
    
    private final DocumentSplitter defaultSplitter;
    private final DocumentSplitter chineseSplitter;
    private final DocumentSplitter codeSplitter;
    
    @Autowired
    public SmartDocumentSplitterService(
            @Qualifier("documentSplitter") DocumentSplitter defaultSplitter,
            @Qualifier("chineseDocumentSplitter") DocumentSplitter chineseSplitter,
            @Qualifier("codeDocumentSplitter") DocumentSplitter codeSplitter) {
        this.defaultSplitter = defaultSplitter;
        this.chineseSplitter = chineseSplitter;
        this.codeSplitter = codeSplitter;
    }
    
    /**
     * 智能分割文档
     * 根据文档类型和内容自动选择分割器
     * 
     * @param document 文档对象
     * @param fileName 文件名（用于判断文档类型）
     * @return 分割后的文本片段列表
     */
    public List<TextSegment> smartSplit(Document document, String fileName) {
        try {
            String content = document.text();
            DocumentSplitter selectedSplitter = selectSplitter(content, fileName);
            
            log.info("为文档 {} 选择分割器: {}", fileName, selectedSplitter.getClass().getSimpleName());
            
            List<TextSegment> segments = selectedSplitter.split(document);
            log.info("文档分割完成，共 {} 个片段", segments.size());
            
            return segments;
            
        } catch (Exception e) {
            log.error("智能分割文档失败", e);
            // 降级到默认分割器
            return defaultSplitter.split(document);
        }
    }
    
    /**
     * 动态分割文档
     * 支持自定义分割参数
     * 
     * @param document 文档对象
     * @param chunkSize 块大小
     * @param chunkOverlap 重叠大小
     * @param customSeparators 自定义分隔符列表（可选）
     * @return 分割后的文本片段列表
     */
    public List<TextSegment> dynamicSplit(Document document, int chunkSize, int chunkOverlap, String... customSeparators) {
        try {
            log.info("使用动态分割参数: chunkSize={}, chunkOverlap={}, customSeparators={}", 
                    chunkSize, chunkOverlap, java.util.Arrays.toString(customSeparators));
            
            DocumentSplitter customSplitter = createCustomSplitter(chunkSize, chunkOverlap, customSeparators);
            List<TextSegment> segments = customSplitter.split(document);
            
            log.info("动态分割完成，共 {} 个片段", segments.size());
            log.info("分割统计: {}", getSplitterStats(segments));
            
            return segments;
            
        } catch (Exception e) {
            log.error("动态分割文档失败", e);
            // 降级到默认分割器
            return defaultSplitter.split(document);
        }
    }
    
    /**
     * 创建自定义分割器
     * 
     * @param chunkSize 块大小
     * @param chunkOverlap 重叠大小
     * @param customSeparators 自定义分隔符
     * @return 自定义分割器
     */
    private DocumentSplitter createCustomSplitter(int chunkSize, int chunkOverlap, String... customSeparators) {
        if (customSeparators != null && customSeparators.length > 0) {
            // 使用自定义分隔符创建分割器
            return DocumentSplitters.recursive(chunkSize, chunkOverlap);
        } else {
            // 使用默认分隔符
            return DocumentSplitters.recursive(chunkSize, chunkOverlap);
        }
    }
    
    /**
     * 按文件类型分割文档
     * 
     * @param document 文档对象
     * @param fileName 文件名
     * @param chunkSize 块大小
     * @param chunkOverlap 重叠大小
     * @return 分割后的文本片段列表
     */
    public List<TextSegment> splitByFileType(Document document, String fileName, int chunkSize, int chunkOverlap) {
        try {
            log.info("按文件类型分割文档: fileName={}, chunkSize={}, chunkOverlap={}", 
                    fileName, chunkSize, chunkOverlap);
            
            DocumentSplitter selectedSplitter;
            
            if (isCodeFile(fileName)) {
                selectedSplitter = createCustomSplitter(chunkSize, chunkOverlap);
                log.info("使用代码文件分割器");
            } else if (isChineseContent(document.text())) {
                selectedSplitter = createCustomSplitter(chunkSize, chunkOverlap);
                log.info("使用中文文档分割器");
            } else {
                selectedSplitter = createCustomSplitter(chunkSize, chunkOverlap);
                log.info("使用默认分割器");
            }
            
            List<TextSegment> segments = selectedSplitter.split(document);
            log.info("按文件类型分割完成，共 {} 个片段", segments.size());
            
            return segments;
            
        } catch (Exception e) {
            log.error("按文件类型分割文档失败", e);
            return defaultSplitter.split(document);
        }
    }
    
    /**
     * 根据文档内容和文件名选择合适的分割器
     * 
     * @param content 文档内容
     * @param fileName 文件名
     * @return 选中的分割器
     */
    private DocumentSplitter selectSplitter(String content, String fileName) {
        // 1. 根据文件扩展名判断
        if (isCodeFile(fileName)) {
            log.debug("检测到代码文件，使用代码分割器: {}", fileName);
            return codeSplitter;
        }
        
        // 2. 根据内容特征判断
        if (isChineseContent(content)) {
            log.debug("检测到中文内容，使用中文分割器: {}", fileName);
            return chineseSplitter;
        }
        
        // 3. 根据内容长度判断
        if (content.length() > 5000) {
            log.debug("检测到长文档，使用中文分割器: {}", fileName);
            return chineseSplitter;
        }
        
        // 4. 默认使用标准分割器
        log.debug("使用默认分割器: {}", fileName);
        return defaultSplitter;
    }
    
    /**
     * 判断是否为代码文件
     * 
     * @param fileName 文件名
     * @return 是否为代码文件
     */
    private boolean isCodeFile(String fileName) {
        if (fileName == null) return false;
        
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".java") ||
               lowerFileName.endsWith(".js") ||
               lowerFileName.endsWith(".ts") ||
               lowerFileName.endsWith(".py") ||
               lowerFileName.endsWith(".cpp") ||
               lowerFileName.endsWith(".c") ||
               lowerFileName.endsWith(".h") ||
               lowerFileName.endsWith(".cs") ||
               lowerFileName.endsWith(".go") ||
               lowerFileName.endsWith(".rs") ||
               lowerFileName.endsWith(".php") ||
               lowerFileName.endsWith(".rb") ||
               lowerFileName.endsWith(".swift") ||
               lowerFileName.endsWith(".kt") ||
               lowerFileName.endsWith(".scala") ||
               lowerFileName.endsWith(".xml") ||
               lowerFileName.endsWith(".json") ||
               lowerFileName.endsWith(".yaml") ||
               lowerFileName.endsWith(".yml") ||
               lowerFileName.endsWith(".sql") ||
               lowerFileName.endsWith(".sh") ||
               lowerFileName.endsWith(".bat") ||
               lowerFileName.endsWith(".md");
    }
    
    /**
     * 判断是否为中文内容
     * 
     * @param content 文档内容
     * @return 是否为中文内容
     */
    private boolean isChineseContent(String content) {
        if (content == null || content.isEmpty()) return false;
        
        int chineseCharCount = 0;
        int totalCharCount = 0;
        
        for (char c : content.toCharArray()) {
            if (Character.isLetter(c)) {
                totalCharCount++;
                if (isChinese(c)) {
                    chineseCharCount++;
                }
            }
        }
        
        // 如果中文字符占比超过30%，认为是中文内容
        return totalCharCount > 0 && (double) chineseCharCount / totalCharCount > 0.3;
    }
    
    /**
     * 判断字符是否为中文字符
     * 
     * @param c 字符
     * @return 是否为中文字符
     */
    private boolean isChinese(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) ||  // CJK统一汉字
               (c >= 0x3400 && c <= 0x4DBF) ||  // CJK扩展A
               (c >= 0x20000 && c <= 0x2A6DF) || // CJK扩展B
               (c >= 0x2A700 && c <= 0x2B73F) || // CJK扩展C
               (c >= 0x2B740 && c <= 0x2B81F) || // CJK扩展D
               (c >= 0x2B820 && c <= 0x2CEAF) || // CJK扩展E
               (c >= 0x2CEB0 && c <= 0x2EBEF) || // CJK扩展F
               (c >= 0x30000 && c <= 0x3134F);   // CJK扩展G
    }
    
    /**
     * 获取分割器统计信息
     * 
     * @param segments 分割后的片段
     * @return 统计信息
     */
    public String getSplitterStats(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "无分割片段";
        }
        
        int totalSegments = segments.size();
        int totalChars = segments.stream().mapToInt(s -> s.text().length()).sum();
        int avgChars = totalChars / totalSegments;
        int minChars = segments.stream().mapToInt(s -> s.text().length()).min().orElse(0);
        int maxChars = segments.stream().mapToInt(s -> s.text().length()).max().orElse(0);
        
        return String.format("分割统计: 总片段数=%d, 总字符数=%d, 平均字符数=%d, 最小字符数=%d, 最大字符数=%d", 
                           totalSegments, totalChars, avgChars, minChars, maxChars);
    }
}
