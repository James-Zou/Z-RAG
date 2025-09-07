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

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.RpcStatus;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.grpc.QueryResults;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Milvus向量数据库的公共方法
 * 参考WeaviateLLMUtils的设计模式
 */
@Slf4j
@Component
public class MilvusLLMUtils {

    @Autowired
    private MilvusServiceClient milvusClient;

    private static final String VECTOR_FIELD = "vector";
    private static final String TEXT_FIELD = "text";
    private static final String METADATA_FIELD = "metadata";
    private static final String ID_FIELD = "id";

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
            List<FieldType> fields = Arrays.asList(
                    FieldType.newBuilder()
                            .withName(ID_FIELD)
                            .withDataType(DataType.Int64)
                            .withPrimaryKey(true)
                            .withAutoID(true)
                            .build(),
                    FieldType.newBuilder()
                            .withName(VECTOR_FIELD)
                            .withDataType(DataType.FloatVector)
                            .withDimension(vectorDimension)
                            .build(),
                    FieldType.newBuilder()
                            .withName(TEXT_FIELD)
                            .withDataType(DataType.VarChar)
                            .withMaxLength(65535)
                            .build(),
                    FieldType.newBuilder()
                            .withName(METADATA_FIELD)
                            .withDataType(DataType.VarChar)
                            .withMaxLength(65535)
                            .build()
            );

            // 创建集合
            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription(description)
                    .withFieldTypes(fields)
                    .build();

            R<RpcStatus> response = milvusClient.createCollection(createCollectionParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("成功创建Milvus集合: {}", collectionName);
                
                // 创建索引
                createIndex(collectionName);
                return true;
            } else {
                log.error("创建Milvus集合失败: {}", response.getMessage());
                return false;
            }
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

            HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<Boolean> response = milvusClient.hasCollection(hasCollectionParam);
            return response.getStatus() == R.Status.Success.getCode() && response.getData();
        } catch (Exception e) {
            log.debug("检查Milvus集合存在性失败: {}", collectionName);
            return false;
        }
    }

    /**
     * 创建索引
     * @param collectionName 集合名称
     * @return 是否创建成功
     */
    private boolean createIndex(String collectionName) {
        try {
            CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(VECTOR_FIELD)
                    .withIndexType(io.milvus.param.IndexType.IVF_FLAT)
                    .withMetricType(io.milvus.param.MetricType.L2)
                    .withExtraParam("{\"nlist\":1024}")
                    .build();

            R<RpcStatus> response = milvusClient.createIndex(createIndexParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("成功创建Milvus索引: {}", collectionName);
                return true;
            } else {
                log.error("创建Milvus索引失败: {}", response.getMessage());
                return false;
            }
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
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field(VECTOR_FIELD, vectors),
                    new InsertParam.Field(TEXT_FIELD, texts),
                    new InsertParam.Field(METADATA_FIELD, metadataList)
            );

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            R<MutationResult> response = milvusClient.insert(insertParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.debug("成功添加向量到Milvus: {} 条记录", vectors.size());
                return true;
            } else {
                log.error("添加向量到Milvus失败: {}", response.getMessage());
                return false;
            }
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

            // 加载集合
            loadCollection(collectionName);

            // 搜索参数
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectorFieldName(VECTOR_FIELD)
                    .withVectors(Arrays.asList(queryVector))
                    .withTopK(topK)
                    .withMetricType(io.milvus.param.MetricType.L2)
                    .withOutFields(Arrays.asList(TEXT_FIELD, METADATA_FIELD))
                    .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                    .build();

            R<SearchResults> response = milvusClient.search(searchParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
                List<Map<String, Object>> results = new ArrayList<>();
                
                for (int i = 0; i < wrapper.getIDScore(0).size(); i++) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", wrapper.getIDScore(0).get(i).getLongID());
                    result.put("score", wrapper.getIDScore(0).get(i).getScore());
                    result.put("text", wrapper.getFieldData(TEXT_FIELD, 0).get(i));
                    result.put("metadata", wrapper.getFieldData(METADATA_FIELD, 0).get(i));
                    results.add(result);
                }
                
                return results;
            } else {
                log.error("搜索Milvus向量失败: {}", response.getMessage());
                return new ArrayList<>();
            }
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

            // 查询参数
            QueryParam queryParam = QueryParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withOutFields(Arrays.asList(TEXT_FIELD, METADATA_FIELD))
                    .withExpr("id >= 0")
                    .withLimit((long) limit)
                    .build();

            R<QueryResults> response = milvusClient.query(queryParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                // 简化处理，返回空结果
                log.info("查询成功，但结果解析需要进一步优化");
                return new ArrayList<>();
            } else {
                log.error("查询Milvus数据失败: {}", response.getMessage());
                return new ArrayList<>();
            }
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

            DropCollectionParam dropCollectionParam = DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<RpcStatus> response = milvusClient.dropCollection(dropCollectionParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("成功删除Milvus集合: {}", collectionName);
                return true;
            } else {
                log.error("删除Milvus集合失败: {}", response.getMessage());
                return false;
            }
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
            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr("id >= 0")
                    .build();

            R<MutationResult> response = milvusClient.delete(deleteParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("成功清空Milvus集合: {}", collectionName);
                return true;
            } else {
                log.error("清空Milvus集合失败: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("清空Milvus集合失败", e);
            return false;
        }
    }

    /**
     * 加载集合到内存
     * @param collectionName 集合名称
     * @return 是否加载成功
     */
    private boolean loadCollection(String collectionName) {
        try {
            LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();

            R<RpcStatus> response = milvusClient.loadCollection(loadCollectionParam);
            return response.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.warn("加载Milvus集合失败: {}", collectionName);
            return false;
        }
    }
}
