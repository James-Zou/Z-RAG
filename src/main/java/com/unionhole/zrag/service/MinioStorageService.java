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
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Value("${storage.minio.prefix:documents/}")
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
                    .prefix(prefix)
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
     * 获取文件内容
     * @param objectName 对象名称
     * @return 文件内容
     */
    public String getFileContent(String objectName) {
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
}
