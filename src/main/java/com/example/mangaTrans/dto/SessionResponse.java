package com.example.mangaTrans.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private String sessionId;
    private String fileName;
    private String status;  // 改为String类型，前端更容易处理
    private Integer progress;
    private String currentStage;
    private String errorMessage;
}
