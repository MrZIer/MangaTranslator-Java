// 图片展示页面JavaScript

class GalleryManager {
    constructor() {
        this.currentFolder = null;
        this.currentImages = [];
        this.currentImageIndex = 0;
        this.init();
    }

    init() {
        this.loadBatchFolders();
        this.loadSessionFolders();
        this.setupEventListeners();
        
        // 检查URL参数，如果有folder参数则自动打开
        const urlParams = new URLSearchParams(window.location.search);
        const folderPath = urlParams.get('folder');
        if (folderPath) {
            this.openFolderFromUrl(folderPath);
        }
    }

    setupEventListeners() {
        // 刷新按钮
        document.getElementById('refreshBtn').addEventListener('click', () => {
            this.loadBatchFolders();
            this.loadSessionFolders();
        });

        // 关闭按钮
        document.getElementById('closeBatchView').addEventListener('click', () => {
            this.showWelcome();
        });

        document.getElementById('closeGalleryView').addEventListener('click', () => {
            this.showWelcome();
        });

        // 下载全部按钮
        document.getElementById('downloadAllBtn').addEventListener('click', () => {
            this.downloadAllImages();
        });

        // 模态框
        const modal = document.getElementById('imageModal');
        const modalClose = document.querySelector('.modal-close');

        modalClose.addEventListener('click', () => {
            modal.style.display = 'none';
        });

        window.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });

        // 模态框导航
        document.getElementById('prevImageBtn').addEventListener('click', () => {
            this.showPreviousImage();
        });

        document.getElementById('nextImageBtn').addEventListener('click', () => {
            this.showNextImage();
        });

        document.getElementById('downloadModalImageBtn').addEventListener('click', () => {
            this.downloadCurrentImage();
        });

        // 键盘导航
        document.addEventListener('keydown', (e) => {
            if (modal.style.display === 'block') {
                if (e.key === 'ArrowLeft') {
                    this.showPreviousImage();
                } else if (e.key === 'ArrowRight') {
                    this.showNextImage();
                } else if (e.key === 'Escape') {
                    modal.style.display = 'none';
                }
            }
        });
    }

    // 加载批量文件夹列表
    async loadBatchFolders() {
        const container = document.getElementById('batchFoldersList');
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> 加载中...</div>';

        try {
            const response = await fetch('/api/gallery/batch-folders');
            const result = await response.json();

            if (result.success && result.data && result.data.length > 0) {
                container.innerHTML = '';
                result.data.forEach(folder => {
                    const folderItem = this.createFolderItem(folder, 'batch');
                    container.appendChild(folderItem);
                });
            } else {
                container.innerHTML = '<div class="empty-state"><i class="fas fa-folder-open"></i><p>暂无批量文件夹</p></div>';
            }
        } catch (error) {
            console.error('Failed to load batch folders:', error);
            container.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><p>加载失败</p></div>';
        }
    }

    // 加载单个会话文件夹列表
    async loadSessionFolders() {
        const container = document.getElementById('sessionFoldersList');
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> 加载中...</div>';

        try {
            const response = await fetch('/api/gallery/session-folders');
            const result = await response.json();

            if (result.success && result.data && result.data.length > 0) {
                container.innerHTML = '';
                result.data.forEach(folder => {
                    const folderItem = this.createFolderItem(folder, 'session');
                    container.appendChild(folderItem);
                });
            } else {
                container.innerHTML = '<div class="empty-state"><i class="fas fa-folder-open"></i><p>暂无单个文件夹</p></div>';
            }
        } catch (error) {
            console.error('Failed to load session folders:', error);
            container.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><p>加载失败</p></div>';
        }
    }

    // 创建文件夹项
    createFolderItem(folder, type) {
        const div = document.createElement('div');
        div.className = 'folder-item';
        
        const icon = type === 'batch' ? 'fa-layer-group' : 'fa-folder';
        
        // 判断是否是汇总文件夹
        const isCollected = folder.isCollected || folder.name.includes('_collected_');
        let countLabel, count;
        
        if (isCollected) {
            // 汇总文件夹显示图片数
            countLabel = '图片';
            count = folder.imageCount || 0;
        } else if (type === 'batch') {
            // 批量文件夹显示会话数
            countLabel = '会话';
            count = folder.sessionCount || 0;
        } else {
            // 单个文件夹显示图片数
            countLabel = '图片';
            count = folder.imageCount || 0;
        }

        div.innerHTML = `
            <div class="folder-name">
                <i class="fas ${icon}"></i>
                ${folder.name}
            </div>
            <div class="folder-meta">
                <span><i class="fas fa-${isCollected ? 'images' : (type === 'batch' ? 'folder' : 'image')}"></i> ${count} ${countLabel}</span>
            </div>
        `;

        div.addEventListener('click', () => {
            // 移除其他active状态
            document.querySelectorAll('.folder-item').forEach(item => {
                item.classList.remove('active');
            });
            div.classList.add('active');

            // 如果是汇总文件夹，直接显示图片
            if (isCollected) {
                this.showFolderImages(folder);
            } else if (type === 'batch') {
                this.showBatchSessions(folder);
            } else {
                this.showFolderImages(folder);
            }
        });

        return div;
    }

    // 显示批量文件夹的会话列表
    async showBatchSessions(batchFolder) {
        this.hideAllViews();
        const view = document.getElementById('batchSessionsView');
        view.style.display = 'block';

        document.getElementById('batchFolderTitle').innerHTML = `
            ${batchFolder.name}
            <button class="btn-collect" onclick="galleryManager.collectAndShowBatch('${batchFolder.path}')" title="收集所有图片到新文件夹并展示">
                <i class="fas fa-folder-plus"></i> 收集并展示所有图片
            </button>
        `;

        const container = document.getElementById('batchSessionsList');
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> 加载会话...</div>';

        try {
            const response = await fetch(`/api/gallery/batch-folders/${encodeURIComponent(batchFolder.name)}/sessions`);
            const result = await response.json();

            if (result.success && result.data && result.data.length > 0) {
                container.innerHTML = '';
                result.data.forEach(session => {
                    const card = this.createSessionCard(session);
                    container.appendChild(card);
                });
            } else {
                container.innerHTML = '<div class="empty-state"><i class="fas fa-folder-open"></i><p>此批量文件夹中没有会话</p></div>';
            }
        } catch (error) {
            console.error('Failed to load batch sessions:', error);
            container.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><p>加载失败</p></div>';
        }
    }

    // 创建会话卡片
    createSessionCard(session) {
        const div = document.createElement('div');
        div.className = 'session-card';

        div.innerHTML = `
            <div class="session-card-header">
                <div class="session-icon">
                    <i class="fas fa-image"></i>
                </div>
                <div>
                    <div class="session-card-title">${session.name}</div>
                    <div class="session-card-meta">
                        <i class="fas fa-images"></i> ${session.imageCount} 张图片
                    </div>
                </div>
            </div>
        `;

        div.addEventListener('click', () => {
            this.showFolderImages(session);
        });

        return div;
    }

    // 显示文件夹的图片列表
    async showFolderImages(folder) {
        this.hideAllViews();
        this.currentFolder = folder;

        const view = document.getElementById('imageGalleryView');
        view.style.display = 'block';

        document.getElementById('galleryTitle').textContent = folder.name;
        document.getElementById('gallerySubtitle').textContent = `共 ${folder.imageCount || 0} 张图片`;

        const container = document.getElementById('imagesList');
        container.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> 加载图片...</div>';

        try {
            const response = await fetch(`/api/gallery/images?folderPath=${encodeURIComponent(folder.path)}`);
            const result = await response.json();

            if (result.success && result.data && result.data.images.length > 0) {
                this.currentImages = result.data.images;
                container.innerHTML = '';
                
                result.data.images.forEach((image, index) => {
                    const imageItem = this.createImageItem(image, folder.path, index);
                    container.appendChild(imageItem);
                });
            } else {
                container.innerHTML = '<div class="empty-state"><i class="fas fa-image"></i><p>此文件夹中没有图片</p></div>';
            }
        } catch (error) {
            console.error('Failed to load images:', error);
            container.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><p>加载失败</p></div>';
        }
    }

    // 创建图片项（垂直瀑布流展示）
    createImageItem(image, folderPath, index) {
        const div = document.createElement('div');
        div.className = 'image-card';

        const imageUrl = `/api/gallery/view?folderPath=${encodeURIComponent(folderPath)}&filename=${encodeURIComponent(image.filename)}`;
        const downloadUrl = `/api/gallery/download?folderPath=${encodeURIComponent(folderPath)}&filename=${encodeURIComponent(image.filename)}`;

        div.innerHTML = `
            <div class="image-card-header">
                <div class="image-card-title">
                    <i class="fas fa-image"></i>
                    <span>${image.filename}</span>
                </div>
                <div class="image-card-actions">
                    <button class="btn-icon-small zoom-btn" title="查看大图">
                        <i class="fas fa-search-plus"></i>
                    </button>
                    <button class="btn-icon-small download-btn" title="下载图片" data-url="${downloadUrl}">
                        <i class="fas fa-download"></i>
                    </button>
                </div>
            </div>
            <div class="image-card-body">
                <img src="${imageUrl}" alt="${image.filename}" loading="lazy" class="full-width-image">
            </div>
            <div class="image-card-footer">
                <span class="image-meta">
                    <i class="fas fa-info-circle"></i> 大小: ${this.formatFileSize(image.size)}
                </span>
            </div>
        `;

        // 点击放大按钮查看大图
        const zoomBtn = div.querySelector('.zoom-btn');
        zoomBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.showImageModal(index);
        });

        // 点击图片也可以查看大图
        const img = div.querySelector('.full-width-image');
        img.addEventListener('click', () => {
            this.showImageModal(index);
        });
        img.style.cursor = 'pointer';

        // 下载按钮
        const downloadBtn = div.querySelector('.download-btn');
        downloadBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            window.location.href = downloadUrl;
        });

        return div;
    }

    // 显示图片模态框
    showImageModal(index) {
        this.currentImageIndex = index;
        const image = this.currentImages[index];
        
        const modal = document.getElementById('imageModal');
        const modalImage = document.getElementById('modalImage');
        const modalCaption = document.getElementById('modalCaption');

        const imageUrl = `/api/gallery/view?folderPath=${encodeURIComponent(this.currentFolder.path)}&filename=${encodeURIComponent(image.filename)}`;
        
        modalImage.src = imageUrl;
        modalCaption.textContent = `${image.filename} (${index + 1}/${this.currentImages.length})`;
        
        modal.style.display = 'block';

        // 更新按钮状态
        document.getElementById('prevImageBtn').disabled = index === 0;
        document.getElementById('nextImageBtn').disabled = index === this.currentImages.length - 1;
    }

    // 上一张图片
    showPreviousImage() {
        if (this.currentImageIndex > 0) {
            this.showImageModal(this.currentImageIndex - 1);
        }
    }

    // 下一张图片
    showNextImage() {
        if (this.currentImageIndex < this.currentImages.length - 1) {
            this.showImageModal(this.currentImageIndex + 1);
        }
    }

    // 下载当前图片
    downloadCurrentImage() {
        const image = this.currentImages[this.currentImageIndex];
        const downloadUrl = `/api/gallery/download?folderPath=${encodeURIComponent(this.currentFolder.path)}&filename=${encodeURIComponent(image.filename)}`;
        window.location.href = downloadUrl;
    }

    // 下载所有图片
    downloadAllImages() {
        if (!this.currentFolder) return;

        const downloadUrl = `/api/gallery/download-folder?folderPath=${encodeURIComponent(this.currentFolder.path)}`;
        window.location.href = downloadUrl;
    }

    // 隐藏所有视图
    hideAllViews() {
        document.getElementById('welcomeMessage').style.display = 'none';
        document.getElementById('batchSessionsView').style.display = 'none';
        document.getElementById('imageGalleryView').style.display = 'none';
    }

    // 显示欢迎消息
    showWelcome() {
        this.hideAllViews();
        document.getElementById('welcomeMessage').style.display = 'block';
        
        // 移除所有active状态
        document.querySelectorAll('.folder-item').forEach(item => {
            item.classList.remove('active');
        });
    }

    // 格式化文件大小
    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
    }
    
    // 从URL参数打开文件夹
    async openFolderFromUrl(folderPath) {
        try {
            // 等待文件夹列表加载完成
            await new Promise(resolve => setTimeout(resolve, 500));
            
            // 构造文件夹对象
            const folder = {
                path: folderPath,
                name: folderPath.split(/[\/\\]/).pop(),
                imageCount: 0
            };
            
            // 直接显示图片
            this.showFolderImages(folder);
        } catch (error) {
            console.error('Failed to open folder from URL:', error);
        }
    }

    // 收集批量文件夹的所有图片并展示
    async collectAndShowBatch(batchFolderPath) {
        const container = document.getElementById('batchSessionsList');
        
        try {
            // 显示加载状态
            container.innerHTML = `
                <div class="loading" style="text-align: center; padding: 40px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #4f46e5;"></i>
                    <p style="margin-top: 15px; font-size: 16px; color: #64748b;">正在收集所有图片...</p>
                    <p style="margin-top: 5px; font-size: 14px; color: #94a3b8;">这可能需要一些时间，请稍候</p>
                </div>
            `;

            // 调用收集API
            const response = await fetch('/api/upload/collect-batch', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    batchFolderPath: `storage/${batchFolderPath}`
                })
            });

            const result = await response.json();
            console.log('收集结果:', result);

            if (!result.success) {
                throw new Error(result.message || '收集失败');
            }

            // 提取收集文件夹的名称（从路径中获取）
            const collectedFolderName = result.collectionPath.split(/[\/\\]/).pop();
            
            // 显示成功消息
            container.innerHTML = `
                <div class="success-message" style="text-align: center; padding: 40px; background: #f0fdf4; border-radius: 12px; margin: 20px;">
                    <i class="fas fa-check-circle" style="font-size: 48px; color: #10b981;"></i>
                    <h3 style="margin-top: 15px; color: #059669;">收集成功！</h3>
                    <p style="margin-top: 10px; color: #64748b;">成功收集 ${result.collectedCount || 0} 张图片</p>
                    <p style="margin-top: 5px; font-size: 14px; color: #94a3b8;">文件夹: ${collectedFolderName}</p>
                </div>
            `;

            // 等待一下让用户看到成功消息
            await new Promise(resolve => setTimeout(resolve, 1000));

            // 自动展示收集后的图片
            const folder = {
                path: collectedFolderName,
                name: collectedFolderName,
                imageCount: result.collectedCount || 0
            };

            await this.showFolderImages(folder);

            // 刷新文件夹列表以显示新的收集文件夹
            await this.loadBatchFolders();

        } catch (error) {
            console.error('收集失败:', error);
            container.innerHTML = `
                <div class="error-message" style="text-align: center; padding: 40px; background: #fef2f2; border-radius: 12px; margin: 20px;">
                    <i class="fas fa-exclamation-triangle" style="font-size: 48px; color: #ef4444;"></i>
                    <h3 style="margin-top: 15px; color: #dc2626;">收集失败</h3>
                    <p style="margin-top: 10px; color: #64748b;">${error.message}</p>
                </div>
            `;
        }
    }
}

// 初始化 - 声明为全局变量
let galleryManager;
document.addEventListener('DOMContentLoaded', () => {
    galleryManager = new GalleryManager();
});
