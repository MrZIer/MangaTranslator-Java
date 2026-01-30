// API基础URL
const API_BASE = window.location.origin;

// 全局状态
let currentTasks = new Map();
let pollingIntervals = new Map();

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
        for (const file of files) {
            await uploadFile(file, sourceLanguage, targetLanguage, engine, outputFormat);
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
async function uploadFile(file, sourceLanguage, targetLanguage, engine, outputFormat) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceLanguage', sourceLanguage);
    formData.append('targetLanguage', targetLanguage);
    formData.append('engine', engine);
    formData.append('outputFormat', outputFormat);
    
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
    const interval = setInterval(async () => {
        try {
            const response = await fetch(`${API_BASE}/api/tasks/${sessionId}/progress`);
            const result = await response.json();
            
            if (result.success) {
                updateTaskProgress(sessionId, result.data);
                
                // 如果任务完成或失败，停止轮询
                if (result.data.status === 'COMPLETED' || result.data.status === 'FAILED') {
                    clearInterval(interval);
                    pollingIntervals.delete(sessionId);
                    loadHistory(); // 刷新历史记录
                }
            }
        } catch (error) {
            console.error('轮询失败:', error);
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
    
    // 完成后启用下载按钮
    if (progress.status === 'COMPLETED') {
        downloadBtn.disabled = false;
        downloadBtn.onclick = () => downloadResult(sessionId);
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
        const response = await fetch(`${API_BASE}/api/download/${sessionId}`);
        
        if (!response.ok) {
            throw new Error('下载失败');
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `translated_${sessionId}.zip`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showNotification('下载成功', 'success');
    } catch (error) {
        showNotification('下载失败: ' + error.message, 'error');
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

// 定期检查服务器状态
setInterval(checkServerStatus, 30000);
