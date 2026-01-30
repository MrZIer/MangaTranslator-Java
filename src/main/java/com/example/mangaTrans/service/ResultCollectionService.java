package com.example.mangaTrans.service;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 结果汇总服务（新架构）
 * 从会话的translator_result目录提取翻译结果，输出到原始文件同级目录
 */
@Service
public class ResultCollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultCollectionService.class);
    private static final String TRANSLATOR_RESULT_DIR = "translator_result";
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * 汇总所有已完成的翻译结果
     * 输出到临时目录（因为没有指定原始文件路径）
     * @return 汇总结果信息
     */
    public Map<String, Object> collectAllResults() {
        logger.info("开始汇总所有翻译结果...");
        
        // 获取所有已完成的会话
        List<TranslationSession> completedSessions = sessionService.getAllCompletedSessions();
        
        if (completedSessions.isEmpty()) {
            logger.info("没有已完成的翻译任务");
            return createResult(false, "没有已完成的翻译任务", null, 0);
        }
        
        // 创建临时汇总目录
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File tempCollectionDir = new File("storage", "collection_" + timestamp);
        
        if (!tempCollectionDir.exists()) {
            tempCollectionDir.mkdirs();
            logger.info("创建临时汇总目录: {}", tempCollectionDir.getAbsolutePath());
        }
        
        // 汇总文件
        int successCount = 0;
        int failCount = 0;
        List<String> collectedFiles = new ArrayList<>();
        
        for (TranslationSession session : completedSessions) {
            try {
                // 使用新架构：从会话的translator_result目录获取文件
                File[] files = fileStorageService.getTranslatedResultFiles(session.getSessionDirectory());
                
                if (files.length > 0) {
                    for (File file : files) {
                        String targetFileName = generateUniqueFileName(tempCollectionDir, file.getName(), session.getId());
                        File targetFile = new File(tempCollectionDir, targetFileName);
                        
                        Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        collectedFiles.add(targetFileName);
                        logger.debug("复制文件: {} -> {}", file.getName(), targetFileName);
                    }
                    successCount++;
                } else {
                    logger.warn("会话 {} 没有找到翻译结果文件", session.getId());
                    failCount++;
                }
                
            } catch (IOException e) {
                logger.error("处理会话 {} 时发生错误: {}", session.getId(), e.getMessage());
                failCount++;
            }
        }
        
        logger.info("汇总完成: 成功 {} 个会话, 失败 {} 个会话, 共 {} 个文件", 
                successCount, failCount, collectedFiles.size());
        
        return createResult(true, "汇总完成", 
                tempCollectionDir.getAbsolutePath(), collectedFiles.size());
    }
    
    /**
     * 汇总指定会话的翻译结果到原始文件同级目录
     * @param sessionId 会话ID
     * @return 汇总结果信息
     */
    public Map<String, Object> collectSessionResult(String sessionId) {
        logger.info("开始汇总会话 {} 的翻译结果...", sessionId);
        
        TranslationSession session = sessionService.getSession(sessionId).orElse(null);
        if (session == null) {
            return createResult(false, "会话不存在", null, 0);
        }
        
        if (session.getStatus() != TaskStatus.COMPLETED) {
            return createResult(false, "会话尚未完成翻译", null, 0);
        }
        
        try {
            // 从translator_result目录获取翻译结果文件
            File[] files = fileStorageService.getTranslatedResultFiles(session.getSessionDirectory());
            
            if (files == null || files.length == 0) {
                return createResult(false, "没有找到翻译结果文件", null, 0);
            }
            
            // 确定汇总目录（使用原始文件所在目录的同级目录）
            File collectionDir;
            if (session.getOriginalFileDirectory() != null) {
                // 使用原始文件所在目录的同级目录
                File originalDir = new File(session.getOriginalFileDirectory());
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                collectionDir = new File(originalDir.getParent(), "translated_" + timestamp);
            } else {
                // 降级方案：使用storage下的临时目录
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                collectionDir = new File("storage", "collection_" + timestamp);
            }
            
            if (!collectionDir.exists()) {
                collectionDir.mkdirs();
                logger.info("创建汇总目录: {}", collectionDir.getAbsolutePath());
            }
            
            // 复制翻译结果文件
            List<String> collectedFiles = new ArrayList<>();
            for (File file : files) {
                File targetFile = new File(collectionDir, file.getName());
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                collectedFiles.add(file.getName());
                logger.debug("复制文件: {} -> {}", file.getName(), targetFile.getAbsolutePath());
            }
            
            logger.info("会话 {} 汇总完成，共 {} 个文件到: {}", 
                    sessionId, collectedFiles.size(), collectionDir.getAbsolutePath());
            
            return createResult(true, "汇总完成", 
                    collectionDir.getAbsolutePath(), collectedFiles.size());
            
        } catch (IOException e) {
            logger.error("汇总会话 {} 时发生错误: {}", sessionId, e.getMessage());
            return createResult(false, "汇总失败: " + e.getMessage(), null, 0);
        }
    }
    
    /**
     * 汇总指定批量文件夹下所有会话的翻译结果
     * @param batchFolderPath 批量文件夹路径
     * @return 汇总结果信息
     */
    public Map<String, Object> collectBatchFolderResults(String batchFolderPath) {
        logger.info("开始汇总批量文件夹: {}", batchFolderPath);
        
        File batchFolder = new File(batchFolderPath);
        if (!batchFolder.exists() || !batchFolder.isDirectory()) {
            return createResult(false, "批量文件夹不存在: " + batchFolderPath, null, 0);
        }
        
        // 创建输出目录（在批量文件夹同级）
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String collectionDirName = batchFolder.getName() + "_collected_" + timestamp;
        File collectionDir = new File(batchFolder.getParent(), collectionDirName);
        
        if (!collectionDir.exists()) {
            collectionDir.mkdirs();
            logger.info("创建汇总目录: {}", collectionDir.getAbsolutePath());
        }
        
        // 获取批量文件夹下的所有会话目录
        List<File> sessionFolders = fileStorageService.getSessionsInBatchFolder(batchFolderPath);
        
        if (sessionFolders.isEmpty()) {
            return createResult(false, "批量文件夹中没有找到会话目录", collectionDir.getAbsolutePath(), 0);
        }
        
        // 汇总所有会话的翻译结果
        int successCount = 0;
        int failCount = 0;
        List<String> collectedFiles = new ArrayList<>();
        
        for (File sessionFolder : sessionFolders) {
            try {
                File[] files = fileStorageService.getTranslatedResultFiles(sessionFolder.getAbsolutePath());
                
                if (files.length > 0) {
                    for (File file : files) {
                        String targetFileName = file.getName();
                        File targetFile = new File(collectionDir, targetFileName);
                        
                        // 如果文件名冲突，添加会话ID后缀
                        if (targetFile.exists()) {
                            String sessionId = extractSessionId(sessionFolder.getName());
                            String nameWithoutExt = targetFileName.substring(0, targetFileName.lastIndexOf('.'));
                            String ext = targetFileName.substring(targetFileName.lastIndexOf('.'));
                            targetFileName = nameWithoutExt + "_" + sessionId + ext;
                            targetFile = new File(collectionDir, targetFileName);
                        }
                        
                        Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        collectedFiles.add(targetFileName);
                        logger.debug("复制文件: {} -> {}", file.getName(), targetFileName);
                    }
                    successCount++;
                } else {
                    logger.warn("会话目录 {} 没有找到翻译结果文件", sessionFolder.getName());
                    failCount++;
                }
                
            } catch (Exception e) {
                logger.error("处理会话目录 {} 时出错: {}", sessionFolder.getName(), e.getMessage());
                failCount++;
            }
        }
        
        String message = String.format("汇总完成！共处理 %d 个会话，成功 %d 个，失败 %d 个，汇总 %d 个文件",
                sessionFolders.size(), successCount, failCount, collectedFiles.size());
        logger.info(message);
        
        return createResult(true, message, collectionDir.getAbsolutePath(), collectedFiles.size());
    }
    
    /**
     * 从会话文件夹名称中提取会话ID
     */
    private String extractSessionId(String sessionFolderName) {
        // session_20240131_143022_abc123 -> abc123
        String[] parts = sessionFolderName.split("_");
        return parts.length > 3 ? parts[3] : "unknown";
    }
    
    /**
     * 汇总指定会话目录的结果到指定目标目录
     * @param sessionDirectory 会话目录完整路径
     * @param targetDirectory 目标汇总目录路径
     * @return 汇总结果信息
     */
    public Map<String, Object> collectFromSessionDirectory(String sessionDirectory, String targetDirectory) {
        logger.info("从会话目录 {} 汇总结果到 {}", sessionDirectory, targetDirectory);
        
        try {
            // 验证会话目录存在
            File sessionDir = new File(sessionDirectory);
            if (!sessionDir.exists() || !sessionDir.isDirectory()) {
                return createResult(false, "会话目录不存在: " + sessionDirectory, null, 0);
            }
            
            // 获取translator_result目录中的文件
            File translatorResultDir = new File(sessionDir, TRANSLATOR_RESULT_DIR);
            if (!translatorResultDir.exists() || !translatorResultDir.isDirectory()) {
                return createResult(false, "translator_result目录不存在", null, 0);
            }
            
            File[] files = translatorResultDir.listFiles((dir, name) -> 
                name.endsWith("_cn.jpg") || name.endsWith("_cn.png"));
            
            if (files == null || files.length == 0) {
                return createResult(false, "没有找到翻译结果文件", null, 0);
            }
            
            // 创建目标目录
            File targetDir = new File(targetDirectory);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
                logger.info("创建目标汇总目录: {}", targetDir.getAbsolutePath());
            }
            
            // 复制文件
            List<String> collectedFiles = new ArrayList<>();
            for (File file : files) {
                File targetFile = new File(targetDir, file.getName());
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                collectedFiles.add(file.getName());
                logger.debug("复制文件: {} -> {}", file.getName(), targetFile.getAbsolutePath());
            }
            
            logger.info("汇总完成，共 {} 个文件到: {}", collectedFiles.size(), targetDir.getAbsolutePath());
            
            return createResult(true, "汇总完成", 
                    targetDir.getAbsolutePath(), collectedFiles.size());
            
        } catch (IOException e) {
            logger.error("汇总过程中发生错误: {}", e.getMessage());
            return createResult(false, "汇总失败: " + e.getMessage(), null, 0);
        }
    }
    
    /**
     * 生成唯一的文件名（避免冲突）
     */
    private String generateUniqueFileName(File dir, String originalName, String sessionId) {
        // 如果文件名已包含会话ID前缀，直接返回
        if (originalName.startsWith(sessionId.substring(0, 8))) {
            return originalName;
        }
        
        // 添加会话ID前缀（取前8位）避免冲突
        String prefix = sessionId.substring(0, 8);
        String name = prefix + "_" + originalName;
        
        // 如果文件已存在，添加序号
        File targetFile = new File(dir, name);
        if (targetFile.exists()) {
            String baseName = name.substring(0, name.lastIndexOf('.'));
            String extension = name.substring(name.lastIndexOf('.'));
            int counter = 1;
            
            while (targetFile.exists()) {
                name = baseName + "_" + counter + extension;
                targetFile = new File(dir, name);
                counter++;
            }
        }
        
        return name;
    }
    
    /**
     * 创建结果对象
     */
    private Map<String, Object> createResult(boolean success, String message, 
                                            String path, int fileCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", message);
        result.put("collectionPath", path);
        result.put("fileCount", fileCount);
        return result;
    }
    
    /**
     * 获取所有汇总目录列表
     */
    public List<Map<String, Object>> getCollectionDirectories() {
        File storageDir = new File("storage");
        List<Map<String, Object>> directories = new ArrayList<>();
        
        if (!storageDir.exists()) {
            return directories;
        }
        
        File[] dirs = storageDir.listFiles((dir, name) -> 
            name.startsWith("collection_"));
        
        if (dirs != null) {
            for (File dir : dirs) {
                Map<String, Object> info = new HashMap<>();
                info.put("name", dir.getName());
                info.put("path", dir.getAbsolutePath());
                info.put("fileCount", countFiles(dir));
                info.put("createdTime", dir.lastModified());
                directories.add(info);
            }
            
            // 按创建时间降序排序
            directories.sort((a, b) -> 
                Long.compare((Long) b.get("createdTime"), (Long) a.get("createdTime")));
        }
        
        return directories;
    }
    
    /**
     * 统计目录中的文件数量
     */
    private int countFiles(File dir) {
        if (!dir.isDirectory()) {
            return 0;
        }
        
        File[] files = dir.listFiles((d, name) -> 
            name.endsWith(".jpg") || name.endsWith(".png"));
        
        return files != null ? files.length : 0;
    }
}
