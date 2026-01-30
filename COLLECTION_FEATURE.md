# 翻译结果汇总功能说明

## 功能概述

系统新增了**翻译结果汇总**功能，可以将分散在各个会话目录中的翻译结果文件自动收集到统一的目录中，方便用户批量管理和使用翻译成果。

## 功能特点

### 1. 自动汇总
- ✅ 扫描所有已完成的翻译会话
- ✅ 自动复制所有翻译结果文件（`*_cn.jpg`、`*_cn.png`）
- ✅ 按时间戳创建独立的汇总目录
- ✅ 避免文件名冲突（自动添加会话ID前缀）

### 2. 灵活操作
- 📦 **汇总所有结果**：一键收集所有已完成翻译的文件
- 🎯 **汇总单个会话**：针对特定会话进行结果汇总
- 📋 **查看汇总历史**：列出所有历史汇总目录及文件数量

### 3. 安全可靠
- 🔒 原始文件保持不变（使用复制而非移动）
- 📝 详细的日志记录
- ⚠️ 完善的错误处理

## 使用方法

### 前端界面操作

#### 1. 汇总所有结果
在"翻译结果浏览"区域，点击 **"汇总所有结果"** 按钮：

```
🔵 翻译结果浏览
   ┌─────────────────────┐
   │ 汇总所有结果 | 刷新  │
   └─────────────────────┘
```

系统会：
1. 弹出确认对话框
2. 扫描所有已完成的会话
3. 将翻译结果复制到新创建的汇总目录
4. 显示汇总完成信息（路径、文件数量）

#### 2. 查看汇总结果
汇总完成后会显示信息框：
```
✅ 汇总完成
📁 汇总路径: F:\some_project\mangaTrans\storage\collected_results_20260131_032045
📊 文件数量: 15 个

💡 所有翻译结果已集中到上述目录，您可以在文件管理器中打开该目录查看所有文件。
```

### API接口使用

#### 1. 汇总所有翻译结果
```bash
POST http://localhost:8080/api/upload/collect-all
```

**响应示例**：
```json
{
  "success": true,
  "message": "汇总完成",
  "collectionPath": "F:\\some_project\\mangaTrans\\storage\\collected_results_20260131_032045",
  "fileCount": 15
}
```

#### 2. 汇总指定会话结果
```bash
POST http://localhost:8080/api/upload/collect/{sessionId}
```

**参数**：
- `sessionId`: 会话ID

**响应示例**：
```json
{
  "success": true,
  "message": "汇总完成",
  "collectionPath": "F:\\some_project\\mangaTrans\\storage\\collected_results_20260131_032100",
  "fileCount": 1
}
```

#### 3. 获取汇总目录列表
```bash
GET http://localhost:8080/api/upload/collections
```

**响应示例**：
```json
{
  "success": true,
  "data": [
    {
      "name": "collected_results_20260131_032045",
      "path": "F:\\some_project\\mangaTrans\\storage\\collected_results_20260131_032045",
      "fileCount": 15,
      "createdTime": 1738281645000
    },
    {
      "name": "collected_results_20260131_031520",
      "path": "F:\\some_project\\mangaTrans\\storage\\collected_results_20260131_031520",
      "fileCount": 8,
      "createdTime": 1738281320000
    }
  ]
}
```

## 目录结构

### 汇总前（分散状态）
```
storage/
├── session-id-1/
│   └── translated_output/
│       ├── 001_cn.jpg
│       └── 001_cn.json
├── session-id-2/
│   └── translated_output/
│       ├── 002_cn.jpg
│       └── 002_cn.json
└── session-id-3/
    └── translated_output/
        ├── 003_cn.jpg
        └── 003_cn.json
```

### 汇总后（集中状态）
```
storage/
├── collected_results_20260131_032045/
│   ├── session-1_001_cn.jpg    # 添加会话ID前缀避免冲突
│   ├── session-2_002_cn.jpg
│   └── session-3_003_cn.jpg
├── session-id-1/                # 原始文件保持不变
│   └── translated_output/
│       ├── 001_cn.jpg
│       └── 001_cn.json
├── session-id-2/
│   └── translated_output/
│       └── ...
└── session-id-3/
    └── translated_output/
        └── ...
```

## 文件命名规则

### 自动避免冲突
系统采用智能命名策略避免文件名冲突：

1. **添加会话ID前缀**：
   ```
   原文件名: 001_cn.jpg
   汇总后:   afd9d878_001_cn.jpg  # 添加会话ID前8位作为前缀
   ```

2. **序号递增**（如果仍有冲突）：
   ```
   afd9d878_001_cn.jpg
   afd9d878_001_cn_1.jpg
   afd9d878_001_cn_2.jpg
   ```

## 实现细节

### 后端服务类
- **ResultCollectionService**：结果汇总核心服务
  - `collectAllResults()`: 汇总所有已完成会话的结果
  - `collectSessionResult(sessionId)`: 汇总指定会话的结果
  - `getCollectionDirectories()`: 获取所有汇总目录列表
  - `generateUniqueFileName()`: 生成唯一文件名避免冲突

### API控制器
- **UploadController**新增端点：
  - `POST /api/upload/collect-all`: 汇总所有结果
  - `POST /api/upload/collect/{sessionId}`: 汇总单个会话
  - `GET /api/upload/collections`: 查询汇总目录列表

### 前端功能
- **app.js**新增函数：
  - `collectAllResults()`: 触发汇总所有结果
  - `collectSessionResult(sessionId)`: 汇总单个会话结果
  - `displayCollectionInfo(result)`: 显示汇总结果信息

## 使用场景

### 场景1：批量处理完成后统一管理
```
用户翻译了一整本漫画（50张图片）
→ 点击"汇总所有结果"
→ 系统自动将50个翻译文件复制到统一目录
→ 用户可以直接打开该目录查看或分享所有翻译成果
```

### 场景2：定期整理翻译成果
```
用户每天翻译一些图片
→ 每周点击一次"汇总所有结果"
→ 按周整理翻译成果到独立目录
→ 便于归档和版本管理
```

### 场景3：选择性汇总
```
用户只想汇总某次翻译任务的结果
→ 通过API调用指定会话ID的汇总接口
→ 只汇总该次任务的翻译文件
```

## 注意事项

1. **存储空间**：
   - 汇总操作会复制文件，确保有足够的磁盘空间
   - 每次汇总会创建新目录，建议定期清理旧的汇总目录

2. **并发安全**：
   - 汇总操作是线程安全的
   - 多个用户可以同时进行汇总操作

3. **原始文件**：
   - 汇总操作不会删除或移动原始文件
   - 原始文件保持在各自的会话目录中

4. **文件格式**：
   - 目前只汇总图片文件（`.jpg`、`.png`）
   - JSON文件不会被汇总（仅供系统内部使用）

## 未来扩展

可能的功能增强：
- [ ] 支持批量下载汇总目录为ZIP文件
- [ ] 添加汇总目录的自动清理策略
- [ ] 支持用户自定义汇总目录名称
- [ ] 添加汇总进度显示
- [ ] 支持选择性汇总（勾选特定会话）

## 技术栈

- **后端**：Spring Boot 3.5.10 + Java 17
- **存储**：本地文件系统（NIO）
- **前端**：Vanilla JavaScript
- **样式**：CSS3 动画和渐变效果

---

**更新时间**：2026-01-31  
**版本**：v1.0.0
