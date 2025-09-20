# Z-RAG 部署指南

## 概述

Z-RAG 是一个基于 LangChain4j 的 RAG（检索增强生成）全流程项目，支持多种文档格式的解析、向量化存储和智能问答。

## 打包结构

打包后的 tar.gz 文件包含以下目录结构：

```
zrag-1.0-SNAPSHOT/
├── bin/                    # 脚本目录
│   ├── zrag               # 服务管理脚本
│   ├── zrag.service       # systemd 服务配置
│   ├── install.sh         # 安装脚本
│   ├── uninstall.sh       # 卸载脚本
│   ├── start.sh           # 启动脚本
│   ├── start-with-minio.sh # 带 MinIO 启动脚本
│   ├── test-rag.sh        # RAG 测试脚本
│   └── test-stream.sh     # 流式测试脚本
├── config/                 # 配置目录
│   ├── application.yml     # 主配置文件
│   ├── log4j2.xml         # 日志配置
│   └── sample-documents/   # 示例文档
├── lib/                    # JAR 文件目录
│   ├── ZRAG-1.0-SNAPSHOT.jar  # 主应用程序JAR
│   ├── langchain4j-0.29.1.jar # 依赖JAR包
│   ├── spring-boot-2.5.15.jar # 依赖JAR包
│   └── ...                     # 其他依赖JAR包
├── web/                    # 静态资源目录
│   ├── css/
│   ├── js/
│   ├── lib/
│   └── *.html
├── logs/                   # 日志目录（空）
├── data/                   # 数据目录（空）
├── temp/                   # 临时文件目录（空）
├── docs/                   # 文档目录
├── README.md
├── LICENSE
└── NOTICE
```

## 系统要求

### 硬件要求
- CPU: 2 核心以上
- 内存: 4GB 以上（推荐 8GB）
- 磁盘: 10GB 以上可用空间

### 软件要求
- 操作系统: Linux (Ubuntu 18.04+, CentOS 7+, RHEL 7+)
- Java: OpenJDK 8 或更高版本
- 系统服务: systemd
- 可选: Docker (用于 MinIO、Weaviate 等组件)

## 快速部署

### 1. 构建项目

```bash
# 克隆项目
git clone https://github.com/james-zou/zrag.git
cd zrag

# 构建项目
mvn clean package

# 或者使用测试脚本（推荐）
./script/test-package.sh
```

构建完成后，会在 `target/` 目录下生成 `zrag-1.0-SNAPSHOT.tar.gz` 文件。

**打包特点：**
- 主JAR包和依赖JAR包分离，都放在 `lib/` 目录中
- 启动时通过classpath加载所有JAR包
- 避免单个超大JAR文件的问题
- 便于依赖管理和更新

### 2. 服务器部署

#### 方法一：使用安装脚本（推荐）

```bash
# 上传 tar.gz 文件到服务器
scp target/zrag-1.0-SNAPSHOT.tar.gz user@server:/tmp/

# 登录服务器
ssh user@server

# 解压并安装
cd /tmp
tar -xzf zrag-1.0-SNAPSHOT.tar.gz
cd zrag-1.0-SNAPSHOT
sudo ./bin/install.sh
```

#### 方法二：手动安装

```bash
# 解压到目标目录
sudo tar -xzf zrag-1.0-SNAPSHOT.tar.gz -C /opt/
sudo mv /opt/zrag-1.0-SNAPSHOT /opt/zrag

# 创建服务用户
sudo useradd -r -s /bin/false -d /opt/zrag zrag
sudo chown -R zrag:zrag /opt/zrag

# 安装 systemd 服务
sudo cp /opt/zrag/bin/zrag.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable zrag

# 启动服务
sudo systemctl start zrag
```

### 3. 验证部署

```bash
# 检查服务状态
sudo systemctl status zrag

# 查看服务日志
sudo journalctl -u zrag -f

# 检查端口
netstat -tlnp | grep :8080

# 访问 Web 界面
curl http://localhost:8080
```

## 服务管理

### 使用 systemd 管理

```bash
# 启动服务
sudo systemctl start zrag

# 停止服务
sudo systemctl stop zrag

# 重启服务
sudo systemctl restart zrag

# 查看状态
sudo systemctl status zrag

# 查看日志
sudo journalctl -u zrag -f

# 开机自启
sudo systemctl enable zrag

# 禁用自启
sudo systemctl disable zrag
```

