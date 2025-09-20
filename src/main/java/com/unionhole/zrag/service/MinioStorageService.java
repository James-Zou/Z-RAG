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

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Apache POI imports for Office document processing
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * MinIO存储服务
 * 提供文件上传、下载、删除等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {
    
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name:zrag-documents}")
    private String bucketName;
    
    @Value("${storage.minio.prefix:''}")
    private String prefix;
    
    /**
     * 初始化MinIO存储
     */
    public void initializeStorage() {
        try {
            // 检查存储桶是否存在
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            
            if (!bucketExists) {
                // 创建存储桶
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("创建MinIO存储桶: {}", bucketName);
            } else {
                log.info("MinIO存储桶已存在: {}", bucketName);
            }
            
        } catch (Exception e) {
            log.error("初始化MinIO存储失败", e);
            throw new RuntimeException("初始化MinIO存储失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传文件
     * @param file 文件
     * @return 文件路径
     */
    public String uploadFile(MultipartFile file) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String objectName = prefix + fileName;
            
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            log.info("文件上传成功: {}", objectName);
            return objectName;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传文本内容
     * @param content 文本内容
     * @param fileName 文件名
     * @return 文件路径
     */
    public String uploadText(String content, String fileName) {
        try {
            String objectName = prefix + generateFileName(fileName);
            byte[] contentBytes = content.getBytes("UTF-8");
            
            // 上传文本内容
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                            .contentType("text/plain")
                            .build()
            );
            
            log.info("文本内容上传成功: {}", objectName);
            return objectName;
            
        } catch (Exception e) {
            log.error("文本内容上传失败", e);
            throw new RuntimeException("文本内容上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载文件
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件下载失败: {}", objectName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除文件
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 列出所有文件
     * @return 文件列表
     */
    public List<String> listFiles() {
        try {
            List<String> files = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix("")
                            .recursive(true)
                            .build()
            );
            
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    files.add(item.objectName());
                }
            }
            
            return files;
        } catch (Exception e) {
            log.error("列出文件失败", e);
            throw new RuntimeException("列出文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 列出所有文件的详细信息
     * @return 文件详细信息列表
     */
    public List<Map<String, Object>> listFilesWithDetails() {
        try {
            List<Map<String, Object>> files = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix("")
                            .recursive(true)
                            .build()
            );
            
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    Map<String, Object> fileInfo = createFileInfo(item);
                    files.add(fileInfo);
                }
            }
            
            return files;
        } catch (Exception e) {
            log.error("列出文件详细信息失败", e);
            throw new RuntimeException("列出文件详细信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分页列出文件的详细信息
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param search 搜索关键词
     * @return 分页文件信息
     */
    public Map<String, Object> listFilesWithDetailsPaged(int page, int size, String sortBy, String sortOrder, String search) {
        try {
            List<Map<String, Object>> allFiles = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix("")
                            .recursive(true)
                            .build()
            );
            
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    Map<String, Object> fileInfo = createFileInfo(item);
                    
                    // 搜索过滤
                    if (search == null || search.trim().isEmpty() || 
                        fileInfo.get("originalName").toString().toLowerCase().contains(search.toLowerCase())) {
                        allFiles.add(fileInfo);
                    }
                }
            }
            
            // 排序
            allFiles.sort((a, b) -> {
                Object aValue = a.get(sortBy);
                Object bValue = b.get(sortBy);
                
                if (aValue == null && bValue == null) return 0;
                if (aValue == null) return 1;
                if (bValue == null) return -1;
                
                int comparison = 0;
                if (aValue instanceof Comparable && bValue instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> aComparable = (Comparable<Object>) aValue;
                    comparison = aComparable.compareTo(bValue);
                } else {
                    comparison = aValue.toString().compareTo(bValue.toString());
                }
                
                return "desc".equals(sortOrder) ? -comparison : comparison;
            });
            
            // 分页
            int totalElements = allFiles.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            
            List<Map<String, Object>> pageContent = allFiles.subList(startIndex, endIndex);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", pageContent);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("size", size);
            result.put("first", page == 0);
            result.put("last", page >= totalPages - 1);
            result.put("numberOfElements", pageContent.size());
            
            return result;
        } catch (Exception e) {
            log.error("分页列出文件详细信息失败", e);
            throw new RuntimeException("分页列出文件详细信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建文件信息对象
     * @param item MinIO Item对象
     * @return 文件信息Map
     */
    private Map<String, Object> createFileInfo(Item item) {
        Map<String, Object> fileInfo = new HashMap<>();
        String objectName = item.objectName();
        
        // 直接使用MinIO中的文件名
        String fileName = objectName;
        if (objectName.contains("/")) {
            fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
        }
        
        fileInfo.put("name", objectName); // 存储路径
        fileInfo.put("originalName", fileName); // 文件名（从MinIO路径中提取）
        fileInfo.put("size", item.size());
        fileInfo.put("lastModified", item.lastModified());
        
        // 从文件名推断文件类型
        String fileType = getFileTypeFromName(fileName);
        fileInfo.put("type", fileType);
        
        return fileInfo;
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
     * 获取文件信息
     * @param objectName 对象名称
     * @return 文件信息
     */
    public String getFileInfo(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            
            return String.format("文件: %s, 大小: %d bytes, 类型: %s, 修改时间: %s",
                    objectName,
                    stat.size(),
                    stat.contentType(),
                    stat.lastModified()
            );
        } catch (Exception e) {
            log.error("获取文件信息失败: {}", objectName, e);
            return "获取文件信息失败: " + e.getMessage();
        }
    }
    
    /**
     * 生成唯一文件名
     * @param originalFileName 原始文件名
     * @return 生成的文件名
     */
    private String generateFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        if (originalFileName != null && !originalFileName.isEmpty()) {
            String extension = "";
            int lastDotIndex = originalFileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = originalFileName.substring(lastDotIndex);
            }
            return timestamp + "_" + uuid + extension;
        } else {
            return timestamp + "_" + uuid + ".txt";
        }
    }
    
    /**
     * 获取文件内容（Base64编码）
     * @param objectName 对象名称
     * @return 文件内容的Base64编码
     */
    public String getFileContent(String objectName) {
        try {
            InputStream inputStream = downloadFile(objectName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            
            // 将字节数组转换为Base64字符串
            byte[] fileBytes = outputStream.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(fileBytes);
        } catch (Exception e) {
            log.error("获取文件内容失败: {}", objectName, e);
            return null;
        }
    }
    
    /**
     * 获取存储统计信息
     * @return 统计信息
     */
    public String getStorageStats() {
        try {
            List<String> files = listFiles();
            long totalSize = 0;
            
            for (String fileName : files) {
                try {
                    StatObjectResponse stat = minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .build()
                    );
                    totalSize += stat.size();
                } catch (Exception e) {
                    log.warn("获取文件大小失败: {}", fileName, e);
                }
            }
            
            return String.format("MinIO存储统计:\n- 存储桶: %s\n- 文件数量: %d\n- 总大小: %d bytes (%.2f MB)",
                    bucketName,
                    files.size(),
                    totalSize,
                    totalSize / 1024.0 / 1024.0
            );
        } catch (Exception e) {
            log.error("获取存储统计失败", e);
            return "获取存储统计失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取文件内容作为文本
     * @param objectName 对象名称
     * @return 文本内容
     */
    public String getFileContentAsText(String objectName) {
        try {
            InputStream inputStream = downloadFile(objectName);
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead, "UTF-8"));
            }
            inputStream.close();
            return content.toString();
        } catch (Exception e) {
            log.error("获取文件文本内容失败: {}", objectName, e);
            return null;
        }
    }
    
    /**
     * 获取文件内容作为Base64编码
     * @param objectName 对象名称
     * @return Base64编码的内容
     */
    public String getFileContentAsBase64(String objectName) {
        try {
            InputStream inputStream = downloadFile(objectName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            
            byte[] fileBytes = outputStream.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(fileBytes);
        } catch (Exception e) {
            log.error("获取文件Base64内容失败: {}", objectName, e);
            return null;
        }
    }
    
    /**
     * 将Word文档转换为HTML
     * @param objectName 对象名称
     * @return HTML内容
     */
    public String convertWordToHtml(String objectName) {
        try {
            InputStream inputStream = downloadFile(objectName);
            XWPFDocument document = new XWPFDocument(inputStream);
            
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"word-document\">");
            
            // 处理段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    html.append("<p>").append(escapeHtml(text)).append("</p>");
                }
            }
            
            // 处理表格
            for (XWPFTable table : document.getTables()) {
                html.append("<table class=\"word-table\">");
                for (XWPFTableRow row : table.getRows()) {
                    html.append("<tr>");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        html.append("<td>").append(escapeHtml(cell.getText())).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
            }
            
            html.append("</div>");
            
            document.close();
            inputStream.close();
            
            return html.toString();
        } catch (Exception e) {
            log.error("转换Word文档为HTML失败: {}", objectName, e);
            return "<p class=\"error\">Word文档转换失败: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * 将Excel文档转换为HTML表格
     * @param objectName 对象名称
     * @return HTML表格内容
     */
    public String convertExcelToHtml(String objectName) {
        try {
            InputStream inputStream = downloadFile(objectName);
            Workbook workbook = null;
            
            // 根据文件扩展名选择合适的工作簿类型
            if (objectName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (objectName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                // 尝试自动检测
                workbook = new XSSFWorkbook(inputStream);
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"excel-document\">");
            
            // 处理第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            html.append("<table class=\"excel-table\">");
            
            for (Row row : sheet) {
                html.append("<tr>");
                for (Cell cell : row) {
                    html.append("<td>");
                    String cellValue = getCellValueAsString(cell);
                    html.append(escapeHtml(cellValue));
                    html.append("</td>");
                }
                html.append("</tr>");
            }
            
            html.append("</table>");
            html.append("</div>");
            
            workbook.close();
            inputStream.close();
            
            return html.toString();
        } catch (Exception e) {
            log.error("转换Excel文档为HTML失败: {}", objectName, e);
            return "<p class=\"error\">Excel文档转换失败: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * 获取单元格值作为字符串
     * @param cell 单元格
     * @return 字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * HTML转义
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
