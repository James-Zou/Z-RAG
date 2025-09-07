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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus向量数据库配置类
 */
@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;
    
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

    @Value("${milvus.idle-timeout:60000}")
    private int idleTimeout;

    /**
     * 配置Milvus客户端
     */
    @Bean
    public MilvusServiceClient milvusClient() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .withDatabaseName(database)
                    .withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .withIdleTimeout(idleTimeout,TimeUnit.MILLISECONDS)
                    .build();

            MilvusServiceClient client = new MilvusServiceClient(connectParam);
            
            log.info("Milvus客户端配置成功: {}:{}", host, port);
            return client;
        } catch (Exception e) {
            log.error("Milvus客户端配置失败", e);
            throw new RuntimeException("Milvus客户端配置失败: " + e.getMessage(), e);
        }
    }
    
}
