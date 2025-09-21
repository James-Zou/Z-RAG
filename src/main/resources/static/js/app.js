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

        // 分页相关状态
        this.currentPage = 0;
        this.pageSize = 12;
        this.totalPages = 0;
        this.totalElements = 0;
        this.sortBy = 'lastModified';
        this.sortOrder = 'desc';
        this.searchKeyword = '';

        // 知识管理相关状态
        this.currentKnowledgeTab = 'chunks';
        this.knowledgeChunks = [];
        this.knowledgeChunksPage = 0;
        this.knowledgeChunksSize = 20;
        this.vectorData = {};
        this.analyticsData = {};

        this.init();
        this.bindThemeSwitch();
    }

    init() {
        this.bindEvents();
        this.bindReferencesPanelEvents();
        this.initMobileMenu();
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
            this.currentPage = 0;
            this.loadDocuments();
        });

        // 搜索功能
        const searchInput = document.getElementById('searchDocuments');
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.searchKeyword = e.target.value;
                    this.currentPage = 0;
                    this.loadDocuments();
                }, 500);
            });
        }

        // 排序功能
        const sortSelect = document.getElementById('sortDocuments');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                const value = e.target.value;
                if (value === 'name') {
                    this.sortBy = 'originalName';
                } else if (value === 'date') {
                    this.sortBy = 'lastModified';
                } else if (value === 'size') {
                    this.sortBy = 'size';
                }
                this.currentPage = 0;
                this.loadDocuments();
            });
        }

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

        // 知识管理标签页切换
        document.querySelectorAll('[data-knowledge-tab]').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const tabName = e.target.getAttribute('data-knowledge-tab');
                this.switchKnowledgeTab(tabName);
            });
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

    initMobileMenu() {
        const mobileMenuToggle = document.getElementById('mobileMenuToggle');
        const sidebar = document.getElementById('sidebar');
        const sidebarClose = document.getElementById('sidebarClose');
        const mobileOverlay = document.getElementById('mobileOverlay');

        if (mobileMenuToggle) {
            mobileMenuToggle.addEventListener('click', () => {
                this.toggleMobileMenu();
            });
        }

        if (sidebarClose) {
            sidebarClose.addEventListener('click', () => {
                this.closeMobileMenu();
            });
        }

        if (mobileOverlay) {
            mobileOverlay.addEventListener('click', () => {
                this.closeMobileMenu();
            });
        }

        // 监听窗口大小变化，自动关闭移动端菜单
        window.addEventListener('resize', () => {
            if (window.innerWidth > 768) {
                this.closeMobileMenu();
            }
        });

        // 监听导航项点击，在移动端自动关闭菜单
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', () => {
                if (window.innerWidth <= 768) {
                    this.closeMobileMenu();
                }
            });
        });
    }

    toggleMobileMenu() {
        const sidebar = document.getElementById('sidebar');
        const mobileOverlay = document.getElementById('mobileOverlay');

        if (sidebar && mobileOverlay) {
            sidebar.classList.toggle('show');
            mobileOverlay.classList.toggle('active');

            // 防止背景滚动
            if (sidebar.classList.contains('show')) {
                document.body.style.overflow = 'hidden';
            } else {
                document.body.style.overflow = '';
            }
        }
    }

    closeMobileMenu() {
        const sidebar = document.getElementById('sidebar');
        const mobileOverlay = document.getElementById('mobileOverlay');

        if (sidebar && mobileOverlay) {
            sidebar.classList.remove('show');
            mobileOverlay.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    bindThemeSwitch() {
        // 绑定主题切换功能
        window.switchToStock = () => {
            if (confirm('确定要切换到股神投资知识平台吗？')) {
                window.location.href = '/stock.html';
            }
        };
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
            this.loadKnowledgeTabContent(); // 加载当前知识管理标签页内容
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
        console.log('=== 使用新版本的sendMessage方法 ===');
        const input = document.getElementById('chatInput');
        const sendButton = document.getElementById('sendMessage');
        const message = input.value.trim();

        if (!message) return;

        // 禁用输入框和发送按钮
        this.setChatInputEnabled(false);

        // 清空输入框
        input.value = '';

        // 添加用户消息到聊天界面
        this.addMessage('user', message);

        // 创建AI回复消息
        const aiMessageId = this.addMessage('assistant', '', true); // 设置为loading状态
        console.log('创建的AI消息ID:', aiMessageId);

        try {
            // 使用流式端点
            const response = await fetch('/api/rag/query/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ query: message })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // 使用setTimeout确保DOM更新完成，然后处理流式响应
            setTimeout(() => {
                const aiMessageElement = document.getElementById(aiMessageId);
                console.log('AI消息元素:', aiMessageElement);
                const aiBubble = aiMessageElement ? aiMessageElement.querySelector('.message-bubble') : null;
                console.log('AI气泡元素:', aiBubble);
                console.log('AI气泡初始内容:', aiBubble ? aiBubble.innerHTML : 'null');

                if (!aiBubble) {
                    console.error('无法找到AI气泡元素');
                    return;
                }

                // 清空气泡内容，准备接收流式响应
                aiBubble.innerHTML = '<div class="loading"></div> 正在思考...';
                console.log('清空后的AI气泡内容:', aiBubble.innerHTML);

                // 检查页面上是否有多个AI消息
                const allAiMessages = document.querySelectorAll('.message.assistant');
                console.log('页面上AI消息数量:', allAiMessages.length);
                allAiMessages.forEach((msg, index) => {
                    const bubble = msg.querySelector('.message-bubble');
                    console.log(`AI消息${index + 1}内容:`, bubble ? bubble.innerHTML : 'null');
                });

                // 处理流式响应
                this.processStreamingResponse(response, aiMessageId);
            }, 10);

        } catch (error) {
            console.error('发送消息失败:', error);
            this.removeMessage(aiMessageId);
            this.addMessage('assistant', '抱歉，发生了错误，请稍后重试。');
            this.showNotification('发送消息失败', 'error');
            this.setChatInputEnabled(true); // 重新启用输入
        }
    }

    async processStreamingResponse(response, aiMessageId) {
        console.log('processStreamingResponse接收到的aiMessageId:', aiMessageId);

        try {
            // 处理流式响应
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();

                if (done) break;

                buffer += decoder.decode(value, { stream: true });

                // 处理SSE格式的数据
                const lines = buffer.split('\n');
                buffer = lines.pop() || ''; // 保留最后一个不完整的行

                for (const line of lines) {
                    if (line.trim() === '') continue;

                    // 处理SSE格式: data: {...}
                    if (line.startsWith('data: ')) {
                        try {
                            const jsonStr = line.substring(6); // 移除 "data: " 前缀
                            console.log('解析SSE数据:', jsonStr);
                            const data = JSON.parse(jsonStr);
                            this.handleStreamingResponse(data, aiMessageId);
                        } catch (e) {
                            console.warn('解析SSE流式响应失败:', line, e);
                        }
                    } else if (line.startsWith('data:')) {
                        // 处理没有空格的 data: 格式
                        try {
                            const jsonStr = line.substring(5); // 移除 "data:" 前缀
                            console.log('解析data数据:', jsonStr);
                            const data = JSON.parse(jsonStr);
                            this.handleStreamingResponse(data, aiMessageId);
                        } catch (e) {
                            console.warn('解析data流式响应失败:', line, e);
                        }
                    } else {
                        // 如果不是SSE格式，尝试直接解析JSON
                        try {
                            console.log('尝试直接解析JSON:', line);
                            const data = JSON.parse(line);
                            this.handleStreamingResponse(data, aiMessageId);
                        } catch (e) {
                            console.warn('解析JSON流式响应失败:', line, e);
                        }
                    }
                }
            }
        } catch (error) {
            console.error('处理流式响应失败:', error);
            // 显示错误信息
            const aiMessageElement = document.getElementById(aiMessageId);
            if (aiMessageElement) {
                const aiBubble = aiMessageElement.querySelector('.message-bubble');
                if (aiBubble) {
                    aiBubble.innerHTML = '<span style="color: #ef4444;">❌ 处理响应时发生错误，请稍后重试</span>';
                }
            }
            // 重新启用输入
            this.setChatInputEnabled(true);
        }
    }

    handleStreamingResponse(data, aiMessageId) {
        console.log('收到流式响应:', data);
        console.log('handleStreamingResponse接收到的aiMessageId:', aiMessageId);

        // 获取AI消息元素
        const aiMessageElement = document.getElementById(aiMessageId);
        if (!aiMessageElement) {
            console.error('无法找到AI消息元素:', aiMessageId);
            return;
        }

        const aiBubble = aiMessageElement.querySelector('.message-bubble');
        if (!aiBubble) {
            console.error('无法找到AI气泡元素');
            return;
        }

        console.log('handleStreamingResponse中aiBubble内容:', aiBubble.innerHTML);

        const { type, content, finished } = data;

        // 确保content存在且不为空
        if (!content) {
            console.warn('流式响应内容为空:', data);
            return;
        }

        // 更新状态信息，不覆盖已有内容
        const updateStatusContent = (newContent) => {
            // 查找或创建状态容器
            let statusContainer = aiBubble.querySelector('.status-container');
            if (!statusContainer) {
                statusContainer = document.createElement('div');
                statusContainer.className = 'status-container';
                aiBubble.appendChild(statusContainer);
            }

            // 更新状态内容
            statusContainer.innerHTML = `<div class="loading"></div> ${newContent}`;
            console.log('DOM更新后状态内容:', statusContainer.innerHTML);

            // 强制浏览器重绘
            statusContainer.style.display = 'none';
            statusContainer.offsetHeight; // 触发重排
            statusContainer.style.display = '';

            // 滚动到底部
            this.scrollToBottom();
        };

        switch (type) {
            case 'thinking':
                console.log(`[THINKING] 准备更新内容: ${content}`);
                updateStatusContent(content);
                console.log('[THINKING] 更新完成');
                break;
            case 'retrieval':
                console.log(`[RETRIEVAL] 准备更新内容: ${content}`);
                updateStatusContent(content);
                console.log('[RETRIEVAL] 更新完成');
                break;
            case 'rerank':
                console.log(`[RERANK] 准备更新内容: ${content}`);
                updateStatusContent(content);
                console.log('[RERANK] 更新完成');
                break;
            case 'generation':
                console.log(`[GENERATION] 准备更新内容: ${content}`);
                updateStatusContent(content);
                console.log('[GENERATION] 更新完成');
                break;
            case 'references':
                console.log(`[REFERENCES] 准备更新内容: ${content}`);
                // 将引用信息显示到右侧固定区域
                this.updateReferencesPanel(content);
                console.log('[REFERENCES] 更新完成');
                break;
            case 'documentDetails':
                console.log(`[DOCUMENT_DETAILS] 准备处理文档详情: ${content}`);
                console.log(`[DOCUMENT_DETAILS] 文档详情内容长度: ${content.length}`);
                try {
                    const parsedContent = JSON.parse(content);
                    console.log(`[DOCUMENT_DETAILS] 解析后的文档数量: ${parsedContent.length}`);
                } catch (e) {
                    console.error(`[DOCUMENT_DETAILS] JSON解析失败:`, e);
                }
                // 将文档详情显示到右侧固定区域
                this.updateReferencesPanel(null, content);
                console.log('[DOCUMENT_DETAILS] 处理完成');
                break;
            case 'answer':
                console.log(`[ANSWER] 准备逐字显示内容: ${content.substring(0, 50)}...`);
                // 最终答案，移除loading状态并实现逐字显示
                this.displayAnswerWithStreaming(aiBubble, content);
                console.log('[ANSWER] 开始逐字显示');
                // 逐字显示完成后重新启用输入
                setTimeout(() => {
                    this.setChatInputEnabled(true);
                }, content.length * 50 + 1000); // 根据内容长度计算完成时间
                break;
            case 'error':
                console.log(`[ERROR] 准备更新内容: ${content}`);
                updateStatusContent(`<span style="color: #ef4444;">❌ ${content}</span>`);
                console.log('[ERROR] 更新完成');
                break;
            default:
                console.log(`[UNKNOWN] 未知类型 ${type}, 内容: ${content}`);
                // 如果是未知类型但有内容，更新状态
                if (content) {
                    updateStatusContent(content);
                    console.log('[UNKNOWN] 更新完成');
                }
        }
    }

    displayAnswerWithStreaming(aiBubble, content) {
        console.log('开始逐字显示，内容长度:', content.length);

        // 清空状态容器，但保留其结构
        const statusContainer = aiBubble.querySelector('.status-container');
        if (statusContainer) {
            statusContainer.innerHTML = '';
        }

        // 查找是否已存在answer-content容器
        let textContainer = aiBubble.querySelector('.answer-content');
        if (!textContainer) {
            // 创建文本容器
            textContainer = document.createElement('div');
            textContainer.className = 'answer-content typing'; // 添加typing类显示光标
            aiBubble.appendChild(textContainer);
        } else {
            // 如果已存在，清空内容并添加typing类
            textContainer.innerHTML = '';
            textContainer.className = 'answer-content typing';
        }

        // 立即清除loading状态
        const loadingElement = aiBubble.querySelector('.loading');
        if (loadingElement) {
            loadingElement.remove();
        }

        // 逐字显示文本
        let index = 0;
        const speed = 50; // 每50ms显示一个字符

        const typeWriter = () => {
            if (index < content.length) {
                // 处理Markdown格式
                const currentText = content.substring(0, index + 1);
                textContainer.innerHTML = this.renderMarkdown(currentText);
                console.log(`逐字显示进度: ${index + 1}/${content.length}`);
                index++;

                // 滚动到底部
                this.scrollToBottom();

                setTimeout(typeWriter, speed);
            } else {
                console.log('逐字显示完成');
                // 完成后移除光标
                textContainer.classList.remove('typing');

                // 确保所有loading状态都被清除
                const allLoadingElements = aiBubble.querySelectorAll('.loading');
                allLoadingElements.forEach(element => element.remove());

                // 确保状态容器被清空
                if (statusContainer) {
                    statusContainer.innerHTML = '';
                }
            }
        };

        // 立即开始
        typeWriter();
    }

    setChatInputEnabled(enabled) {
        const input = document.getElementById('chatInput');
        const sendButton = document.getElementById('sendMessage');

        if (enabled) {
            input.disabled = false;
            sendButton.disabled = false;
            input.placeholder = '请输入您的问题...';
            sendButton.innerHTML = '<i class="fas fa-paper-plane"></i>';
            // 重新聚焦到输入框，但不影响页面滚动
            input.focus({ preventScroll: true });
        } else {
            input.disabled = true;
            sendButton.disabled = true;
            input.placeholder = 'AI正在思考中...';
            sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            // 失去焦点但不影响页面滚动
            input.blur();
        }
    }

    scrollToBottom() {
        const messagesContainer = document.getElementById('chatMessages');
        if (messagesContainer) {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    }

    // 更新右侧引用信息面板
    updateReferencesPanel(referencesContent, documentDetailsContent) {
        const referencesContentElement = document.getElementById('referencesContent');
        if (!referencesContentElement) {
            console.error('无法找到引用内容区域');
            return;
        }

        let html = '';

        // 处理引用来源
        if (referencesContent) {
            html += '<div class="references-section">';
            html += this.renderMarkdown(referencesContent);
            html += '</div>';
        }

        // 处理文档详情
        if (documentDetailsContent) {
            try {
                const documents = JSON.parse(documentDetailsContent);
                html += '<div class="document-details-section">';
                html += '<h5><i class="fas fa-file-alt"></i> 相关文档详情</h5>';
                html += '<div class="document-list">';

                documents.forEach((doc, index) => {
                    const fileName = doc.fileName || '未知文档';
                    const fileId = doc.fileId || '';
                    const chunkIndex = doc.chunkIndex || '';
                    const textPreview = doc.textPreview || '';

                    html += `
                        <div class="document-item" data-file-id="${fileId}" data-chunk-index="${chunkIndex}">
                            <div class="document-header">
                                <i class="fas fa-file-pdf"></i>
                                <span class="document-name">${fileName}</span>
                                ${chunkIndex ? `<span class="chunk-info">片段 ${chunkIndex}</span>` : ''}
                            </div>
                            <div class="document-preview">${textPreview}</div>
                            <div class="document-actions">
                                <button class="btn-view-detail" onclick="app.viewDocumentDetail('${fileId}', '${fileName}', '${chunkIndex}')">
                                    <i class="fas fa-eye"></i> 查看详情
                                </button>
                                <button class="btn-download" onclick="app.downloadDocument('${fileId}', '${fileName}')">
                                    <i class="fas fa-download"></i> 下载
                                </button>
                            </div>
                        </div>
                    `;
                });

                html += '</div></div>';
            } catch (error) {
                console.error('解析文档详情失败:', error);
            }
        }

        // 如果没有内容，显示默认信息
        if (!html) {
            html = '<div class="no-references"><i class="fas fa-info-circle"></i><p>暂无引用信息</p></div>';
        }

        referencesContentElement.innerHTML = html;

        // 显示引用面板
        const referencesPanel = document.getElementById('referencesPanel');
        if (referencesPanel) {
            referencesPanel.classList.add('has-content');
        }
    }

    handleDocumentDetails(content, aiMessageId) {
        // 这个方法现在被 updateReferencesPanel 替代，保留以防其他地方调用
        this.updateReferencesPanel(null, content);
    }

    // 绑定引用面板事件
    bindReferencesPanelEvents() {
        const toggleButton = document.getElementById('toggleReferences');
        const referencesPanel = document.getElementById('referencesPanel');

        if (toggleButton && referencesPanel) {
            toggleButton.addEventListener('click', () => {
                const isCollapsed = referencesPanel.classList.contains('collapsed');
                const icon = toggleButton.querySelector('i');

                if (isCollapsed) {
                    referencesPanel.classList.remove('collapsed');
                    icon.className = 'fas fa-chevron-right';
                } else {
                    referencesPanel.classList.add('collapsed');
                    icon.className = 'fas fa-chevron-left';
                }
            });
        }
    }

    viewDocumentDetail(fileId, fileName, chunkIndex) {
        // 创建文档详情模态框
        const modal = document.createElement('div');
        modal.className = 'document-modal';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3><i class="fas fa-file-alt"></i> ${fileName}</h3>
                    <button class="modal-close" onclick="this.closest('.document-modal').remove()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="document-info">
                        <p><strong>文档ID:</strong> ${fileId}</p>
                        ${chunkIndex ? `<p><strong>片段索引:</strong> ${chunkIndex}</p>` : ''}
                    </div>
                    <div class="document-content">
                        <h4>文档内容预览</h4>
                        <div class="content-preview">
                            <p>正在加载文档内容...</p>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn-download" onclick="app.downloadDocument('${fileId}', '${fileName}')">
                        <i class="fas fa-download"></i> 下载完整文档
                    </button>
                    <button class="btn-close" onclick="this.closest('.document-modal').remove()">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // 加载文档内容（这里可以调用后端API获取完整内容）
        this.loadDocumentContent(fileId, chunkIndex, modal.querySelector('.content-preview'));
    }

    loadDocumentContent(fileId, chunkIndex, container) {
        // 这里可以调用后端API获取文档的完整内容
        // 暂时显示占位内容
        container.innerHTML = `
            <p>文档ID: ${fileId}</p>
            ${chunkIndex ? `<p>片段: ${chunkIndex}</p>` : ''}
            <p><em>文档内容加载功能需要后端API支持</em></p>
        `;
    }

    downloadDocument(fileId, fileName) {
        // 创建下载链接
        const downloadUrl = `/api/rag/download/${fileId}`;
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = fileName;
        link.click();
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
            // 检查内容是否包含HTML标签
            if (content.includes('<') && content.includes('>')) {
                bubble.innerHTML = content;
            } else {
                bubble.textContent = content;
            }
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
        this.scrollToBottom();

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
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortOrder: this.sortOrder,
                search: this.searchKeyword || ''
            });

            const response = await fetch(`/api/rag/storage/files/paged?${params}`);
            const data = await response.json();

            if (data.error) {
                throw new Error(data.error);
            }

            this.documents = data.content || [];
            this.totalPages = data.totalPages || 0;
            this.totalElements = data.totalElements || 0;

            console.log('加载文档数据:', {
                documentsCount: this.documents.length,
                totalPages: this.totalPages,
                currentPage: this.currentPage,
                totalElements: this.totalElements
            });

            this.renderDocuments();
            this.renderPagination();
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
                        <div class="document-name" title="${doc.originalName || doc.name || '未知文档'}">${doc.originalName || doc.name || '未知文档'}</div>
                        <div class="document-type">${doc.type || '未知类型'}</div>
                    </div>
                </div>
                <div class="document-info">
                    大小: ${this.formatFileSize(doc.size || 0)} | 
                    上传时间: ${new Date(doc.lastModified || doc.uploadTime || Date.now()).toLocaleDateString()}
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

    renderPagination() {
        const container = document.getElementById('paginationContainer');
        console.log('分页容器元素:', container);
        if (!container) {
            console.error('分页容器未找到！');
            return;
        }

        console.log('渲染分页控件:', {
            totalPages: this.totalPages,
            currentPage: this.currentPage,
            totalElements: this.totalElements
        });

        if (this.totalPages <= 1) {
            container.innerHTML = '<p style="color: #6b7280; text-align: center;">只有一页数据，无需显示分页控件</p>';
            return;
        }

        let paginationHTML = '<div class="pagination">';

        // 上一页按钮
        paginationHTML += `<button class="btn btn-sm ${this.currentPage === 0 ? 'btn-disabled' : 'btn-secondary'}" 
            ${this.currentPage === 0 ? 'disabled' : ''} onclick="app.goToPage(${this.currentPage - 1})">
            <i class="fas fa-chevron-left"></i> 上一页
        </button>`;

        // 页码按钮
        const startPage = Math.max(0, this.currentPage - 2);
        const endPage = Math.min(this.totalPages - 1, this.currentPage + 2);

        if (startPage > 0) {
            paginationHTML += `<button class="btn btn-sm btn-secondary" onclick="app.goToPage(0)">1</button>`;
            if (startPage > 1) {
                paginationHTML += '<span class="pagination-ellipsis">...</span>';
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `<button class="btn btn-sm ${i === this.currentPage ? 'btn-primary' : 'btn-secondary'}" 
                onclick="app.goToPage(${i})">${i + 1}</button>`;
        }

        if (endPage < this.totalPages - 1) {
            if (endPage < this.totalPages - 2) {
                paginationHTML += '<span class="pagination-ellipsis">...</span>';
            }
            paginationHTML += `<button class="btn btn-sm btn-secondary" onclick="app.goToPage(${this.totalPages - 1})">${this.totalPages}</button>`;
        }

        // 下一页按钮
        paginationHTML += `<button class="btn btn-sm ${this.currentPage >= this.totalPages - 1 ? 'btn-disabled' : 'btn-secondary'}" 
            ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''} onclick="app.goToPage(${this.currentPage + 1})">
            下一页 <i class="fas fa-chevron-right"></i>
        </button>`;

        paginationHTML += '</div>';

        // 添加分页信息
        paginationHTML += `<div class="pagination-info">
            共 ${this.totalElements} 个文档，第 ${this.currentPage + 1} 页，共 ${this.totalPages} 页
        </div>`;

        container.innerHTML = paginationHTML;

        console.log('分页HTML内容:', paginationHTML);
        console.log('分页容器内容长度:', container.innerHTML.length);
        console.log('分页容器是否可见:', container.offsetHeight > 0);
    }

    goToPage(page) {
        if (page < 0 || page >= this.totalPages || page === this.currentPage) {
            return;
        }
        this.currentPage = page;
        this.loadDocuments();
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

    async viewDocument(fileName) {
        try {
            this.showNotification('正在加载文件预览...', 'info');

            const response = await fetch(`/api/rag/storage/preview?fileName=${encodeURIComponent(fileName)}`);
            const data = await response.json();

            if (data.success) {
                this.showPreviewModal(data);
            } else {
                this.showNotification(`预览失败: ${data.message}`, 'error');
            }
        } catch (error) {
            console.error('预览文档失败:', error);
            this.showNotification('预览文档失败', 'error');
        }
    }

    showPreviewModal(fileData) {
        console.log('显示预览模态框:', fileData);

        // 创建预览模态框
        const modal = document.createElement('div');
        modal.className = 'modal preview-modal';
        modal.id = 'previewModal';

        const previewContent = this.renderPreviewContent(fileData);
        console.log('渲染的预览内容:', previewContent);

        modal.innerHTML = `
            <div class="modal-content preview-content">
                <div class="modal-header">
                    <h3>文件预览 - ${fileData.fileName}</h3>
                    <button class="modal-close" onclick="app.closePreviewModal()">&times;</button>
                </div>
                <div class="modal-body preview-body">
                    <div class="file-info">
                        <span class="file-type">${fileData.fileType}</span>
                        <span class="file-size">${this.formatFileSize(fileData.content.length)}</span>
                    </div>
                    <div class="preview-container" id="previewContainer">
                        ${previewContent}
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="app.closePreviewModal()">关闭</button>
                    <button class="btn btn-primary" onclick="app.downloadFile('${fileData.fileName}')">下载</button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        modal.classList.add('active');

        // 如果是PDF文件，使用简化的预览方式
        if (fileData.previewType === 'pdf') {
            setTimeout(() => {
                const container = document.getElementById('previewContainer');
                const pdfContainer = container.querySelector('.pdf-viewer');
                if (pdfContainer) {
                    // 显示PDF信息而不是尝试渲染
                    pdfContainer.innerHTML = `
                        <div class="pdf-info">
                            <div class="pdf-icon">
                                <i class="fas fa-file-pdf" style="font-size: 4rem; color: #dc2626;"></i>
                            </div>
                            <h4>PDF 文档预览</h4>
                            <p>文件大小: ${this.formatFileSize(fileData.content.length)}</p>
                            <p>由于PDF文件较大，建议下载后查看完整内容</p>
                            <div class="pdf-actions">
                                <button class="btn btn-primary" onclick="app.downloadFile('${fileData.fileName}')">
                                    <i class="fas fa-download"></i> 下载 PDF
                                </button>
                                <button class="btn btn-secondary" onclick="app.openPdfInNewTab('${fileData.fileName}')">
                                    <i class="fas fa-external-link-alt"></i> 新窗口打开
                                </button>
                            </div>
                        </div>
                    `;
                }
            }, 100);
        }

        // 检查内容是否正确插入
        setTimeout(() => {
            const container = document.getElementById('previewContainer');
            console.log('预览容器内容:', container ? container.innerHTML : '容器未找到');
        }, 100);
    }

    renderPreviewContent(fileData) {
        const { previewType, content, fileName } = fileData;

        switch (previewType) {
            case 'markdown':
                return `<div class="markdown-preview">${this.renderMarkdown(content)}</div>`;
            case 'text':
                return `<pre class="text-preview">${this.escapeHtml(content)}</pre>`;
            case 'image':
                return `<img src="data:image/png;base64,${content}" alt="${fileName}" class="image-preview">`;
            case 'pdf':
                return this.renderPdfPreview(content, fileName);
            case 'word':
                console.log('渲染 Word 文档，内容长度:', content.length);
                return `<div class="word-preview-container">
                    <div class="word-viewer">${content}</div>
                    <div class="word-controls">
                        <button class="btn btn-sm btn-secondary" onclick="app.downloadFile('${fileName}')">
                            <i class="fas fa-download"></i> 下载 Word
                        </button>
                    </div>
                </div>`;
            case 'excel':
                return `<div class="excel-preview-container">
                    <div class="excel-viewer">${content}</div>
                    <div class="excel-controls">
                        <button class="btn btn-sm btn-secondary" onclick="app.downloadFile('${fileName}')">
                            <i class="fas fa-download"></i> 下载 Excel
                        </button>
                    </div>
                </div>`;
            case 'office':
                return `<div class="office-preview">
                    <p>Office 文档预览需要下载后查看</p>
                    <p>文件大小: ${this.formatFileSize(content.length)}</p>
                    <button class="btn btn-primary" onclick="app.downloadFile('${fileName}')">下载文档</button>
                </div>`;
            default:
                return `<pre class="text-preview">${this.escapeHtml(content)}</pre>`;
        }
    }

    renderPdfPreview(content, fileName) {
        const containerId = `pdf-container-${Date.now()}`;
        return `
            <div class="pdf-preview-container">
                <div id="${containerId}" class="pdf-viewer">
                    <div class="pdf-loading">正在加载PDF...</div>
                </div>
                <div class="pdf-controls">
                    <button class="btn btn-sm btn-secondary" onclick="app.downloadFile('${fileName}')">
                        <i class="fas fa-download"></i> 下载 PDF
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="app.openPdfInNewTab('${fileName}')">
                        <i class="fas fa-external-link-alt"></i> 新窗口打开
                    </button>
                </div>
            </div>
        `;
    }

    // 单独的方法来渲染PDF，避免在HTML字符串中嵌入大量数据
    async renderPdfToContainer(containerId, pdfData) {
        const container = document.getElementById(containerId);
        if (!container) {
            console.error('PDF容器未找到:', containerId);
            return;
        }

        // 显示加载状态
        container.innerHTML = '<div class="pdf-loading">正在加载PDF...</div>';

        try {
            // 检查PDF数据是否完整
            if (!pdfData || pdfData.length < 100) {
                throw new Error('PDF数据不完整或为空');
            }

            console.log('PDF数据长度:', pdfData.length);

            // 检查PDF.js是否可用
            if (typeof pdfjsLib === 'undefined') {
                throw new Error('PDF.js 库未加载');
            }

            // 将 base64 字符串转换为 Uint8Array
            const binaryString = atob(pdfData);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }

            console.log('PDF二进制数据长度:', bytes.length);

            // 使用 PDF.js 渲染 PDF
            const pdf = await pdfjsLib.getDocument({data: bytes}).promise;
            console.log('PDF加载成功，页数:', pdf.numPages);

            // 渲染第一页
            const page = await pdf.getPage(1);
            const scale = 1.5;
            const viewport = page.getViewport({scale: scale});

            const canvas = document.createElement('canvas');
            const context = canvas.getContext('2d');
            canvas.height = viewport.height;
            canvas.width = viewport.width;
            canvas.style.maxWidth = '100%';
            canvas.style.height = 'auto';
            canvas.style.border = '1px solid #ddd';

            const renderContext = {
                canvasContext: context,
                viewport: viewport
            };

            await page.render(renderContext).promise;
            container.innerHTML = ''; // 清空加载状态
            container.appendChild(canvas);

            // 添加页面导航（如果有多个页面）
            if (pdf.numPages > 1) {
                const pageInfo = document.createElement('div');
                pageInfo.className = 'pdf-page-info';
                pageInfo.innerHTML = `第 1 页，共 ${pdf.numPages} 页`;
                container.appendChild(pageInfo);
            }

            console.log('PDF渲染完成');

        } catch (error) {
            console.error('PDF渲染失败:', error);
            // 显示备用预览方案
            container.innerHTML = `
                <div class="pdf-fallback">
                    <div class="pdf-icon">
                        <i class="fas fa-file-pdf" style="font-size: 4rem; color: #dc2626;"></i>
                    </div>
                    <h4>PDF 文档</h4>
                    <p>PDF 预览需要特殊处理，请下载文件查看完整内容。</p>
                    <div class="pdf-actions">
                        <button class="btn btn-primary" onclick="app.downloadFile('${fileName}')">
                            <i class="fas fa-download"></i> 下载 PDF
                        </button>
                        <button class="btn btn-secondary" onclick="app.openPdfInNewTab('${fileName}')">
                            <i class="fas fa-external-link-alt"></i> 新窗口打开
                        </button>
                    </div>
                </div>
            `;
        }
    }


    renderMarkdown(content) {
        // 改进的 Markdown 渲染
        return content
        // 标题
        .replace(/^### (.*$)/gim, '<h3>$1</h3>')
        .replace(/^## (.*$)/gim, '<h2>$1</h2>')
        .replace(/^# (.*$)/gim, '<h1>$1</h1>')
        // 粗体和斜体
        .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/gim, '<em>$1</em>')
        // 代码块
        .replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>')
        .replace(/`(.*?)`/gim, '<code>$1</code>')
        // 链接
        .replace(/\[([^\]]+)\]\(([^)]+)\)/gim, '<a href="$2" target="_blank">$1</a>')
        // 列表
        .replace(/^\* (.*$)/gim, '<li>$1</li>')
        .replace(/^- (.*$)/gim, '<li>$1</li>')
        .replace(/^\d+\. (.*$)/gim, '<li>$1</li>')
        // 换行
        .replace(/\n\n/gim, '</p><p>')
        .replace(/\n/gim, '<br>')
        // 包装段落
        .replace(/^(?!<[h|p|l|u|o|d|s])/gim, '<p>')
        .replace(/(?<!>)$/gim, '</p>')
        // 清理空段落
        .replace(/<p><\/p>/gim, '')
        .replace(/<p><br><\/p>/gim, '');
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    encodeBase64(content) {
        // 对于图片文件，这里需要特殊处理
        // 这里简化处理，实际应该根据文件类型进行正确的编码
        return btoa(unescape(encodeURIComponent(content)));
    }

    closePreviewModal() {
        const modal = document.getElementById('previewModal');
        if (modal) {
            modal.classList.remove('active');
            setTimeout(() => modal.remove(), 300);
        }
    }

    async downloadFile(fileName) {
        try {
            const response = await fetch(`/api/rag/storage/preview?fileName=${encodeURIComponent(fileName)}`);
            const data = await response.json();

            if (data.success) {
                // 创建下载链接
                const blob = new Blob([data.content], { type: 'application/octet-stream' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = fileName.split('/').pop(); // 获取文件名
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);

                this.showNotification('文件下载开始', 'success');
            } else {
                this.showNotification(`下载失败: ${data.message}`, 'error');
            }
        } catch (error) {
            console.error('下载文件失败:', error);
            this.showNotification('下载文件失败', 'error');
        }
    }

    openPdfInNewTab(fileName) {
        // 直接在新窗口中打开PDF文件
        const url = `/api/rag/storage/pdf?fileName=${encodeURIComponent(fileName)}`;
        window.open(url, '_blank');
    }

    async loadKnowledgeStats() {
        try {
            const response = await fetch('/api/rag/knowledge/stats');
            const stats = await response.json();

            if (stats.success !== false) {
                document.getElementById('totalDocuments').textContent = stats.totalDocuments || 0;
                document.getElementById('totalChunks').textContent = stats.vectorCount || 0;
                document.getElementById('knowledgeBaseName').textContent = stats.knowledgeBaseName || 'Z-RAG 知识库';
                document.getElementById('vectorCount').textContent = stats.vectorCount || 0;
            } else {
                // 如果API失败，使用模拟数据
                document.getElementById('totalDocuments').textContent = this.documents.length;
                document.getElementById('totalChunks').textContent = Math.floor(Math.random() * 1000) + 100;
                document.getElementById('knowledgeBaseName').textContent = 'Z-RAG 知识库';
                document.getElementById('vectorCount').textContent = Math.floor(Math.random() * 5000) + 1000;
            }
        } catch (error) {
            console.error('加载知识统计失败:', error);
            // 使用模拟数据作为后备
            document.getElementById('totalDocuments').textContent = this.documents.length;
            document.getElementById('totalChunks').textContent = Math.floor(Math.random() * 1000) + 100;
            document.getElementById('knowledgeBaseName').textContent = 'Z-RAG 知识库';
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
                this.loadKnowledgeTabContent(); // 刷新当前标签页内容
            } else {
                this.showNotification(`刷新知识库失败: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('刷新知识库失败:', error);
            this.showNotification('刷新知识库失败，请稍后重试', 'error');
        }
    }

    // 知识管理标签页切换
    switchKnowledgeTab(tabName) {
        // 更新标签页状态
        document.querySelectorAll('[data-knowledge-tab]').forEach(tab => {
            tab.classList.remove('active');
        });
        const knowledgeTab = document.querySelector(`[data-knowledge-tab="${tabName}"]`);
        if (knowledgeTab) {
            knowledgeTab.classList.add('active');
        }

        // 更新内容区域
        document.querySelectorAll('.knowledge-tab').forEach(tab => {
            tab.classList.remove('active');
        });
        const knowledgeContent = document.getElementById(tabName);
        if (knowledgeContent) {
            knowledgeContent.classList.add('active');
        }

        this.currentKnowledgeTab = tabName;
        this.loadKnowledgeTabContent();
    }

    // 加载知识管理标签页内容
    async loadKnowledgeTabContent() {
        switch (this.currentKnowledgeTab) {
            case 'chunks':
                await this.loadKnowledgeChunks();
                break;
            case 'vectors':
                await this.loadVectorData();
                break;
            case 'analytics':
                await this.loadAnalyticsData();
                break;
        }
    }

    // 加载知识片段
    async loadKnowledgeChunks() {
        try {
            const params = new URLSearchParams({
                page: this.knowledgeChunksPage,
                size: this.knowledgeChunksSize
            });

            const response = await fetch(`/api/rag/knowledge/chunks?${params}`);
            const data = await response.json();

            if (data.error) {
                throw new Error(data.error);
            }

            this.knowledgeChunks = data.content || [];
            this.renderKnowledgeChunks();
        } catch (error) {
            console.error('加载知识片段失败:', error);
            this.showNotification('加载知识片段失败', 'error');
        }
    }

    // 渲染知识片段
    renderKnowledgeChunks() {
        const container = document.getElementById('chunksList');
        if (!container) return;

        container.innerHTML = '';

        if (this.knowledgeChunks.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">暂无知识片段</p>';
            return;
        }

        this.knowledgeChunks.forEach(chunk => {
            const chunkElement = document.createElement('div');
            chunkElement.className = 'chunk-item';
            chunkElement.innerHTML = `
                <div class="chunk-header">
                    <div class="chunk-meta">
                        <span class="chunk-id">${chunk.id}</span>
                        <span class="chunk-source">来源: ${chunk.source}</span>
                        <span class="chunk-similarity">相似度: ${(chunk.similarity * 100).toFixed(1)}%</span>
                    </div>
                    <div class="chunk-actions">
                        <button class="btn btn-sm btn-secondary" onclick="app.viewChunk('${chunk.id}')">
                            <i class="fas fa-eye"></i> 查看
                        </button>
                        <button class="btn btn-sm btn-secondary" onclick="app.editChunk('${chunk.id}')">
                            <i class="fas fa-edit"></i> 编辑
                        </button>
                    </div>
                </div>
                <div class="chunk-content">
                    ${chunk.content}
                </div>
                <div class="chunk-footer">
                    <span class="chunk-index">片段 ${chunk.chunkIndex}/${chunk.totalChunks}</span>
                    <span class="chunk-time">创建时间: ${new Date(chunk.createdAt).toLocaleString()}</span>
                </div>
            `;
            container.appendChild(chunkElement);
        });
    }

    // 加载向量数据
    async loadVectorData() {
        try {
            const response = await fetch('/api/rag/knowledge/vectors');
            const data = await response.json();

            if (data.error) {
                throw new Error(data.error);
            }

            this.vectorData = data;
            this.renderVectorData();
        } catch (error) {
            console.error('加载向量数据失败:', error);
            this.showNotification('加载向量数据失败', 'error');
        }
    }

    // 渲染向量数据
    renderVectorData() {
        const container = document.querySelector('#vectors .vectors-info');
        if (!container) return;

        container.innerHTML = `
            <h4>向量数据概览</h4>
            <div class="vector-stats">
                <div class="stat-item">
                    <span class="stat-label">向量维度:</span>
                    <span class="stat-value">${this.vectorData.vectorDimension || 1024}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">向量数量:</span>
                    <span class="stat-value">${this.vectorData.vectorCount || 0}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">存储类型:</span>
                    <span class="stat-value">${this.vectorData.storageType || 'Milvus'}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">索引类型:</span>
                    <span class="stat-value">${this.vectorData.indexType || 'IVF_FLAT'}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">距离度量:</span>
                    <span class="stat-value">${this.vectorData.metricType || 'COSINE'}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">内存使用:</span>
                    <span class="stat-value">${this.vectorData.memoryUsage || '0 MB'}</span>
                </div>
            </div>
            <div class="vector-actions">
                <button class="btn btn-primary" onclick="app.optimizeVectorIndex()">
                    <i class="fas fa-tools"></i> 优化索引
                </button>
                <button class="btn btn-secondary" onclick="app.exportVectorData()">
                    <i class="fas fa-download"></i> 导出数据
                </button>
            </div>
        `;
    }

    // 加载分析数据
    async loadAnalyticsData() {
        try {
            const response = await fetch('/api/rag/knowledge/analytics');
            const data = await response.json();

            if (data.error) {
                throw new Error(data.error);
            }

            this.analyticsData = data;
            this.renderAnalyticsData();
        } catch (error) {
            console.error('加载分析数据失败:', error);
            this.showNotification('加载分析数据失败', 'error');
        }
    }

    // 渲染分析数据
    renderAnalyticsData() {
        const container = document.querySelector('#analytics .analytics-content');
        if (!container) return;

        const queryStats = this.analyticsData.queryStats || {};
        const documentStats = this.analyticsData.documentStats || {};
        const dailyStats = this.analyticsData.dailyStats || [];

        container.innerHTML = `
            <h4>使用分析</h4>
            <div class="analytics-grid">
                <div class="analytics-card">
                    <h5>查询统计</h5>
                    <div class="stat-row">
                        <span>总查询次数:</span>
                        <span class="stat-number">${queryStats.totalQueries || 0}</span>
                    </div>
                    <div class="stat-row">
                        <span>成功查询:</span>
                        <span class="stat-number success">${queryStats.successfulQueries || 0}</span>
                    </div>
                    <div class="stat-row">
                        <span>失败查询:</span>
                        <span class="stat-number error">${queryStats.failedQueries || 0}</span>
                    </div>
                    <div class="stat-row">
                        <span>平均响应时间:</span>
                        <span class="stat-number">${queryStats.averageResponseTime || 0}秒</span>
                    </div>
                </div>
                
                <div class="analytics-card">
                    <h5>文档统计</h5>
                    <div class="stat-row">
                        <span>总文档数:</span>
                        <span class="stat-number">${documentStats.totalDocuments || 0}</span>
                    </div>
                    <div class="stat-row">
                        <span>已处理文档:</span>
                        <span class="stat-number">${documentStats.processedDocuments || 0}</span>
                    </div>
                    <div class="stat-row">
                        <span>总片段数:</span>
                        <span class="stat-number">${documentStats.totalChunks || 0}</span>
                    </div>
                    <div class="stat-row">
                        <span>平均片段大小:</span>
                        <span class="stat-number">${documentStats.averageChunkSize || 0}字符</span>
                    </div>
                </div>
            </div>
            
            <div class="analytics-chart">
                <h5>最近7天使用趋势</h5>
                <div class="chart-container">
                    <div class="chart-placeholder">
                        <i class="fas fa-chart-line"></i>
                        <p>使用趋势图表</p>
                        <div class="chart-data">
                            ${dailyStats.map(day => `
                                <div class="chart-item">
                                    <span class="chart-date">${day.date}</span>
                                    <div class="chart-bar">
                                        <div class="bar queries" style="height: ${(day.queries / 30) * 100}%"></div>
                                        <div class="bar documents" style="height: ${(day.documents / 5) * 100}%"></div>
                                    </div>
                                    <span class="chart-label">查询: ${day.queries}, 文档: ${day.documents}</span>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    // 查看知识片段
    viewChunk(chunkId) {
        const chunk = this.knowledgeChunks.find(c => c.id === chunkId);
        if (chunk) {
            alert(`知识片段详情:\n\nID: ${chunk.id}\n来源: ${chunk.source}\n内容: ${chunk.content}`);
        }
    }

    // 编辑知识片段
    editChunk(chunkId) {
        this.showNotification('编辑功能开发中...', 'info');
    }

    // 优化向量索引
    optimizeVectorIndex() {
        this.showNotification('索引优化功能开发中...', 'info');
    }

    // 导出向量数据
    exportVectorData() {
        this.showNotification('数据导出功能开发中...', 'info');
    }

    async checkSystemStatus() {
        try {
            const response = await fetch('/api/rag/status');

            // 检查响应是否成功
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // 检查响应内容类型
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                // 如果不是JSON，尝试读取文本内容
                const text = await response.text();
                console.warn('系统状态响应不是JSON格式:', text);
                throw new Error('服务器返回非JSON格式响应');
            }

            const status = await response.json();

            const indicator = document.getElementById('statusIndicator');
            const text = document.getElementById('statusText');

            if (status && status.status === 'running') {
                indicator.style.backgroundColor = '#4ade80';
                text.textContent = '系统正常';
            } else if (status && status.status === 'error') {
                indicator.style.backgroundColor = '#ef4444';
                text.textContent = '系统异常';
                console.error('系统状态异常:', status.message);
            } else {
                indicator.style.backgroundColor = '#f59e0b';
                text.textContent = '状态未知';
                console.warn('未知的系统状态:', status);
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