### 使用应用脚本管理

```bash
# 启动服务
/opt/zrag/bin/zrag start

# 停止服务
/opt/zrag/bin/zrag stop

# 重启服务
/opt/zrag/bin/zrag restart

# 查看状态
/opt/zrag/bin/zrag status

# 查看日志
/opt/zrag/bin/zrag logs
```

## 配置说明

### 主配置文件

配置文件位于 `/opt/zrag/config/application.yml`，主要配置项：

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  profiles:
    active: prod

# RAG 配置
rag:
  # 向量数据库配置
  vector-store:
    type: weaviate  # 或 milvus
    weaviate:
      host: localhost
      port: 8080
    milvus:
      host: localhost
      port: 19530
  
  # 嵌入模型配置
  embedding:
    type: qwen
    model: text-embedding-v1
  
  # 聊天模型配置
  chat:
    type: qwen
    model: qwen-turbo
```

### 日志配置

日志配置文件位于 `/opt/zrag/config/log4j2.xml`，支持：

- 控制台输出
- 文件输出（按日期轮转）
- 不同级别的日志分离
- 日志文件压缩

### 环境变量

可以通过环境变量覆盖配置：

```bash
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080
export RAG_VECTOR_STORE_TYPE=weaviate
```

## 监控和维护

### 日志管理

```bash
# 查看应用日志
tail -f /opt/zrag/logs/zrag.log

# 查看系统日志
sudo journalctl -u zrag -f

# 日志轮转（已自动配置）
sudo logrotate -f /etc/logrotate.d/zrag
```

### 性能监控

```bash
# 查看进程信息
ps aux | grep zrag

# 查看内存使用
free -h

# 查看磁盘使用
df -h

# 查看端口占用
netstat -tlnp | grep :8080
```

### 数据备份

```bash
# 备份数据目录
tar -czf zrag-data-backup-$(date +%Y%m%d).tar.gz /opt/zrag/data

# 备份配置
tar -czf zrag-config-backup-$(date +%Y%m%d).tar.gz /opt/zrag/config
```

## 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 查看详细错误信息
   sudo journalctl -u zrag --no-pager -l
   
   # 检查配置文件语法
   java -jar /opt/zrag/lib/ZRAG-1.0-SNAPSHOT.jar --spring.config.location=file:/opt/zrag/config/application.yml --debug
   ```

2. **端口被占用**
   ```bash
   # 查看端口占用
   sudo netstat -tlnp | grep :8080
   
   # 杀死占用进程
   sudo kill -9 <PID>
   ```

3. **内存不足**
   ```bash
   # 调整 JVM 参数
   sudo vim /opt/zrag/bin/zrag
   # 修改 JAVA_OPTS 中的 -Xmx 参数
   ```

4. **权限问题**
   ```bash
   # 修复权限
   sudo chown -R zrag:zrag /opt/zrag
   sudo chmod +x /opt/zrag/bin/*
   ```

### 日志分析

```bash
# 查看错误日志
grep -i error /opt/zrag/logs/zrag.log

# 查看警告日志
grep -i warn /opt/zrag/logs/zrag.log

# 统计访问量
grep "GET /" /opt/zrag/logs/zrag.log | wc -l
```

## 卸载

### 使用卸载脚本

```bash
sudo /opt/zrag/bin/uninstall.sh
```

### 手动卸载

```bash
# 停止服务
sudo systemctl stop zrag
sudo systemctl disable zrag

# 删除服务文件
sudo rm -f /etc/systemd/system/zrag.service
sudo systemctl daemon-reload

# 删除应用文件
sudo rm -rf /opt/zrag

# 删除用户和组
sudo userdel zrag
sudo groupdel zrag

# 删除日志轮转配置
sudo rm -f /etc/logrotate.d/zrag
```

## 高级配置

### 集群部署

对于高可用部署，可以：

1. 使用负载均衡器（如 Nginx）
2. 配置多个实例
3. 使用共享的向量数据库
4. 配置健康检查

### 安全配置

1. 配置防火墙规则
2. 使用 HTTPS
3. 配置访问控制
4. 定期更新依赖

### 性能优化

1. 调整 JVM 参数
2. 配置连接池
3. 启用缓存
4. 优化数据库查询

## 支持

如有问题，请：

1. 查看日志文件
2. 检查配置文件
3. 参考文档
4. 提交 Issue: https://github.com/james-zou/zrag/issues
