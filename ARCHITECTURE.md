# 项目架构说明

## 📐 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         前端/客户端                          │
│                    (Browser / Mobile App)                   │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST API
┌────────────────────────┴────────────────────────────────────┐
│                    Spring Boot应用层                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Controller  │  │   Service    │  │  Repository  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                  │              │
│         └────────┬────────┘                  │              │
│                  │                           │              │
│         ┌────────┴────────┐                  │              │
│         │  Async Task     │                  │              │
│         │  Thread Pool    │                  │              │
│         └────────┬────────┘                  │              │
└──────────────────┼─────────────────────────┬─┘              
                   │                         │                
        ┌──────────┴──────────┐   ┌──────────┴──────────┐    
        │   External APIs     │   │    Data Storage     │    
        ├─────────────────────┤   ├─────────────────────┤    
        │ • OCR Service       │   │ • MongoDB           │    
        │ • OpenAI API        │   │ • Redis Cache       │    
        │ • Claude API        │   │ • File System       │    
        │ • DeepSeek API      │   │                     │    
        └─────────────────────┘   └─────────────────────┘    
```

## 🏗️ 分层架构

### 1. Controller层 (控制器)

负责处理HTTP请求和响应。

**主要控制器**:
- `UploadController`: 文件上传和任务创建
- `TaskController`: 任务进度查询
- `HistoryController`: 历史记录管理

**职责**:
- 接收HTTP请求
- 参数验证
- 调用Service层
- 返回统一格式的响应

### 2. Service层 (业务逻辑)

核心业务逻辑处理。

**核心服务**:

#### SessionService
- 会话管理
- 文件MD5计算
- 去重检查
- 状态更新

#### AsyncTaskService
- 异步任务编排
- 完整翻译流程控制
- 进度追踪
- 错误处理

#### OcrService
- 调用manga_ocr微服务
- OCR结果缓存
- 文本区域提取

#### TranslationService
- 多引擎翻译适配
  - OpenAI GPT-4o
  - Claude 3.5
  - DeepSeek
- 翻译缓存
- 术语一致性维护

#### ImageProcessingService
- 图像标准化
- 文本渲染
- 水印添加
- 格式转换

#### FileStorageService
- 文件保存和读取
- 目录管理
- 文件清理

#### HistoryService
- 历史记录CRUD
- 访问统计
- 过期清理

#### FingerprintService
- 浏览器指纹生成
- IP地址提取

### 3. Repository层 (数据访问)

MongoDB数据访问接口。

**主要Repository**:
- `TranslationSessionRepository`: 会话数据访问
- `TranslationHistoryRepository`: 历史记录访问

**特性**:
- 自动索引创建
- TTL索引（自动过期）
- 复杂查询支持

### 4. Entity/Model层 (数据模型)

**实体类**:
- `TranslationSession`: 翻译会话
- `TranslationHistory`: 历史记录

**模型类**:
- `TextRegion`: 文本区域信息
- `BoundingBox`: 边界框坐标

**枚举**:
- `TaskStatus`: 任务状态
- `TranslationEngine`: 翻译引擎
- `OutputFormat`: 输出格式

## 🔄 核心流程详解

### 1. 文件上传流程

```
用户上传文件
    ↓
UploadController接收请求
    ↓
验证文件（大小、格式）
    ↓
FingerprintService生成用户指纹
    ↓
计算文件MD5哈希
    ↓
检查MongoDB中是否存在相同哈希
    ↓
  ┌─→ 存在：返回已有会话
  │
  └─→ 不存在：创建新会话
      ↓
      保存文件到storage/{sessionId}/
      ↓
      启动异步翻译任务
      ↓
      返回sessionId给用户
```

### 2. 异步翻译流程

```
AsyncTaskService.processTranslationTask()
    ↓
[阶段1: 预处理 10%]
    • 读取上传的图像
    • 标准化尺寸（最大2000px）
    • 转换为PNG格式
    ↓
[阶段2: OCR识别 30%]
    • 检查Redis缓存
    • 调用manga_ocr服务
    • 提取文本区域和坐标
    • 缓存OCR结果（24小时）
    ↓
[阶段3: 翻译 50-70%]
    • 遍历每个文本区域
    • 检查Redis翻译缓存
    • 调用翻译API
    • 更新会话术语表
    • 缓存翻译结果（7天）
    ↓
[阶段4: 渲染 70-90%]
    • 根据语言选择字体
    • 白色填充覆盖原文
    • 绘制翻译文本
    • 自适应字体大小
    ↓
