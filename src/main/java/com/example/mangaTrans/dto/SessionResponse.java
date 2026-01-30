package com.example.mangaTrans.dto;

import com.example.mangaTrans.enums.TaskStatus;
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
    private TaskStatus status;
    private Integer progress;
    private String currentStage;
    private String errorMessage;
}
