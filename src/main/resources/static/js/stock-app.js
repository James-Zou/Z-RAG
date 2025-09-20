// 股神投资知识平台前端应用
class StockApp extends ZRAGApp {
    constructor() {
        super();
        this.currentTheme = 'stock';
        this.marketData = {
            shanghaiIndex: 3245.67,
            shenzhenIndex: 10234.56,
            chineseIndex: 2156.78,
            volume: 8234.5
        };
        this.newsTicker = [
            "沪指上涨1.2%，科技股领涨",
            "央行降准释放流动性，市场情绪乐观",
            "新能源汽车板块表现强劲",
            "外资持续流入A股市场",
            "政策利好推动基建板块上涨",
            "银行股集体走强，估值修复进行中",
            "消费板块回暖，食品饮料领涨",
            "医药生物板块表现分化",
            "房地产政策边际放松，板块企稳",
            "军工板块受政策利好刺激上涨"
        ];
        this.hotStocks = [
            { code: "000001", name: "平安银行", price: 12.45, change: 2.1 },
            { code: "000002", name: "万科A", price: 18.67, change: 1.8 },
            { code: "600036", name: "招商银行", price: 45.23, change: -0.5 },
            { code: "000858", name: "五粮液", price: 156.78, change: 3.2 },
            { code: "600519", name: "贵州茅台", price: 1890.45, change: 1.5 }
        ];

        // 初始化所有必要的属性
        this.tradingRecords = [];
        this.totalAssets = 100000;
        this.positions = [];
        this.klineData = [];
        this.technicalIndicators = {};
        this.klineChart = null;
        this.currentStock = null;

        // 重新初始化移动端菜单，确保使用StockApp的实现
        this.initMobileMenu();

        this.initStockFeatures();

        // 防止与主应用的上传功能冲突
        this.preventUploadConflict();
    }

    preventUploadConflict() {
        // 移除主应用绑定的事件监听器，避免重复调用
        const uploadDocument = document.getElementById('uploadDocument');
        const fileInput = document.getElementById('fileInput');
        const confirmUpload = document.getElementById('confirmUpload');
        const uploadArea = document.getElementById('uploadArea');

        if (uploadDocument) {
            // 克隆元素以移除所有事件监听器
            const newUploadDocument = uploadDocument.cloneNode(true);
            uploadDocument.parentNode.replaceChild(newUploadDocument, uploadDocument);

            // 重新绑定事件监听器
            newUploadDocument.addEventListener('click', () => {
                this.showUploadModal();
            });
        }

        if (fileInput) {
            const newFileInput = fileInput.cloneNode(true);
            fileInput.parentNode.replaceChild(newFileInput, fileInput);

            newFileInput.addEventListener('change', (e) => {
                this.handleFileUpload(e.target.files);
            });
        }

        if (confirmUpload) {
            const newConfirmUpload = confirmUpload.cloneNode(true);
            confirmUpload.parentNode.replaceChild(newConfirmUpload, confirmUpload);

            newConfirmUpload.addEventListener('click', () => {
                this.confirmUpload();
            });
        }

        if (uploadArea) {
            // 移除拖拽事件监听器
            uploadArea.removeEventListener('dragover', this.handleDragOver);
            uploadArea.removeEventListener('dragleave', this.handleDragLeave);
            uploadArea.removeEventListener('drop', this.handleDrop);

            // 重新绑定拖拽事件
            uploadArea.addEventListener('dragover', (e) => {
                e.preventDefault();
                uploadArea.classList.add('dragover');
            });

            uploadArea.addEventListener('dragleave', (e) => {
                e.preventDefault();
                uploadArea.classList.remove('dragover');
            });

            uploadArea.addEventListener('drop', (e) => {
                e.preventDefault();
                uploadArea.classList.remove('dragover');
                const files = e.dataTransfer.files;
                this.handleFileUpload(files);
            });
        }
    }

    showUploadModal() {
        // 清理之前的状态
        const modalFileInput = document.getElementById('modalFileInput');
        if (modalFileInput) {
            modalFileInput.value = ''; // 清空文件选择
        }

        // 隐藏进度条
        const progress = document.getElementById('uploadProgress');
        if (progress) {
            progress.style.display = 'none';
        }

        document.getElementById('uploadModal').classList.add('active');
    }

    hideUploadModal() {
        document.getElementById('uploadModal').classList.remove('active');

        // 清理文件选择状态
        const modalFileInput = document.getElementById('modalFileInput');
        if (modalFileInput) {
            modalFileInput.value = '';
        }
    }

    confirmUpload() {
        const fileInput = document.getElementById('modalFileInput');
        if (fileInput.files.length > 0) {
            this.handleFileUpload(fileInput.files);
            this.hideUploadModal();
        } else {
            this.showNotification('请先选择文件', 'warning');
        }
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

    initStockFeatures() {
        console.log('开始初始化股票功能');
        // 先调用父类的初始化方法，确保引用面板等功能正常
        super.init();

        // 重新初始化移动端菜单，确保使用股票主题的样式
        console.log('开始初始化移动端菜单');
        this.initMobileMenu();
        this.initNewsTicker();
        this.initMarketData();
        this.initHotStocks();
        this.initKlineFeatures();
        this.initAISelectionFeatures();
        this.initAITradingFeatures();
        this.bindStockEvents();

        // 添加调试信息，检查输入框和按钮是否正确绑定
        this.debugInputElements();
    }

    debugInputElements() {
        console.log('=== 调试输入元素 ===');
        const chatInput = document.getElementById('chatInput');
        const sendButton = document.getElementById('sendMessage');

        console.log('输入框元素:', chatInput);
        console.log('发送按钮元素:', sendButton);

        if (chatInput) {
            console.log('输入框属性:', {
                disabled: chatInput.disabled,
                readonly: chatInput.readOnly,
                style: chatInput.style.cssText,
                computedStyle: window.getComputedStyle(chatInput).pointerEvents
            });

            // 测试输入框事件
            chatInput.addEventListener('focus', () => {
                console.log('输入框获得焦点');
            });

            chatInput.addEventListener('input', () => {
                console.log('输入框内容变化:', chatInput.value);
            });
        }

        if (sendButton) {
            console.log('发送按钮属性:', {
                disabled: sendButton.disabled,
                style: sendButton.style.cssText,
                computedStyle: window.getComputedStyle(sendButton).pointerEvents
            });

            // 测试按钮事件
            sendButton.addEventListener('click', () => {
                console.log('发送按钮被点击');
            });
        }

        console.log('=== 调试完成 ===');
    }

    initMobileMenu() {
        // 移动端菜单功能
        console.log('正在初始化移动端菜单...');
        const mobileMenuToggle = document.getElementById('mobileMenuToggle');
        const sidebar = document.getElementById('sidebar');
        const sidebarClose = document.getElementById('sidebarClose');
        const mobileOverlay = document.getElementById('mobileOverlay');

        console.log('找到的元素:', {
            mobileMenuToggle: !!mobileMenuToggle,
            sidebar: !!sidebar,
            sidebarClose: !!sidebarClose,
            mobileOverlay: !!mobileOverlay
        });

        if (mobileMenuToggle) {
            console.log('绑定移动端菜单按钮点击事件');
            mobileMenuToggle.addEventListener('click', () => {
                console.log('移动端菜单按钮被点击！');
                this.toggleMobileMenu();
            });
        } else {
            console.error('未找到移动端菜单按钮元素！');
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

        // 监听窗口大小变化
        window.addEventListener('resize', () => {
            this.handleWindowResize();
        });

        // 监听导航项点击 - 在移动端关闭菜单，但保持原有的切换功能
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            // 添加移动端菜单关闭功能，但不覆盖父类的事件绑定
            const originalClickHandler = item.onclick;
            item.addEventListener('click', (e) => {
                // 在移动端关闭菜单
                if (window.innerWidth <= 768) {
                    this.closeMobileMenu();
                }
            });
        });

        // 添加触摸手势支持
        this.initTouchGestures();
    }

    toggleMobileMenu() {
        console.log('toggleMobileMenu 被调用');
        const sidebar = document.getElementById('sidebar');
        const mobileOverlay = document.getElementById('mobileOverlay');

        console.log('toggleMobileMenu 找到的元素:', {
            sidebar: !!sidebar,
            mobileOverlay: !!mobileOverlay,
            sidebarClass: sidebar ? sidebar.className : 'null',
            overlayClass: mobileOverlay ? mobileOverlay.className : 'null'
        });

        if (sidebar && mobileOverlay) {
            console.log('开始切换菜单状态');

            // 检查当前状态
            const isCurrentlyOpen = sidebar.classList.contains('show');
            console.log('当前菜单状态:', isCurrentlyOpen ? '打开' : '关闭');

            if (isCurrentlyOpen) {
                // 关闭菜单
                console.log('关闭菜单');
                sidebar.classList.remove('show');
                mobileOverlay.classList.remove('active');

                // 强制设置隐藏样式
                sidebar.style.cssText = `
                    position: fixed !important;
                    top: 0px !important;
                    left: -100% !important;
                    width: 280px !important;
                    height: 100vh !important;
                    z-index: 1000 !important;
                    background: linear-gradient(135deg, #8b1538 0%, #a91e3a 100%) !important;
                    box-shadow: 2px 0 20px rgba(139, 21, 56, 0.4) !important;
                    transition: left 0.3s ease !important;
                `;

                mobileOverlay.style.cssText = `
                    display: block !important;
                    opacity: 0 !important;
                    position: fixed !important;
                    top: 0px !important;
                    left: 0px !important;
                    width: 100% !important;
                    height: 100vh !important;
                    z-index: 999 !important;
                    background-color: rgba(0, 0, 0, 0.5) !important;
                    transition: opacity 0.3s ease !important;
                `;

                document.body.style.overflow = '';
                console.log('菜单已关闭，恢复背景滚动');
            } else {
                // 打开菜单
                console.log('打开菜单');
                sidebar.classList.add('show');
                mobileOverlay.classList.add('active');

                // 强制设置显示样式
                sidebar.style.cssText = `
                    position: fixed !important;
                    top: 0px !important;
                    left: 0px !important;
                    width: 280px !important;
                    height: 100vh !important;
                    z-index: 1000 !important;
                    background: linear-gradient(135deg, #8b1538 0%, #a91e3a 100%) !important;
                    box-shadow: 2px 0 20px rgba(139, 21, 56, 0.4) !important;
                    transition: left 0.3s ease !important;
                `;

                mobileOverlay.style.cssText = `
                    display: block !important;
                    opacity: 1 !important;
                    position: fixed !important;
                    top: 0px !important;
                    left: 0px !important;
                    width: 100% !important;
                    height: 100vh !important;
                    z-index: 999 !important;
                    background-color: rgba(0, 0, 0, 0.5) !important;
                    transition: opacity 0.3s ease !important;
                `;

                document.body.style.overflow = 'hidden';
                console.log('菜单已打开，禁用背景滚动');
            }

            console.log('切换后的状态:', {
                sidebarClass: sidebar.className,
                overlayClass: mobileOverlay.className,
                hasShow: sidebar.classList.contains('show'),
                hasActive: mobileOverlay.classList.contains('active'),
                sidebarLeft: sidebar.style.left,
                overlayOpacity: mobileOverlay.style.opacity
            });
        } else {
            console.error('侧边栏或遮罩层元素未找到');
        }
    }

    closeMobileMenu() {
        console.log('closeMobileMenu 被调用');
        const sidebar = document.getElementById('sidebar');
        const mobileOverlay = document.getElementById('mobileOverlay');

        if (sidebar && mobileOverlay) {
            console.log('关闭菜单');
            sidebar.classList.remove('show');
            mobileOverlay.classList.remove('active');

            // 强制设置隐藏样式，与toggleMobileMenu保持一致
            sidebar.style.cssText = `
                position: fixed !important;
                top: 0px !important;
                left: -100% !important;
                width: 280px !important;
                height: 100vh !important;
                z-index: 1000 !important;
                background: linear-gradient(135deg, #8b1538 0%, #a91e3a 100%) !important;
                box-shadow: 2px 0 20px rgba(139, 21, 56, 0.4) !important;
                transition: left 0.3s ease !important;
            `;

            mobileOverlay.style.cssText = `
                display: block !important;
                opacity: 0 !important;
                position: fixed !important;
                top: 0px !important;
                left: 0px !important;
                width: 100% !important;
                height: 100vh !important;
                z-index: 999 !important;
                background-color: rgba(0, 0, 0, 0.5) !important;
                transition: opacity 0.3s ease !important;
            `;

            document.body.style.overflow = '';
            console.log('菜单已关闭，恢复背景滚动');
        }
    }

