package com.example.mangaTrans.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件存储服务
 * 实现标准化的会话目录结构管理
 */
@Slf4j
@Service
public class FileStorageService {
    
    @Value("${file.storage.base-path}")
    private String basePath;
    
    private static final String ORIGINAL_DIR = "original";
    private static final String PROCESSED_DIR = "processed";
    private static final String OCR_DIR = "ocr";
    private static final String TRANSLATED_DIR = "translated";
    private static final String METADATA_FILE = "metadata.json";
    
    /**
     * 生成会话目录名称
     * 格式: session_YYYYMMDD_HHMMSS_会话ID前8位
     */
    public String generateSessionDirectoryName(String sessionId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String shortId = sessionId.length() > 8 ? sessionId.substring(0, 8) : sessionId;
        return String.format("session_%s_%s", timestamp, shortId);
    }
    
    /**
     * 创建会话目录结构
     * @param sessionId 会话ID
     * @param isBatch 是否为批量上传（true=创建父文件夹, false=直接创建会话文件夹）
     */
    public String createSessionDirectory(String sessionId, boolean isBatch) throws IOException {
        String sessionDirName = generateSessionDirectoryName(sessionId);
        Path sessionPath;
        
        if (isBatch) {
            // 批量模式：创建父文件夹/会话文件夹结构
            String batchFolderName = "batch_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            sessionPath = Paths.get(basePath, batchFolderName, sessionDirName);
            log.info("批量模式: 创建父文件夹 {}", batchFolderName);
        } else {
            // 单张模式：直接创建会话文件夹
            sessionPath = Paths.get(basePath, sessionDirName);
            log.info("单张模式: 直接创建会话文件夹");
        }
        
        // 创建主会话目录
        Files.createDirectories(sessionPath);
        
        // 创建子目录
        Files.createDirectories(sessionPath.resolve(ORIGINAL_DIR));
        Files.createDirectories(sessionPath.resolve(PROCESSED_DIR));
        Files.createDirectories(sessionPath.resolve(OCR_DIR));
        Files.createDirectories(sessionPath.resolve(TRANSLATED_DIR));
        
        log.info("Created session directory structure: {}", sessionPath);
        return sessionPath.toString();
    }
    
    /**
     * 保存上传的文件到original目录
     */
    public String saveUploadedFile(MultipartFile file, String sessionDirectory, String originalFileName) throws IOException {
        Path uploadsPath = Paths.get(sessionDirectory, ORIGINAL_DIR);
        Files.createDirectories(uploadsPath);
        
        // 使用原始文件名，如果重复则添加UUID前缀
        String filename = originalFileName;
        Path targetPath = uploadsPath.resolve(filename);
        
        if (Files.exists(targetPath)) {
            String uniquePrefix = UUID.randomUUID().toString().substring(0, 8);
            filename = uniquePrefix + "_" + originalFileName;
            targetPath = uploadsPath.resolve(filename);
        }
        
        // 保存文件
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Saved uploaded file to: {}", targetPath);
        return targetPath.toString();
    }
    
    /**
     * 获取会话目录的original路径
     */
    public String getOriginalPath(String sessionDirectory) {
        return Paths.get(sessionDirectory, ORIGINAL_DIR).toString();
    }
    
    /**
     * 获取会话目录的processed路径
     */
    public String getProcessedPath(String sessionDirectory) {
        return Paths.get(sessionDirectory, PROCESSED_DIR).toString();
    }
    
    /**
     * 获取会话目录的ocr路径
     */
    public String getOcrPath(String sessionDirectory) {
        return Paths.get(sessionDirectory, OCR_DIR).toString();
    }
    
    /**
     * 获取会话目录的translated路径
     */
    public String getTranslatedPath(String sessionDirectory) {
        return Paths.get(sessionDirectory, TRANSLATED_DIR).toString();
    }
    
    /**
     * 读取文件
     */
    public byte[] readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }
    
    /**
     * 删除会话目录及其所有文件
     */
    public void deleteSessionDirectory(String sessionDirectory) throws IOException {
        Path sessionPath = Paths.get(sessionDirectory);
        if (Files.exists(sessionPath)) {
            deleteDirectory(sessionPath.toFile());
            log.info("Deleted session directory: {}", sessionDirectory);
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
    
    /**
     * 获取文件路径
     */
    public Path getFilePath(String filePath) {
        return Paths.get(filePath);
    }
    
    /**
     * 加载文件为Resource
     */
    public Resource loadFileAsResource(String filePath) throws IOException {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("File not found or not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new IOException("Malformed file path: " + filePath, e);
        }
    }
    
    /**
     * 获取所有批量文件夹列表
     */
    public List<File> getBatchFolders() {
        File storageDir = new File(basePath);
        List<File> batchFolders = new ArrayList<>();
        
        if (storageDir.exists() && storageDir.isDirectory()) {
            File[] files = storageDir.listFiles((dir, name) -> name.startsWith("batch_"));
            if (files != null) {
                batchFolders.addAll(Arrays.asList(files));
            }
        }
        
        log.info("Found {} batch folders", batchFolders.size());
        return batchFolders;
    }
    
    /**
     * 获取指定批量文件夹下的所有会话目录
     */
    public List<File> getSessionsInBatchFolder(String batchFolderPath) {
        File batchFolder = new File(batchFolderPath);
        List<File> sessionFolders = new ArrayList<>();
        
        if (batchFolder.exists() && batchFolder.isDirectory()) {
            File[] files = batchFolder.listFiles((dir, name) -> name.startsWith("session_"));
            if (files != null) {
                sessionFolders.addAll(Arrays.asList(files));
            }
        }
        
        log.info("Found {} session folders in {}", sessionFolders.size(), batchFolderPath);
        return sessionFolders;
    }
    
    /**
     * 根据会话目录获取translated中的所有翻译结果文件
     */
    public File[] getTranslatedResultFiles(String sessionDirectory) {
        Path translatorResultPath = Paths.get(sessionDirectory, TRANSLATED_DIR);
        File translatorResultDir = translatorResultPath.toFile();
        
        if (!translatorResultDir.exists() || !translatorResultDir.isDirectory()) {
            log.warn("Translator result directory not found: {}", translatorResultPath);
            return new File[0];
        }
        
        File[] files = translatorResultDir.listFiles((dir, name) -> 
            name.endsWith("_cn.jpg") || name.endsWith("_cn.png"));
        
        return files != null ? files : new File[0];
    }
}
