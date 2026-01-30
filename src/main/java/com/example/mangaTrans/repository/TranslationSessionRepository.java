package com.example.mangaTrans.repository;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.enums.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 翻译会话Repository
 */
@Repository
public interface TranslationSessionRepository extends MongoRepository<TranslationSession, String> {
    
    /**
     * 根据文件哈希查找会话
     */
    Optional<TranslationSession> findByFileHash(String fileHash);
    
    /**
     * 根据用户指纹查找会话列表
     */
    List<TranslationSession> findByUserFingerprintOrderByCreatedAtDesc(String userFingerprint);
    
    /**
     * 根据状态查找会话
     */
    List<TranslationSession> findByStatus(TaskStatus status);
    
    /**
     * 根据用户指纹和状态查找会话
     */
    List<TranslationSession> findByUserFingerprintAndStatusOrderByCreatedAtDesc(
            String userFingerprint, TaskStatus status);
    
    /**
     * 查找创建时间早于指定时间的会话
     */
    List<TranslationSession> findByCreatedAtBefore(LocalDateTime dateTime);
    
    /**
     * 删除创建时间早于指定时间的会话
     */
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