    handleWindowResize() {
        // 如果窗口宽度大于768px，关闭移动端菜单
        if (window.innerWidth > 768) {
            this.closeMobileMenu();
        }
    }

    initTouchGestures() {
        // 触摸手势支持
        let startX = 0;
        let startY = 0;
        let isScrolling = false;

        // 监听触摸开始
        document.addEventListener('touchstart', (e) => {
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
            isScrolling = false;
        }, { passive: true });

        // 监听触摸移动
        document.addEventListener('touchmove', (e) => {
            if (!startX || !startY) return;

            const currentX = e.touches[0].clientX;
            const currentY = e.touches[0].clientY;
            const diffX = startX - currentX;
            const diffY = startY - currentY;

            // 判断是否为水平滑动
            if (Math.abs(diffX) > Math.abs(diffY)) {
                isScrolling = true;

                // 从屏幕左边缘向右滑动打开菜单
                if (startX < 50 && diffX < -50 && window.innerWidth <= 768) {
                    this.toggleMobileMenu();
                    startX = 0;
                    startY = 0;
                }
            }
        }, { passive: true });

        // 监听触摸结束
        document.addEventListener('touchend', (e) => {
            startX = 0;
            startY = 0;
            isScrolling = false;
        }, { passive: true });

        // 优化移动端滚动
        this.optimizeMobileScrolling();
    }

    optimizeMobileScrolling() {
        // 优化移动端滚动性能
        const scrollElements = document.querySelectorAll('.chat-messages, .references-content, .tab-content');

        scrollElements.forEach(element => {
            element.style.webkitOverflowScrolling = 'touch';
            element.style.overflowScrolling = 'touch';
        });

        // 防止iOS橡皮筋效果，但允许输入框和按钮交互
        document.addEventListener('touchmove', (e) => {
            const target = e.target;
            const scrollableParent = target.closest('.chat-messages, .references-content, .tab-content');

            // 如果是输入框、按钮或其他交互元素，不阻止事件
            const isInteractiveElement = target.matches('input, textarea, button, select, [contenteditable], [onclick]') ||
                target.closest('input, textarea, button, select, [contenteditable], [onclick]');

            if (!scrollableParent && !isInteractiveElement) {
                e.preventDefault();
            }
        }, { passive: false });
    }

    bindStockEvents() {


        // 刷新市场数据
        document.getElementById('refreshMarket').addEventListener('click', () => {
            this.refreshMarketData();
        });

        // 主题切换
        window.switchToGeneral = () => {
            if (confirm('确定要切换到通用版Z-RAG吗？')) {
                window.location.href = '/index.html';
            }
        };

        // 法律声明相关事件
        this.bindLegalEvents();

    }

    bindLegalEvents() {
        // 显示法律声明
        const showLegalDisclaimer = document.getElementById('showLegalDisclaimer');
        if (showLegalDisclaimer) {
            showLegalDisclaimer.addEventListener('click', (e) => {
                e.preventDefault();
                this.showLegalDisclaimer();
            });
        }

        // 显示隐私政策
        const showPrivacyPolicy = document.getElementById('showPrivacyPolicy');
        if (showPrivacyPolicy) {
            showPrivacyPolicy.addEventListener('click', (e) => {
                e.preventDefault();
                this.showPrivacyPolicy();
            });
        }

        // 显示服务条款
        const showTermsOfService = document.getElementById('showTermsOfService');
        if (showTermsOfService) {
            showTermsOfService.addEventListener('click', (e) => {
                e.preventDefault();
                this.showTermsOfService();
            });
        }

        // 法律声明模态框事件
        const legalModal = document.getElementById('legalDisclaimerModal');
        const acceptLegal = document.getElementById('acceptLegal');
        const declineLegal = document.getElementById('declineLegal');
        const modalClose = legalModal?.querySelector('.modal-close');

        if (acceptLegal) {
            acceptLegal.addEventListener('click', () => {
                this.acceptLegalDisclaimer();
            });
        }

        if (declineLegal) {
            declineLegal.addEventListener('click', () => {
                this.declineLegalDisclaimer();
            });
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => {
                this.hideLegalDisclaimer();
            });
        }

