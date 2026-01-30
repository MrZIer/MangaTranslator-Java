package com.example.mangaTrans.entity;

import com.example.mangaTrans.enums.TranslationEngine;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 翻译历史记录实体
 */
@Data
@Document(collection = "translation_history")
public class TranslationHistory {
    @Id
    private String id;                              // 记录ID
    
    @Indexed
    private String userFingerprint;                 // 用户指纹
    
    @Indexed
    private String fileHash;                        // 文件哈希值
    
    private String sessionId;                       // 关联的会话ID
    private String originalFileName;                // 原始文件名
    private Long fileSize;                          // 文件大小
    
    private TranslationEngine engine;               // 翻译引擎
    private String sourceLanguage;                  // 源语言
    private String targetLanguage;                  // 目标语言
    
    private String resultPath;                      // 结果文件路径
    private Integer accessCount;                    // 访问次数
    
    @Indexed
    private LocalDateTime createdAt;                // 创建时间
    
    @Indexed(expireAfterSeconds = 2592000)         // 30天后自动删除
    private LocalDateTime expiresAt;                // 过期时间
    
    private LocalDateTime lastAccessedAt;           // 最后访问时间
    
    public TranslationHistory() {
        this.accessCount = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(30);
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public void extendExpiration(int days) {
        this.expiresAt = LocalDateTime.now().plusDays(days);
    }
}
