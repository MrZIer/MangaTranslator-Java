package com.example.mangaTrans.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件存储服务
 */
@Slf4j
@Service
public class FileStorageService {
    
    @Value("${file.storage.base-path}")
    private String basePath;
    
    /**
     * 保存上传的文件
     */
    public String saveUploadedFile(MultipartFile file, String sessionId) throws IOException {
        // 创建会话目录：storage/{sessionId}/originals
        Path sessionDir = Paths.get(basePath, sessionId, "originals");
        Files.createDirectories(sessionDir);
        
        // 生成唯一文件名
        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = sessionDir.resolve(filename);
        
        // 保存文件
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Saved file to: {}", targetPath);
        return targetPath.toString();
    }
    
    /**
     * 保存处理后的文件
     */
    public String saveProcessedFile(byte[] fileData, String sessionId, String filename) throws IOException {
        Path processedDir = Paths.get(basePath, sessionId, "processed");
        Files.createDirectories(processedDir);
        
        Path targetPath = processedDir.resolve(filename);
        Files.write(targetPath, fileData);
        
        log.info("Saved processed file to: {}", targetPath);
        return targetPath.toString();
    }
    
    /**
     * 保存结果文件
     */
    public String saveResultFile(byte[] fileData, String sessionId, String filename) throws IOException {
        Path resultDir = Paths.get(basePath, sessionId, "results");
        Files.createDirectories(resultDir);
        
        Path targetPath = resultDir.resolve(filename);
        Files.write(targetPath, fileData);
        
        log.info("Saved result file to: {}", targetPath);
        return targetPath.toString();
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
    public void deleteSessionDirectory(String sessionId) throws IOException {
        Path sessionDir = Paths.get(basePath, sessionId);
        if (Files.exists(sessionDir)) {
            deleteDirectory(sessionDir.toFile());
            log.info("Deleted session directory: {}", sessionId);
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
     * 生成唯一文件名
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * 获取文件路径
     */
    public Path getFilePath(String filePath) {
        return Paths.get(filePath);
    }
}
