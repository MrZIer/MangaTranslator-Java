package com.example.mangaTrans.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史记录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponse {
    private String id;
    private String fileName;
    private Long fileSize;
    private String engine;
    private String sourceLanguage;
    private String targetLanguage;
    private Integer accessCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
