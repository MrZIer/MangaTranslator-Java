package com.example.mangaTrans.scheduler;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.repository.TranslationSessionRepository;
import com.example.mangaTrans.service.FileStorageService;
import com.example.mangaTrans.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时清理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {
    
    private final TranslationSessionRepository sessionRepository;
    private final HistoryService historyService;
    private final FileStorageService fileStorageService;
    
    @Value("${file.storage.temp-retention-hours}")
    private int tempRetentionHours;
    
    /**
     * 每天凌晨2点执行清理任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredData() {
        log.info("Starting scheduled cleanup task");
        
        try {
            // 清理过期的临时会话
            cleanupExpiredSessions();
            
            // 清理过期的历史记录
            cleanupExpiredHistory();
            
            log.info("Scheduled cleanup task completed successfully");
            
        } catch (Exception e) {
            log.error("Scheduled cleanup task failed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理过期的会话数据
     */
    private void cleanupExpiredSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(tempRetentionHours);
        List<TranslationSession> expiredSessions = sessionRepository.findByCreatedAtBefore(threshold);
        
        log.info("Found {} expired sessions to clean up", expiredSessions.size());
        
        for (TranslationSession session : expiredSessions) {
            try {
                // 删除会话文件
                fileStorageService.deleteSessionDirectory(session.getId());
                
                // 删除会话记录
                sessionRepository.delete(session);
                
                log.debug("Cleaned up session: {}", session.getId());
                
            } catch (Exception e) {
                log.error("Failed to cleanup session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * 清理过期的历史记录
     */
    private void cleanupExpiredHistory() {
        int cleanedCount = historyService.cleanupExpiredRecords();
        log.info("Cleaned up {} expired history records", cleanedCount);
    }
}
