// API基础URL
const API_BASE = window.location.origin;

// 全局状态
let currentTasks = new Map();
let pollingIntervals = new Map();

// 生成批次ID（用于同一批次的多个文件）
function generateBatchId() {
    const timestamp = new Date().toISOString().replace(/[-:]/g, '').replace('T', '_').split('.')[0];
    const random = Math.random().toString(36).substring(2, 10);
    return `batch_${timestamp}_${random}`;
}

// DOM元素
const uploadArea = document.getElementById('uploadArea');
const fileInput = document.getElementById('fileInput');
const startBtn = document.getElementById('startTranslation');
const tasksList = document.getElementById('tasksList');
const historyList = document.getElementById('historyList');
const progressModal = document.getElementById('progressModal');
const progressBar = document.getElementById('progressBar');
const progressText = document.getElementById('progressText');
const progressStage = document.getElementById('progressStage');
const progressDetail = document.getElementById('progressDetail');

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();
    loadHistory();
    loadResults(0); // 加载翻译结果浏览
    checkServerStatus();
});

// 初始化事件监听
function initEventListeners() {
    // 文件上传
    uploadArea.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', handleFileSelect);
    
    // 拖拽上传
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
    });
    
    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('drag-over');
    });
    
    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        handleFileSelect({ target: { files: e.dataTransfer.files } });
    });
    
    // 开始翻译
    startBtn.addEventListener('click', startTranslation);
    
    // 历史记录按钮
    document.getElementById('refreshHistory').addEventListener('click', loadHistory);
    document.getElementById('clearHistory').addEventListener('click', clearHistory);
}

// 文件选择处理
function handleFileSelect(e) {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    
    updateUploadArea(files);
}

// 更新上传区域显示
function updateUploadArea(files) {
    const placeholder = uploadArea.querySelector('.upload-placeholder');
    if (files.length > 0) {
        placeholder.innerHTML = `
            <i class="fas fa-check-circle" style="color: var(--success-color);"></i>
            <p>已选择 ${files.length} 个文件</p>
            <p class="upload-hint">${files.map(f => f.name).join(', ')}</p>
        `;
    }
}

// 开始翻译
async function startTranslation() {
    const files = fileInput.files;
    if (files.length === 0) {
        showNotification('请先选择文件', 'warning');
        return;
    }
    
    const sourceLanguage = document.getElementById('sourceLanguage').value;
    const targetLanguage = document.getElementById('targetLanguage').value;
    const engine = document.getElementById('translationEngine').value;
    const outputFormat = document.getElementById('outputFormat').value;
    
    if (sourceLanguage === targetLanguage) {
        showNotification('源语言和目标语言不能相同', 'warning');
        return;
    }
    
    startBtn.disabled = true;
    startBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 上传中...';
    
    try {
        const isBatch = files.length > 1; // 判断是否为批量上传
        const batchId = isBatch ? generateBatchId() : null; // 生成批次ID
        
        for (const file of files) {
            await uploadFile(file, sourceLanguage, targetLanguage, engine, outputFormat, isBatch, batchId);
        }
        
        // 重置表单
        fileInput.value = '';
        uploadArea.querySelector('.upload-placeholder').innerHTML = `
            <i class="fas fa-images"></i>
            <p>拖拽文件到此处，或点击选择文件</p>
            <p class="upload-hint">支持 JPG, PNG, PDF, ZIP 格式</p>
        `;
        
        showNotification('任务已提交，开始翻译', 'success');
    } catch (error) {
        showNotification('上传失败: ' + error.message, 'error');
    } finally {
        startBtn.disabled = false;
        startBtn.innerHTML = '<i class="fas fa-play"></i> 开始翻译';
    }
}

// 上传文件
async function uploadFile(file, sourceLanguage, targetLanguage, engine, outputFormat, isBatch = false, batchId = null) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceLanguage', sourceLanguage);
    formData.append('targetLanguage', targetLanguage);
    formData.append('engine', engine);
    formData.append('outputFormat', outputFormat);
    formData.append('isBatch', isBatch); // 传递批量标识
    if (batchId) {
        formData.append('batchId', batchId); // 传递批次ID
    }
    
    const response = await fetch(`${API_BASE}/api/upload`, {
        method: 'POST',
        body: formData
    });
    
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const result = await response.json();
    
    if (result.success) {
        const task = result.data;
        addTask(task);
        startPolling(task.sessionId);
    } else {
        throw new Error(result.message || '上传失败');
    }
}

