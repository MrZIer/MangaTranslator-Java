package com.example.mangaTrans.repository;

import com.example.mangaTrans.entity.TranslationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 翻译历史Repository
 */
@Repository
public interface TranslationHistoryRepository extends MongoRepository<TranslationHistory, String> {
    
    /**
     * 根据用户指纹分页查询历史记录
     */
    Page<TranslationHistory> findByUserFingerprintOrderByCreatedAtDesc(
            String userFingerprint, Pageable pageable);
    
    /**
     * 根据文件哈希查找历史记录
     */
    Optional<TranslationHistory> findByFileHash(String fileHash);
    
    /**
     * 根据会话ID查找历史记录
     */
    Optional<TranslationHistory> findBySessionId(String sessionId);
    
    /**
     * 根据用户指纹和时间范围查询
     */
    List<TranslationHistory> findByUserFingerprintAndCreatedAtBetween(
            String userFingerprint, LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找即将过期的记录（用于清理任务）
     */
    List<TranslationHistory> findByExpiresAtBefore(LocalDateTime dateTime);
    
    /**
     * 删除过期记录
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
