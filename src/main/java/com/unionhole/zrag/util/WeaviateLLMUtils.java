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

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Weaviate向量数据库的公共方法
 * 参考MilvusLLMUtils的设计模式
 */
@Slf4j
@Component
public class WeaviateLLMUtils {

    @Autowired
    private WeaviateClient weaviateClient;

    /**
     * 创建Weaviate类
     * @param className 类名
     * @param description 描述
     * @param vectorDimension 向量维度
     * @return 是否创建成功
     */
    public boolean createClass(String className, String description, int vectorDimension) {
        try {
            if (weaviateClient == null) {
                log.error("Weaviate客户端未初始化");
                return false;
            }

            // 创建WeaviateClass对象
            WeaviateClass weaviateClass = WeaviateClass.builder()
                    .className(className)
                    .description(description)
                    .vectorizer("none") // 不使用Weaviate的向量化器
                    .properties(Arrays.asList(
                            Property.builder()
                                    .name("text")
                                    .dataType(Arrays.asList(DataType.TEXT))
                                    .description("文档文本内容")
                                    .build(),
                            Property.builder()
                                    .name("metadata")
                                    .dataType(Arrays.asList(DataType.OBJECT))
                                    .description("文档元数据")
                                    .build()
                    ))
                    .build();

            // 使用Weaviate客户端创建类
            Result<Boolean> result = weaviateClient.schema().classCreator()
                    .withClass(weaviateClass)
                    .run();

            if (result.hasErrors()) {
                log.error("创建Weaviate类失败: {}", result.getError());
                return false;
            }

            log.info("成功创建Weaviate类: {}", className);
            return true;
        } catch (Exception e) {
            log.error("创建Weaviate类失败", e);
            return false;
        }
    }

    /**
     * 检查类是否存在
     * @param className 类名
     * @return 是否存在
     */
    public boolean classExists(String className) {
        try {
            if (weaviateClient == null) {
                log.error("Weaviate客户端未初始化");
                return false;
            }

            // 获取所有类
            Result<io.weaviate.client.v1.schema.model.Schema> result = weaviateClient.schema().getter().run();
            
            if (result.hasErrors()) {
                log.debug("获取Weaviate schema失败: {}", result.getError());
                return false;
            }

            // 检查类是否存在
            return result.getResult().getClasses().stream()
                    .anyMatch(weaviateClass -> weaviateClass.getClassName().equals(className));
        } catch (Exception e) {
            log.debug("Weaviate类不存在: {}", className);
            return false;
        }
    }

    /**
     * 添加对象到Weaviate
     * @param className 类名
     * @param objectId 对象ID
     * @param properties 属性
     * @param vector 向量
     * @return 是否添加成功
     */
    public boolean addObject(String className, String objectId, Map<String, Object> properties, Float[] vector) {
        try {
            if (weaviateClient == null) {
                log.error("Weaviate客户端未初始化");
                return false;
            }

            // 转换Float[]为float[]
            float[] vectorArray = new float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                vectorArray[i] = vector[i];
            }

            // 转换float[]为Float[]
            Float[] vectorFloatArray = new Float[vectorArray.length];
            for (int i = 0; i < vectorArray.length; i++) {
                vectorFloatArray[i] = vectorArray[i];
            }

            // 使用Weaviate客户端添加对象
            Result<WeaviateObject> result = weaviateClient.data().creator()
                    .withClassName(className)
                    .withID(objectId)
                    .withProperties(properties)
                    .withVector(vectorFloatArray)
                    .run();

            if (result.hasErrors()) {
                log.error("添加对象到Weaviate失败: {}", result.getError());
                return false;
            }

            log.debug("成功添加对象到Weaviate: {}", objectId);
            return true;
        } catch (Exception e) {
            log.error("添加对象到Weaviate失败", e);
            return false;
        }
    }

    /**
     * 查询对象
     * @param className 类名
     * @param limit 限制数量
     * @return 查询结果
     */
    public List<WeaviateObject> queryObjects(String className, int limit) {
        try {
            if (weaviateClient == null) {
                log.error("Weaviate客户端未初始化");
                return new ArrayList<>();
            }

            // 使用GraphQL查询对象
            String query = String.format("{ Get { %s(limit: %d) { _additional { id vector } text metadata } } }", className, limit);
            Result<io.weaviate.client.v1.graphql.model.GraphQLResponse> result = weaviateClient.graphQL().raw()
                    .withQuery(query)
                    .run();

            if (result.hasErrors()) {
                log.error("查询Weaviate对象失败: {}", result.getError());
                return new ArrayList<>();
            }

            // 解析GraphQL结果
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> get = (Map<String, Object>) data.get("Get");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) get.get(className);
            
            List<WeaviateObject> weaviateObjects = new ArrayList<>();
            if (objects != null) {
                for (Map<String, Object> obj : objects) {
                    try {
                        Map<String, Object> additional = (Map<String, Object>) obj.get("_additional");
                        String id = (String) additional.get("id");
                        List<Double> vectorList = (List<Double>) additional.get("vector");
                        
                        // 转换向量
                        Float[] vector = new Float[vectorList.size()];
                        for (int i = 0; i < vectorList.size(); i++) {
                            vector[i] = vectorList.get(i).floatValue();
                        }
                        
                        // 创建属性映射
                        Map<String, Object> properties = new HashMap<>();
                        properties.put("text", obj.get("text"));
                        properties.put("metadata", obj.get("metadata"));
                        
                        // 创建WeaviateObject
                        WeaviateObject weaviateObject = WeaviateObject.builder()
                                .className(className)
                                .id(id)
                                .properties(properties)
                                .vector(vector)
                                .build();
                        
                        weaviateObjects.add(weaviateObject);
                    } catch (Exception e) {
                        log.warn("解析Weaviate对象失败", e);
                    }
                }
            }
            
            return weaviateObjects;
        } catch (Exception e) {
            log.error("查询Weaviate对象失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除对象
     * @param className 类名
     * @param objectId 对象ID
     * @return 是否删除成功
     */
    public boolean deleteObject(String className, String objectId) {
        try {
            if (weaviateClient == null) {
                log.error("Weaviate客户端未初始化");
                return false;
            }

            // 使用Weaviate客户端删除对象
            Result<Boolean> result = weaviateClient.data().deleter()
                    .withClassName(className)
                    .withID(objectId)
                    .run();

            if (result.hasErrors()) {
                log.error("删除Weaviate对象失败: {}", result.getError());
                return false;
            }

            log.debug("成功删除Weaviate对象: {}", objectId);
            return true;
        } catch (Exception e) {
            log.error("删除Weaviate对象失败", e);
            return false;
        }
    }

    /**
     * 删除类
     * @param className 类名
     * @return 是否删除成功
     */
    public boolean deleteClass(String className) {
        try {
            if (weaviateClient == null) {
                log.error("Weaviate客户端未初始化");
                return false;
            }

            // 使用Weaviate客户端删除类
            Result<Boolean> result = weaviateClient.schema().classDeleter()
                    .withClassName(className)
                    .run();

            if (result.hasErrors()) {
                log.error("删除Weaviate类失败: {}", result.getError());
                return false;
            }

            log.info("成功删除Weaviate类: {}", className);
            return true;
        } catch (Exception e) {
            log.error("删除Weaviate类失败", e);
            return false;
        }
    }
}