// 添加任务到列表
function addTask(task) {
    // 移除空状态
    const emptyState = tasksList.querySelector('.empty-state');
    if (emptyState) {
        emptyState.remove();
    }
    
    currentTasks.set(task.sessionId, task);
    
    const taskElement = document.createElement('div');
    taskElement.className = 'task-item';
    taskElement.id = `task-${task.sessionId}`;
    taskElement.innerHTML = `
        <div class="task-header">
            <div class="task-title">
                <i class="fas fa-file-image"></i>
                ${task.fileName || '未知文件'}
            </div>
            <span class="task-status processing">
                <i class="fas fa-spinner fa-spin"></i> 处理中
            </span>
        </div>
        <div class="task-progress">
            <div class="progress-bar-container">
                <div class="progress-bar" style="width: 0%">
                    <span>0%</span>
                </div>
            </div>
        </div>
        <div class="task-info">
            <div>会话ID: ${task.sessionId}</div>
            <div>当前阶段: 初始化</div>
        </div>
        <div class="task-actions">
            <button class="btn-compare" onclick="showCompareView('${task.sessionId}')" style="display:none">
                <i class="fas fa-columns"></i> 对比
            </button>
            <button class="btn-view-gallery" onclick="viewInGallery('${task.sessionId}')" style="display:none">
                <i class="fas fa-images"></i> 查看结果
            </button>
            <button class="btn-download" disabled>
                <i class="fas fa-download"></i> 下载
            </button>
            <button class="btn-cancel" onclick="cancelTask('${task.sessionId}')">
                <i class="fas fa-times"></i> 取消
            </button>
        </div>
    `;
    
    tasksList.insertBefore(taskElement, tasksList.firstChild);
}

// 开始轮询任务进度
function startPolling(sessionId) {
    console.log(`[${sessionId}] 开始轮询任务进度...`);
    
    const interval = setInterval(async () => {
        try {
            const response = await fetch(`${API_BASE}/api/tasks/${sessionId}/progress`);
            const result = await response.json();
            
            if (result.success) {
                updateTaskProgress(sessionId, result.data);
                
                // 只有在真正完成或失败时才停止轮询
                if (result.data.status === 'COMPLETED' && result.data.progress === 100) {
                    console.log(`[${sessionId}] 任务完成，停止轮询`);
                    clearInterval(interval);
                    pollingIntervals.delete(sessionId);
                    loadHistory(); // 刷新历史记录
                } else if (result.data.status === 'FAILED') {
                    console.log(`[${sessionId}] 任务失败，停止轮询`);
                    clearInterval(interval);
                    pollingIntervals.delete(sessionId);
                    loadHistory(); // 刷新历史记录
                }
            }
        } catch (error) {
            console.error(`[${sessionId}] 轮询失败:`, error);
        }
    }, 2000); // 每2秒轮询一次
    
    pollingIntervals.set(sessionId, interval);
}

