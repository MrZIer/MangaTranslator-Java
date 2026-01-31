# 图片展示功能测试指南

## 快速测试步骤

### 1. 启动应用

```bash
# Windows
.\gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

等待应用启动完成，看到 "Started MangaTransApplication" 消息。

### 2. 准备测试数据

创建测试文件夹结构：

```bash
# 在项目根目录创建测试数据
mkdir -p storage/batch_20260131_120000/session_20260131_120001_test01/translated
mkdir -p storage/session_20260131_120002_test02/translated

# 复制一些测试图片到translated目录
# 或使用实际的翻译结果
```

### 3. 测试API接口

#### 测试批量文件夹列表API
```bash
curl http://localhost:8080/api/gallery/batch-folders
```

预期响应：
```json
{
  "code": 200,
  "message": "Batch folders retrieved",
  "data": [
    {
      "name": "batch_20260131_120000",
      "path": "storage\\batch_20260131_120000",
      "sessionCount": 1,
      "createdTime": "2026-01-31T12:00:00Z"
    }
  ]
}
```

#### 测试单个文件夹列表API
```bash
curl http://localhost:8080/api/gallery/session-folders
```

预期响应：
```json
{
  "code": 200,
  "message": "Session folders retrieved",
  "data": [
    {
      "name": "session_20260131_120002_test02",
      "path": "storage\\session_20260131_120002_test02",
      "imageCount": 0,
      "createdTime": "2026-01-31T12:00:02Z"
    }
  ]
}
```

#### 测试获取批量文件夹内的会话
```bash
curl "http://localhost:8080/api/gallery/batch-folders/batch_20260131_120000/sessions"
```

#### 测试获取文件夹图片列表
```bash
curl "http://localhost:8080/api/gallery/images?folderPath=storage/session_20260131_120002_test02"
```

### 4. 测试前端界面

1. **打开主页**
   ```
   http://localhost:8080/index.html
   ```
   - 检查右上角是否有"查看翻译结果"按钮
   - 点击按钮跳转到图片展示页面

2. **测试图片展示页面**
   ```
   http://localhost:8080/gallery.html
   ```
   - 检查左侧边栏是否显示文件夹列表
   - 点击批量文件夹，查看会话列表
   - 点击单个文件夹或会话，查看图片列表

3. **测试图片功能**
   - 滚动查看所有图片（单列布局）
   - 点击图片打开全屏预览
   - 使用键盘方向键切换图片
   - 点击下载按钮下载图片
   - 点击"下载全部"下载ZIP文件

### 5. 浏览器控制台测试

打开浏览器开发者工具（F12），在 Console 中执行：

```javascript
// 测试API调用
fetch('/api/gallery/batch-folders')
  .then(r => r.json())
  .then(data => console.log('Batch Folders:', data));

fetch('/api/gallery/session-folders')
  .then(r => r.json())
  .then(data => console.log('Session Folders:', data));
```

### 6. 功能验证清单

- [ ] API端点正常响应（状态码200）
- [ ] 批量文件夹列表正确显示
- [ ] 单个文件夹列表正确显示
- [ ] 点击批量文件夹显示会话列表
- [ ] 点击会话或单个文件夹显示图片列表
- [ ] 图片以单列形式展示（一行一张）
- [ ] 图片可以正常加载显示
- [ ] 点击图片打开全屏预览
- [ ] 键盘导航正常工作（← → ESC）
- [ ] 下载单张图片功能正常
- [ ] 下载全部图片（ZIP）功能正常
- [ ] 刷新按钮正常工作
- [ ] 关闭按钮正常工作
- [ ] 响应式布局在不同屏幕尺寸下正常

## 测试用例

### 用例1：查看批量翻译结果
**步骤**：
1. 完成一次批量翻译（多个文件）
2. 打开图片展示页面
3. 在左侧选择对应的batch文件夹
4. 查看所有会话
5. 点击任一会话查看图片

**预期**：所有翻译后的图片按顺序单列展示

### 用例2：查看单个翻译结果
**步骤**：
1. 完成一次单个文件翻译
2. 打开图片展示页面
3. 在左侧选择对应的session文件夹
4. 直接查看图片

**预期**：翻译后的图片正常显示

### 用例3：图片预览和导航
**步骤**：
1. 在图片列表中点击任意图片
2. 使用右箭头键切换到下一张
3. 使用左箭头键切换到上一张
4. 按ESC键关闭预览

**预期**：所有操作流畅无卡顿

### 用例4：批量下载
**步骤**：
1. 选择一个包含多张图片的文件夹
2. 点击"下载全部"按钮
3. 等待下载完成
4. 解压ZIP文件

**预期**：ZIP包含所有图片且完整无损

## 性能测试

### 测试大量文件夹
创建100个测试文件夹：
```bash
for i in {1..100}; do
  mkdir -p "storage/session_20260131_$(printf '%06d' $i)_test/translated"
done
```

检查页面加载时间和响应速度。

### 测试大量图片
在一个文件夹中放入50张图片，检查：
- 页面滚动是否流畅
- 图片懒加载是否生效
- 内存占用是否合理

## 故障模拟

### 测试空文件夹
```bash
mkdir -p storage/session_empty/translated
```
**预期**：显示"此文件夹中没有图片"

### 测试不存在的文件夹
访问：
```
http://localhost:8080/api/gallery/images?folderPath=storage/nonexistent
```
**预期**：返回错误消息

### 测试损坏的图片
放入一个损坏的图片文件。
**预期**：该图片显示为空或错误图标，其他图片正常显示

## 日志检查

启动应用后，检查日志输出：

```
# 正常日志示例
2026-01-31 12:00:00.123  INFO --- GalleryController : Batch folders retrieved: 2
2026-01-31 12:00:01.234  INFO --- GalleryController : Session folders retrieved: 5
2026-01-31 12:00:02.345  INFO --- GalleryController : Images loaded for folder: session_xxx
```

## 测试完成标准

✅ 所有API端点返回正确的数据格式
✅ 前端页面正确渲染所有内容
✅ 图片展示功能完整可用
✅ 下载功能正常工作
✅ 无JavaScript错误
✅ 无后端异常日志
✅ 性能表现良好（加载时间 < 2秒）

## 问题报告

如发现问题，请记录：
1. 问题描述
2. 复现步骤
3. 预期结果 vs 实际结果
4. 浏览器控制台错误
5. 后端日志错误
6. 截图（如适用）
