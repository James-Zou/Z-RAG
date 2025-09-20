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

package com.unionhole.zrag.util;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus向量数据库的公共方法
 * 参考WeaviateLLMUtils的设计模式
 */
@Slf4j
@Component
public class MilvusLLMUtils {
    
    @Autowired
    private MilvusClientV2 milvusClient;
    
    private static final String VECTOR_FIELD = "vector";
    private static final String TEXT_FIELD = "content";
    private static final String METADATA_FIELD = "metadata";
    private static final String ID_FIELD = "id";
    private static final String FILE_ID_FIELD = "file_id";
    private static final String TENANT_ID_FIELD = "tenant_id";
    private static final String CHUNK_ID_FIELD = "chunk_id";
    private static final String CREATED_AT_FIELD = "created_at";
    private static final String UPDATED_AT_FIELD = "updated_at";
    
    /**
     * 创建Milvus集合
     * @param collectionName 集合名称
     * @param description 描述
     * @param vectorDimension 向量维度
     * @return 是否创建成功
     */
    public boolean createCollection(String collectionName, String description, int vectorDimension) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return false;
            }
            
            // 检查集合是否存在
            if (hasCollection(collectionName)) {
                log.info("集合 {} 已存在", collectionName);
                return true;
            }
            
            // 创建字段
            CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder()
                    .autoID(true)
                    .dataType(io.milvus.v2.common.DataType.Int64)
                    .isPrimaryKey(true)
                    .name(ID_FIELD)
                    .build();

            // 文件ID字段
            CreateCollectionReq.FieldSchema fileIdField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.VarChar)
                    .name(FILE_ID_FIELD)
                    .isPrimaryKey(false)
                    .maxLength(64)
                    .build();

            // 租户ID字段
            CreateCollectionReq.FieldSchema tenantIdField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.VarChar)
                    .name(TENANT_ID_FIELD)
                    .isPrimaryKey(false)
                    .maxLength(64)
                    .build();

            // 片段ID字段
            CreateCollectionReq.FieldSchema chunkIdField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.VarChar)
                    .name(CHUNK_ID_FIELD)
                    .isPrimaryKey(false)
                    .maxLength(128)
                    .build();

            // 向量字段
            CreateCollectionReq.FieldSchema vectorField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.FloatVector)
                    .name(VECTOR_FIELD)
                    .isPrimaryKey(false)
                    .dimension(vectorDimension)
                    .build();

            // 内容字段
            CreateCollectionReq.FieldSchema contentField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.VarChar)
                    .name(TEXT_FIELD)
                    .isPrimaryKey(false)
                    .maxLength(65535)
                    .build();

            // 元数据字段
            CreateCollectionReq.FieldSchema metadataField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.JSON)
                    .name(METADATA_FIELD)
                    .isPrimaryKey(false)
                    .build();

            // 时间戳字段
            CreateCollectionReq.FieldSchema createdAtField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.Int64)
                    .name(CREATED_AT_FIELD)
                    .isPrimaryKey(false)
                    .build();

            CreateCollectionReq.FieldSchema updatedAtField = CreateCollectionReq.FieldSchema.builder()
                    .dataType(io.milvus.v2.common.DataType.Int64)
                    .name(UPDATED_AT_FIELD)
                    .isPrimaryKey(false)
                    .build();

            List<CreateCollectionReq.FieldSchema> fieldSchemaList = Arrays.asList(
                    idField, fileIdField, tenantIdField, chunkIdField, 
                    vectorField, contentField, metadataField, 
                    createdAtField, updatedAtField
            );

            CreateCollectionReq.CollectionSchema collectionSchema = CreateCollectionReq.CollectionSchema.builder()
                    .fieldSchemaList(fieldSchemaList)
                    .build();

            // 创建集合
            CreateCollectionReq createCollectionParam = CreateCollectionReq.builder()
                    .collectionSchema(collectionSchema)
                    .collectionName(collectionName)
                    .enableDynamicField(false)
                    .description(description)
                    .numShards(1)
                    .build();
            milvusClient.createCollection(createCollectionParam);
            
            log.info("成功创建Milvus集合: {}", collectionName);
            
            // 创建索引
            createIndex(collectionName);
            return true;
            
        } catch (Exception e) {
            log.error("创建Milvus集合失败", e);
            return false;
        }
    }
    
    /**
     * 检查集合是否存在
     * @param collectionName 集合名称
     * @return 是否存在
     */
    public boolean hasCollection(String collectionName) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return false;
            }
            
            HasCollectionReq hasCollectionParam = HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            
            Boolean response = milvusClient.hasCollection(hasCollectionParam);
            return response;
        } catch (Exception e) {
            log.debug("检查Milvus集合存在性失败: {}", collectionName);
            return false;
        }
    }
    
    /**
     * 获取集合详细信息
     * @param collectionName 集合名称
     * @return 集合信息JSON字符串
     */
    public String describeCollection(String collectionName) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return null;
            }
            
            // 使用Milvus客户端的describeCollection方法
            DescribeCollectionReq describeReq = 
                DescribeCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            
            io.milvus.v2.service.collection.response.DescribeCollectionResp response = 
                milvusClient.describeCollection(describeReq);
            
            if (response != null) {
                // 将响应转换为JSON字符串
                com.google.gson.Gson gson = new com.google.gson.Gson();
                return gson.toJson(response);
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取Milvus集合信息失败: {}", collectionName, e);
            return null;
        }
    }
    
    /**
     * 创建索引
     * @param collectionName 集合名称
     * @return 是否创建成功
     */
    private boolean createIndex(String collectionName) {
        try {
            io.milvus.v2.common.IndexParam indexParam = io.milvus.v2.common.IndexParam.builder()
                    .fieldName(VECTOR_FIELD)
                    .indexType(io.milvus.v2.common.IndexParam.IndexType.AUTOINDEX)
                    .metricType(io.milvus.v2.common.IndexParam.MetricType.L2)
                    .build();

            CreateIndexReq createIndexParam = CreateIndexReq.builder()
                    .collectionName(collectionName)
                    .indexParams(Collections.singletonList(indexParam))
                    .build();
            
            milvusClient.createIndex(createIndexParam);
            
                log.info("成功创建Milvus索引: {}", collectionName);
                return true;
        } catch (Exception e) {
            log.error("创建Milvus索引失败", e);
            return false;
        }
    }
    
    /**
     * 添加向量到Milvus
     * @param collectionName 集合名称
     * @param vectors 向量列表
     * @param texts 文本列表
     * @param metadataList 元数据列表
     * @return 是否添加成功
     */
    public boolean addVectors(String collectionName, List<List<Float>> vectors, List<String> texts, List<String> metadataList) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return false;
            }
            
            // 准备数据
            List<com.google.gson.JsonObject> data = new ArrayList<>();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            for (int i = 0; i < vectors.size(); i++) {
                com.google.gson.JsonObject row = new com.google.gson.JsonObject();
                // 生成UUID作为file_id
                String fileId = java.util.UUID.randomUUID().toString();
                String tenantId = "default_tenant";
                String chunkId = "chunk_" + System.currentTimeMillis() + "_" + i;
                long currentTime = System.currentTimeMillis();
                
                row.addProperty(FILE_ID_FIELD, fileId);
                row.addProperty(TENANT_ID_FIELD, tenantId);
                row.addProperty(CHUNK_ID_FIELD, chunkId);
                row.add(VECTOR_FIELD, gson.toJsonTree(vectors.get(i)));
                row.addProperty(TEXT_FIELD, texts.get(i));
                row.addProperty(METADATA_FIELD, metadataList.get(i));
                row.addProperty(CREATED_AT_FIELD, currentTime);
                row.addProperty(UPDATED_AT_FIELD, currentTime);
                data.add(row);
            }

            io.milvus.v2.service.vector.request.InsertReq insertReq = io.milvus.v2.service.vector.request.InsertReq.builder()
                    .collectionName(collectionName)
                    .data(data)
                    .build();
            
            milvusClient.insert(insertReq);
            
            log.debug("成功添加向量到Milvus: {} 条记录", vectors.size());
            return true;
        } catch (Exception e) {
            log.error("添加向量到Milvus失败", e);
            return false;
        }
    }
    
    /**
     * 搜索相似向量
     * @param collectionName 集合名称
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @return 搜索结果
     */
    public List<Map<String, Object>> searchSimilar(String collectionName, List<Float> queryVector, int topK) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return new ArrayList<>();
            }
            
            log.info("=== Milvus 向量搜索执行 ===");
            log.info("集合名称: {}", collectionName);
            log.info("查询向量维度: {}", queryVector.size());
            log.info("返回数量: {}", topK);
            
            // 加载集合
            log.info("确保集合已加载到内存...");
            loadCollection(collectionName);
            
            // 搜索参数
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("level", 1);
            log.info("搜索参数: {}", searchParams);

            io.milvus.v2.service.vector.request.SearchReq searchReq = io.milvus.v2.service.vector.request.SearchReq.builder()
                    .collectionName(collectionName)
                    .data(Collections.singletonList(new io.milvus.v2.service.vector.request.data.FloatVec(queryVector)))
                    .searchParams(searchParams)
                    .outputFields(Arrays.asList(TEXT_FIELD, METADATA_FIELD, FILE_ID_FIELD, CHUNK_ID_FIELD))
                    .metricType(io.milvus.v2.common.IndexParam.MetricType.L2)
                    .topK(topK)
                    .build();
            
            log.info("执行Milvus搜索请求...");
            long startTime = System.currentTimeMillis();
            io.milvus.v2.service.vector.response.SearchResp response = milvusClient.search(searchReq);
            long endTime = System.currentTimeMillis();
            
            log.info("Milvus搜索请求完成，耗时: {} ms", (endTime - startTime));
            log.info("响应结果: {}", response.getSearchResults());
            
            List<Map<String, Object>> results = new ArrayList<>();
            if (response.getSearchResults() != null && !response.getSearchResults().isEmpty()) {
                log.info("搜索成功，原始结果数量: {}", response.getSearchResults().size());
                
                // 处理搜索结果 - 使用反射和通用方法解析Milvus V2 API响应
                for (int i = 0; i < response.getSearchResults().size(); i++) {
                    try {
                        // 获取搜索结果列表
                        Object searchResultList = response.getSearchResults().get(i);
                        
                        // 使用反射获取SearchResult列表
                        if (searchResultList instanceof List) {
                            List<?> searchResults = (List<?>) searchResultList;
                            
                            for (Object searchResult : searchResults) {
                                Map<String, Object> result = new HashMap<>();
                                
                                try {
                                    // 使用反射获取SearchResult的字段
                                    Class<?> searchResultClass = searchResult.getClass();
                                    
                                    // 获取ID
                                    java.lang.reflect.Method getIdMethod = searchResultClass.getMethod("getId");
                                    Object id = getIdMethod.invoke(searchResult);
                                    result.put("id", id);
                                    
                                    // 获取分数
                                    java.lang.reflect.Method getScoreMethod = searchResultClass.getMethod("getScore");
                                    Object score = getScoreMethod.invoke(searchResult);
                                    double similarity = score instanceof Number ? ((Number) score).doubleValue() : 0.0;
                                    // 注意：Milvus V2 API返回的score已经是相似度分数，不需要转换
                                    result.put("score", similarity);
                                    
                                    // 获取实体
                                    java.lang.reflect.Method getEntityMethod = searchResultClass.getMethod("getEntity");
                                    Object entity = getEntityMethod.invoke(searchResult);
                                    
                                    if (entity != null) {
                                        Class<?> entityClass = entity.getClass();
                                        
                                        // 获取文本内容
                                        try {
                                            // 尝试多种方法获取文本内容
                                            String content = "";
                                            
                                            // 方法1: 尝试getField方法
                                            try {
                                                java.lang.reflect.Method getFieldMethod = entityClass.getMethod("getField", String.class);
                                                Object contentObj = getFieldMethod.invoke(entity, TEXT_FIELD);
                                                if (contentObj != null) {
                                                    content = contentObj.toString();
                                                }
                                            } catch (Exception e1) {
                                                // 方法2: 尝试直接访问content字段
                                                try {
                                                    java.lang.reflect.Field contentField = entityClass.getDeclaredField(TEXT_FIELD);
                                                    contentField.setAccessible(true);
                                                    Object contentObj = contentField.get(entity);
                                                    if (contentObj != null) {
                                                        content = contentObj.toString();
                                                    }
                                                } catch (Exception e2) {
                                                    // 方法3: 尝试通过Map接口获取
                                                    if (entity instanceof Map) {
                                                        Map<?, ?> entityMap = (Map<?, ?>) entity;
                                                        Object contentObj = entityMap.get(TEXT_FIELD);
                                                        if (contentObj != null) {
                                                            content = contentObj.toString();
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            result.put("text", content);
                                        } catch (Exception e) {
                                            result.put("text", "");
                                        }
                                        
                                        // 获取元数据
                                        try {
                                            String metadata = "";
                                            
                                            // 尝试多种方法获取元数据
                                            try {
                                                java.lang.reflect.Method getFieldMethod = entityClass.getMethod("getField", String.class);
                                                Object metadataObj = getFieldMethod.invoke(entity, METADATA_FIELD);
                                                if (metadataObj != null) {
                                                    metadata = metadataObj.toString();
                                                }
                                            } catch (Exception e1) {
                                                try {
                                                    java.lang.reflect.Field metadataField = entityClass.getDeclaredField(METADATA_FIELD);
                                                    metadataField.setAccessible(true);
                                                    Object metadataObj = metadataField.get(entity);
                                                    if (metadataObj != null) {
                                                        metadata = metadataObj.toString();
                                                    }
                                                } catch (Exception e2) {
                                                    if (entity instanceof Map) {
                                                        Map<?, ?> entityMap = (Map<?, ?>) entity;
                                                        Object metadataObj = entityMap.get(METADATA_FIELD);
                                                        if (metadataObj != null) {
                                                            metadata = metadataObj.toString();
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            result.put("metadata", metadata.isEmpty() ? "{}" : metadata);
                                        } catch (Exception e) {
                                            result.put("metadata", "{}");
                                        }
                                        
                                        // 获取其他字段
                                        try {
                                            // 获取file_id
                                            String fileId = "";
                                            try {
                                                java.lang.reflect.Method getFieldMethod = entityClass.getMethod("getField", String.class);
                                                Object fileIdObj = getFieldMethod.invoke(entity, FILE_ID_FIELD);
                                                if (fileIdObj != null) {
                                                    fileId = fileIdObj.toString();
                                                }
                                            } catch (Exception e1) {
                                                try {
                                                    java.lang.reflect.Field fileIdField = entityClass.getDeclaredField(FILE_ID_FIELD);
                                                    fileIdField.setAccessible(true);
                                                    Object fileIdObj = fileIdField.get(entity);
                                                    if (fileIdObj != null) {
                                                        fileId = fileIdObj.toString();
                                                    }
                                                } catch (Exception e2) {
                                                    if (entity instanceof Map) {
                                                        Map<?, ?> entityMap = (Map<?, ?>) entity;
                                                        Object fileIdObj = entityMap.get(FILE_ID_FIELD);
                                                        if (fileIdObj != null) {
                                                            fileId = fileIdObj.toString();
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // 获取chunk_id
                                            String chunkId = "";
                                            try {
                                                java.lang.reflect.Method getFieldMethod = entityClass.getMethod("getField", String.class);
                                                Object chunkIdObj = getFieldMethod.invoke(entity, CHUNK_ID_FIELD);
                                                if (chunkIdObj != null) {
                                                    chunkId = chunkIdObj.toString();
                                                }
                                            } catch (Exception e1) {
                                                try {
                                                    java.lang.reflect.Field chunkIdField = entityClass.getDeclaredField(CHUNK_ID_FIELD);
                                                    chunkIdField.setAccessible(true);
                                                    Object chunkIdObj = chunkIdField.get(entity);
                                                    if (chunkIdObj != null) {
                                                        chunkId = chunkIdObj.toString();
                                                    }
                                                } catch (Exception e2) {
                                                    if (entity instanceof Map) {
                                                        Map<?, ?> entityMap = (Map<?, ?>) entity;
                                                        Object chunkIdObj = entityMap.get(CHUNK_ID_FIELD);
                                                        if (chunkIdObj != null) {
                                                            chunkId = chunkIdObj.toString();
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // 获取tenant_id
                                            String tenantId = "";
                                            try {
                                                java.lang.reflect.Method getFieldMethod = entityClass.getMethod("getField", String.class);
                                                Object tenantIdObj = getFieldMethod.invoke(entity, TENANT_ID_FIELD);
                                                if (tenantIdObj != null) {
                                                    tenantId = tenantIdObj.toString();
                                                }
                                            } catch (Exception e1) {
                                                try {
                                                    java.lang.reflect.Field tenantIdField = entityClass.getDeclaredField(TENANT_ID_FIELD);
                                                    tenantIdField.setAccessible(true);
                                                    Object tenantIdObj = tenantIdField.get(entity);
                                                    if (tenantIdObj != null) {
                                                        tenantId = tenantIdObj.toString();
                                                    }
                                                } catch (Exception e2) {
                                                    if (entity instanceof Map) {
                                                        Map<?, ?> entityMap = (Map<?, ?>) entity;
                                                        Object tenantIdObj = entityMap.get(TENANT_ID_FIELD);
                                                        if (tenantIdObj != null) {
                                                            tenantId = tenantIdObj.toString();
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            result.put("file_id", fileId);
                                            result.put("chunk_id", chunkId);
                                            result.put("tenant_id", tenantId);
                                        } catch (Exception e) {
                                            // 忽略字段获取错误
                                        }
                                    }
                                    
                                    results.add(result);
                                    
                                    log.debug("处理搜索结果: ID={}, Score={:.6f}, Text长度={}", 
                                            result.get("id"), similarity, 
                                            result.get("text").toString().length());
                                    
                                } catch (Exception e) {
                                    log.warn("处理单个搜索结果失败", e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("处理搜索结果失败: {}", i, e);
                    }
                }
                
                log.info("搜索结果处理完成，有效结果数量: {}", results.size());
            } else {
                log.warn("搜索未返回结果");
            }
            
            log.info("=== Milvus 向量搜索执行完成 ===");
            return results;
        } catch (Exception e) {
            log.error("搜索Milvus向量失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 查询所有数据
     * @param collectionName 集合名称
     * @param limit 限制数量
     * @return 查询结果
     */
    public List<Map<String, Object>> queryAll(String collectionName, int limit) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return new ArrayList<>();
            }
            
            // 加载集合
            loadCollection(collectionName);
            
            // 查询参数 - 暂时简化处理
            log.info("查询功能需要根据新SDK实现");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("查询Milvus数据失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 删除集合
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    public boolean dropCollection(String collectionName) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return false;
            }
    
            DropCollectionReq dropCollectionParam = DropCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            
            milvusClient.dropCollection(dropCollectionParam);
            
                log.info("成功删除Milvus集合: {}", collectionName);
                return true;
        } catch (Exception e) {
            log.error("删除Milvus集合失败", e);
            return false;
        }
    }
    
    /**
     * 清空集合数据
     * @param collectionName 集合名称
     * @return 是否清空成功
     */
    public boolean clearCollection(String collectionName) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return false;
            }
            
            // 删除所有数据
            DeleteReq deleteParam = DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter("id >= 0")
                    .build();
    
            milvusClient.delete(deleteParam);
            
            log.info("成功清空Milvus集合: {}", collectionName);
            return true;
        } catch (Exception e) {
            log.error("清空Milvus集合失败", e);
            return false;
        }
    }
    
    /**
     * 检查集合是否已加载到内存
     * @param collectionName 集合名称
     * @return 是否已加载
     */
    public boolean isCollectionLoaded(String collectionName) {
        try {
            // 这里需要实现检查集合加载状态的逻辑
            // 由于Milvus V2 API的具体实现可能不同，这里提供框架
            // 实际实现可能需要调用相应的API来检查集合状态
            log.debug("检查集合加载状态: {}", collectionName);
            
            // 暂时返回false，让系统尝试加载
            // 在实际项目中，这里应该调用相应的API检查状态
            return false;
        } catch (Exception e) {
            log.warn("检查集合加载状态失败: {}", collectionName, e);
            return false;
        }
    }
    
    /**
     * 加载集合到内存
     * @param collectionName 集合名称
     * @return 是否加载成功
     */
    public boolean loadCollection(String collectionName) {
        try {
            LoadCollectionReq loadCollectionParam = LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            
            milvusClient.loadCollection(loadCollectionParam);
            log.info("成功加载Milvus集合到内存: {}", collectionName);
            return true;
        } catch (Exception e) {
            log.warn("加载Milvus集合失败: {}", collectionName, e);
            return false;
        }
    }
    
    /**
     * 添加文档向量（支持多租户和文件ID）
     * @param collectionName 集合名称
     * @param fileId 文件ID
     * @param tenantId 租户ID
     * @param chunkId 片段ID
     * @param vector 向量数据
     * @param content 文本内容
     * @param metadata 元数据
     * @return 是否添加成功
     */
    public boolean addDocumentVector(String collectionName, String fileId, String tenantId, 
                                   String chunkId, List<Float> vector, String content, 
                                   Map<String, Object> metadata) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return false;
            }
            
            log.info("开始添加文档向量到Milvus: fileId={}, tenantId={}, chunkId={}, vectorSize={}", 
                    fileId, tenantId, chunkId, vector.size());
            
            long currentTime = System.currentTimeMillis();
            
            // 准备数据
            List<com.google.gson.JsonObject> data = new ArrayList<>();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject row = new com.google.gson.JsonObject();
            
            row.addProperty(FILE_ID_FIELD, fileId);
            row.addProperty(TENANT_ID_FIELD, tenantId);
            row.addProperty(CHUNK_ID_FIELD, chunkId);
            row.add(VECTOR_FIELD, gson.toJsonTree(vector));
            row.addProperty(TEXT_FIELD, content);
            row.addProperty(METADATA_FIELD, gson.toJson(metadata));
            row.addProperty(CREATED_AT_FIELD, currentTime);
            row.addProperty(UPDATED_AT_FIELD, currentTime);
            data.add(row);

            io.milvus.v2.service.vector.request.InsertReq insertReq = io.milvus.v2.service.vector.request.InsertReq.builder()
                    .collectionName(collectionName)
                    .data(data)
                    .build();
            
            milvusClient.insert(insertReq);
            
            log.info("成功添加文档向量到Milvus: fileId={}, chunkId={}", fileId, chunkId);
            return true;
        } catch (Exception e) {
            log.error("添加文档向量失败: fileId={}, chunkId={}", fileId, chunkId, e);
            return false;
        }
    }
    
    /**
     * 按租户搜索相似向量
     * @param collectionName 集合名称
     * @param tenantId 租户ID
     * @param queryVector 查询向量
     * @param maxResults 最大结果数
     * @param minScore 最小相似度分数
     * @return 搜索结果
     */
    public List<Map<String, Object>> searchByTenant(String collectionName, String tenantId, 
                                                   List<Float> queryVector, int maxResults, 
                                                   double minScore) {
        try {
            String filter = String.format("%s == '%s'", TENANT_ID_FIELD, tenantId);
            return searchSimilarWithFilter(collectionName, queryVector, maxResults, minScore, filter);
        } catch (Exception e) {
            log.error("按租户搜索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 按文件搜索相似向量
     * @param collectionName 集合名称
     * @param fileId 文件ID
     * @param queryVector 查询向量
     * @param maxResults 最大结果数
     * @param minScore 最小相似度分数
     * @return 搜索结果
     */
    public List<Map<String, Object>> searchByFile(String collectionName, String fileId, 
                                                 List<Float> queryVector, int maxResults, 
                                                 double minScore) {
        try {
            String filter = String.format("%s == '%s'", FILE_ID_FIELD, fileId);
            return searchSimilarWithFilter(collectionName, queryVector, maxResults, minScore, filter);
        } catch (Exception e) {
            log.error("按文件搜索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 按租户和文件搜索相似向量
     * @param collectionName 集合名称
     * @param tenantId 租户ID
     * @param fileId 文件ID
     * @param queryVector 查询向量
     * @param maxResults 最大结果数
     * @param minScore 最小相似度分数
     * @return 搜索结果
     */
    public List<Map<String, Object>> searchByTenantAndFile(String collectionName, String tenantId, 
                                                          String fileId, List<Float> queryVector, 
                                                          int maxResults, double minScore) {
        try {
            String filter = String.format("%s == '%s' and %s == '%s'", 
                                        TENANT_ID_FIELD, tenantId, FILE_ID_FIELD, fileId);
            return searchSimilarWithFilter(collectionName, queryVector, maxResults, minScore, filter);
        } catch (Exception e) {
            log.error("按租户和文件搜索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 带过滤条件的相似性搜索
     * @param collectionName 集合名称
     * @param queryVector 查询向量
     * @param maxResults 最大结果数
     * @param minScore 最小相似度分数
     * @param filter 过滤条件
     * @return 搜索结果
     */
    private List<Map<String, Object>> searchSimilarWithFilter(String collectionName, 
                                                            List<Float> queryVector, 
                                                            int maxResults, double minScore, 
                                                            String filter) {
        try {
            // 这里需要实现带过滤条件的搜索逻辑
            // 由于Milvus V2 API的具体实现可能不同，这里提供框架
            log.info("执行带过滤条件的搜索: {}", filter);
            
            // 调用原有的搜索方法，然后在结果中应用过滤
            List<Map<String, Object>> allResults = searchSimilar(collectionName, queryVector, maxResults * 2);
            
            // 应用过滤条件（这里简化处理，实际应该在数据库层面过滤）
            return allResults.stream()
                    .filter(result -> {
                        // 根据过滤条件过滤结果
                        if (filter.contains(TENANT_ID_FIELD)) {
                            String tenantId = (String) result.get(TENANT_ID_FIELD);
                            return tenantId != null && filter.contains(tenantId);
                        }
                        if (filter.contains(FILE_ID_FIELD)) {
                            String fileId = (String) result.get(FILE_ID_FIELD);
                            return fileId != null && filter.contains(fileId);
                        }
                        return true;
                    })
                    .limit(maxResults)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("带过滤条件的搜索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 清理test测试数据
     * @param collectionName 集合名称
     * @return 清理的数据条数
     */
    public int cleanTestData(String collectionName) {
        try {
            if (milvusClient == null) {
                log.error("Milvus客户端未初始化");
                return 0;
            }
            
            log.info("开始清理test测试数据: {}", collectionName);
            
            // 构建删除条件：content字段等于"test"
            String filter = String.format("%s == 'test'", TEXT_FIELD);
            
            // 创建删除请求
            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(filter)
                    .build();
            
            // 执行删除操作
            milvusClient.delete(deleteReq);
            
            log.info("test测试数据清理完成: {}", collectionName);
            return 1; // 返回1表示清理操作已执行
        } catch (Exception e) {
            log.error("清理test测试数据失败: {}", collectionName, e);
            return 0;
        }
    }
}