// 更新任务进度
function updateTaskProgress(sessionId, progress) {
    const taskElement = document.getElementById(`task-${sessionId}`);
    if (!taskElement) return;
    
    const progressBar = taskElement.querySelector('.progress-bar');
    const progressSpan = progressBar.querySelector('span');
    const statusBadge = taskElement.querySelector('.task-status');
    const taskInfo = taskElement.querySelector('.task-info');
    const downloadBtn = taskElement.querySelector('.btn-download');
    
    // 记录进度更新（用于调试）
    console.log(`[${sessionId}] 进度更新: ${progress.progress}% - ${progress.status} - ${progress.currentStage}`);
    
    // 更新进度条
    progressBar.style.width = `${progress.progress}%`;
    progressSpan.textContent = `${progress.progress}%`;
    
    // 更新状态
    const statusMap = {
        'UPLOAD': { icon: 'cloud-upload-alt', text: '上传完成', class: 'processing' },
        'PREPROCESS': { icon: 'cog', text: '预处理中', class: 'processing' },
        'OCR': { icon: 'eye', text: '识别中', class: 'processing' },
        'TRANSLATE': { icon: 'language', text: '翻译中', class: 'processing' },
        'RENDER': { icon: 'paint-brush', text: '渲染中', class: 'processing' },
        'PACKAGE': { icon: 'box', text: '打包中', class: 'processing' },
        'COMPLETED': { icon: 'check-circle', text: '已完成', class: 'completed' },
        'FAILED': { icon: 'exclamation-circle', text: '失败', class: 'failed' }
    };
    
    const status = statusMap[progress.status] || statusMap['UPLOAD'];
    statusBadge.className = `task-status ${status.class}`;
    statusBadge.innerHTML = `<i class="fas fa-${status.icon}"></i> ${status.text}`;
    
    // 更新信息
    taskInfo.innerHTML = `
        <div>会话ID: ${sessionId}</div>
        <div>当前阶段: ${progress.currentStage || status.text}</div>
        ${progress.errorMessage ? `<div style="color: var(--error-color);">错误: ${progress.errorMessage}</div>` : ''}
    `;
    
    // 只有真正完成时（status=COMPLETED 且 progress=100）才启用按钮
    if (progress.status === 'COMPLETED' && progress.progress === 100) {
        console.log(`[${sessionId}] 任务真正完成！启用按钮`);
        downloadBtn.disabled = false;
        downloadBtn.onclick = () => downloadResult(sessionId);
        
        // 显示对比按钮
        const compareBtn = taskElement.querySelector('.btn-compare');
        if (compareBtn) {
            compareBtn.style.display = 'inline-flex';
        }
        
        // 显示查看结果按钮
        const viewGalleryBtn = taskElement.querySelector('.btn-view-gallery');
        if (viewGalleryBtn) {
            viewGalleryBtn.style.display = 'inline-flex';
        }
    } else {
        // 未完成时确保按钮保持禁用
        downloadBtn.disabled = true;
        const compareBtn = taskElement.querySelector('.btn-compare');
        if (compareBtn) {
            compareBtn.style.display = 'none';
        }
        const viewGalleryBtn = taskElement.querySelector('.btn-view-gallery');
        if (viewGalleryBtn) {
            viewGalleryBtn.style.display = 'none';
        }
    }
}

