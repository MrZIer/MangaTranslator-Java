package com.example.mangaTrans.controller;

import com.example.mangaTrans.dto.ApiResponse;
import com.example.mangaTrans.dto.SessionResponse;
import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * ???????????????
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final SessionService sessionService;
    
    /**
     * ??????????
     */
    @GetMapping("/{sessionId}/progress")
    public ApiResponse<SessionResponse> getTaskProgress(@PathVariable String sessionId) {
        try {
            Optional<TranslationSession> optSession = sessionService.getSession(sessionId);
            
            if (optSession.isEmpty()) {
                return ApiResponse.error("Session not found");
            }
            
            TranslationSession session = optSession.get();
            SessionResponse response = new SessionResponse(
                    session.getId(),
                    session.getOriginalFileName(),
                    session.getStatus(),
                    session.getProgress(),
                    session.getCurrentStage(),
                    session.getErrorMessage()
            );
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("Failed to get task progress: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get task progress: " + e.getMessage());
        }
    }
    
    /**
     * ???????????
     */
    @GetMapping("/{sessionId}")
    public ApiResponse<TranslationSession> getTaskDetails(@PathVariable String sessionId) {
        try {
            Optional<TranslationSession> optSession = sessionService.getSession(sessionId);
            
            if (optSession.isEmpty()) {
                return ApiResponse.error("Session not found");
            }
            
            return ApiResponse.success(optSession.get());
            
        } catch (Exception e) {
            log.error("Failed to get task details: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get task details: " + e.getMessage());
        }
    }
}
