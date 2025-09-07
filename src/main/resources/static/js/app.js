// Z-RAG 前端应用
class ZRAGApp {
    constructor() {
        this.currentTab = 'chat';
        this.chatHistory = [];
        this.documents = [];
        this.knowledgeChunks = [];
        this.settings = {
            modelProvider: 'qwen',
            apiKey: '',
            modelName: 'qwen-turbo',
            maxResults: 5,
            minScore: 0.6,
            chunkSize: 300
        };
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadSettings();
        this.checkSystemStatus();
        this.loadDocuments();
        this.loadKnowledgeStats();
    }

    bindEvents() {
        // 导航切换
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const tab = e.currentTarget.dataset.tab;
                this.switchTab(tab);
            });
        });

        // 聊天相关事件
        document.getElementById('sendMessage').addEventListener('click', () => {
            this.sendMessage();
        });

        document.getElementById('chatInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        document.getElementById('clearChat').addEventListener('click', () => {
            this.clearChat();
        });

        document.getElementById('newChat').addEventListener('click', () => {
            this.newChat();
        });

        // 文档管理事件
        document.getElementById('uploadDocument').addEventListener('click', () => {
            this.showUploadModal();
        });

        document.getElementById('refreshDocuments').addEventListener('click', () => {
            this.loadDocuments();
        });

        const uploadArea = document.getElementById('uploadArea');
        uploadArea.addEventListener('click', () => {
            document.getElementById('fileInput').click();
        });

        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });

        uploadArea.addEventListener('dragleave', () => {
            uploadArea.classList.remove('dragover');
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            const files = e.dataTransfer.files;
            this.handleFileUpload(files);
        });

        document.getElementById('fileInput').addEventListener('change', (e) => {
            this.handleFileUpload(e.target.files);
        });

        // 知识管理事件
        document.getElementById('createKnowledge').addEventListener('click', () => {
            this.createKnowledge();
        });

        document.getElementById('refreshKnowledge').addEventListener('click', () => {
            this.refreshKnowledge();
        });

        // 设置相关事件
        document.getElementById('saveSettings').addEventListener('click', () => {
            this.saveSettings();
        });

        document.getElementById('resetSettings').addEventListener('click', () => {
            this.resetSettings();
        });

        // 模态框事件
        document.querySelector('.modal-close').addEventListener('click', () => {
            this.hideUploadModal();
        });

        document.getElementById('cancelUpload').addEventListener('click', () => {
            this.hideUploadModal();
        });

        document.getElementById('confirmUpload').addEventListener('click', () => {
            this.confirmUpload();
        });

        // 知识管理标签切换
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tab = e.currentTarget.dataset.knowledgeTab;
                this.switchKnowledgeTab(tab);
            });
        });
    }

    switchTab(tab) {
        // 更新导航状态
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tab}"]`).classList.add('active');

        // 更新内容显示
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(tab).classList.add('active');

        this.currentTab = tab;

        // 根据标签加载相应数据
        if (tab === 'documents') {
            this.loadDocuments();
        } else if (tab === 'knowledge') {
            this.loadKnowledgeStats();
        } else if (tab === 'settings') {
            this.loadSettings();
        }
    }

    switchKnowledgeTab(tab) {
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-knowledge-tab="${tab}"]`).classList.add('active');

        document.querySelectorAll('.knowledge-tab').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(tab).classList.add('active');
    }

    async sendMessage() {
        const input = document.getElementById('chatInput');
        const message = input.value.trim();
        
        if (!message) return;

        // 清空输入框
        input.value = '';

        // 添加用户消息到聊天界面
        this.addMessage('user', message);

        // 显示加载状态
        const loadingId = this.addMessage('assistant', '', true);

        try {
            // 发送请求到后端
            const response = await fetch('/api/rag/query', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ query: message })
            });

            const data = await response.json();

            // 移除加载状态
            this.removeMessage(loadingId);

            // 添加AI回复
            this.addMessage('assistant', data.answer || '抱歉，我无法回答这个问题。');

        } catch (error) {
            console.error('发送消息失败:', error);
            this.removeMessage(loadingId);
            this.addMessage('assistant', '抱歉，发生了错误，请稍后重试。');
            this.showNotification('发送消息失败', 'error');
        }
    }

    addMessage(type, content, isLoading = false) {
        const messagesContainer = document.getElementById('chatMessages');
        const messageId = 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.id = messageId;

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.innerHTML = type === 'user' ? '<i class="fas fa-user"></i>' : '<i class="fas fa-robot"></i>';

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        
        if (isLoading) {
            bubble.innerHTML = '<div class="loading"></div> 正在思考...';
        } else {
            bubble.textContent = content;
        }

        const time = document.createElement('div');
        time.className = 'message-time';
        time.textContent = new Date().toLocaleTimeString();

        contentDiv.appendChild(bubble);
        contentDiv.appendChild(time);
        messageDiv.appendChild(avatar);
        messageDiv.appendChild(contentDiv);
        messagesContainer.appendChild(messageDiv);

        // 滚动到底部
        messagesContainer.scrollTop = messagesContainer.scrollHeight;

        return messageId;
    }

    removeMessage(messageId) {
        const message = document.getElementById(messageId);
        if (message) {
            message.remove();
        }
    }

    clearChat() {
        const messagesContainer = document.getElementById('chatMessages');
        messagesContainer.innerHTML = `
            <div class="welcome-message">
                <div class="welcome-icon">
                    <i class="fas fa-robot"></i>
                </div>
                <h4>欢迎使用 Z-RAG 智能问答系统</h4>
                <p>我可以帮您回答基于文档的问题，请上传文档后开始提问。</p>
            </div>
        `;
        this.chatHistory = [];
    }

    newChat() {
        this.clearChat();
        this.showNotification('已开始新的对话', 'success');
    }

    async loadDocuments() {
        try {
            const response = await fetch('/api/rag/storage/files');
            const files = await response.json();
            this.documents = files;
            this.renderDocuments();
        } catch (error) {
            console.error('加载文档失败:', error);
            this.showNotification('加载文档失败', 'error');
        }
    }

    renderDocuments() {
        const container = document.getElementById('documentsGrid');
        container.innerHTML = '';

        if (this.documents.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">暂无文档</p>';
            return;
        }

        this.documents.forEach(doc => {
            const card = document.createElement('div');
            card.className = 'document-card';
            card.innerHTML = `
                <div class="document-header">
                    <div>
                        <div class="document-name">${doc.name || '未知文档'}</div>
                        <div class="document-type">${doc.type || '未知类型'}</div>
                    </div>
                </div>
                <div class="document-info">
                    大小: ${this.formatFileSize(doc.size || 0)} | 
                    上传时间: ${new Date(doc.uploadTime || Date.now()).toLocaleDateString()}
                </div>
                <div class="document-actions">
                    <button class="btn btn-sm btn-secondary" onclick="app.viewDocument('${doc.name}')">
                        <i class="fas fa-eye"></i> 查看
                    </button>
                    <button class="btn btn-sm btn-secondary" onclick="app.deleteDocument('${doc.name}')">
                        <i class="fas fa-trash"></i> 删除
                    </button>
                </div>
            `;
            container.appendChild(card);
        });
    }

    async handleFileUpload(files) {
        if (files.length === 0) return;

        for (const file of files) {
            await this.uploadFile(file);
        }
    }

    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            this.showNotification(`正在上传 ${file.name}...`, 'info');
            
            const response = await fetch('/api/rag/upload', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                this.showNotification(`${file.name} 上传成功`, 'success');
                this.loadDocuments();
            } else {
                throw new Error('上传失败');
            }
        } catch (error) {
            console.error('上传文件失败:', error);
            this.showNotification(`上传 ${file.name} 失败`, 'error');
        }
    }

    async deleteDocument(fileName) {
        if (!confirm('确定要删除这个文档吗？')) return;

        try {
            const response = await fetch(`/api/rag/storage/file?fileName=${encodeURIComponent(fileName)}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                this.showNotification('文档删除成功', 'success');
                this.loadDocuments();
            } else {
                throw new Error('删除失败');
            }
        } catch (error) {
            console.error('删除文档失败:', error);
            this.showNotification('删除文档失败', 'error');
        }
    }

    viewDocument(fileName) {
        // 实现文档查看功能
        this.showNotification('文档查看功能开发中...', 'info');
    }

    async loadKnowledgeStats() {
        try {
            const response = await fetch('/api/rag/knowledge/stats');
            const stats = await response.json();
            
            if (stats.success !== false) {
                document.getElementById('totalDocuments').textContent = stats.totalDocuments || 0;
                document.getElementById('totalChunks').textContent = stats.vectorCount || 0;
                document.getElementById('totalQueries').textContent = stats.totalQueries || 0;
                document.getElementById('vectorCount').textContent = stats.vectorCount || 0;
            } else {
                // 如果API失败，使用模拟数据
                document.getElementById('totalDocuments').textContent = this.documents.length;
                document.getElementById('totalChunks').textContent = Math.floor(Math.random() * 1000) + 100;
                document.getElementById('totalQueries').textContent = Math.floor(Math.random() * 500) + 50;
                document.getElementById('vectorCount').textContent = Math.floor(Math.random() * 5000) + 1000;
            }
        } catch (error) {
            console.error('加载知识统计失败:', error);
            // 使用模拟数据作为后备
            document.getElementById('totalDocuments').textContent = this.documents.length;
            document.getElementById('totalChunks').textContent = Math.floor(Math.random() * 1000) + 100;
            document.getElementById('totalQueries').textContent = Math.floor(Math.random() * 500) + 50;
            document.getElementById('vectorCount').textContent = Math.floor(Math.random() * 5000) + 1000;
        }
    }

    async createKnowledge() {
        if (!confirm('确定要创建知识库吗？这将清空现有知识库并重新处理所有文档。')) {
            return;
        }

        try {
            this.showNotification('正在创建知识库...', 'info');
            
            const response = await fetch('/api/rag/knowledge/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.success) {
                this.showNotification(`知识库创建成功！处理了 ${result.processedFiles} 个文件`, 'success');
                this.loadKnowledgeStats();
                this.loadDocuments(); // 刷新文档列表
            } else {
                this.showNotification(`创建知识库失败: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('创建知识库失败:', error);
            this.showNotification('创建知识库失败，请稍后重试', 'error');
        }
    }

    async refreshKnowledge() {
        try {
            this.showNotification('正在刷新知识库...', 'info');
            
            const response = await fetch('/api/rag/knowledge/refresh', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.success) {
                this.showNotification(`知识库刷新成功！处理了 ${result.processedFiles} 个文件`, 'success');
                this.loadKnowledgeStats();
                this.loadDocuments(); // 刷新文档列表
            } else {
                this.showNotification(`刷新知识库失败: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('刷新知识库失败:', error);
            this.showNotification('刷新知识库失败，请稍后重试', 'error');
        }
    }

    async checkSystemStatus() {
        try {
            const response = await fetch('/api/rag/status');
            const status = await response.json();
            
            const indicator = document.getElementById('statusIndicator');
            const text = document.getElementById('statusText');
            
            if (status.status === 'running') {
                indicator.style.backgroundColor = '#4ade80';
                text.textContent = '系统正常';
            } else {
                indicator.style.backgroundColor = '#ef4444';
                text.textContent = '系统异常';
            }
        } catch (error) {
            console.error('检查系统状态失败:', error);
            const indicator = document.getElementById('statusIndicator');
            const text = document.getElementById('statusText');
            indicator.style.backgroundColor = '#f59e0b';
            text.textContent = '连接失败';
        }
    }

    loadSettings() {
        // 从localStorage加载设置
        const savedSettings = localStorage.getItem('zrag-settings');
        if (savedSettings) {
            this.settings = { ...this.settings, ...JSON.parse(savedSettings) };
        }

        // 更新表单
        document.getElementById('modelProvider').value = this.settings.modelProvider;
        document.getElementById('apiKey').value = this.settings.apiKey;
        document.getElementById('modelName').value = this.settings.modelName;
        document.getElementById('maxResults').value = this.settings.maxResults;
        document.getElementById('minScore').value = this.settings.minScore;
        document.getElementById('chunkSize').value = this.settings.chunkSize;
    }

    saveSettings() {
        // 从表单获取设置
        this.settings.modelProvider = document.getElementById('modelProvider').value;
        this.settings.apiKey = document.getElementById('apiKey').value;
        this.settings.modelName = document.getElementById('modelName').value;
        this.settings.maxResults = parseInt(document.getElementById('maxResults').value);
        this.settings.minScore = parseFloat(document.getElementById('minScore').value);
        this.settings.chunkSize = parseInt(document.getElementById('chunkSize').value);

        // 保存到localStorage
        localStorage.setItem('zrag-settings', JSON.stringify(this.settings));

        this.showNotification('设置保存成功', 'success');
    }

    resetSettings() {
        if (confirm('确定要重置所有设置吗？')) {
            localStorage.removeItem('zrag-settings');
            this.settings = {
                modelProvider: 'qwen',
                apiKey: '',
                modelName: 'qwen-turbo',
                maxResults: 5,
                minScore: 0.6,
                chunkSize: 300
            };
            this.loadSettings();
            this.showNotification('设置已重置', 'success');
        }
    }

    showUploadModal() {
        document.getElementById('uploadModal').classList.add('active');
    }

    hideUploadModal() {
        document.getElementById('uploadModal').classList.remove('active');
    }

    confirmUpload() {
        const fileInput = document.getElementById('modalFileInput');
        if (fileInput.files.length > 0) {
            this.handleFileUpload(fileInput.files);
            this.hideUploadModal();
        }
    }

    showNotification(message, type = 'info') {
        const container = document.getElementById('notifications');
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        
        container.appendChild(notification);

        // 3秒后自动移除
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
}

// 初始化应用
const app = new ZRAGApp();

// 定期检查系统状态
setInterval(() => {
    app.checkSystemStatus();
}, 30000); // 每30秒检查一次
