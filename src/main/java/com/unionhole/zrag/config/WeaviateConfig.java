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

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Weaviate配置类
 * 配置Weaviate客户端连接
 */
@Slf4j
@Configuration
public class WeaviateConfig {

    @Value("${weaviate.host:localhost}")
    private String host;

    @Value("${weaviate.port:8080}")
    private Integer port;

    @Value("${weaviate.scheme:http}")
    private String scheme;

    @Value("${weaviate.api-key:}")
    private String apiKey;

    @Value("${weaviate.username:}")
    private String username;

    @Value("${weaviate.password:}")
    private String password;

    @Value("${weaviate.access-token:}")
    private String accessToken;

    @Bean
    public WeaviateClient weaviateClient() {
        try {
            String url = String.format("%s://%s:%d", scheme, host, port);
            log.info("连接Weaviate: {}", url);

            // 创建Config对象
            Config config = new Config(scheme, host + ":" + port);
            
            // 根据配置选择认证方式
            WeaviateClient client;
            if (!apiKey.isEmpty()) {
                log.info("使用API Key认证");
                config.getHeaders().put("Authorization", "Bearer " + apiKey);
            } else if (!username.isEmpty() && !password.isEmpty()) {
                log.info("使用用户名密码认证");
                String credentials = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                config.getHeaders().put("Authorization", "Basic " + credentials);
            } else if (!accessToken.isEmpty()) {
                log.info("使用Access Token认证");
                config.getHeaders().put("Authorization", "Bearer " + accessToken);
            } else {
                log.info("使用匿名访问");
            }
            
            client = new WeaviateClient(config);
            
            // 测试连接
            testConnection(client);
            
            return client;
        } catch (Exception e) {
            log.error("创建Weaviate客户端失败", e);
            throw new RuntimeException("创建Weaviate客户端失败: " + e.getMessage(), e);
        }
    }

    private void testConnection(WeaviateClient client) {
        try {
            // 测试连接是否正常
            io.weaviate.client.base.Result<io.weaviate.client.v1.misc.model.Meta> result = client.misc().metaGetter().run();
            if (result.hasErrors()) {
                log.warn("Weaviate连接测试失败: {}", result.getError());
            } else {
                log.info("Weaviate连接测试成功");
            }
        } catch (Exception e) {
            log.warn("Weaviate连接测试异常: {}", e.getMessage());
        }
    }

}