// 取消任务
function cancelTask(sessionId) {
    if (confirm('确定要取消这个任务吗？')) {
        const interval = pollingIntervals.get(sessionId);
        if (interval) {
            clearInterval(interval);
            pollingIntervals.delete(sessionId);
        }
        
        const taskElement = document.getElementById(`task-${sessionId}`);
        if (taskElement) {
            taskElement.remove();
        }
        
        currentTasks.delete(sessionId);
        
        if (currentTasks.size === 0) {
            tasksList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <p>暂无翻译任务</p>
                </div>
            `;
        }
        
        showNotification('任务已取消', 'success');
    }
}

// 下载结果
async function downloadResult(sessionId) {
    try {
        // 直接使用链接方式下载，而不是fetch
        window.location.href = `${API_BASE}/api/upload/download/${sessionId}`;
        showNotification('下载已开始', 'success');
    } catch (error) {
        showNotification('下载失败: ' + error.message, 'error');
    }
}

// 在图片库中查看结果
async function viewInGallery(sessionId) {
    try {
        // 获取会话详情以获得会话目录路径
        const response = await fetch(`${API_BASE}/api/tasks/${sessionId}`);
        const result = await response.json();
        
        if (result.success && result.data && result.data.sessionDirectory) {
            // 跳转到gallery页面并传递文件夹路径
            window.open(`gallery.html?folder=${encodeURIComponent(result.data.sessionDirectory)}`, '_blank');
        } else {
            showNotification('无法获取会话目录信息', 'warning');
        }
    } catch (error) {
        console.error('查看结果失败:', error);
        showNotification('查看结果失败: ' + error.message, 'error');
    }
}

// 加载历史记录
async function loadHistory() {
    try {
        const response = await fetch(`${API_BASE}/api/history?page=0&size=20`);
        const result = await response.json();
        
        if (result.success && result.data.content.length > 0) {
            historyList.innerHTML = '';
            result.data.content.forEach(record => {
                const historyItem = document.createElement('div');
                historyItem.className = 'history-item';
                historyItem.innerHTML = `
                    <div class="history-item-header">
                        <i class="fas fa-file-alt"></i>
                        <div class="history-item-title">${record.originalFileName || '未知文件'}</div>
                    </div>
                    <div class="history-item-info">
                        <div><i class="fas fa-clock"></i> ${formatDate(record.startTime)}</div>
                        <div><i class="fas fa-language"></i> ${record.sourceLanguage} → ${record.targetLanguage}</div>
                        <div><i class="fas fa-robot"></i> ${record.engine}</div>
                        ${record.completionTime ? `<div><i class="fas fa-stopwatch"></i> 用时: ${calculateDuration(record.startTime, record.completionTime)}</div>` : ''}
                    </div>
                `;
                historyItem.onclick = () => viewHistoryDetail(record);
                historyList.appendChild(historyItem);
            });
        } else {
            historyList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-clock"></i>
                    <p>暂无历史记录</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('加载历史记录失败:', error);
    }
}

// 查看历史详情
function viewHistoryDetail(record) {
    showNotification(`文件: ${record.originalFileName}\n状态: ${record.status}`, 'success');
}

// 清空历史
async function clearHistory() {
    if (confirm('确定要清空所有历史记录吗？')) {
        // TODO: 实现清空历史记录的API
        showNotification('历史记录已清空', 'success');
        loadHistory();
    }
}

// 检查服务器状态
async function checkServerStatus() {
    const statusBadge = document.getElementById('connection-status');
    try {
        const response = await fetch(`${API_BASE}/actuator/health`);
        const result = await response.json();
        
        if (result.status === 'UP') {
            statusBadge.innerHTML = '<i class="fas fa-circle"></i> 连接正常';
            statusBadge.style.background = '#f0fdf4';
            statusBadge.style.color = 'var(--success-color)';
        } else {
            throw new Error('服务异常');
        }
    } catch (error) {
        statusBadge.innerHTML = '<i class="fas fa-circle"></i> 连接失败';
        statusBadge.style.background = '#fee2e2';
        statusBadge.style.color = 'var(--error-color)';
    }
}

// 显示通知
function showNotification(message, type = 'success') {
    const container = document.getElementById('notificationContainer');
    
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    
    const iconMap = {
        success: 'check-circle',
        error: 'exclamation-circle',
        warning: 'exclamation-triangle'
    };
    
    notification.innerHTML = `
        <i class="fas fa-${iconMap[type]}"></i>
        <div>${message}</div>
    `;
    
    container.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideIn 0.3s ease reverse';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// 格式化日期
function formatDate(dateString) {
    if (!dateString) return '未知';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 计算时长
function calculateDuration(startTime, endTime) {
    if (!startTime || !endTime) return '未知';
    const start = new Date(startTime);
    const end = new Date(endTime);
    const duration = Math.floor((end - start) / 1000); // 秒
    
    if (duration < 60) return `${duration}秒`;
    if (duration < 3600) return `${Math.floor(duration / 60)}分${duration % 60}秒`;
    return `${Math.floor(duration / 3600)}小时${Math.floor((duration % 3600) / 60)}分`;
}

// 显示对比视图
async function showCompareView(sessionId) {
    try {
        const response = await fetch(`${API_BASE}/api/upload/compare/${sessionId}`);
        if (!response.ok) throw new Error('获取对比图片失败');
        
        const result = await response.json();
        if (!result.success) throw new Error(result.message);
        
        const data = result.data;
        
        // 创建对比弹窗
        const modal = document.createElement('div');
        modal.className = 'compare-modal';
        modal.innerHTML = `
            <div class="compare-container">
                <div class="compare-header">
                    <h3><i class="fas fa-columns"></i> 翻译对比 - ${data.fileName}</h3>
                    <button class="btn-close" onclick="closeCompareView()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="compare-content">
                    <div class="compare-image-panel">
                        <h4>原图</h4>
                        <div class="image-wrapper">
                            <img src="${data.originalImage}" alt="原图">
                        </div>
                    </div>
                    <div class="compare-image-panel">
                        <h4>翻译后</h4>
                        <div class="image-wrapper">
                            ${data.translatedImage ? 
                                `<img src="${data.translatedImage}" alt="翻译后">` : 
                                '<div class="loading-placeholder"><i class="fas fa-spinner fa-spin"></i> 翻译中...</div>'
                            }
                        </div>
                    </div>
                </div>
                <div class="compare-actions">
                    <button class="btn-primary" onclick="downloadTranslated('${sessionId}')">
                        <i class="fas fa-download"></i> 下载翻译结果
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // 点击背景关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeCompareView();
            }
        });
        
    } catch (error) {
        console.error('显示对比视图失败:', error);
        showNotification('显示对比视图失败: ' + error.message, 'error');
    }
}

