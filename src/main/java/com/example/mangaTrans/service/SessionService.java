package com.example.mangaTrans.service;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.enums.TaskStatus;
import com.example.mangaTrans.repository.TranslationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final TranslationSessionRepository sessionRepository;
    private final FingerprintService fingerprintService;
    private final FileStorageService fileStorageService;
    
    /**
     * 创建新的翻译会话
     * @param isBatch 是否为批量上传（多张图片）
     * @param batchId 批次ID（同一批次使用相同ID）
     */
    public TranslationSession createSession(MultipartFile file, String userFingerprint, boolean isBatch, String batchId) throws IOException {
        // 计算文件MD5哈希（用于记录，不用于去重）
        String fileHash = calculateFileHash(file.getInputStream());
        
        // 注释掉去重逻辑：用户希望每次上传都创建新任务
        // Optional<TranslationSession> existingSession = sessionRepository.findByFileHash(fileHash);
        // if (existingSession.isPresent()) {
        //     log.info("Found existing session for file hash: {}", fileHash);
        //     return existingSession.get();
        // }
        
        log.info("Creating new session for file hash: {} (no deduplication)", fileHash);
        
        // 创建新会话
        TranslationSession session = new TranslationSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserFingerprint(userFingerprint);
        session.setFileHash(fileHash);
        session.setOriginalFileName(file.getOriginalFilename());
        session.setFileSize(file.getSize());
        session.setMimeType(file.getContentType());
        session.setStatus(TaskStatus.UPLOAD);
        session.setProgress(0);
        
        // 创建会话专用目录（根据isBatch和batchId决定是否创建父文件夹）
        String sessionDirectory = fileStorageService.createSessionDirectory(session.getId(), isBatch, batchId);
        session.setSessionDirectory(sessionDirectory);
        session.setOriginalFileDirectory(new java.io.File(sessionDirectory).getParent()); // 存储原始文件所在父目录
        
        session = sessionRepository.save(session);
        log.info("Created new session: {} for file: {} with directory: {}", 
                session.getId(), file.getOriginalFilename(), sessionDirectory);
        
        return session;
    }
    
    /**
     * 保存会话
     */
    public TranslationSession saveSession(TranslationSession session) {
        return sessionRepository.save(session);
    }
    
    /**
     * 更新会话状态和进度
     */
    public void updateSessionProgress(String sessionId, TaskStatus status, Integer progress, String stage) {
        Optional<TranslationSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isPresent()) {
            TranslationSession session = optSession.get();
            session.updateProgress(status, progress, stage);
            sessionRepository.save(session);
            log.debug("Updated session {} progress: {}% - {}", sessionId, progress, stage);
        }
    }
    
    /**
     * 标记会话失败
     */
    public void markSessionFailed(String sessionId, String errorMessage) {
        Optional<TranslationSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isPresent()) {
            TranslationSession session = optSession.get();
            session.setStatus(TaskStatus.FAILED);
            session.setErrorMessage(errorMessage);
            sessionRepository.save(session);
            log.error("Session {} failed: {}", sessionId, errorMessage);
        }
    }
    
    /**
     * 根据ID获取会话
     */
    public Optional<TranslationSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<TranslationSession> getUserSessions(String userFingerprint) {
        return sessionRepository.findByUserFingerprintOrderByCreatedAtDesc(userFingerprint);
    }
    
    /**
     * 更新会话
     */
    public TranslationSession updateSession(TranslationSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }
    
    /**
     * 获取所有已完成的会话
     */
    public List<TranslationSession> getAllCompletedSessions() {
        return sessionRepository.findByStatusOrderByCompletedAtDesc(TaskStatus.COMPLETED);
    }
    
    /**
     * 计算文件MD5哈希
     */
    private String calculateFileHash(InputStream inputStream) throws IOException {
        return DigestUtils.md5Hex(inputStream);
    }
}
