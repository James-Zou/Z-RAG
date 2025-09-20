#!/bin/bash

# Z-RAG 安装脚本
# 用于在服务器上安装和配置Z-RAG服务

set -e

# 配置变量
APP_NAME="zrag"
INSTALL_DIR="/opt/$APP_NAME"
SERVICE_USER="zrag"
SERVICE_GROUP="zrag"
SERVICE_FILE="/etc/systemd/system/$APP_NAME.service"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        log_error "Please run as root (use sudo)"
        exit 1
    fi
}

# 检查系统要求
check_requirements() {
    log_step "Checking system requirements..."
    
    # 检查操作系统
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        log_info "Operating System: $PRETTY_NAME"
    else
        log_warn "Cannot determine operating system"
    fi
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed. Please install Java 8 or higher."
        log_info "For Ubuntu/Debian: sudo apt-get install openjdk-8-jdk"
        log_info "For CentOS/RHEL: sudo yum install java-1.8.0-openjdk"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 8 ]; then
        log_error "Java 8 or higher is required, found version: $JAVA_VERSION"
        exit 1
    fi
    log_info "Java version: $JAVA_VERSION"
    
    # 检查systemd
    if ! command -v systemctl &> /dev/null; then
        log_error "systemd is not available. This script requires systemd."
        exit 1
    fi
    log_info "systemd is available"
}

# 创建用户和组
create_user() {
    log_step "Creating user and group..."
    
    if ! id "$SERVICE_USER" &>/dev/null; then
        useradd -r -s /bin/false -d "$INSTALL_DIR" "$SERVICE_USER"
        log_info "Created user: $SERVICE_USER"
    else
        log_info "User $SERVICE_USER already exists"
    fi
    
    if ! getent group "$SERVICE_GROUP" &>/dev/null; then
        groupadd -r "$SERVICE_GROUP"
        log_info "Created group: $SERVICE_GROUP"
    else
        log_info "Group $SERVICE_GROUP already exists"
    fi
}

# 安装应用
install_app() {
    log_step "Installing application..."
    
    # 检查tar包是否存在
    TAR_FILE=$(find . -name "zrag-*.tar.gz" | head -n 1)
    if [ -z "$TAR_FILE" ]; then
        log_error "Z-RAG tar.gz file not found. Please run 'mvn clean package' first."
        exit 1
    fi
    
    log_info "Found tar file: $TAR_FILE"
    
    # 停止现有服务
    if systemctl is-active --quiet "$APP_NAME"; then
        log_info "Stopping existing service..."
        systemctl stop "$APP_NAME"
    fi
    
    # 备份现有安装
    if [ -d "$INSTALL_DIR" ]; then
        log_info "Backing up existing installation..."
        mv "$INSTALL_DIR" "${INSTALL_DIR}.backup.$(date +%Y%m%d_%H%M%S)"
    fi
    
    # 创建安装目录
    mkdir -p "$INSTALL_DIR"
    
    # 解压应用
    log_info "Extracting application to $INSTALL_DIR..."
    tar -xzf "$TAR_FILE" -C "$INSTALL_DIR" --strip-components=1
    
    # 设置权限
    chown -R "$SERVICE_USER:$SERVICE_GROUP" "$INSTALL_DIR"
    chmod +x "$INSTALL_DIR/bin"/*
    
    log_info "Application installed successfully"
}

# 安装systemd服务
install_service() {
    log_step "Installing systemd service..."
    
    # 复制服务文件
    cp "$INSTALL_DIR/bin/zrag.service" "$SERVICE_FILE"
    
    # 更新服务文件中的路径
    sed -i "s|/opt/zrag|$INSTALL_DIR|g" "$SERVICE_FILE"
    
    # 重新加载systemd
    systemctl daemon-reload
    
    # 启用服务
    systemctl enable "$APP_NAME"
    
    log_info "Systemd service installed and enabled"
}

# 配置防火墙
configure_firewall() {
    log_step "Configuring firewall..."
    
    if command -v ufw &> /dev/null; then
        log_info "Configuring UFW firewall..."
        ufw allow 8080/tcp comment "Z-RAG HTTP"
        ufw allow 8081/tcp comment "Z-RAG Management"
    elif command -v firewall-cmd &> /dev/null; then
        log_info "Configuring firewalld..."
        firewall-cmd --permanent --add-port=8080/tcp
        firewall-cmd --permanent --add-port=8081/tcp
        firewall-cmd --reload
    else
        log_warn "No supported firewall found. Please manually open ports 8080 and 8081"
    fi
}

# 创建日志轮转配置
configure_logrotate() {
    log_step "Configuring log rotation..."
    
    cat > "/etc/logrotate.d/$APP_NAME" << EOF
$INSTALL_DIR/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 $SERVICE_USER $SERVICE_GROUP
    postrotate
        systemctl reload $APP_NAME > /dev/null 2>&1 || true
    endscript
}
EOF
    
    log_info "Log rotation configured"
}

# 启动服务
start_service() {
    log_step "Starting service..."
    
    systemctl start "$APP_NAME"
    
    # 等待服务启动
    sleep 5
    
    if systemctl is-active --quiet "$APP_NAME"; then
        log_info "Service started successfully"
        log_info "Service status:"
        systemctl status "$APP_NAME" --no-pager -l
    else
        log_error "Failed to start service"
        log_error "Service logs:"
        journalctl -u "$APP_NAME" --no-pager -l
        exit 1
    fi
}

# 显示安装信息
show_info() {
    log_step "Installation completed!"
    
    echo ""
    echo "=========================================="
    echo "Z-RAG Installation Summary"
    echo "=========================================="
    echo "Installation directory: $INSTALL_DIR"
    echo "Service name: $APP_NAME"
    echo "Service user: $SERVICE_USER"
    echo "Service file: $SERVICE_FILE"
    echo ""
    echo "Service management commands:"
    echo "  Start:   systemctl start $APP_NAME"
    echo "  Stop:    systemctl stop $APP_NAME"
    echo "  Restart: systemctl restart $APP_NAME"
    echo "  Status:  systemctl status $APP_NAME"
    echo "  Logs:    journalctl -u $APP_NAME -f"
    echo ""
    echo "Application management:"
    echo "  Start:   $INSTALL_DIR/bin/zrag start"
    echo "  Stop:    $INSTALL_DIR/bin/zrag stop"
    echo "  Status:  $INSTALL_DIR/bin/zrag status"
    echo "  Logs:    $INSTALL_DIR/bin/zrag logs"
    echo ""
    echo "Configuration files:"
    echo "  Main config: $INSTALL_DIR/config/application.yml"
    echo "  Log config:  $INSTALL_DIR/config/log4j2.xml"
    echo ""
    echo "Web interface:"
    echo "  HTTP: http://localhost:8080"
    echo "  Management: http://localhost:8081"
    echo ""
    echo "Log files:"
    echo "  Application: $INSTALL_DIR/logs/zrag.log"
    echo "  System: journalctl -u $APP_NAME"
    echo "=========================================="
}

# 主安装流程
main() {
    log_info "Starting Z-RAG installation..."
    
    check_root
    check_requirements
    create_user
    install_app
    install_service
    configure_firewall
    configure_logrotate
    start_service
    show_info
    
    log_info "Installation completed successfully!"
}

# 运行主函数
main "$@"
