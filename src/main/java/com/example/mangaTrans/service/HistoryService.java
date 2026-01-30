package com.example.mangaTrans.service;

import com.example.mangaTrans.entity.TranslationHistory;
import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.repository.TranslationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 历史记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {
    
    private final TranslationHistoryRepository historyRepository;
    
    /**
     * 创建历史记录
     */
    public TranslationHistory createHistory(TranslationSession session) {
        TranslationHistory history = new TranslationHistory();
        history.setUserFingerprint(session.getUserFingerprint());
        history.setFileHash(session.getFileHash());
        history.setSessionId(session.getId());
        history.setOriginalFileName(session.getOriginalFileName());
        history.setFileSize(session.getFileSize());
        history.setEngine(session.getEngine());
        history.setSourceLanguage(session.getSourceLanguage());
        history.setTargetLanguage(session.getTargetLanguage());
        history.setResultPath(session.getResultPath());
        
        history = historyRepository.save(history);
        log.info("Created history record: {} for session: {}", history.getId(), session.getId());
        
        return history;
    }
    
    /**
     * 获取用户的历史记录（分页）
     */
    public Page<TranslationHistory> getUserHistory(String userFingerprint, Pageable pageable) {
        return historyRepository.findByUserFingerprintOrderByCreatedAtDesc(userFingerprint, pageable);
    }
    
    /**
     * 根据文件哈希查找历史记录（用于去重）
     */
    public Optional<TranslationHistory> findByFileHash(String fileHash) {
        return historyRepository.findByFileHash(fileHash);
    }
    
    /**
     * 增加访问次数
     */
    public void incrementAccessCount(String historyId) {
        Optional<TranslationHistory> optHistory = historyRepository.findById(historyId);
        if (optHistory.isPresent()) {
            TranslationHistory history = optHistory.get();
            history.incrementAccessCount();
            historyRepository.save(history);
        }
    }
    
    /**
     * 延长保留期限
     */
    public void extendExpiration(String historyId, int days) {
        Optional<TranslationHistory> optHistory = historyRepository.findById(historyId);
        if (optHistory.isPresent()) {
            TranslationHistory history = optHistory.get();
            history.extendExpiration(days);
            historyRepository.save(history);
            log.info("Extended expiration for history: {} by {} days", historyId, days);
        }
    }
    
    /**
     * 清理过期记录
     */
    public int cleanupExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        var expiredRecords = historyRepository.findByExpiresAtBefore(now);
        int count = expiredRecords.size();
        
        if (count > 0) {
            historyRepository.deleteByExpiresAtBefore(now);
            log.info("Cleaned up {} expired history records", count);
        }
        
        return count;
    }
}