[阶段5: 打包 90%]
    • 添加水印（可选）
    • 转换为输出格式
    • 保存到results/
    ↓
[阶段6: 完成 100%]
    • 创建历史记录
    • 设置30天过期时间
    • 更新会话状态
```

### 3. 缓存策略

#### Redis缓存层次

```
1. OCR结果缓存
   键: ocr:{fileHash}
   TTL: 24小时
   目的: 避免重复识别相同图像

2. 翻译结果缓存
   键: translation:{sha256(text|lang|engine)}
   TTL: 7天
   目的: 复用相同文本的翻译

3. 为什么分层？
   • OCR结果较大，短期缓存
   • 翻译结果小且通用，长期缓存
```

#### MongoDB TTL索引

```
TranslationSession:
  • createdAt字段设置TTL索引
  • 24小时后自动删除
  • 清理临时会话数据

TranslationHistory:
  • expiresAt字段设置TTL索引
  • 30天后自动删除
  • 可通过API延长
```

### 4. 文件组织结构

```
storage/
├── {sessionId-1}/
│   ├── originals/          # 原始上传文件
│   │   └── uuid.png
│   ├── processed/          # 预处理后的文件
│   │   └── normalized.png
│   └── results/            # 最终翻译结果
│       └── translated_manga.png
├── {sessionId-2}/
│   └── ...
└── {sessionId-n}/
    └── ...

优点:
• 会话隔离，便于管理
• 清理简单（删除整个目录）
• 支持并发处理
```

## 🔐 安全性设计

### 1. 无登录设计

```
浏览器指纹 = SHA256(IP地址 + User-Agent)

优点:
• 无需用户注册
• 低门槛使用
• 隐私友好

限制:
• 清除浏览器缓存/换IP会丢失历史
• 不支持跨设备同步
```

### 2. 文件验证

```
上传前检查:
✓ 文件大小 ≤ 10MB
✓ 文件类型: PNG/JPG/ZIP
✓ MIME类型验证
```

### 3. API密钥管理

```
推荐方式:
1. 环境变量（生产环境）
2. .env文件（开发环境）
3. 密钥管理服务（云部署）

❌ 不要硬编码在代码中
```

## 📊 性能优化

### 1. 异步处理

```
Spring @Async + 线程池
• 核心线程: 4
• 最大线程: 8
• 队列容量: 100

好处:
• 请求立即返回
• 后台并行处理
• 资源有效利用
```

### 2. 缓存策略

```
三级缓存:
1. Redis (分布式)
   • OCR结果
   • 翻译结果

2. MongoDB (持久化)
   • 会话数据
   • 历史记录

3. 文件系统 (结果存储)
   • 处理后的图像
```

### 3. 批量处理优化

```
• 合并相邻文本区域
• 批量调用翻译API
• 并行渲染多个区域
```

## 🔧 可扩展性

### 1. 水平扩展

```
负载均衡
    ↓
┌───────┬───────┬───────┐
│ App 1 │ App 2 │ App 3 │
└───┬───┴───┬───┴───┬───┘
    │       │       │
    └───┬───┴───┬───┘
        │       │
    MongoDB  Redis (共享)
```

### 2. 微服务拆分

```
当前: 单体应用
未来可拆分为:
• 文件上传服务
• OCR服务
• 翻译服务
• 渲染服务
• 历史管理服务
```

### 3. 翻译引擎扩展

添加新引擎只需:
1. 在TranslationEngine枚举添加选项
2. 在TranslationService添加实现方法
3. 在application.properties添加配置

## 📈 监控和日志

### 日志级别

```
生产环境:
• root: INFO
• com.example.mangaTrans: INFO

开发环境:
• root: INFO
• com.example.mangaTrans: DEBUG
```

### 关键监控指标

```
• 任务处理时间
• OCR服务响应时间
• 翻译API调用成功率
• 缓存命中率
• 磁盘使用量
```

## 🎯 设计模式应用

1. **策略模式**: TranslationService支持多引擎切换
2. **工厂模式**: 根据语言选择合适的字体
3. **观察者模式**: 任务进度更新通知
4. **单例模式**: Spring Bean管理
5. **异步模式**: @Async异步任务处理

## 🚀 未来改进方向

1. **消息队列**: 使用RabbitMQ/Kafka替代@Async
2. **分布式追踪**: 集成Zipkin/Jaeger
3. **API网关**: 统一入口和限流
4. **服务网格**: Istio/Linkerd
5. **Kubernetes部署**: 容器编排和自动扩缩容

---

**注**: 本架构设计注重实用性和可维护性，适合中小规模部署。
