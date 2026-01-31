package com.example.mangaTrans.controller;

import com.example.mangaTrans.dto.ApiResponse;
import com.example.mangaTrans.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片展示控制器
 * 提供批量文件夹和单个文件夹的翻译结果展示功能
 */
@Slf4j
@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {
    
    private final FileStorageService fileStorageService;
    
    /**
     * 获取所有批量文件夹列表
     * 扫描storage目录下所有batch_开头的文件夹（包括汇总文件夹）
     */
    @GetMapping("/batch-folders")
    public ApiResponse<List<Map<String, Object>>> getBatchFolders() {
        try {
            String storageBasePath = fileStorageService.getBasePath();
            Path storagePath = Paths.get(storageBasePath);
            if (!Files.exists(storagePath)) {
                return ApiResponse.success("No batch folders found", new ArrayList<>());
            }
            
            List<Map<String, Object>> batchFolders = Files.list(storagePath)
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        // 匹配: batch_xxx, batch_xxx_collected_xxx, collection_xxx
                        return name.startsWith("batch_") || name.contains("_collected_") || name.startsWith("collection_");
                    })
                    .map(path -> {
                        Map<String, Object> folder = new HashMap<>();
                        String folderName = path.getFileName().toString();
                        folder.put("name", folderName);
                        folder.put("path", folderName); // 使用相对路径（文件夹名称）
                        
                        try {
                            // 汇总文件夹: 包含_collected_或以collection_开头
                            if (folderName.contains("_collected_") || folderName.startsWith("collection_")) {
                                // 汇总文件夹：统计图片数量
                                long imageCount = Files.list(path)
                                        .filter(Files::isRegularFile)
                                        .filter(p -> {
                                            String name = p.getFileName().toString().toLowerCase();
                                            return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                                                   name.endsWith(".png") || name.endsWith(".webp");
                                        })
                                        .count();
                                folder.put("sessionCount", 0);
                                folder.put("imageCount", imageCount);
                                folder.put("isCollected", true);
                            } else {
                                // 批量文件夹：统计会话数量
                                long sessionCount = Files.list(path)
                                        .filter(Files::isDirectory)
                                        .filter(p -> p.getFileName().toString().startsWith("session_"))
                                        .count();
                                folder.put("sessionCount", sessionCount);
                                folder.put("imageCount", 0);
                                folder.put("isCollected", false);
                            }
                            
                            // 获取创建时间
                            folder.put("createdTime", Files.getLastModifiedTime(path).toString());
                        } catch (IOException e) {
                            folder.put("sessionCount", 0);
                            folder.put("imageCount", 0);
                            folder.put("isCollected", false);
                            folder.put("createdTime", "Unknown");
                        }
                        
                        return folder;
                    })
                    .sorted((a, b) -> b.get("createdTime").toString().compareTo(a.get("createdTime").toString()))
                    .collect(Collectors.toList());
            
            return ApiResponse.success("Batch folders retrieved", batchFolders);
            
        } catch (Exception e) {
            log.error("Failed to get batch folders: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get batch folders: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有单个会话文件夹列表
     * 扫描storage目录下所有session_开头的文件夹（不在batch文件夹内的）
     */
    @GetMapping("/session-folders")
    public ApiResponse<List<Map<String, Object>>> getSessionFolders() {
        try {
            String storageBasePath = fileStorageService.getBasePath();
            Path storagePath = Paths.get(storageBasePath);
            if (!Files.exists(storagePath)) {
                return ApiResponse.success("No session folders found", new ArrayList<>());
            }
            
            List<Map<String, Object>> sessionFolders = Files.list(storagePath)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("session_"))
                    .map(path -> {
                        String folderName = path.getFileName().toString();
                        Map<String, Object> folder = createSessionFolderInfo(path);
                        folder.put("path", folderName); // 使用相对路径
                        return folder;
                    })
                    .sorted((a, b) -> b.get("createdTime").toString().compareTo(a.get("createdTime").toString()))
                    .collect(Collectors.toList());
            
            return ApiResponse.success("Session folders retrieved", sessionFolders);
            
        } catch (Exception e) {
            log.error("Failed to get session folders: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get session folders: " + e.getMessage());
        }
    }
    
    /**
     * 获取批量文件夹内的所有会话文件夹
     */
    @GetMapping("/batch-folders/{batchFolderName}/sessions")
    public ApiResponse<List<Map<String, Object>>> getBatchSessions(@PathVariable String batchFolderName) {
        try {
            String storageBasePath = fileStorageService.getBasePath();
            Path batchPath = Paths.get(storageBasePath, batchFolderName);
            if (!Files.exists(batchPath)) {
                return ApiResponse.error("Batch folder not found: " + batchFolderName);
            }
            
            List<Map<String, Object>> sessions = Files.list(batchPath)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("session_"))
                    .map(path -> {
                        Map<String, Object> session = createSessionFolderInfo(path);
                        // 修改path为完整的相对路径: batch_xxx/session_xxx
                        String sessionName = path.getFileName().toString();
                        session.put("path", batchFolderName + "/" + sessionName);
                        return session;
                    })
                    .sorted(Comparator.comparing(m -> m.get("name").toString()))
                    .collect(Collectors.toList());
            
            return ApiResponse.success("Batch sessions retrieved", sessions);
            
        } catch (Exception e) {
            log.error("Failed to get batch sessions: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get batch sessions: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定文件夹内的所有翻译结果图片
     * 支持批量文件夹路径和单个会话文件夹路径
     */
    @GetMapping("/images")
    public ApiResponse<Map<String, Object>> getFolderImages(@RequestParam String folderPath) {
        try {
            // 如果是相对路径，添加storage基础路径
            String storageBasePath = fileStorageService.getBasePath();
            Path folder = Paths.get(folderPath).isAbsolute() ? 
                    Paths.get(folderPath) : 
                    Paths.get(storageBasePath, folderPath);
            
            if (!Files.exists(folder)) {
                return ApiResponse.error("Folder not found: " + folderPath);
            }
            
            // 获取translated子目录，如果不存在则使用根目录（支持汇总文件夹）
            Path translatedPath = folder.resolve("translated");
            Path imagePath = Files.exists(translatedPath) ? translatedPath : folder;
            
            // 获取所有图片文件
            List<Map<String, Object>> images = Files.list(imagePath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                               name.endsWith(".png") || name.endsWith(".webp") || 
                               name.endsWith(".bmp");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> {
                        Map<String, Object> imageInfo = new HashMap<>();
                        imageInfo.put("filename", path.getFileName().toString());
                        // 计算相对于文件夹的相对路径
                        String relativePath = imagePath.equals(folder) ? 
                                path.getFileName().toString() : 
                                "translated/" + path.getFileName().toString();
                        imageInfo.put("relativePath", relativePath);
                        
                        try {
                            imageInfo.put("size", Files.size(path));
                            imageInfo.put("modifiedTime", Files.getLastModifiedTime(path).toString());
                        } catch (IOException e) {
                            imageInfo.put("size", 0);
                            imageInfo.put("modifiedTime", "Unknown");
                        }
                        
                        return imageInfo;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("folderPath", folderPath);
            result.put("folderName", folder.getFileName().toString());
            result.put("imageCount", images.size());
            result.put("images", images);
            
            return ApiResponse.success("Images retrieved", result);
            
        } catch (Exception e) {
            log.error("Failed to get folder images: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get folder images: " + e.getMessage());
        }
    }
    
    /**
     * 下载单个图片文件
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadImage(
            @RequestParam String folderPath,
            @RequestParam String filename) {
        
        try {
            // 如果是相对路径，添加storage基础路径
            String storageBasePath = fileStorageService.getBasePath();
            Path folder = Paths.get(folderPath).isAbsolute() ? 
                    Paths.get(folderPath) : 
                    Paths.get(storageBasePath, folderPath);
            
            // 先尝试translated子目录，如果不存在则尝试根目录
            Path translatedPath = folder.resolve("translated").resolve(filename);
            Path rootPath = folder.resolve(filename);
            Path imagePath = Files.exists(translatedPath) ? translatedPath : rootPath;
            
            if (!Files.exists(imagePath)) {
                log.error("Image not found: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(imagePath.toFile());
            
            // 确定Content-Type
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("Download failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 查看单个图片（内联显示，不下载）
     */
    @GetMapping("/view")
    public ResponseEntity<Resource> viewImage(
            @RequestParam String folderPath,
            @RequestParam String filename) {
        
        try {
            // 如果是相对路径，添加storage基础路径
            String storageBasePath = fileStorageService.getBasePath();
            Path folder = Paths.get(folderPath).isAbsolute() ? 
                    Paths.get(folderPath) : 
                    Paths.get(storageBasePath, folderPath);
            
            // 先尝试translated子目录，如果不存在则尝试根目录
            Path translatedPath = folder.resolve("translated").resolve(filename);
            Path rootPath = folder.resolve(filename);
            Path imagePath = Files.exists(translatedPath) ? translatedPath : rootPath;
            
            if (!Files.exists(imagePath)) {
                log.error("Image not found: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(imagePath.toFile());
            
            // 确定Content-Type
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("View image failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量下载整个文件夹的所有图片（打包成ZIP）
     */
    @GetMapping("/download-folder")
    public ResponseEntity<Resource> downloadFolder(@RequestParam String folderPath) {
        try {
            // 如果是相对路径，添加storage基础路径
            String storageBasePath = fileStorageService.getBasePath();
            Path folder = Paths.get(folderPath).isAbsolute() ? 
                    Paths.get(folderPath) : 
                    Paths.get(storageBasePath, folderPath);
            Path translatedPath = folder.resolve("translated");
            
            // 使用translated子目录，如果不存在则使用根目录
            Path imagePath = Files.exists(translatedPath) ? translatedPath : folder;
            
            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // 创建临时ZIP文件
            Path tempZip = Files.createTempFile("manga_translated_", ".zip");
            
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    Files.newOutputStream(tempZip))) {
                
                Files.list(imagePath)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(file.getFileName().toString());
                                zos.putNextEntry(entry);
                                Files.copy(file, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                log.error("Failed to add file to zip: {}", file, e);
                            }
                        });
            }
            
            Resource resource = new FileSystemResource(tempZip.toFile());
            String zipFilename = folder.getFileName().toString() + ".zip";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + zipFilename + "\"")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("Download folder failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建会话文件夹信息
     */
    private Map<String, Object> createSessionFolderInfo(Path path) {
        Map<String, Object> info = new HashMap<>();
        String folderName = path.getFileName().toString();
        info.put("name", folderName);
        info.put("path", folderName); // 使用相对路径（文件夹名称）
        
        try {
            // 统计翻译图片数量
            Path translatedPath = path.resolve("translated");
            long imageCount = 0;
            
            // 检查translated子目录
            if (Files.exists(translatedPath) && Files.isDirectory(translatedPath)) {
                imageCount = Files.list(translatedPath)
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".jpg") || name.endsWith(".png") || 
                                   name.endsWith(".jpeg") || name.endsWith(".webp") || 
                                   name.endsWith(".bmp");
                        })
                        .count();
            } else {
                // 如果没有translated子目录，检查根目录（汇总文件夹情况）
                imageCount = Files.list(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".jpg") || name.endsWith(".png") || 
                                   name.endsWith(".jpeg") || name.endsWith(".webp") || 
                                   name.endsWith(".bmp");
                        })
                        .count();
            }
            info.put("imageCount", imageCount);
            
            // 获取创建时间
            info.put("createdTime", Files.getLastModifiedTime(path).toString());
        } catch (IOException e) {
            info.put("imageCount", 0);
            info.put("createdTime", "Unknown");
        }
        
        return info;
    }
}
