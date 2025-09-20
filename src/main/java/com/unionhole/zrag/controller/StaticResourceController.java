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

import com.unionhole.zrag.service.StaticResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 静态资源控制器
 * 专门处理静态资源请求，解决代理服务器问题
 */
@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class StaticResourceController {

    private final StaticResourceService staticResourceService;

    /**
     * 处理CSS文件请求
     */
    @GetMapping("/css/{filename}")
    public ResponseEntity<Resource> getCss(@PathVariable String filename) {
        return getStaticResource("css/" + filename);
    }

    /**
     * 处理JS文件请求
     */
    @GetMapping("/js/{filename}")
    public ResponseEntity<Resource> getJs(@PathVariable String filename) {
        return getStaticResource("js/" + filename);
    }

    /**
     * 处理库文件请求 - 使用更具体的路径匹配
     */
    @GetMapping("/lib/{path1}/{filename}")
    public ResponseEntity<Resource> getLibFile(@PathVariable String path1, @PathVariable String filename) {
        return getStaticResource("lib/" + path1 + "/" + filename);
    }

    /**
     * 处理库文件请求 - 单层路径
     */
    @GetMapping("/lib/{filename}")
    public ResponseEntity<Resource> getLibFileSingle(@PathVariable String filename) {
        return getStaticResource("lib/" + filename);
    }

    /**
     * 处理其他静态资源
     */
    @GetMapping("/{path1}/{path2}")
    public ResponseEntity<Resource> getStaticResource(@PathVariable String path1, @PathVariable String path2) {
        String resourcePath = path1 + "/" + path2;
        return getStaticResource(resourcePath);
    }

    /**
     * 获取静态资源
     */
    private ResponseEntity<Resource> getStaticResource(String resourcePath) {
        try {
            if (!staticResourceService.resourceExists(resourcePath)) {
                log.warn("静态资源不存在: {}", resourcePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = staticResourceService.getStaticResource(resourcePath);
            HttpHeaders headers = staticResourceService.createHeaders(resourcePath);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("获取静态资源失败: {}", resourcePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
