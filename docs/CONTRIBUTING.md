# 贡献指南

感谢您对Z-RAG项目的关注！我们欢迎任何形式的贡献，包括但不限于：

- 报告Bug
- 提出新功能建议
- 提交代码修复
- 改进文档
- 分享使用经验

## 如何贡献

### 1. 报告问题

如果您发现了Bug或有功能建议，请：

1. 在GitHub上创建Issue
2. 详细描述问题或建议
3. 提供复现步骤（如果是Bug）
4. 附上相关的日志或截图

### 2. 提交代码

如果您想提交代码，请遵循以下步骤：

1. Fork本项目
2. 创建特性分支：`git checkout -b feature/your-feature-name`
3. 提交更改：`git commit -m "Add your feature"`
4. 推送分支：`git push origin feature/your-feature-name`
5. 创建Pull Request

### 3. 代码规范

- 遵循Java编码规范
- 添加适当的注释和文档
- 确保代码通过所有测试
- 保持代码简洁和可读性

### 4. 提交信息规范

提交信息应遵循以下格式：

```
<类型>(<范围>): <描述>

<详细说明>

<相关Issue>
```

类型包括：
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

## 开发环境设置

### 1. 环境要求

- Java 8+
- Maven 3.6+
- Git

### 2. 克隆项目

```bash
git clone https://github.com/james-zou/zrag.git
cd zrag
```

### 3. 构建项目

```bash
mvn clean compile
```

### 4. 运行测试

```bash
mvn test
```

### 5. 运行应用

```bash
mvn spring-boot:run
```

## 许可证

本项目采用Apache License 2.0许可证。通过贡献代码，您同意将您的贡献也采用相同的许可证。

## 联系方式

如果您有任何问题，可以通过以下方式联系：

- 邮箱：18301545237@163.com
- GitHub Issues：https://github.com/james-zou/zrag/issues

## 致谢

感谢所有为Z-RAG项目做出贡献的开发者！
