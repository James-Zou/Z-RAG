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


package com.unionhole.zrag;

import com.unionhole.zrag.service.RagService;
import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * RAG服务测试类
 */
@SpringBootTest
public class RagServiceTest {

    @Autowired
    private RagService ragService;

    @Test
    public void testRagWorkflow() {
        // 准备测试文档
        Document document1 = Document.from("人工智能是计算机科学的一个分支，它企图了解智能的实质。");
        Document document2 = Document.from("机器学习是人工智能的一个子领域，它使计算机能够在没有明确编程的情况下学习。");
        Document document3 = Document.from("深度学习是机器学习的一个子集，它使用多层神经网络来模拟人脑的工作方式。");

        // 处理文档
        ragService.processDocuments(java.util.Arrays.asList(document1, document2, document3));

        // 测试查询
        String query = "什么是人工智能？";
        String answer = ragService.query(query);

        System.out.println("查询: " + query);
        System.out.println("回答: " + answer);

        // 验证结果
        assert answer != null && !answer.isEmpty();
        assert ragService.getDocumentCount() > 0;
    }

    @Test
    public void testQueryWithParameters() {
        // 准备测试文档
        Document document = Document.from("RAG（Retrieval-Augmented Generation）是一种结合检索和生成的AI技术。");
        ragService.processDocument(document);

        // 测试带参数的查询
        String query = "什么是RAG？";
        String answer = ragService.query(query, 3, 0.5);

        System.out.println("查询: " + query);
        System.out.println("回答: " + answer);

        // 验证结果
        assert answer != null && !answer.isEmpty();
    }
}
