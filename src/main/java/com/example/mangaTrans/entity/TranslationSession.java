package com.example.mangaTrans.entity;

import com.example.mangaTrans.enums.OutputFormat;
import com.example.mangaTrans.enums.TaskStatus;
import com.example.mangaTrans.enums.TranslationEngine;
import com.example.mangaTrans.model.TextRegion;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 翻译会话实体
 */
@Data
@Document(collection = "translation_sessions")
public class TranslationSession {
    @Id
    private String id;                              // 会话ID
    
    @Indexed
    private String userFingerprint;                 // 用户指纹（浏览器指纹）
    
    @Indexed
    private String fileHash;                        // 文件MD5哈希值
    
    private String originalFileName;                // 原始文件名
    private Long fileSize;                          // 文件大小（字节）
    private String mimeType;                        // 文件MIME类型
    
    @Indexed
    private TaskStatus status;                      // 当前状态
    
    private Integer progress;                       // 进度百分比 (0-100)
    private String currentStage;                    // 当前阶段描述
    private String errorMessage;                    // 错误信息
    
    private TranslationEngine engine;               // 使用的翻译引擎
    private String sourceLanguage;                  // 源语言
    private String targetLanguage;                  // 目标语言
    private OutputFormat outputFormat;              // 输出格式
    
    private List<TextRegion> textRegions;           // 文本区域列表
    private Map<String, String> glossary;           // 术语表（用于翻译一致性）
    
    private String uploadPath;                      // 上传文件路径
    private String processedPath;                   // 处理后文件路径
    private String resultPath;                      // 结果文件路径
    
    @Indexed(expireAfterSeconds = 86400)           // 24小时后自动删除
    private LocalDateTime createdAt;                // 创建时间
    
    private LocalDateTime updatedAt;                // 更新时间
    private LocalDateTime completedAt;              // 完成时间
    
    public TranslationSession() {
        this.progress = 0;
        this.status = TaskStatus.UPLOAD;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.textRegions = new ArrayList<>();
        this.glossary = new HashMap<>();
    }
    
    public void updateProgress(TaskStatus status, Integer progress, String stage) {
        this.status = status;
        this.progress = progress;
        this.currentStage = stage;
        this.updatedAt = LocalDateTime.now();
        
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
