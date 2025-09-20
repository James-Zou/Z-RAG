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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 静态资源服务
 * 处理静态资源的加载和MIME类型识别
 */
@Slf4j
@Service
public class StaticResourceService {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    
    static {
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("json", "application/json; charset=utf-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("ico", "image/x-icon");
    }

    /**
     * 获取静态资源
     */
    public Resource getStaticResource(String resourcePath) {
        return new ClassPathResource("static/" + resourcePath);
    }

    /**
     * 检查资源是否存在
     */
    public boolean resourceExists(String resourcePath) {
        Resource resource = getStaticResource(resourcePath);
        return resource.exists();
    }

    /**
     * 获取资源的Content-Type
     */
    public String getContentType(String resourcePath) {
        String extension = getFileExtension(resourcePath).toLowerCase();
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * 创建HTTP响应头
     */
    public HttpHeaders createHeaders(String resourcePath) {
        HttpHeaders headers = new HttpHeaders();
        
        // 设置Content-Type
        String contentType = getContentType(resourcePath);
        headers.setContentType(MediaType.parseMediaType(contentType));
        
        // 设置缓存控制
        headers.setCacheControl("public, max-age=3600");
        headers.set("Accept-Ranges", "bytes");
        
        // 设置Content-Length
        try {
            Resource resource = getStaticResource(resourcePath);
            if (resource.exists()) {
                long contentLength = resource.contentLength();
                if (contentLength > 0) {
                    headers.setContentLength(contentLength);
                }
            }
        } catch (IOException e) {
            log.warn("无法获取资源长度: {}", resourcePath, e);
        }
        
        return headers;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 获取资源大小
     */
    public long getResourceSize(String resourcePath) {
        try {
            Resource resource = getStaticResource(resourcePath);
            if (resource.exists()) {
                return resource.contentLength();
            }
        } catch (IOException e) {
            log.warn("无法获取资源大小: {}", resourcePath, e);
        }
        return -1;
    }
}
