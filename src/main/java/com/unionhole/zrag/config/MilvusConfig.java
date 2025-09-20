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


package com.unionhole.zrag.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus向量数据库配置类
 */
@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.uri:https://in03-feed7b0f527e86f.serverless.aws-eu-central-1.cloud.zilliz.com}")
    private String uri;
    
    @Value("${milvus.token:}")
    private String token;
    
    /**
     * -- GETTER --
     *  获取数据库名称
     */
    @Getter
    @Value("${milvus.database:zrag}")
    private String database;
    
    /**
     * -- GETTER --
     *  获取集合名称
     */
    @Getter
    @Value("${milvus.collection:documents}")
    private String collection;

    @Value("${milvus.connect-timeout:10000}")
    private int connectTimeout;

    /**
     * 配置Milvus客户端
     */
    @Bean
    public MilvusClientV2 milvusClient() {
        try {
            // 添加调试信息
            log.info("Milvus连接参数 - URI: {}, Database: {}, Token: {}", 
                    uri, database, token != null ? "已设置" : "未设置");
            
            // 如果提供了Token，则添加Token认证
            String cleanToken = token;
            if (token != null && !token.trim().isEmpty()) {
                // 移除Bearer前缀（如果有的话）
                cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
                log.info("使用Token认证连接Milvus，Token长度: {}", cleanToken.length());
            } else {
                log.warn("未提供Token，尝试无认证连接（可能失败）");
            }
            
            log.info("开始创建MilvusClientV2...");
            MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                    .uri(uri)
                    .token(cleanToken)
                    .secure(true)  // Zilliz Cloud 使用 HTTPS
                    .connectTimeoutMs(5000L)
                    .build());
            
            log.info("Milvus客户端配置成功: {}", uri);
            return client;
        } catch (Exception e) {
            log.error("Milvus客户端配置失败", e);
            throw new RuntimeException("Milvus客户端配置失败: " + e.getMessage(), e);
        }
    }
    
}