#!/bin/bash

# Z-RAG 卸载脚本
# 用于从服务器上卸载Z-RAG服务

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

# 停止服务
stop_service() {
    log_step "Stopping service..."
    
    if systemctl is-active --quiet "$APP_NAME"; then
        log_info "Stopping $APP_NAME service..."
        systemctl stop "$APP_NAME"
        log_info "Service stopped"
    else
        log_info "Service is not running"
    fi
}

# 禁用服务
disable_service() {
    log_step "Disabling service..."
    
    if systemctl is-enabled --quiet "$APP_NAME"; then
        log_info "Disabling $APP_NAME service..."
        systemctl disable "$APP_NAME"
        log_info "Service disabled"
    else
        log_info "Service is not enabled"
    fi
}

# 删除systemd服务文件
remove_service() {
    log_step "Removing systemd service..."
    
    if [ -f "$SERVICE_FILE" ]; then
        rm -f "$SERVICE_FILE"
        systemctl daemon-reload
        log_info "Service file removed"
    else
        log_info "Service file not found"
    fi
}

# 删除应用文件
remove_app() {
    log_step "Removing application files..."
    
    if [ -d "$INSTALL_DIR" ]; then
        # 备份数据目录
        if [ -d "$INSTALL_DIR/data" ] && [ "$(ls -A "$INSTALL_DIR/data")" ]; then
            BACKUP_DIR="/opt/${APP_NAME}_backup_$(date +%Y%m%d_%H%M%S)"
            log_info "Backing up data to $BACKUP_DIR"
            mkdir -p "$BACKUP_DIR"
            cp -r "$INSTALL_DIR/data" "$BACKUP_DIR/"
            cp -r "$INSTALL_DIR/logs" "$BACKUP_DIR/" 2>/dev/null || true
            log_info "Data backed up to $BACKUP_DIR"
        fi
        
        rm -rf "$INSTALL_DIR"
        log_info "Application files removed"
    else
        log_info "Application directory not found"
    fi
}

# 删除用户和组
remove_user() {
    log_step "Removing user and group..."
    
    if id "$SERVICE_USER" &>/dev/null; then
        log_info "Removing user $SERVICE_USER..."
        userdel "$SERVICE_USER"
        log_info "User removed"
    else
        log_info "User $SERVICE_USER not found"
    fi
    
    if getent group "$SERVICE_GROUP" &>/dev/null; then
        log_info "Removing group $SERVICE_GROUP..."
        groupdel "$SERVICE_GROUP"
        log_info "Group removed"
    else
        log_info "Group $SERVICE_GROUP not found"
    fi
}

# 删除日志轮转配置
remove_logrotate() {
    log_step "Removing log rotation configuration..."
    
    if [ -f "/etc/logrotate.d/$APP_NAME" ]; then
        rm -f "/etc/logrotate.d/$APP_NAME"
        log_info "Log rotation configuration removed"
    else
        log_info "Log rotation configuration not found"
    fi
}

# 清理防火墙规则
cleanup_firewall() {
    log_step "Cleaning up firewall rules..."
    
    if command -v ufw &> /dev/null; then
        log_info "Removing UFW firewall rules..."
        ufw delete allow 8080/tcp 2>/dev/null || true
        ufw delete allow 8081/tcp 2>/dev/null || true
    elif command -v firewall-cmd &> /dev/null; then
        log_info "Removing firewalld rules..."
        firewall-cmd --permanent --remove-port=8080/tcp 2>/dev/null || true
        firewall-cmd --permanent --remove-port=8081/tcp 2>/dev/null || true
        firewall-cmd --reload 2>/dev/null || true
    else
        log_info "No supported firewall found"
    fi
}

# 确认卸载
confirm_uninstall() {
    echo ""
    echo "=========================================="
    echo "Z-RAG Uninstall Confirmation"
    echo "=========================================="
    echo "This will remove:"
    echo "  - $APP_NAME service"
    echo "  - Application files in $INSTALL_DIR"
    echo "  - Service user: $SERVICE_USER"
    echo "  - Service group: $SERVICE_GROUP"
    echo "  - Systemd service file"
    echo "  - Log rotation configuration"
    echo "  - Firewall rules"
    echo ""
    echo "Data will be backed up before removal."
    echo ""
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo ""
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Uninstall cancelled"
        exit 0
    fi
}

# 显示卸载信息
show_info() {
    log_step "Uninstall completed!"
    
    echo ""
    echo "=========================================="
    echo "Z-RAG Uninstall Summary"
    echo "=========================================="
    echo "The following have been removed:"
    echo "  - $APP_NAME service"
    echo "  - Application files"
    echo "  - Service user and group"
    echo "  - Systemd service file"
    echo "  - Log rotation configuration"
    echo "  - Firewall rules"
    echo ""
    echo "Data backup location: /opt/${APP_NAME}_backup_*"
    echo "=========================================="
}

# 主卸载流程
main() {
    log_info "Starting Z-RAG uninstall..."
    
    check_root
    confirm_uninstall
    stop_service
    disable_service
    remove_service
    remove_app
    remove_user
    remove_logrotate
    cleanup_firewall
    show_info
    
    log_info "Uninstall completed successfully!"
}

# 运行主函数
main "$@"