        // 页面加载时检查是否已同意法律声明
        this.checkLegalDisclaimer();
    }

    showLegalDisclaimer() {
        const modal = document.getElementById('legalDisclaimerModal');
        if (modal) {
            modal.classList.add('active');
        }
    }

    hideLegalDisclaimer() {
        const modal = document.getElementById('legalDisclaimerModal');
        if (modal) {
            modal.classList.remove('active');
        }
    }

    acceptLegalDisclaimer() {
        // 保存用户同意状态
        localStorage.setItem('legalDisclaimerAccepted', 'true');
        localStorage.setItem('legalDisclaimerAcceptedTime', new Date().toISOString());

        this.hideLegalDisclaimer();
        this.showNotification('感谢您的理解与支持', 'success');
    }

    declineLegalDisclaimer() {
        this.hideLegalDisclaimer();
        this.showNotification('请仔细阅读法律声明后再使用本平台', 'warning');

        // 可以选择重定向到其他页面或显示更多信息
        setTimeout(() => {
            if (confirm('是否要查看详细的法律声明？')) {
                this.showLegalDisclaimer();
            }
        }, 2000);
    }

    checkLegalDisclaimer() {
        const accepted = localStorage.getItem('legalDisclaimerAccepted');
        if (!accepted) {
            // 延迟显示法律声明，让页面先加载完成
            setTimeout(() => {
                this.showLegalDisclaimer();
            }, 1000);
        }
    }

    showPrivacyPolicy() {
        this.showNotification('隐私政策功能开发中...', 'info');
        // 这里可以添加隐私政策的具体内容
    }

    showTermsOfService() {
        this.showNotification('服务条款功能开发中...', 'info');
        // 这里可以添加服务条款的具体内容
    }

    initNewsTicker() {
        const tickerContent = document.getElementById('newsTicker');
        if (!tickerContent) return;

        // 清空现有内容
        tickerContent.innerHTML = '';

        // 添加新闻项目
        this.newsTicker.forEach((news, index) => {
            const item = document.createElement('div');
            item.className = 'ticker-item';
            item.textContent = news;
            item.style.animationDelay = `${index * 3}s`;
            tickerContent.appendChild(item);
        });

        // 启动跑马灯动画
        this.startTickerAnimation();
    }

    startTickerAnimation() {
        const tickerItems = document.querySelectorAll('.ticker-item');
        tickerItems.forEach((item, index) => {
            item.style.animation = `ticker 30s linear infinite`;
            item.style.animationDelay = `${index * 3}s`;
        });
    }

    initMarketData() {
        // 更新市场指数显示
        this.updateMarketDisplay();

        // 模拟实时数据更新
        setInterval(() => {
            this.updateMarketData();
        }, 5000); // 每5秒更新一次
    }

    updateMarketData() {
        // 模拟市场数据变化
        const changeRange = 0.02; // 2%的变化范围
        this.marketData.shanghaiIndex += (Math.random() - 0.5) * changeRange * this.marketData.shanghaiIndex;
        this.marketData.shenzhenIndex += (Math.random() - 0.5) * changeRange * this.marketData.shenzhenIndex;
        this.marketData.chineseIndex += (Math.random() - 0.5) * changeRange * this.marketData.chineseIndex;
        this.marketData.volume += (Math.random() - 0.5) * 100;

        this.updateMarketDisplay();
    }

    updateMarketDisplay() {
        // 更新上证指数
        const shanghaiEl = document.getElementById('shanghaiIndex');
        if (shanghaiEl) {
            shanghaiEl.textContent = this.marketData.shanghaiIndex.toFixed(2);
        }

        // 更新深证成指
        const shenzhenEl = document.getElementById('shenzhenIndex');
        if (shenzhenEl) {
            shenzhenEl.textContent = this.marketData.shenzhenIndex.toFixed(2);
        }

        // 更新创业板指
        const chineseEl = document.getElementById('chineseIndex');
        if (chineseEl) {
            chineseEl.textContent = this.marketData.chineseIndex.toFixed(2);
        }

        // 更新成交额
        const volumeEl = document.getElementById('volume');
        if (volumeEl) {
            volumeEl.textContent = this.marketData.volume.toFixed(1) + '亿';
        }
    }

    initHotStocks() {
        const hotStocksList = document.getElementById('hotStocksList');
        if (!hotStocksList) return;

        hotStocksList.innerHTML = '';

        this.hotStocks.forEach(stock => {
            const stockItem = document.createElement('div');
            stockItem.className = 'stock-item';
            stockItem.innerHTML = `
                <div class="stock-info">
                    <span class="stock-code">${stock.code}</span>
                    <span class="stock-name">${stock.name}</span>
                </div>
                <div class="stock-price">
                    <span class="price">${stock.price.toFixed(2)}</span>
                    <span class="change ${stock.change > 0 ? 'positive' : 'negative'}">
                        ${stock.change > 0 ? '+' : ''}${stock.change.toFixed(1)}%
                    </span>
                </div>
            `;
            hotStocksList.appendChild(stockItem);
        });
    }

    refreshMarketData() {
        this.showNotification('正在刷新市场数据...', 'info');

        // 模拟API调用延迟
        setTimeout(() => {
            this.updateMarketData();
            this.showNotification('市场数据已更新', 'success');
        }, 1000);
    }

    // 重写sendMessage方法，使用股神投资主题API
    async sendMessage() {
        console.log('=== SendMessage方法开始 ===');
        const input = document.getElementById('chatInput');
        const sendButton = document.getElementById('sendMessage');
        const message = input.value.trim();

        console.log('用户输入消息:', message);

        if (!message) {
            console.log('消息为空，退出');
            return;
        }

        // 禁用输入框和发送按钮
        this.setChatInputEnabled(false);
        console.log('已禁用输入框和发送按钮');

        // 清空输入框
        input.value = '';

        // 添加用户消息到聊天界面
        this.addMessage('user', message);
        console.log('已添加用户消息到界面');

        // 创建AI回复消息
        const aiMessageId = this.addMessage('assistant', '', true); // 设置为loading状态
        console.log('已创建AI消息，ID:', aiMessageId);

        try {
            console.log('开始发送请求到 /api/rag/query/stream/stock');
            // 使用股神投资主题流式端点
            const response = await fetch('/api/rag/query/stream/stock', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ query: message })
            });

            console.log('收到响应，状态:', response.status, response.statusText);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // 使用setTimeout确保DOM更新完成，然后处理流式响应
            setTimeout(() => {
                console.log('开始处理流式响应，aiMessageId:', aiMessageId);
                const aiMessageElement = document.getElementById(aiMessageId);
                const aiBubble = aiMessageElement ? aiMessageElement.querySelector('.message-bubble') : null;

                if (!aiBubble) {
                    console.error('无法找到AI气泡元素');
                    return;
                }

                console.log('找到AI气泡元素，准备更新内容');
                // 清空气泡内容，准备接收流式响应
                aiBubble.innerHTML = '<div class="loading"></div> 正在分析投资问题...';

                // 处理流式响应
                this.processStreamingResponse(response, aiMessageId);
            }, 10);

        } catch (error) {
            console.error('发送消息失败:', error);
            this.removeMessage(aiMessageId);
            this.addMessage('assistant', '抱歉，投资分析失败，请稍后重试。');
            this.showNotification('发送消息失败', 'error');
            this.setChatInputEnabled(true); // 重新启用输入
        }
    }

    enhanceInvestmentMessage(message) {
        // 为投资相关的问题添加上下文
        const investmentKeywords = ['股票', '投资', '基金', '债券', '期货', '期权', '技术分析', '基本面', '估值', '风险', '收益', '市场', '趋势', 'K线', '均线', 'RSI', 'MACD', '布林带', '成交量', '换手率'];

        const hasInvestmentKeyword = investmentKeywords.some(keyword =>
            message.includes(keyword)
        );

        if (hasInvestmentKeyword) {
            return `作为专业的投资顾问，请基于以下问题提供详细的投资建议和分析：${message}`;
        }

        return message;
    }

    // 重写addMessage方法，添加投资主题的样式
    addMessage(type, content, isLoading = false) {
        const messageId = super.addMessage(type, content, isLoading);

        // 为投资主题添加特殊样式
        const messageElement = document.getElementById(messageId);
        if (messageElement) {
            if (type === 'assistant') {
                messageElement.classList.add('investment-advice');
            }
        }

        return messageId;
    }

    // 重写clearChat方法，添加投资主题的欢迎信息
    clearChat() {
        const messagesContainer = document.getElementById('chatMessages');
        messagesContainer.innerHTML = `
            <div class="welcome-message stock-welcome">
                <div class="welcome-icon">
                    <i class="fas fa-chart-line"></i>
                </div>
                <h4>欢迎使用股神投资知识平台</h4>
                <p>我是您的专业投资顾问，可以帮您分析股票、解答投资问题、提供市场洞察。</p>
                
                <!-- 投资风险提示 -->
                <div class="risk-warning">
                    <h5><i class="fas fa-exclamation-triangle"></i> 投资风险提示</h5>
                    <p>本平台仅提供投资知识问答服务，不构成任何投资建议。所有信息仅供参考，投资有风险，决策需谨慎。</p>
                </div>
                
            </div>
        `;
        this.chatHistory = [];
    }

    // 重写handleStreamingResponse方法，添加RAG日志处理
    handleStreamingResponse(data, aiMessageId) {
        console.log('=== handleStreamingResponse 开始 ===');
        console.log('收到流式响应:', data);
        console.log('响应类型:', data.type);
        console.log('AI消息ID:', aiMessageId);


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

        const { type, content, finished } = data;

        // 确保content存在且不为空
        if (!content) {
            console.warn('流式响应内容为空:', data);
            return;
        }

        // 强制更新DOM
        const updateContent = (newContent) => {
            // 立即更新DOM
            aiBubble.innerHTML = newContent;
            console.log('DOM更新后aiBubble内容:', aiBubble.innerHTML);

            // 强制浏览器重绘
            aiBubble.style.display = 'none';
            aiBubble.offsetHeight; // 触发重排
            aiBubble.style.display = '';

            // 滚动到底部
            this.scrollToBottom();
        };

        switch (type) {
            case 'thinking':
                console.log(`[THINKING] 准备更新内容: ${content}`);
                updateContent(`<div class="loading"></div> ${content}`);
                console.log('[THINKING] 更新完成');
                break;
            case 'retrieval':
                console.log(`[RETRIEVAL] 准备更新内容: ${content}`);
                updateContent(`<div class="loading"></div> ${content}`);
                console.log('[RETRIEVAL] 更新完成');
                break;
            case 'rerank':
                console.log(`[RERANK] 准备更新内容: ${content}`);
                updateContent(`<div class="loading"></div> ${content}`);
                console.log('[RERANK] 更新完成');
                break;
            case 'generation':
                console.log(`[GENERATION] 准备更新内容: ${content}`);
                updateContent(`<div class="loading"></div> ${content}`);
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
                updateContent(`<span style="color: #ef4444;">❌ ${content}</span>`);
                console.log('[ERROR] 更新完成');
                break;
            default:
                console.log(`[UNKNOWN] 未知类型 ${type}, 内容: ${content}`);
                // 如果是未知类型但有内容，直接显示
                if (content) {
                    updateContent(content);
                    console.log('[UNKNOWN] 更新完成');
                }
        }
    }


    // 重写loadKnowledgeStats方法，更新知识库名称
    async loadKnowledgeStats() {
        try {
            const response = await fetch('/api/rag/knowledge/stats');
            const stats = await response.json();

            if (stats.success !== false) {
                document.getElementById('totalDocuments').textContent = stats.totalDocuments || 0;
                document.getElementById('totalChunks').textContent = stats.vectorCount || 0;
                document.getElementById('knowledgeBaseName').textContent = '股神投资知识库';
                document.getElementById('vectorCount').textContent = stats.vectorCount || 0;
            } else {
                // 如果API失败，使用模拟数据
                document.getElementById('totalDocuments').textContent = this.documents.length;
                document.getElementById('totalChunks').textContent = Math.floor(Math.random() * 1000) + 100;
                document.getElementById('knowledgeBaseName').textContent = '股神投资知识库';
                document.getElementById('vectorCount').textContent = Math.floor(Math.random() * 5000) + 1000;
            }
        } catch (error) {
            console.error('加载知识统计失败:', error);
            // 使用模拟数据作为后备
            document.getElementById('totalDocuments').textContent = this.documents.length;
            document.getElementById('totalChunks').textContent = Math.floor(Math.random() * 1000) + 100;
            document.getElementById('knowledgeBaseName').textContent = '股神投资知识库';
            document.getElementById('vectorCount').textContent = Math.floor(Math.random() * 5000) + 1000;
        }
    }


    // 添加市场数据实时更新
    startRealTimeUpdates() {
        // 每30秒更新一次新闻跑马灯
        setInterval(() => {
            this.updateNewsTicker();
        }, 30000);

        // 每10秒更新一次热门股票数据
        setInterval(() => {
            this.updateHotStocks();
        }, 10000);
    }

    updateNewsTicker() {
        // 随机更新新闻内容
        const newNews = [
            "科技股继续领涨，AI概念股表现强劲",
            "新能源汽车销量创新高，相关板块受益",
            "银行股估值修复，投资价值凸显",
            "消费板块回暖，食品饮料领涨",
            "医药生物板块分化，创新药受关注",
            "军工板块受政策利好刺激上涨",
            "房地产政策边际放松，板块企稳",
            "外资持续流入，A股吸引力增强"
        ];

        // 随机选择一条新新闻替换现有新闻
        const randomNews = newNews[Math.floor(Math.random() * newNews.length)];
        const tickerItems = document.querySelectorAll('.ticker-item');
        if (tickerItems.length > 0) {
            const randomIndex = Math.floor(Math.random() * tickerItems.length);
            tickerItems[randomIndex].textContent = randomNews;
        }
    }

    updateHotStocks() {
        // 模拟股票价格变化
        this.hotStocks.forEach(stock => {
            const change = (Math.random() - 0.5) * 0.1; // ±5%的变化
            stock.price += stock.price * change;
            stock.change = change * 100;
        });

        this.initHotStocks();
    }

    // K线监控功能
    initKlineFeatures() {
        this.currentStock = null;
        this.klineData = [];
        this.technicalIndicators = {};
        this.klineChart = null;

        // 初始化股票搜索
        this.initStockSearch();

        // 初始化技术指标
        this.initTechnicalIndicators();

        // 初始化ECharts
        this.initECharts();
    }

    initStockSearch() {
        const searchInput = document.getElementById('stockSearchInput');
        const searchButton = document.getElementById('searchStock');
        const suggestions = document.getElementById('searchSuggestions');

        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.trim();
                if (query.length > 0) {
                    this.showStockSuggestions(query);
                } else {
                    this.hideSuggestions();
                }
            });

            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.searchStock();
                }
            });
        }

        if (searchButton) {
            searchButton.addEventListener('click', () => {
                this.searchStock();
            });
        }

        // 全屏图表功能
        const fullscreenButton = document.getElementById('fullscreenChart');
        if (fullscreenButton) {
            fullscreenButton.addEventListener('click', () => {
                this.toggleFullscreen();
            });
        }

        // 时间框架切换功能
        const timeframeSelect = document.getElementById('klineTimeframe');
        if (timeframeSelect) {
            timeframeSelect.addEventListener('change', (e) => {
                this.changeTimeframe(e.target.value);
            });
        }

        // 刷新K线图功能
        const refreshButton = document.getElementById('refreshKline');
        if (refreshButton) {
            refreshButton.addEventListener('click', () => {
                this.refreshKlineData();
            });
        }
    }

    showStockSuggestions(query) {
        const suggestions = document.getElementById('searchSuggestions');
        if (!suggestions) return;

        // 模拟股票搜索建议
        const mockStocks = [
            { code: '000001', name: '平安银行', price: 12.45, change: 2.1 },
            { code: '000002', name: '万科A', price: 18.67, change: 1.8 },
            { code: '600036', name: '招商银行', price: 45.23, change: -0.5 },
            { code: '000858', name: '五粮液', price: 156.78, change: 3.2 },
            { code: '600519', name: '贵州茅台', price: 1890.45, change: 1.5 }
        ];

        const filteredStocks = mockStocks.filter(stock =>
            stock.code.includes(query) || stock.name.includes(query)
        );

        if (filteredStocks.length > 0) {
            suggestions.innerHTML = filteredStocks.map((stock, index) => `
                <div class="suggestion-item" data-stock-code="${stock.code}" data-stock-name="${stock.name}">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <strong>${stock.name}</strong>
                            <span style="color: #6b7280; margin-left: 0.5rem;">${stock.code}</span>
                        </div>
                        <div style="text-align: right;">
                            <div style="font-weight: 600;">¥${stock.price.toFixed(2)}</div>
                            <div class="change ${stock.change > 0 ? 'positive' : 'negative'}">
                                ${stock.change > 0 ? '+' : ''}${stock.change.toFixed(1)}%
                            </div>
                        </div>
                    </div>
                </div>
            `).join('');
            suggestions.style.display = 'block';

            // 添加事件监听器
            suggestions.querySelectorAll('.suggestion-item').forEach(item => {
                item.addEventListener('click', () => {
                    const code = item.dataset.stockCode;
                    const name = item.dataset.stockName;
                    this.selectStock(code, name);
                });
            });
        } else {
            this.hideSuggestions();
        }
    }

    hideSuggestions() {
        const suggestions = document.getElementById('searchSuggestions');
        if (suggestions) {
            suggestions.style.display = 'none';
        }
    }

    selectStock(code, name) {
        this.currentStock = { code, name };
        document.getElementById('stockSearchInput').value = `${name} (${code})`;
        this.hideSuggestions();
        this.loadKlineData(code);
    }

    searchStock() {
        const searchInput = document.getElementById('stockSearchInput');
        const query = searchInput.value.trim();

        if (query) {
            // 模拟搜索逻辑
            const mockStocks = [
                { code: '000001', name: '平安银行' },
                { code: '000002', name: '万科A' },
                { code: '600036', name: '招商银行' },
                { code: '000858', name: '五粮液' },
                { code: '600519', name: '贵州茅台' }
            ];

            const foundStock = mockStocks.find(stock =>
                stock.code === query || stock.name === query ||
                stock.code.includes(query) || stock.name.includes(query)
            );

            if (foundStock) {
                this.selectStock(foundStock.code, foundStock.name);
            } else {
                this.showNotification('未找到该股票', 'error');
            }
        }
    }

    loadKlineData(stockCode) {
        // 模拟加载K线数据
        this.showNotification(`正在加载 ${stockCode} 的K线数据...`, 'info');

        setTimeout(() => {
            this.generateMockKlineData();
            this.updateKlineChart();
            this.updateStockInfo();
            this.updateTechnicalIndicators();
            this.showNotification('K线数据加载完成', 'success');
        }, 1000);
    }

    generateMockKlineData(timeframe = '1d') {
        // 生成模拟K线数据
        this.klineData = [];
        const basePrice = 100;
        let currentPrice = basePrice;

        // 根据时间框架确定数据点数量
        let dataPoints = 30;
        let timeInterval = 24 * 60 * 60 * 1000; // 默认日线

        switch(timeframe) {
            case '1m':
                dataPoints = 240; // 4小时的数据
                timeInterval = 60 * 1000; // 1分钟
                break;
            case '5m':
                dataPoints = 288; // 24小时的数据
                timeInterval = 5 * 60 * 1000; // 5分钟
                break;
            case '15m':
                dataPoints = 192; // 48小时的数据
                timeInterval = 15 * 60 * 1000; // 15分钟
                break;
            case '30m':
                dataPoints = 168; // 84小时的数据
                timeInterval = 30 * 60 * 1000; // 30分钟
                break;
            case '1h':
                dataPoints = 168; // 7天的数据
                timeInterval = 60 * 60 * 1000; // 1小时
                break;
            case '4h':
                dataPoints = 168; // 28天的数据
                timeInterval = 4 * 60 * 60 * 1000; // 4小时
                break;
            case '1d':
                dataPoints = 30; // 30天的数据
                timeInterval = 24 * 60 * 60 * 1000; // 1天
                break;
            case '1w':
                dataPoints = 52; // 52周的数据
                timeInterval = 7 * 24 * 60 * 60 * 1000; // 1周
                break;
        }

        for (let i = 0; i < dataPoints; i++) {
            const change = (Math.random() - 0.5) * 0.1; // ±5%的变化
            const open = currentPrice;
            const close = currentPrice * (1 + change);
            const high = Math.max(open, close) * (1 + Math.random() * 0.02);
            const low = Math.min(open, close) * (1 - Math.random() * 0.02);
            const volume = Math.floor(Math.random() * 1000000) + 100000;

            this.klineData.push({
                date: new Date(Date.now() - (dataPoints - 1 - i) * timeInterval),
                open: open,
                high: high,
                low: low,
                close: close,
                volume: volume
            });

            currentPrice = close;
        }

        // 生成K线数据后立即计算技术指标
        this.calculateTechnicalIndicators();
    }

    initECharts() {
        // 检查ECharts是否加载成功
        if (typeof echarts === 'undefined') {
            console.warn('ECharts未加载，使用备用图表显示');
            this.showEChartsFallback();
            return;
        }

        // 初始化ECharts实例
        const echartsContainer = document.getElementById('echartsContainer');
        if (echartsContainer) {
            try {
                // 确保容器有正确的尺寸
                const chartContainer = document.getElementById('klineChart');
                if (chartContainer) {
                    const containerRect = chartContainer.getBoundingClientRect();
                    echartsContainer.style.width = containerRect.width + 'px';
                    echartsContainer.style.height = containerRect.height + 'px';
                }

                this.klineChart = echarts.init(echartsContainer, null, {
                    renderer: 'canvas',
                    useDirtyRect: false
                });

                // 监听窗口大小变化
                const resizeHandler = () => {
                    if (this.klineChart) {
                        // 延迟执行resize，确保DOM更新完成
                        setTimeout(() => {
                            this.klineChart.resize();
                        }, 100);
                    }
                };

                window.addEventListener('resize', resizeHandler);

                // 监听容器大小变化
                if (window.ResizeObserver) {
                    const resizeObserver = new ResizeObserver(resizeHandler);
                    resizeObserver.observe(echartsContainer);
                }

                console.log('ECharts初始化成功');
            } catch (error) {
                console.error('ECharts初始化失败:', error);
                this.showEChartsFallback();
            }
        }
    }

    showEChartsFallback() {
        // 显示备用图表内容
        const echartsContainer = document.getElementById('echartsContainer');
        if (echartsContainer) {
            echartsContainer.innerHTML = `
                <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #8b1538;">
                    <i class="fas fa-chart-bar" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                    <h4>专业K线图表</h4>
                    <p style="color: #6b7280; text-align: center; margin: 0.5rem 0;">
                        图表库加载中，请稍后刷新页面
                    </p>
                    <button class="btn btn-primary" onclick="location.reload()" style="margin-top: 1rem;">
                        <i class="fas fa-sync"></i> 刷新页面
                    </button>
                </div>
            `;
        }
    }

    updateKlineChart() {
        const chartContainer = document.getElementById('klineChart');
        const placeholder = document.getElementById('chartPlaceholder');
        const echartsContainer = document.getElementById('echartsContainer');

        if (!chartContainer || !this.klineData.length) return;

        // 隐藏占位符，显示ECharts容器
        if (placeholder) placeholder.style.display = 'none';
        if (echartsContainer) echartsContainer.style.display = 'block';

        // 使用ECharts绘制K线图
        this.renderKlineChart();
    }

    renderKlineChart() {
        if (!this.klineChart || !this.klineData.length) {
            // 如果ECharts未加载，显示备用内容
            if (typeof echarts === 'undefined') {
                this.showEChartsFallback();
            }
            return;
        }

        // 准备K线数据
        const dates = this.klineData.map(item => item.date.toISOString().split('T')[0]);
        const ohlcData = this.klineData.map(item => [item.open, item.close, item.low, item.high]);
        const volumeData = this.klineData.map(item => item.volume);

        // 计算MA均线
        const ma5 = this.calculateMA(this.klineData.map(d => d.close), 5);
        const ma10 = this.calculateMA(this.klineData.map(d => d.close), 10);
        const ma20 = this.calculateMA(this.klineData.map(d => d.close), 20);

        // 获取容器尺寸，用于响应式配置
        const containerWidth = document.getElementById('klineChart')?.offsetWidth || 800;
        const isMobile = window.innerWidth <= 768;
        const isSmallMobile = window.innerWidth <= 480;

        const option = {
            backgroundColor: '#faf7f7',
            animation: true,
            responsive: true,
            maintainAspectRatio: false,
            legend: {
                data: ['K线', 'MA5', 'MA10', 'MA20', '成交量'],
                top: isSmallMobile ? 2 : (isMobile ? 5 : 10),
                left: 'center',
                textStyle: {
                    color: '#8b1538',
                    fontSize: isSmallMobile ? 10 : (isMobile ? 12 : 14)
                },
                itemWidth: isSmallMobile ? 10 : (isMobile ? 12 : 16),
                itemHeight: isSmallMobile ? 6 : (isMobile ? 8 : 10),
                show: !isSmallMobile // 小屏幕隐藏图例
            },
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'cross'
                },
                backgroundColor: 'rgba(139, 21, 56, 0.9)',
                borderColor: '#8b1538',
                textStyle: {
                    color: '#fff'
                },
                formatter: function (params) {
                    let result = params[0].axisValue + '<br/>';
                    params.forEach(param => {
                        if (param.seriesName === 'K线') {
                            const data = param.data;
                            result += `开盘: ${data[1].toFixed(2)}<br/>`;
                            result += `收盘: ${data[2].toFixed(2)}<br/>`;
                            result += `最低: ${data[3].toFixed(2)}<br/>`;
                            result += `最高: ${data[4].toFixed(2)}<br/>`;
                        } else if (param.seriesName.includes('MA')) {
                            result += `${param.seriesName}: ${param.data.toFixed(2)}<br/>`;
                        } else if (param.seriesName === '成交量') {
                            result += `成交量: ${param.data.toLocaleString()}<br/>`;
                        }
                    });
                    return result;
                }
            },
            axisPointer: {
                link: { xAxisIndex: 'all' },
                label: {
                    backgroundColor: '#8b1538'
                }
            },
            grid: [
                {
                    left: isSmallMobile ? '8%' : (isMobile ? '5%' : '3%'),
                    right: isSmallMobile ? '8%' : (isMobile ? '5%' : '3%'),
                    height: isSmallMobile ? '50%' : (isMobile ? '55%' : '60%'),
                    top: isSmallMobile ? '15%' : (isMobile ? '20%' : '15%')
                },
                {
                    left: isSmallMobile ? '8%' : (isMobile ? '5%' : '3%'),
                    right: isSmallMobile ? '8%' : (isMobile ? '5%' : '3%'),
                    top: isSmallMobile ? '70%' : (isMobile ? '80%' : '80%'),
                    height: isSmallMobile ? '20%' : (isMobile ? '15%' : '15%')
                }
            ],
            xAxis: [
                {
                    type: 'category',
                    data: dates,
                    scale: true,
                    boundaryGap: false,
                    axisLine: { onZero: false },
                    splitLine: { show: false },
                    min: 'dataMin',
                    max: 'dataMax',
                    axisLabel: {
                        color: '#8b1538',
                        fontSize: isSmallMobile ? 8 : (isMobile ? 10 : 12),
                        interval: isSmallMobile ? 'auto' : (isMobile ? 'auto' : 0),
                        rotate: isSmallMobile ? 60 : (isMobile ? 45 : 0)
                    }
                },
                {
                    type: 'category',
                    gridIndex: 1,
                    data: dates,
                    scale: true,
                    boundaryGap: false,
                    axisLine: { onZero: false },
                    axisTick: { show: false },
                    splitLine: { show: false },
                    axisLabel: { show: false },
                    min: 'dataMin',
                    max: 'dataMax'
                }
            ],
            yAxis: [
                {
                    scale: true,
                    splitArea: {
                        show: true,
                        areaStyle: {
                            color: ['rgba(139, 21, 56, 0.05)', 'rgba(139, 21, 56, 0.1)']
                        }
                    },
                    axisLabel: {
                        color: '#8b1538',
                        fontSize: isSmallMobile ? 8 : (isMobile ? 10 : 12),
                        formatter: isSmallMobile ? '{value}' : '¥{value}'
                    }
                },
                {
                    scale: true,
                    gridIndex: 1,
                    splitNumber: 2,
                    axisLabel: { show: false },
                    axisLine: { show: false },
                    axisTick: { show: false },
                    splitLine: { show: false }
                }
            ],
            dataZoom: [
                {
                    type: 'inside',
                    xAxisIndex: [0, 1],
                    start: 50,
                    end: 100
                },
                {
                    show: !isSmallMobile, // 小屏幕隐藏滑块
                    xAxisIndex: [0, 1],
                    type: 'slider',
                    top: isSmallMobile ? '90%' : (isMobile ? '92%' : '95%'),
                    start: 50,
                    end: 100,
                    height: isSmallMobile ? 12 : (isMobile ? 15 : 20),
                    borderColor: '#8b1538',
                    fillerColor: 'rgba(139, 21, 56, 0.2)',
                    handleStyle: {
                        color: '#8b1538',
                        borderColor: '#8b1538',
                        width: isSmallMobile ? 8 : 12,
                        height: isSmallMobile ? 8 : 12
                    }
                }
            ],
            series: [
                {
                    name: 'K线',
                    type: 'candlestick',
                    data: ohlcData,
                    itemStyle: {
                        color: '#8b1538',
                        color0: '#059669',
                        borderColor: '#8b1538',
                        borderColor0: '#059669'
                    }
                },
                {
                    name: 'MA5',
                    type: 'line',
                    data: ma5,
                    smooth: true,
                    lineStyle: {
                        color: '#d4af37',
                        width: 1
                    },
                    showSymbol: false
                },
                {
                    name: 'MA10',
                    type: 'line',
                    data: ma10,
                    smooth: true,
                    lineStyle: {
                        color: '#8b1538',
                        width: 1
                    },
                    showSymbol: false
                },
                {
                    name: 'MA20',
                    type: 'line',
                    data: ma20,
                    smooth: true,
                    lineStyle: {
                        color: '#a91e3a',
                        width: 1
                    },
                    showSymbol: false
                },
                {
                    name: '成交量',
                    type: 'bar',
                    xAxisIndex: 1,
                    yAxisIndex: 1,
                    data: volumeData,
                    itemStyle: {
                        color: function(params) {
                            const dataIndex = params.dataIndex;
                            if (dataIndex === 0) return '#8b1538';
                            const current = ohlcData[dataIndex];
                            const previous = ohlcData[dataIndex - 1];
                            return current[2] >= previous[2] ? '#8b1538' : '#059669';
                        }
                    }
                }
            ]
        };

        this.klineChart.setOption(option, true);

        // 确保图表正确渲染
        setTimeout(() => {
            if (this.klineChart) {
                this.klineChart.resize();
            }
        }, 100);
    }

    toggleFullscreen() {
        const chartContainer = document.getElementById('klineChart');
        if (!chartContainer) return;

        if (!document.fullscreenElement) {
            // 进入全屏
            chartContainer.requestFullscreen().then(() => {
                if (this.klineChart) {
                    setTimeout(() => {
                        this.klineChart.resize();
                    }, 100);
                }
            }).catch(err => {
                console.error('无法进入全屏模式:', err);
            });
        } else {
            // 退出全屏
            document.exitFullscreen().then(() => {
                if (this.klineChart) {
                    setTimeout(() => {
                        this.klineChart.resize();
                    }, 100);
                }
            }).catch(err => {
                console.error('无法退出全屏模式:', err);
            });
        }
    }

    changeTimeframe(timeframe) {
        if (!this.currentStock) return;

        this.showNotification(`切换到${timeframe}时间框架...`, 'info');

        // 模拟不同时间框架的数据
        setTimeout(() => {
            this.generateMockKlineData(timeframe);
            this.updateKlineChart();
            this.updateStockInfo();
            this.updateTechnicalIndicators();
            this.showNotification(`${timeframe}时间框架数据加载完成`, 'success');
        }, 500);
    }

    refreshKlineData() {
        if (!this.currentStock) {
            this.showNotification('请先选择股票', 'warning');
            return;
        }

        this.showNotification('正在刷新K线数据...', 'info');

        setTimeout(() => {
            this.generateMockKlineData();
            this.updateKlineChart();
            this.updateStockInfo();
            this.updateTechnicalIndicators();
            this.showNotification('K线数据刷新完成', 'success');
        }, 1000);
    }

    updateStockInfo() {
        if (!this.currentStock || !this.klineData.length) return;

        const latestData = this.klineData[this.klineData.length - 1];
        const change = ((latestData.close - latestData.open) / latestData.open) * 100;

        document.getElementById('currentStockName').textContent = this.currentStock.name;
        document.getElementById('currentStockCode').textContent = this.currentStock.code;
        document.getElementById('currentStockPrice').textContent = `¥${latestData.close.toFixed(2)}`;

        const changeElement = document.getElementById('currentStockChange');
        changeElement.textContent = `${change > 0 ? '+' : ''}${change.toFixed(2)}%`;
        changeElement.className = `change ${change > 0 ? 'positive' : 'negative'}`;
    }

    initTechnicalIndicators() {
        // 初始化技术指标对象
        this.technicalIndicators = {};
        // 注意：不在这里计算指标，因为此时还没有K线数据
    }

    calculateTechnicalIndicators() {
        if (!this.klineData.length) return;

        // 计算MA均线
        const closes = this.klineData.map(d => d.close);
        this.technicalIndicators.ma5 = this.calculateMA(closes, 5);
        this.technicalIndicators.ma10 = this.calculateMA(closes, 10);
        this.technicalIndicators.ma20 = this.calculateMA(closes, 20);

        // 计算MACD
        const macd = this.calculateMACD(closes);
        this.technicalIndicators.macdDIF = macd.dif;
        this.technicalIndicators.macdDEA = macd.dea;
        this.technicalIndicators.macdMACD = macd.macd;

        // 计算RSI
        this.technicalIndicators.rsi6 = this.calculateRSI(closes, 6);
        this.technicalIndicators.rsi12 = this.calculateRSI(closes, 12);
        this.technicalIndicators.rsi24 = this.calculateRSI(closes, 24);

        // 计算布林带
        const boll = this.calculateBollingerBands(closes, 20, 2);
        this.technicalIndicators.bollUpper = boll.upper;
        this.technicalIndicators.bollMiddle = boll.middle;
        this.technicalIndicators.bollLower = boll.lower;
    }

    calculateMA(data, period) {
        if (data.length < period) return 0;
        const sum = data.slice(-period).reduce((a, b) => a + b, 0);
        return sum / period;
    }

    calculateMACD(data) {
        // 简化的MACD计算
        const ema12 = this.calculateEMA(data, 12);
        const ema26 = this.calculateEMA(data, 26);
        const dif = ema12 - ema26;
        const dea = dif * 0.9; // 简化的DEA计算
        const macd = (dif - dea) * 2;
        return { dif, dea, macd };
    }

    calculateEMA(data, period) {
        if (data.length < period) return data[data.length - 1];
        const multiplier = 2 / (period + 1);
        let ema = data[0];
        for (let i = 1; i < data.length; i++) {
            ema = (data[i] * multiplier) + (ema * (1 - multiplier));
        }
        return ema;
    }

    calculateRSI(data, period) {
        if (data.length < period + 1) return 50;
        let gains = 0;
        let losses = 0;

        for (let i = 1; i <= period; i++) {
            const change = data[data.length - i] - data[data.length - i - 1];
            if (change > 0) gains += change;
            else losses -= change;
        }

        const avgGain = gains / period;
        const avgLoss = losses / period;

        // 避免除零错误
        if (avgLoss === 0) return 100;

        const rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    calculateBollingerBands(data, period, multiplier) {
        const ma = this.calculateMA(data, period);
        const variance = this.calculateVariance(data.slice(-period), ma);
        const stdDev = Math.sqrt(variance);

        return {
            upper: ma + (stdDev * multiplier),
            middle: ma,
            lower: ma - (stdDev * multiplier)
        };
    }

    calculateVariance(data, mean) {
        const sum = data.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0);
        return sum / data.length;
    }

    updateTechnicalIndicators() {
        // 更新技术指标显示
        document.getElementById('ma5').textContent = this.technicalIndicators.ma5?.toFixed(2) || '-';
        document.getElementById('ma10').textContent = this.technicalIndicators.ma10?.toFixed(2) || '-';
        document.getElementById('ma20').textContent = this.technicalIndicators.ma20?.toFixed(2) || '-';

        document.getElementById('macdDIF').textContent = this.technicalIndicators.macdDIF?.toFixed(4) || '-';
        document.getElementById('macdDEA').textContent = this.technicalIndicators.macdDEA?.toFixed(4) || '-';
        document.getElementById('macdMACD').textContent = this.technicalIndicators.macdMACD?.toFixed(4) || '-';

        document.getElementById('rsi6').textContent = this.technicalIndicators.rsi6?.toFixed(2) || '-';
        document.getElementById('rsi12').textContent = this.technicalIndicators.rsi12?.toFixed(2) || '-';
        document.getElementById('rsi24').textContent = this.technicalIndicators.rsi24?.toFixed(2) || '-';

        document.getElementById('bollUpper').textContent = this.technicalIndicators.bollUpper?.toFixed(2) || '-';
        document.getElementById('bollMiddle').textContent = this.technicalIndicators.bollMiddle?.toFixed(2) || '-';
        document.getElementById('bollLower').textContent = this.technicalIndicators.bollLower?.toFixed(2) || '-';
    }

    // AI选股功能
    initAISelectionFeatures() {
        this.aiStocks = [];
        this.selectionCriteria = {
            industry: '',
            marketCap: '',
            technical: '',
            risk: ''
        };

        // 绑定AI选股事件
        const startButton = document.getElementById('startAISelection');
        const refreshButton = document.getElementById('refreshAIStocks');

        if (startButton) {
            startButton.addEventListener('click', () => {
                this.startAISelection();
            });
        }

        if (refreshButton) {
            refreshButton.addEventListener('click', () => {
                this.refreshAIStocks();
            });
        }

        // 绑定筛选条件变化事件
        const filters = ['industryFilter', 'marketCapFilter', 'technicalFilter', 'riskFilter'];
        filters.forEach(filterId => {
            const element = document.getElementById(filterId);
            if (element) {
                element.addEventListener('change', (e) => {
                    this.selectionCriteria[filterId.replace('Filter', '')] = e.target.value;
                });
            }
        });
    }

    startAISelection() {
        // 显示风险提示
        if (!confirm('AI选股功能仅供学习研究使用，不构成投资建议。投资有风险，决策需谨慎。是否继续？')) {
            return;
        }

        this.showNotification('AI正在分析股票...', 'info');

        // 获取当前选股条件
        const selectionCriteria = {
            industry: document.getElementById('industryFilter').value,
            marketCap: document.getElementById('marketCapFilter').value,
            technical: document.getElementById('technicalFilter').value,
            risk: document.getElementById('riskFilter').value,
            currentTime: new Date().toLocaleString('zh-CN')
        };

        // 调用AI选股API
        this.callAIStockSelectionAPI(selectionCriteria);
    }

    // 调用AI选股API
    async callAIStockSelectionAPI(selectionCriteria) {
        try {
            const response = await fetch('/api/rag/ai-stock-selection', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(selectionCriteria)
            });

            const result = await response.json();

            if (result.success) {
                // 处理成功的选股结果
                this.handleAIStockSelectionSuccess(result);
            } else {
                // 处理失败情况（如休盘时间）
                this.handleAIStockSelectionError(result);
            }
        } catch (error) {
            console.error('AI选股API调用失败:', error);
            this.showNotification('AI选股失败: ' + error.message, 'error');
        }
    }

    // 处理AI选股成功结果
    handleAIStockSelectionSuccess(result) {
        const recommendedStock = result.recommendedStock;

        // 更新AI推荐股票列表
        this.aiStocks = [recommendedStock];

        // 显示推荐股票
        this.displayAIStocks();

        // 更新统计信息
        this.updateSelectionStats();

        // 显示成功通知
        this.showNotification('AI选股完成', 'success');

        // 更新最后更新时间
        document.getElementById('lastUpdateTime').textContent = result.currentTime;
    }

    // 处理AI选股错误（如休盘时间）
    handleAIStockSelectionError(result) {
        this.showNotification(result.message, 'warning');

        // 清空推荐股票
        this.aiStocks = [];
        this.displayAIStocks();
        this.updateSelectionStats();

        // 显示交易时间信息
        if (result.tradingTime) {
            setTimeout(() => {
                this.showNotification(result.tradingTime, 'info');
            }, 2000);
        }
    }

    generateAIStocks() {
        // 生成模拟AI推荐股票
        const mockStocks = [
            { code: '000001', name: '平安银行', price: 12.45, change: 2.1, score: 85, industry: 'bank', risk: 'medium' },
            { code: '000002', name: '万科A', price: 18.67, change: 1.8, score: 78, industry: 'realestate', risk: 'low' },
            { code: '600036', name: '招商银行', price: 45.23, change: -0.5, score: 92, industry: 'bank', risk: 'low' },
            { code: '000858', name: '五粮液', price: 156.78, change: 3.2, score: 88, industry: 'consumer', risk: 'medium' },
            { code: '600519', name: '贵州茅台', price: 1890.45, change: 1.5, score: 95, industry: 'consumer', risk: 'low' },
            { code: '000725', name: '京东方A', price: 4.56, change: 2.8, score: 72, industry: 'tech', risk: 'high' },
            { code: '002415', name: '海康威视', price: 32.15, change: 1.2, score: 89, industry: 'tech', risk: 'medium' },
            { code: '300059', name: '东方财富', price: 15.67, change: 4.1, score: 81, industry: 'finance', risk: 'high' }
        ];

        // 根据筛选条件过滤
        this.aiStocks = mockStocks.filter(stock => {
            if (this.selectionCriteria.industry && stock.industry !== this.selectionCriteria.industry) return false;
            if (this.selectionCriteria.risk && stock.risk !== this.selectionCriteria.risk) return false;
            return true;
        }).sort((a, b) => b.score - a.score);
    }

    displayAIStocks() {
        const grid = document.getElementById('aiStocksGrid');
        if (!grid) return;

        if (this.aiStocks.length === 0) {
            grid.innerHTML = `
                <div class="no-stocks-message">
                    <i class="fas fa-chart-line"></i>
                    <p>暂无推荐股票</p>
                    <p>请点击"开始选股"获取AI推荐</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = this.aiStocks.map(stock => `
            <div class="stock-card" data-stock-code="${stock.code}">
                <div class="stock-card-header">
                    <div class="stock-card-name">${stock.name}</div>
                    <div class="stock-card-score">${stock.score}分</div>
                </div>
                <div class="stock-card-details">
                    <div class="stock-card-price">¥${stock.price.toFixed(2)}</div>
                    <div class="stock-card-change ${stock.change > 0 ? 'positive' : 'negative'}">
                        ${stock.change > 0 ? '+' : ''}${stock.change.toFixed(1)}%
                    </div>
                </div>
                <div class="stock-card-info">
                    <div>代码: ${stock.code}</div>
                    <div>行业: ${this.getIndustryName(stock.industry)}</div>
                    <div>风险: ${this.getRiskName(stock.risk)}</div>
                </div>
                ${stock.reason ? `
                    <div class="stock-card-reason">
                        <h6>推荐理由:</h6>
                        <p>${stock.reason}</p>
                    </div>
                ` : ''}
                ${stock.riskWarning ? `
                    <div class="stock-card-warning">
                        <i class="fas fa-exclamation-triangle"></i>
                        <span>${stock.riskWarning}</span>
                    </div>
                ` : ''}
            </div>
        `).join('');

        // 添加事件监听器
        grid.querySelectorAll('.stock-card').forEach(card => {
            card.addEventListener('click', () => {
                const code = card.dataset.stockCode;
                this.viewStockDetail(code);
            });
        });
    }

    getIndustryName(industry) {
        const industries = {
            'bank': '银行',
            'tech': '科技',
            'consumer': '消费',
            'finance': '金融',
            'realestate': '房地产'
        };
        return industries[industry] || industry;
    }

    getRiskName(risk) {
        const risks = {
            'low': '低风险',
            'medium': '中风险',
            'high': '高风险'
        };
        return risks[risk] || risk;
    }

    updateSelectionStats() {
        document.getElementById('recommendationCount').textContent = this.aiStocks.length;

        const avgScore = this.aiStocks.length > 0
            ? (this.aiStocks.reduce((sum, stock) => sum + stock.score, 0) / this.aiStocks.length).toFixed(1)
            : '-';
        document.getElementById('averageScore').textContent = avgScore;

        document.getElementById('lastUpdateTime').textContent = new Date().toLocaleTimeString();
    }

    refreshAIStocks() {
        this.startAISelection();
    }

    viewStockDetail(stockCode) {
        this.showNotification(`查看 ${stockCode} 详细信息`, 'info');
        // 这里可以跳转到股票详情页面或打开详情模态框
    }

    // AI交易功能
    initAITradingFeatures() {
        this.tradingStatus = 'stopped';
        this.tradingSettings = {
            mode: 'balanced',
            maxPosition: 30,
            stopLoss: 5,
            takeProfit: 10
        };
        this.tradingRecords = [];
        this.totalAssets = 100000;
        this.positions = [];

        // 绑定AI交易事件
        const startButton = document.getElementById('startAITrading');
        const pauseButton = document.getElementById('pauseAITrading');

        if (startButton) {
            startButton.addEventListener('click', () => {
                this.startAITrading();
            });
        }

        if (pauseButton) {
            pauseButton.addEventListener('click', () => {
                this.pauseAITrading();
            });
        }

        // 绑定交易设置变化事件
        const settings = ['tradingMode', 'maxPosition', 'stopLoss', 'takeProfit'];
        settings.forEach(settingId => {
            const element = document.getElementById(settingId);
            if (element) {
                element.addEventListener('change', (e) => {
                    this.tradingSettings[settingId] = e.target.value;
                });
            }
        });

        // 初始化交易记录
        this.initTradingRecords();
    }

    startAITrading() {
        // 显示模拟交易警告
        if (!confirm('AI交易功能为模拟环境，不涉及真实资金。所有交易记录均为虚拟数据，仅供学习研究使用。是否继续？')) {
            return;
        }

        this.tradingStatus = 'running';
        this.updateTradingStatus();
        this.showNotification('AI模拟交易已启动', 'success');

        // 模拟交易过程
        this.simulateTrading();
    }

    pauseAITrading() {
        this.tradingStatus = 'paused';
        this.updateTradingStatus();
        this.showNotification('AI交易已暂停', 'warning');
    }

    updateTradingStatus() {
        const statusElement = document.getElementById('tradingStatus');
        if (statusElement) {
            const statusTexts = {
                'stopped': '未启动',
                'running': '运行中',
                'paused': '已暂停'
            };
            statusElement.textContent = statusTexts[this.tradingStatus] || '未知';
        }
    }

    simulateTrading() {
        if (this.tradingStatus !== 'running') return;

        // 模拟交易逻辑
        setTimeout(() => {
            if (Math.random() > 0.7) { // 30%概率产生交易
                this.generateTradingRecord();
            }
            this.updateTradingAssets();
            this.simulateTrading(); // 递归调用
        }, 5000); // 每5秒检查一次
    }

    generateTradingRecord() {
        const operations = ['买入', '卖出'];
        const stocks = ['000001', '000002', '600036', '000858', '600519'];
        const operation = operations[Math.floor(Math.random() * operations.length)];
        const stock = stocks[Math.floor(Math.random() * stocks.length)];
        const quantity = Math.floor(Math.random() * 1000) + 100;
        const price = (Math.random() * 100 + 10).toFixed(2);
        const amount = (quantity * price).toFixed(2);
        const status = Math.random() > 0.1 ? '成功' : '失败';

        const record = {
            time: new Date().toLocaleTimeString(),
            stock: stock,
            operation: operation,
            quantity: quantity,
            price: price,
            amount: amount,
            status: status
        };

        this.tradingRecords.unshift(record);
        this.updateTradingRecordsTable();
    }

    updateTradingAssets() {
        // 模拟资产变化
        const change = (Math.random() - 0.5) * 0.02; // ±1%的变化
        this.totalAssets *= (1 + change);

        document.getElementById('totalAssets').textContent = `¥${this.totalAssets.toFixed(2)}`;
        document.getElementById('positionCount').textContent = this.positions.length;

        const todayProfit = (Math.random() - 0.5) * 0.05; // ±2.5%的今日收益
        const profitElement = document.getElementById('todayProfit');
        profitElement.textContent = `${todayProfit > 0 ? '+' : ''}${(todayProfit * 100).toFixed(2)}%`;
        profitElement.className = `change ${todayProfit > 0 ? 'positive' : 'negative'}`;
    }

    initTradingRecords() {
        this.updateTradingRecordsTable();
    }

    updateTradingRecordsTable() {
        const tbody = document.getElementById('tradingRecordsTable');
        if (!tbody) return;

        // 确保tradingRecords已初始化
        if (!this.tradingRecords) {
            this.tradingRecords = [];
        }

        tbody.innerHTML = this.tradingRecords.slice(0, 10).map(record => `
            <tr>
                <td>${record.time}</td>
                <td>${record.stock}</td>
                <td>${record.operation}</td>
                <td>${record.quantity}</td>
                <td>¥${record.price}</td>
                <td>¥${record.amount}</td>
                <td class="${record.status === '成功' ? 'positive' : 'negative'}">${record.status}</td>
            </tr>
        `).join('');
    }
}

// 初始化股票应用
const stockApp = new StockApp();

// 启动实时更新
stockApp.startRealTimeUpdates();

// 添加投资相关的CSS样式
const investmentStyles = `
<style>
.investment-advice .message-bubble {
    background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
    border: 1px solid #3b82f6;
    color: #1e40af;
}

</style>
`;

// 将样式添加到页面
document.head.insertAdjacentHTML('beforeend', investmentStyles);

// Z-R模型训练页面样式
const zrTrainingStyles = `
<style>
/* Z-R模型选择器样式 */
.model-selector {
    display: flex;
    align-items: center;
    margin-right: 15px;
}

.model-switch {
    position: relative;
    display: inline-flex;
    align-items: center;
    cursor: pointer;
}

.model-switch input[type="checkbox"] {
    opacity: 0;
    width: 0;
    height: 0;
}

.slider {
    position: relative;
    display: inline-block;
    width: 50px;
    height: 24px;
    background-color: #ccc;
    border-radius: 24px;
    transition: 0.4s;
    margin-right: 8px;
}

.slider:before {
    position: absolute;
    content: "";
    height: 18px;
    width: 18px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    border-radius: 50%;
    transition: 0.4s;
}

.model-switch input:checked + .slider {
    background-color: #2196F3;
}

.model-switch input:checked + .slider:before {
    transform: translateX(26px);
}

.model-label {
    font-size: 14px;
    color: #333;
    font-weight: 500;
}

/* Z-R模型训练页面样式 */
.training-overview {
    margin-bottom: 30px;
}

.overview-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
}

.training-stat {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 12px;
    padding: 20px;
    text-align: center;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.training-stat .stat-icon {
    font-size: 2rem;
    margin-bottom: 10px;
}

.training-stat h4 {
    font-size: 1.8rem;
    margin: 10px 0 5px 0;
    font-weight: 700;
}

.training-stat p {
    margin: 0;
    opacity: 0.9;
    font-size: 0.9rem;
}

/* 数据录入区域 */
.data-input-section {
    background: white;
    border-radius: 12px;
    padding: 25px;
    margin-bottom: 30px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 25px;
    padding-bottom: 15px;
    border-bottom: 2px solid #f0f0f0;
}

.section-header h4 {
    margin: 0;
    color: #333;
    font-size: 1.3rem;
    display: flex;
    align-items: center;
    gap: 10px;
}

.input-tabs {
    display: flex;
    gap: 10px;
}

.tab-btn {
    padding: 8px 16px;
    border: 2px solid #e0e0e0;
    background: white;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.3s ease;
    font-size: 14px;
    font-weight: 500;
}

.tab-btn.active {
    background: #667eea;
    color: white;
    border-color: #667eea;
}

.tab-btn:hover:not(.active) {
    border-color: #667eea;
    color: #667eea;
}

.input-tab-content {
    display: none;
}

.input-tab-content.active {
    display: block;
}

.input-form {
    max-width: 100%;
}

.form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
}

.form-group {
    display: flex;
    flex-direction: column;
}

.form-group.full-width {
    grid-column: 1 / -1;
}

.form-group label {
    margin-bottom: 8px;
    font-weight: 600;
    color: #333;
    font-size: 14px;
}

.form-input, .form-select {
    padding: 12px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    font-size: 14px;
    transition: border-color 0.3s ease;
}

.form-input:focus, .form-select:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-actions {
    display: flex;
    gap: 15px;
    margin-top: 25px;
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;
}

/* 上传区域 */
.upload-area {
    border: 2px dashed #667eea;
    border-radius: 12px;
    padding: 40px;
    text-align: center;
    background: #f8f9ff;
    margin-bottom: 20px;
    transition: all 0.3s ease;
}

.upload-area:hover {
    border-color: #4c63d2;
    background: #f0f2ff;
}

.upload-content i {
    font-size: 3rem;
    color: #667eea;
    margin-bottom: 15px;
}

.upload-content h4 {
    margin: 15px 0 10px 0;
    color: #333;
}

.upload-content p {
    color: #666;
    margin-bottom: 20px;
}

.upload-tips {
    background: #f8f9fa;
    padding: 20px;
    border-radius: 8px;
    border-left: 4px solid #667eea;
}

.upload-tips h5 {
    margin: 0 0 10px 0;
    color: #333;
}

.upload-tips ul {
    margin: 0;
    padding-left: 20px;
    color: #666;
}

.upload-tips li {
    margin-bottom: 5px;
}

/* 导入选项 */
.import-options {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
}

.import-card {
    background: white;
    border: 2px solid #e0e0e0;
    border-radius: 12px;
    padding: 25px;
    text-align: center;
    transition: all 0.3s ease;
}

.import-card:hover {
    border-color: #667eea;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.1);
}

.import-icon {
    font-size: 2.5rem;
    color: #667eea;
    margin-bottom: 15px;
}

.import-card h5 {
    margin: 15px 0 10px 0;
    color: #333;
}

.import-card p {
    color: #666;
    margin-bottom: 20px;
    line-height: 1.5;
}

/* 训练数据管理 */
.training-data-management {
    background: white;
    border-radius: 12px;
    padding: 25px;
    margin-bottom: 30px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.data-controls {
    display: flex;
    gap: 15px;
    align-items: center;
    flex-wrap: wrap;
}

.data-controls .form-input {
    min-width: 200px;
}

.data-table-container {
    overflow-x: auto;
    margin: 20px 0;
}

.data-table {
    width: 100%;
    border-collapse: collapse;
    background: white;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.data-table th {
    background: #667eea;
    color: white;
    padding: 15px 12px;
    text-align: left;
    font-weight: 600;
    font-size: 14px;
}

.data-table td {
    padding: 12px;
    border-bottom: 1px solid #f0f0f0;
    font-size: 14px;
}

.data-table tr:hover {
    background: #f8f9ff;
}

.operation-badge {
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
}

.operation-badge.buy {
    background: #d4edda;
    color: #155724;
}

.operation-badge.sell {
    background: #f8d7da;
    color: #721c24;
}

.operation-badge.hold {
    background: #d1ecf1;
    color: #0c5460;
}

.environment-badge {
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
}

.environment-badge.bull {
    background: #d4edda;
    color: #155724;
}

.environment-badge.bear {
    background: #f8d7da;
    color: #721c24;
}

.environment-badge.sideways {
    background: #fff3cd;
    color: #856404;
}

.positive {
    color: #28a745;
    font-weight: 600;
}

.negative {
    color: #dc3545;
    font-weight: 600;
}

/* 训练配置 */
.training-config {
    background: white;
    border-radius: 12px;
    padding: 25px;
    margin-bottom: 30px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.config-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
}

.config-group {
    display: flex;
    flex-direction: column;
}

.config-group label {
    margin-bottom: 8px;
    font-weight: 600;
    color: #333;
    font-size: 14px;
}

/* 训练可视化 */
.training-visualization {
    background: white;
    border-radius: 12px;
    padding: 25px;
    margin-bottom: 30px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.visualization-tabs {
    display: flex;
    gap: 10px;
    margin-bottom: 25px;
    border-bottom: 2px solid #f0f0f0;
    padding-bottom: 15px;
}

.visualization-content {
    position: relative;
}

.viz-tab {
    display: none;
}

.viz-tab.active {
    display: block;
}

.chart-container {
    width: 100%;
    border-radius: 8px;
    background: #fafafa;
    border: 1px solid #e0e0e0;
}

/* 模型评估 */
.model-evaluation {
    background: white;
    border-radius: 12px;
    padding: 25px;
    margin-bottom: 30px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.evaluation-metrics {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
}

.metric-card {
    background: linear-gradient(135deg, #4ecdc4 0%, #44a08d 100%);
    color: white;
    border-radius: 12px;
    padding: 20px;
    text-align: center;
    box-shadow: 0 4px 15px rgba(78, 205, 196, 0.3);
}

.metric-card .metric-icon {
    font-size: 2rem;
    margin-bottom: 10px;
}

.metric-card h5 {
    margin: 10px 0 5px 0;
    font-size: 1rem;
    opacity: 0.9;
}

.metric-value {
    font-size: 2rem;
    font-weight: 700;
    margin: 10px 0;
}

.evaluation-charts {
    margin-top: 30px;
}

.chart-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

.chart-item {
    background: #f8f9fa;
    border-radius: 8px;
    padding: 20px;
}

.chart-item h5 {
    margin: 0 0 15px 0;
    color: #333;
    text-align: center;
}

/* 分页控件 */
.pagination-container {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 10px;
    margin-top: 20px;
}

.pagination-btn {
    padding: 8px 12px;
    border: 1px solid #ddd;
    background: white;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.pagination-btn:hover:not(:disabled) {
    background: #667eea;
    color: white;
    border-color: #667eea;
}

.pagination-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.pagination-info {
    color: #666;
    font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .form-row {
        grid-template-columns: 1fr;
    }
    
    .data-controls {
        flex-direction: column;
        align-items: stretch;
    }
    
    .data-controls .form-input {
        min-width: auto;
    }
    
    .chart-row {
        grid-template-columns: 1fr;
    }
    
    .overview-stats {
        grid-template-columns: repeat(2, 1fr);
    }
    
    .evaluation-metrics {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 480px) {
    .overview-stats {
        grid-template-columns: 1fr;
    }
    
    .evaluation-metrics {
        grid-template-columns: 1fr;
    }
    
    .input-tabs {
        flex-direction: column;
    }
    
    .visualization-tabs {
        flex-direction: column;
    }
}
</style>
`;

// 将Z-R训练样式添加到页面
document.head.insertAdjacentHTML('beforeend', zrTrainingStyles);

// Z-R模型训练功能
class ZRModelTraining {
    constructor() {
        this.trainingData = [];
        this.modelMetrics = {
            accuracy: 0,
            precision: 0,
            recall: 0,
            f1: 0
        };
        this.trainingCharts = {};
        this.isTraining = false;
        this.init();
    }

    init() {
        this.initEventListeners();
        this.initCharts();
        this.loadMockData();
        this.updateOverview();
    }

    initEventListeners() {
        // 数据录入相关事件
        document.getElementById('addTrainingData')?.addEventListener('click', () => this.addTrainingData());
        document.getElementById('clearForm')?.addEventListener('click', () => this.clearForm());
        document.getElementById('startTraining')?.addEventListener('click', () => this.startTraining());
        document.getElementById('refreshTraining')?.addEventListener('click', () => this.refreshTraining());
        
        // 文件上传事件
        document.getElementById('trainingFileInput')?.addEventListener('change', (e) => this.handleFileUpload(e));
        
        // 数据导入事件
        document.getElementById('importFromAPI')?.addEventListener('click', () => this.importFromAPI());
        document.getElementById('importFromKnowledge')?.addEventListener('click', () => this.importFromKnowledge());
        
        // 数据管理事件
        document.getElementById('searchTrainingData')?.addEventListener('input', (e) => this.searchTrainingData(e.target.value));
        document.getElementById('filterTrainingData')?.addEventListener('change', (e) => this.filterTrainingData(e.target.value));
        document.getElementById('deleteSelectedData')?.addEventListener('click', () => this.deleteSelectedData());
        document.getElementById('selectAllData')?.addEventListener('change', (e) => this.selectAllData(e.target.checked));
        
        // 标签页切换事件
        this.initTabSwitching();
    }

    initTabSwitching() {
        // 输入标签页切换
        document.querySelectorAll('[data-input-tab]').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const tabName = e.target.getAttribute('data-input-tab');
                this.switchInputTab(tabName);
            });
        });

        // 可视化标签页切换
        document.querySelectorAll('[data-viz-tab]').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const tabName = e.target.getAttribute('data-viz-tab');
                this.switchVizTab(tabName);
            });
        });
    }

    switchInputTab(tabName) {
        // 移除所有活动状态
        document.querySelectorAll('.input-tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.querySelectorAll('[data-input-tab]').forEach(tab => {
            tab.classList.remove('active');
        });

        // 激活选中的标签页
        document.getElementById(tabName)?.classList.add('active');
        document.querySelector(`[data-input-tab="${tabName}"]`)?.classList.add('active');
    }

    switchVizTab(tabName) {
        // 移除所有活动状态
        document.querySelectorAll('.viz-tab').forEach(content => {
            content.classList.remove('active');
        });
        document.querySelectorAll('[data-viz-tab]').forEach(tab => {
            tab.classList.remove('active');
        });

        // 激活选中的标签页
        document.getElementById(tabName)?.classList.add('active');
        document.querySelector(`[data-viz-tab="${tabName}"]`)?.classList.add('active');
    }

    addTrainingData() {
        const formData = {
            stockCode: document.getElementById('stockCode').value,
            stockName: document.getElementById('stockName').value,
            operationType: document.getElementById('operationType').value,
            stockPrice: parseFloat(document.getElementById('stockPrice').value) || 0,
            quantity: parseInt(document.getElementById('quantity').value) || 0,
            operationTime: document.getElementById('operationTime').value || new Date().toISOString().slice(0, 16),
            profitLoss: parseFloat(document.getElementById('profitLoss').value) || 0,
            profitLossRatio: parseFloat(document.getElementById('profitLossRatio').value) || 0,
            marketEnvironment: document.getElementById('marketEnvironment').value,
            strategyDescription: document.getElementById('strategyDescription').value,
            id: Date.now()
        };

        if (!formData.stockCode || !formData.stockName) {
            this.showNotification('请填写股票代码和名称', 'warning');
            return;
        }

        this.trainingData.push(formData);
        this.updateTrainingDataTable();
        this.updateOverview();
        this.clearForm();
        this.showNotification('训练数据添加成功', 'success');
    }

    clearForm() {
        document.getElementById('stockCode').value = '';
        document.getElementById('stockName').value = '';
        document.getElementById('operationType').value = 'buy';
        document.getElementById('stockPrice').value = '';
        document.getElementById('quantity').value = '';
        document.getElementById('operationTime').value = '';
        document.getElementById('profitLoss').value = '';
        document.getElementById('profitLossRatio').value = '';
        document.getElementById('marketEnvironment').value = 'bull';
        document.getElementById('strategyDescription').value = '';
    }

    handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const data = this.parseFileData(e.target.result, file.name);
                this.trainingData = this.trainingData.concat(data);
                this.updateTrainingDataTable();
                this.updateOverview();
                this.showNotification(`成功导入 ${data.length} 条数据`, 'success');
            } catch (error) {
                this.showNotification('文件解析失败: ' + error.message, 'error');
            }
        };
        reader.readAsText(file);
    }

    parseFileData(content, fileName) {
        const extension = fileName.split('.').pop().toLowerCase();
        
        switch (extension) {
            case 'csv':
                return this.parseCSV(content);
            case 'json':
                return JSON.parse(content);
            case 'xlsx':
            case 'xls':
                // 这里需要xlsx库支持，暂时返回模拟数据
                return this.generateMockData(10);
            default:
                throw new Error('不支持的文件格式');
        }
    }

    parseCSV(content) {
        const lines = content.split('\n');
        const headers = lines[0].split(',').map(h => h.trim());
        const data = [];

        for (let i = 1; i < lines.length; i++) {
            if (lines[i].trim()) {
                const values = lines[i].split(',').map(v => v.trim());
                const row = {};
                headers.forEach((header, index) => {
                    row[header] = values[index] || '';
                });
                data.push({
                    ...row,
                    id: Date.now() + i
                });
            }
        }
        return data;
    }

    importFromAPI() {
        this.showNotification('正在从API导入数据...', 'info');
        // 模拟API导入
        setTimeout(() => {
            const mockData = this.generateMockData(50);
            this.trainingData = this.trainingData.concat(mockData);
            this.updateTrainingDataTable();
            this.updateOverview();
            this.showNotification('API导入完成，新增50条数据', 'success');
        }, 2000);
    }

    importFromKnowledge() {
        this.showNotification('正在从知识库导入数据...', 'info');
        // 模拟知识库导入
        setTimeout(() => {
            const mockData = this.generateMockData(30);
            this.trainingData = this.trainingData.concat(mockData);
            this.updateTrainingDataTable();
            this.updateOverview();
            this.showNotification('知识库导入完成，新增30条数据', 'success');
        }, 1500);
    }

    generateMockData(count) {
        const stocks = [
            { code: '000001', name: '平安银行' },
            { code: '000002', name: '万科A' },
            { code: '600036', name: '招商银行' },
            { code: '000858', name: '五粮液' },
            { code: '600519', name: '贵州茅台' },
            { code: '000725', name: '京东方A' },
            { code: '002415', name: '海康威视' },
            { code: '300059', name: '东方财富' }
        ];
        
        const operations = ['buy', 'sell', 'hold'];
        const environments = ['bull', 'bear', 'sideways'];
        
        return Array.from({ length: count }, (_, i) => {
            const stock = stocks[Math.floor(Math.random() * stocks.length)];
            const operation = operations[Math.floor(Math.random() * operations.length)];
            const price = Math.random() * 200 + 10;
            const quantity = Math.floor(Math.random() * 1000) + 100;
            const profitLoss = (Math.random() - 0.5) * 10000;
            const profitLossRatio = (Math.random() - 0.5) * 20;
            
            return {
                id: Date.now() + i,
                stockCode: stock.code,
                stockName: stock.name,
                operationType: operation,
                stockPrice: price.toFixed(2),
                quantity: quantity,
                operationTime: new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
                profitLoss: profitLoss.toFixed(2),
                profitLossRatio: profitLossRatio.toFixed(2),
                marketEnvironment: environments[Math.floor(Math.random() * environments.length)],
                strategyDescription: `基于技术分析的${operation === 'buy' ? '买入' : operation === 'sell' ? '卖出' : '持有'}策略`
            };
        });
    }

    loadMockData() {
        this.trainingData = this.generateMockData(20);
        this.updateTrainingDataTable();
    }

    updateOverview() {
        document.getElementById('trainingDataCount').textContent = this.trainingData.length;
        document.getElementById('modelAccuracy').textContent = `${this.modelMetrics.accuracy}%`;
        document.getElementById('lastTrainingTime').textContent = this.isTraining ? '训练中...' : '2024-01-15 14:30';
        document.getElementById('trainingStatus').textContent = this.isTraining ? '训练中' : '待训练';
    }

    updateTrainingDataTable() {
        const tbody = document.getElementById('trainingDataTableBody');
        if (!tbody) return;

        tbody.innerHTML = this.trainingData.map(data => `
            <tr>
                <td><input type="checkbox" class="data-checkbox" data-id="${data.id}"></td>
                <td>${data.stockCode}</td>
                <td>${data.stockName}</td>
                <td><span class="operation-badge ${data.operationType}">${this.getOperationText(data.operationType)}</span></td>
                <td>¥${data.stockPrice}</td>
                <td>${data.quantity}</td>
                <td>${data.operationTime}</td>
                <td class="${data.profitLoss >= 0 ? 'positive' : 'negative'}">¥${data.profitLoss}</td>
                <td class="${data.profitLossRatio >= 0 ? 'positive' : 'negative'}">${data.profitLossRatio}%</td>
                <td><span class="environment-badge ${data.marketEnvironment}">${this.getEnvironmentText(data.marketEnvironment)}</span></td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="zrTraining.deleteData(${data.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    getOperationText(type) {
        const types = {
            'buy': '买入',
            'sell': '卖出',
            'hold': '持有'
        };
        return types[type] || type;
    }

    getEnvironmentText(env) {
        const environments = {
            'bull': '牛市',
            'bear': '熊市',
            'sideways': '震荡市'
        };
        return environments[env] || env;
    }

    deleteData(id) {
        this.trainingData = this.trainingData.filter(data => data.id !== id);
        this.updateTrainingDataTable();
        this.updateOverview();
        this.showNotification('数据删除成功', 'success');
    }

    searchTrainingData(query) {
        const filteredData = this.trainingData.filter(data => 
            data.stockCode.includes(query) || 
            data.stockName.includes(query) ||
            data.operationType.includes(query)
        );
        this.updateTableWithData(filteredData);
    }

    filterTrainingData(filter) {
        let filteredData = this.trainingData;
        
        if (filter) {
            switch (filter) {
                case 'buy':
                case 'sell':
                    filteredData = this.trainingData.filter(data => data.operationType === filter);
                    break;
                case 'profit':
                    filteredData = this.trainingData.filter(data => parseFloat(data.profitLoss) > 0);
                    break;
                case 'loss':
                    filteredData = this.trainingData.filter(data => parseFloat(data.profitLoss) < 0);
                    break;
            }
        }
        
        this.updateTableWithData(filteredData);
    }

    updateTableWithData(data) {
        const tbody = document.getElementById('trainingDataTableBody');
        if (!tbody) return;

        tbody.innerHTML = data.map(item => `
            <tr>
                <td><input type="checkbox" class="data-checkbox" data-id="${item.id}"></td>
                <td>${item.stockCode}</td>
                <td>${item.stockName}</td>
                <td><span class="operation-badge ${item.operationType}">${this.getOperationText(item.operationType)}</span></td>
                <td>¥${item.stockPrice}</td>
                <td>${item.quantity}</td>
                <td>${item.operationTime}</td>
                <td class="${item.profitLoss >= 0 ? 'positive' : 'negative'}">¥${item.profitLoss}</td>
                <td class="${item.profitLossRatio >= 0 ? 'positive' : 'negative'}">${item.profitLossRatio}%</td>
                <td><span class="environment-badge ${item.marketEnvironment}">${this.getEnvironmentText(item.marketEnvironment)}</span></td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="zrTraining.deleteData(${item.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    selectAllData(checked) {
        document.querySelectorAll('.data-checkbox').forEach(checkbox => {
            checkbox.checked = checked;
        });
    }

    deleteSelectedData() {
        const selectedIds = Array.from(document.querySelectorAll('.data-checkbox:checked'))
            .map(checkbox => parseInt(checkbox.getAttribute('data-id')));
        
        if (selectedIds.length === 0) {
            this.showNotification('请选择要删除的数据', 'warning');
            return;
        }

        this.trainingData = this.trainingData.filter(data => !selectedIds.includes(data.id));
        this.updateTrainingDataTable();
        this.updateOverview();
        this.showNotification(`成功删除 ${selectedIds.length} 条数据`, 'success');
    }

    async startTraining() {
        if (this.trainingData.length < 10) {
            this.showNotification('训练数据不足，至少需要10条数据', 'warning');
            return;
        }

        this.isTraining = true;
        this.updateOverview();
        this.showNotification('开始训练Z-R模型...', 'info');

        // 模拟训练过程
        await this.simulateTraining();
    }

    async simulateTraining() {
        const epochs = parseInt(document.getElementById('epochs').value) || 100;
        const batchSize = parseInt(document.getElementById('batchSize').value) || 32;
        
        for (let epoch = 0; epoch < epochs; epoch++) {
            // 模拟训练进度
            const progress = ((epoch + 1) / epochs) * 100;
            
            // 更新损失函数图表
            this.updateLossChart(epoch, Math.random() * 2 + 0.1);
            
            // 更新准确率图表
            this.updateAccuracyChart(epoch, Math.min(95, 60 + (epoch / epochs) * 35 + Math.random() * 5));
            
            // 每10个epoch更新一次界面
            if (epoch % 10 === 0) {
                await new Promise(resolve => setTimeout(resolve, 100));
            }
        }

        // 训练完成
        this.isTraining = false;
        this.modelMetrics = {
            accuracy: 87.5,
            precision: 85.2,
            recall: 89.1,
            f1: 87.1
        };
        
        this.updateModelEvaluation();
        this.updateOverview();
        this.showNotification('Z-R模型训练完成！', 'success');
    }

    initCharts() {
        this.initLossChart();
        this.initAccuracyChart();
        this.initPredictionChart();
        this.initFeatureChart();
        this.initConfusionMatrixChart();
        this.initROCChart();
    }

    initLossChart() {
        const chart = echarts.init(document.getElementById('lossChart'));
        this.trainingCharts.loss = chart;
        
        const option = {
            title: {
                text: '训练损失',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis'
            },
            xAxis: {
                type: 'category',
                data: []
            },
            yAxis: {
                type: 'value',
                name: '损失值'
            },
            series: [{
                name: '训练损失',
                type: 'line',
                data: [],
                smooth: true,
                lineStyle: {
                    color: '#ff6b6b'
                },
                areaStyle: {
                    color: {
                        type: 'linear',
                        x: 0, y: 0, x2: 0, y2: 1,
                        colorStops: [{
                            offset: 0, color: 'rgba(255, 107, 107, 0.3)'
                        }, {
                            offset: 1, color: 'rgba(255, 107, 107, 0.1)'
                        }]
                    }
                }
            }]
        };
        
        chart.setOption(option);
    }

    initAccuracyChart() {
        const chart = echarts.init(document.getElementById('accuracyChart'));
        this.trainingCharts.accuracy = chart;
        
        const option = {
            title: {
                text: '训练准确率',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis'
            },
            xAxis: {
                type: 'category',
                data: []
            },
            yAxis: {
                type: 'value',
                name: '准确率(%)',
                min: 0,
                max: 100
            },
            series: [{
                name: '训练准确率',
                type: 'line',
                data: [],
                smooth: true,
                lineStyle: {
                    color: '#4ecdc4'
                },
                areaStyle: {
                    color: {
                        type: 'linear',
                        x: 0, y: 0, x2: 0, y2: 1,
                        colorStops: [{
                            offset: 0, color: 'rgba(78, 205, 196, 0.3)'
                        }, {
                            offset: 1, color: 'rgba(78, 205, 196, 0.1)'
                        }]
                    }
                }
            }]
        };
        
        chart.setOption(option);
    }

    initPredictionChart() {
        const chart = echarts.init(document.getElementById('predictionChart'));
        this.trainingCharts.prediction = chart;
        
        const option = {
            title: {
                text: '预测分析',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis'
            },
            legend: {
                data: ['实际值', '预测值'],
                top: 30
            },
            xAxis: {
                type: 'category',
                data: ['买入', '卖出', '持有']
            },
            yAxis: {
                type: 'value',
                name: '数量'
            },
            series: [
                {
                    name: '实际值',
                    type: 'bar',
                    data: [45, 38, 17],
                    itemStyle: {
                        color: '#ff9f43'
                    }
                },
                {
                    name: '预测值',
                    type: 'bar',
                    data: [42, 41, 17],
                    itemStyle: {
                        color: '#10ac84'
                    }
                }
            ]
        };
        
        chart.setOption(option);
    }

    initFeatureChart() {
        const chart = echarts.init(document.getElementById('featureChart'));
        this.trainingCharts.feature = chart;
        
        const option = {
            title: {
                text: '特征重要性',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'shadow'
                }
            },
            xAxis: {
                type: 'value'
            },
            yAxis: {
                type: 'category',
                data: ['技术指标', '市场环境', '历史价格', '成交量', '基本面', '情绪指标']
            },
            series: [{
                name: '重要性',
                type: 'bar',
                data: [0.25, 0.20, 0.18, 0.15, 0.12, 0.10],
                itemStyle: {
                    color: {
                        type: 'linear',
                        x: 0, y: 0, x2: 1, y2: 0,
                        colorStops: [{
                            offset: 0, color: '#667eea'
                        }, {
                            offset: 1, color: '#764ba2'
                        }]
                    }
                }
            }]
        };
        
        chart.setOption(option);
    }

    initConfusionMatrixChart() {
        const chart = echarts.init(document.getElementById('confusionMatrixChart'));
        this.trainingCharts.confusionMatrix = chart;
        
        const data = [
            [0, 0, 15], [0, 1, 3], [0, 2, 2],
            [1, 0, 2], [1, 1, 12], [1, 2, 1],
            [2, 0, 1], [2, 1, 2], [2, 2, 8]
        ];
        
        const option = {
            title: {
                text: '混淆矩阵',
                left: 'center'
            },
            tooltip: {
                position: 'top'
            },
            grid: {
                height: '50%',
                top: '10%'
            },
            xAxis: {
                type: 'category',
                data: ['买入', '卖出', '持有'],
                splitArea: {
                    show: true
                }
            },
            yAxis: {
                type: 'category',
                data: ['买入', '卖出', '持有'],
                splitArea: {
                    show: true
                }
            },
            visualMap: {
                min: 0,
                max: 15,
                calculable: true,
                orient: 'horizontal',
                left: 'center',
                bottom: '15%'
            },
            series: [{
                name: '混淆矩阵',
                type: 'heatmap',
                data: data,
                label: {
                    show: true
                },
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }]
        };
        
        chart.setOption(option);
    }

    initROCChart() {
        const chart = echarts.init(document.getElementById('rocChart'));
        this.trainingCharts.roc = chart;
        
        const option = {
            title: {
                text: 'ROC曲线',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis'
            },
            legend: {
                data: ['ROC曲线', '随机分类器'],
                top: 30
            },
            xAxis: {
                type: 'value',
                name: '假正率(FPR)',
                min: 0,
                max: 1
            },
            yAxis: {
                type: 'value',
                name: '真正率(TPR)',
                min: 0,
                max: 1
            },
            series: [
                {
                    name: 'ROC曲线',
                    type: 'line',
                    data: [
                        [0, 0], [0.1, 0.2], [0.2, 0.4], [0.3, 0.6], 
                        [0.4, 0.75], [0.5, 0.85], [0.6, 0.9], [0.7, 0.93], 
                        [0.8, 0.96], [0.9, 0.98], [1, 1]
                    ],
                    smooth: true,
                    lineStyle: {
                        color: '#ff6b6b'
                    }
                },
                {
                    name: '随机分类器',
                    type: 'line',
                    data: [[0, 0], [1, 1]],
                    lineStyle: {
                        color: '#ddd',
                        type: 'dashed'
                    }
                }
            ]
        };
        
        chart.setOption(option);
    }

    updateLossChart(epoch, loss) {
        if (!this.trainingCharts.loss) return;
        
        const option = this.trainingCharts.loss.getOption();
        option.xAxis[0].data.push(epoch);
        option.series[0].data.push(loss);
        
        this.trainingCharts.loss.setOption(option);
    }

    updateAccuracyChart(epoch, accuracy) {
        if (!this.trainingCharts.accuracy) return;
        
        const option = this.trainingCharts.accuracy.getOption();
        option.xAxis[0].data.push(epoch);
        option.series[0].data.push(accuracy);
        
        this.trainingCharts.accuracy.setOption(option);
    }

    updateModelEvaluation() {
        document.getElementById('accuracyMetric').textContent = `${this.modelMetrics.accuracy}%`;
        document.getElementById('precisionMetric').textContent = `${this.modelMetrics.precision}%`;
        document.getElementById('recallMetric').textContent = `${this.modelMetrics.recall}%`;
        document.getElementById('f1Metric').textContent = `${this.modelMetrics.f1}%`;
    }

    refreshTraining() {
        this.loadMockData();
        this.updateOverview();
        this.showNotification('训练数据已刷新', 'success');
    }

    showNotification(message, type = 'info') {
        // 使用现有的通知系统
        if (window.app && window.app.showNotification) {
            window.app.showNotification(message, type);
        } else {
            console.log(`[${type.toUpperCase()}] ${message}`);
        }
    }
}

// 在StockApp中集成Z-R模型训练功能
StockApp.prototype.initZRTraining = function() {
    this.zrTraining = new ZRModelTraining();
};

// 扩展StockApp的initStockFeatures方法
const originalInitStockFeatures = StockApp.prototype.initStockFeatures;
StockApp.prototype.initStockFeatures = function() {
    originalInitStockFeatures.call(this);
    this.initZRTraining();
};

// 添加Z-R模型选择功能到聊天功能
StockApp.prototype.sendMessage = function() {
    const input = document.getElementById('chatInput');
    const message = input.value.trim();
    
    if (!message) return;
    
    // 检查是否使用Z-R模型
    const useZRModel = document.getElementById('useZRModel')?.checked || false;
    
    if (useZRModel) {
        this.sendZRMessage(message);
    } else {
        // 使用原有的RAG流程
        this.sendRAGMessage(message);
    }
    
    input.value = '';
};

StockApp.prototype.sendZRMessage = function(message) {
    // 添加用户消息到聊天界面
    this.addMessage(message, 'user');
    
    // 显示Z-R模型处理中
    const botMessage = this.addMessage('Z-R模型正在分析中...', 'bot');
    
    // 模拟Z-R模型响应
    setTimeout(() => {
        const zrResponse = this.generateZRResponse(message);
        this.updateMessage(botMessage, zrResponse);
    }, 1500);
};

StockApp.prototype.generateZRResponse = function(message) {
    // 基于训练数据生成智能响应
    const responses = [
        "基于您的历史交易数据，我建议在当前市场环境下采用保守策略。",
        "根据Z-R模型分析，该股票的技术指标显示买入信号，但需要关注风险控制。",
        "模型预测显示，此类操作在相似市场环境下的成功率为75%。",
        "建议设置止损位在-5%，止盈位在+10%，以控制风险。",
        "基于您的交易模式，建议分批建仓，避免一次性重仓操作。"
    ];
    
    return responses[Math.floor(Math.random() * responses.length)];
};

StockApp.prototype.sendRAGMessage = function(message) {
    // 原有的RAG消息处理逻辑
    this.addMessage(message, 'user');
    
    // 显示处理中
    const botMessage = this.addMessage('正在分析中...', 'bot');
    
    // 模拟RAG响应
    setTimeout(() => {
        const ragResponse = "这是基于RAG知识库的回答...";
        this.updateMessage(botMessage, ragResponse);
    }, 1000);
};

// 初始化Z-R模型训练
let zrTraining;
document.addEventListener('DOMContentLoaded', function() {
    if (window.app && window.app.zrTraining) {
        zrTraining = window.app.zrTraining;
    }
});