// 关闭对比视图
function closeCompareView() {
    const modal = document.querySelector('.compare-modal');
    if (modal) {
        modal.remove();
    }
}

// 下载翻译结果
function downloadTranslated(sessionId) {
    window.location.href = `${API_BASE}/api/upload/download/${sessionId}`;
}

// ============ 翻译结果浏览功能 ============

// 分页状态
let currentPage = 0;
const pageSize = 12;
let totalPages = 0;

// 加载翻译结果列表
async function loadResults(page = 0) {
    try {
        const response = await fetch(`${API_BASE}/api/upload/results?page=${page}&size=${pageSize}`);
        const result = await response.json();
        
        if (result.success && result.data) {
            const results = result.data;
            const pagination = result.pagination;
            
            currentPage = pagination.page;
            totalPages = pagination.totalPages;
            
            renderResults(results);
            renderPagination(pagination);
        } else {
            showEmptyResults();
        }
    } catch (error) {
        console.error('加载结果失败:', error);
        showNotification('加载结果失败: ' + error.message, 'error');
        showEmptyResults();
    }
}

// 渲染结果网格
function renderResults(results) {
    const resultsGrid = document.getElementById('resultsGrid');
    
    if (!results || results.length === 0) {
        showEmptyResults();
        return;
    }
    
    resultsGrid.innerHTML = results.map(result => `
        <div class="result-card" onclick="showCompareView('${result.sessionId}')">
            <div class="result-card-image">
                <img src="${result.translatedImage}" 
                     alt="${result.resultFileName}"
                     onerror="this.src='data:image/svg+xml,%3Csvg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'100\\' height=\\'100\\'%3E%3Crect fill=\\'%23ddd\\' width=\\'100\\' height=\\'100\\'/%3E%3Ctext x=\\'50%25\\' y=\\'50%25\\' text-anchor=\\'middle\\' dy=\\'.3em\\' fill=\\'%23999\\'%3E暂无图片%3C/text%3E%3C/svg%3E'">
                <div class="result-card-badge">
                    <i class="fas fa-check"></i> 已完成
                </div>
            </div>
            <div class="result-card-content">
                <h3 class="result-card-title" title="${result.resultFileName}">
                    ${result.resultFileName}
                </h3>
                <div class="result-card-meta">
                    <span><i class="fas fa-file"></i> 原文件: ${result.originalFileName}</span>
                    <span><i class="fas fa-calendar-alt"></i> 完成时间: ${formatDateTime(result.completedAt)}</span>
                </div>
                <div class="result-card-actions">
                    <button class="btn-card btn-view" onclick="event.stopPropagation(); showCompareView('${result.sessionId}')">
                        <i class="fas fa-eye"></i> 查看对比
                    </button>
                    <button class="btn-card btn-download" onclick="event.stopPropagation(); downloadTranslated('${result.sessionId}')">
                        <i class="fas fa-download"></i> 下载
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

// 显示空结果
function showEmptyResults() {
    const resultsGrid = document.getElementById('resultsGrid');
    resultsGrid.innerHTML = `
        <div class="results-empty" style="grid-column: 1 / -1;">
            <i class="fas fa-folder-open"></i>
            <h3>暂无翻译结果</h3>
            <p>上传图片开始翻译吧！</p>
        </div>
    `;
    
    // 隐藏分页
    document.getElementById('pagination').style.display = 'none';
}

// 渲染分页控件
function renderPagination(pagination) {
    const paginationDiv = document.getElementById('pagination');
    const prevBtn = document.getElementById('prevPage');
    const nextBtn = document.getElementById('nextPage');
    const pageNumbers = document.getElementById('pageNumbers');
    
    if (pagination.totalPages <= 1) {
        paginationDiv.style.display = 'none';
        return;
    }
    
    paginationDiv.style.display = 'flex';
    
    // 上一页按钮
    prevBtn.disabled = !pagination.hasPrevious;
    
    // 下一页按钮
    nextBtn.disabled = !pagination.hasNext;
    
    // 页码按钮
    pageNumbers.innerHTML = '';
    const startPage = Math.max(0, pagination.page - 2);
    const endPage = Math.min(pagination.totalPages - 1, pagination.page + 2);
    
    // 第一页
    if (startPage > 0) {
        pageNumbers.innerHTML += `
            <button class="page-number" onclick="goToPage(0)">1</button>
        `;
        if (startPage > 1) {
            pageNumbers.innerHTML += `<span style="color: white;">...</span>`;
        }
    }
    
    // 中间页码
    for (let i = startPage; i <= endPage; i++) {
        const isActive = i === pagination.page ? 'active' : '';
        pageNumbers.innerHTML += `
            <button class="page-number ${isActive}" onclick="goToPage(${i})">${i + 1}</button>
        `;
    }
    
    // 最后一页
    if (endPage < pagination.totalPages - 1) {
        if (endPage < pagination.totalPages - 2) {
            pageNumbers.innerHTML += `<span style="color: white;">...</span>`;
        }
        pageNumbers.innerHTML += `
            <button class="page-number" onclick="goToPage(${pagination.totalPages - 1})">${pagination.totalPages}</button>
        `;
    }
}

// 跳转到指定页
function goToPage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    loadResults(page);
    
    // 滚动到结果区域
    document.getElementById('resultsSection').scrollIntoView({ behavior: 'smooth' });
}

// 刷新结果列表
function refreshResults() {
    loadResults(currentPage);
    showNotification('结果列表已刷新', 'success');
}

// 格式化日期时间
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '未知';
    
    const date = new Date(dateTimeStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return '刚刚';
    if (diffMins < 60) return `${diffMins}分钟前`;
    
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}小时前`;
    
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${month}-${day} ${hours}:${minutes}`;
}

// 页面加载时加载结果
document.addEventListener('DOMContentLoaded', () => {
    loadResults(0);
});

// ============ 结果汇总功能 ============

// 汇总批量文件夹的翻译结果
async function collectBatchFolderResults() {
    try {
        // 获取批量文件夹列表
        showNotification('正在加载批量文件夹列表...', 'info');
        
        const response = await fetch(`${API_BASE}/api/upload/batch-folders`);
        const result = await response.json();
        
        if (!result.success || !result.folders || result.folders.length === 0) {
            showNotification('没有找到批量文件夹', 'warning');
            return;
        }
        
        // 创建选择对话框
        const folderList = result.folders.map((folder, index) => 
            `${index + 1}. ${folder.name} (${folder.sessionCount} 个会话)`
        ).join('\n');
        
        const selectedIndex = prompt(
            `请选择要汇总的批量文件夹（输入序号）：\n\n${folderList}`,
            '1'
        );
        
        if (!selectedIndex) {
            return; // 用户取消
        }
        
        const index = parseInt(selectedIndex) - 1;
        if (index < 0 || index >= result.folders.length) {
            showNotification('无效的选择', 'error');
            return;
        }
        
        const selectedFolder = result.folders[index];
        
        // 确认汇总
        if (!confirm(`确认汇总批量文件夹 "${selectedFolder.name}" 吗？\n包含 ${selectedFolder.sessionCount} 个会话`)) {
            return;
        }
        
        // 执行汇总
        showNotification('正在汇总批量文件夹...', 'info');
        
        const collectResponse = await fetch(`${API_BASE}/api/upload/collect-batch`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                batchFolderPath: selectedFolder.path
            })
        });
        
        const collectResult = await collectResponse.json();
        
        if (collectResult.success) {
            const message = `✅ 批量汇总完成！\n📁 汇总路径: ${collectResult.collectionPath}\n📊 文件数量: ${collectResult.fileCount}`;
            alert(message);
            showNotification(collectResult.message, 'success');
            
            // 显示汇总目录位置
            displayCollectionInfo(collectResult);
        } else {
            showNotification(collectResult.message, 'error');
        }
        
    } catch (error) {
        console.error('批量汇总失败:', error);
        showNotification('批量汇总失败: ' + error.message, 'error');
    }
}

// 汇总所有翻译结果
async function collectAllResults() {
    try {
        // 显示确认对话框
        if (!confirm('是否将所有已完成的翻译结果汇总到统一目录？')) {
            return;
        }
        
        showNotification('正在汇总结果...', 'info');
        
        const response = await fetch(`${API_BASE}/api/upload/collect-all`, {
            method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.success) {
            const message = `✅ 汇总完成！\n📁 汇总路径: ${result.collectionPath}\n📊 文件数量: ${result.fileCount}`;
            alert(message);
            showNotification(result.message, 'success');
            
            // 显示汇总目录位置
            displayCollectionInfo(result);
        } else {
            showNotification(result.message, 'error');
        }
        
    } catch (error) {
        console.error('汇总失败:', error);
        showNotification('汇总失败: ' + error.message, 'error');
    }
}

// 汇总单个会话结果
async function collectSessionResult(sessionId) {
    try {
        showNotification('正在汇总该会话结果...', 'info');
        
        const response = await fetch(`${API_BASE}/api/upload/collect/${sessionId}`, {
            method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.success) {
            const message = `✅ 汇总完成！\n📁 汇总路径: ${result.collectionPath}\n📊 文件数量: ${result.fileCount}`;
            alert(message);
            showNotification(result.message, 'success');
        } else {
            showNotification(result.message, 'error');
        }
        
    } catch (error) {
        console.error('汇总失败:', error);
        showNotification('汇总失败: ' + error.message, 'error');
    }
}

// 显示汇总信息
function displayCollectionInfo(result) {
    // 创建信息显示框
    const infoBox = document.createElement('div');
    infoBox.className = 'collection-info-box';
    infoBox.innerHTML = `
        <div class="collection-info-content">
            <div class="collection-info-header">
                <i class="fas fa-check-circle"></i>
                <h3>汇总完成</h3>
            </div>
            <div class="collection-info-body">
                <p><strong>汇总路径:</strong></p>
                <p class="collection-path">${result.collectionPath}</p>
                <p><strong>文件数量:</strong> ${result.fileCount} 个</p>
                <p class="collection-tip">
                    <i class="fas fa-info-circle"></i> 
                    所有翻译结果已集中到上述目录，您可以在文件管理器中打开该目录查看所有文件。
                </p>
            </div>
            <div class="collection-info-footer">
                <button onclick="this.parentElement.parentElement.parentElement.remove()">
                    关闭
                </button>
            </div>
        </div>
    `;
    
    // 添加样式
    const style = document.createElement('style');
    style.textContent = `
        .collection-info-box {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: white;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
            z-index: 10000;
            max-width: 600px;
            width: 90%;
        }
        
        .collection-info-content {
            padding: 30px;
        }
        
        .collection-info-header {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 20px;
            color: var(--success-color);
        }
        
        .collection-info-header i {
            font-size: 2rem;
        }
        
        .collection-info-header h3 {
            font-size: 1.5rem;
            margin: 0;
        }
        
        .collection-info-body p {
            margin: 12px 0;
            line-height: 1.6;
        }
        
        .collection-path {
            background: var(--bg-color);
            padding: 12px;
            border-radius: 8px;
            font-family: monospace;
            word-break: break-all;
            color: var(--primary-color);
        }
        
        .collection-tip {
            background: #e8f5e9;
            padding: 12px;
            border-radius: 8px;
            border-left: 4px solid var(--success-color);
            color: #2e7d32;
            font-size: 0.9rem;
        }
        
        .collection-info-footer {
            margin-top: 20px;
            text-align: right;
        }
        
        .collection-info-footer button {
            background: var(--primary-color);
            color: white;
            border: none;
            padding: 10px 24px;
            border-radius: 8px;
            cursor: pointer;
            font-weight: 600;
            transition: all 0.3s ease;
        }
        
        .collection-info-footer button:hover {
            background: var(--primary-hover);
            transform: translateY(-2px);
        }
    `;
    
    document.head.appendChild(style);
    document.body.appendChild(infoBox);
    
    // 3秒后自动关闭
    setTimeout(() => {
        if (infoBox.parentElement) {
            infoBox.remove();
        }
    }, 10000);
}

// 定期检查服务器状态
setInterval(checkServerStatus, 30000);
