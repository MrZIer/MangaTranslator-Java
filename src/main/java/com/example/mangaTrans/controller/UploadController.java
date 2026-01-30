package com.example.mangaTrans.controller;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.service.AsyncTaskService;
import com.example.mangaTrans.service.FileStorageService;
import com.example.mangaTrans.service.SessionService;
import com.example.mangaTrans.service.ResultCollectionService;
import com.example.mangaTrans.enums.OutputFormat;
import com.example.mangaTrans.enums.TaskStatus;
import com.example.mangaTrans.enums.TranslationEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AsyncTaskService asyncTaskService;
    
    @Autowired
    private ResultCollectionService resultCollectionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceLanguage", defaultValue = "JA") String sourceLanguage,
            @RequestParam(value = "targetLanguage", defaultValue = "ZH") String targetLanguage,
            @RequestParam(value = "engine", defaultValue = "ZHIPU") String engineStr,
            @RequestParam(value = "outputFormat", defaultValue = "PNG") String formatStr,
            @RequestParam(value = "isBatch", defaultValue = "false") boolean isBatch) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证文件
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 解析枚举值（支持小写）
            TranslationEngine engine = TranslationEngine.valueOf(engineStr.toUpperCase());
            OutputFormat outputFormat = OutputFormat.valueOf(formatStr.toUpperCase());

            // 创建会话（如果文件已存在，会返回现有session）
            TranslationSession session = sessionService.createSession(file, "default-user", isBatch);
            
            // 检查是否是已存在的session
            boolean isExistingSession = session.getSessionDirectory() != null && session.getUploadPath() != null;
            
            if (isExistingSession) {
                // 如果是已存在的session，检查状态
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", session.getId());
                data.put("fileName", session.getOriginalFileName());
                data.put("status", session.getStatus().name());
                data.put("progress", session.getProgress());
                
                response.put("success", true);
                response.put("data", data);
                
                if (session.getStatus() == TaskStatus.COMPLETED) {
                    response.put("message", "该文件已翻译完成，直接返回结果");
                    return ResponseEntity.ok(response);
                } else if (session.getStatus() == TaskStatus.FAILED) {
                    // 失败的任务，允许重新翻译
                    response.put("message", "该文件之前翻译失败，正在重新翻译");
                    
                    // 重置状态并重新翻译
                    session.setEngine(engine);
                    session.setSourceLanguage(sourceLanguage);
                    session.setTargetLanguage(targetLanguage);
                    session.setOutputFormat(outputFormat);
                    session.setStatus(TaskStatus.UPLOAD);
                    session.setProgress(0);
                    session.setErrorMessage(null);
                    session = sessionService.saveSession(session);
                    
                    // 异步执行翻译任务
                    asyncTaskService.processTranslationTask(session.getId(), engine, targetLanguage);
                    return ResponseEntity.ok(response);
                } else {
                    // 正在处理中的任务
                    response.put("message", "该文件正在翻译中，请稍候查看进度");
                    return ResponseEntity.ok(response);
                }
            }
            
            // 新session：设置翻译参数
            session.setEngine(engine);
            session.setSourceLanguage(sourceLanguage);
            session.setTargetLanguage(targetLanguage);
            session.setOutputFormat(outputFormat);
            
            // 保存文件到uploads目录
            String savedPath = fileStorageService.saveUploadedFile(file, 
                    session.getSessionDirectory(), 
                    session.getOriginalFileName());
            session.setUploadPath(savedPath);
            
            // 保存 session 更改到数据库
            session = sessionService.saveSession(session);

            // 异步执行翻译任务
            asyncTaskService.processTranslationTask(session.getId(), engine, targetLanguage);

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", session.getId());
            data.put("fileName", session.getOriginalFileName());
            data.put("status", session.getStatus().name());
            data.put("progress", session.getProgress());
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "翻译任务已开始");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "无效的参数: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<TranslationSession> getStatus(@PathVariable String sessionId) {
        Optional<TranslationSession> sessionOpt = sessionService.getSession(sessionId);
        
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(sessionOpt.get());
    }

    @GetMapping("/download/{sessionId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String sessionId) {
        try {
            Optional<TranslationSession> sessionOpt = sessionService.getSession(sessionId);
            
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TranslationSession session = sessionOpt.get();
            
            if (session.getStatus() != TaskStatus.COMPLETED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            if (session.getResultPath() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = fileStorageService.loadFileAsResource(session.getResultPath());

            String contentType = "application/octet-stream";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/compare/{sessionId}")
    public ResponseEntity<Map<String, Object>> getCompareImages(@PathVariable String sessionId) {
        try {
            Optional<TranslationSession> sessionOpt = sessionService.getSession(sessionId);
            
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TranslationSession session = sessionOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            
            // 原图路径
            data.put("originalImage", "/api/upload/image/" + sessionId + "/original");
            
            // 翻译后图片路径（如果已完成）
            if (session.getStatus() == TaskStatus.COMPLETED && session.getResultPath() != null) {
                data.put("translatedImage", "/api/upload/image/" + sessionId + "/translated");
            }
            
            data.put("fileName", session.getOriginalFileName());
            data.put("status", session.getStatus().name());
            data.put("progress", session.getProgress());
            
            response.put("success", true);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/image/{sessionId}/{type}")
    public ResponseEntity<Resource> getImage(@PathVariable String sessionId, @PathVariable String type) {
        try {
            Optional<TranslationSession> sessionOpt = sessionService.getSession(sessionId);
            
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TranslationSession session = sessionOpt.get();
            String imagePath;
            
            if ("original".equals(type)) {
                imagePath = session.getUploadPath();
            } else if ("translated".equals(type)) {
                imagePath = session.getResultPath();
            } else {
                return ResponseEntity.badRequest().build();
            }
            
            if (imagePath == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = fileStorageService.loadFileAsResource(imagePath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getTranslationResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sessionId) {
        
        try {
            Map<String, Object> response = new HashMap<>();
            
            // 获取所有已完成的翻译结果
            List<Map<String, Object>> results = new ArrayList<>();
            
            if (sessionId != null) {
                // 获取特定session的结果
                Optional<TranslationSession> sessionOpt = sessionService.getSession(sessionId);
                if (sessionOpt.isPresent()) {
                    TranslationSession session = sessionOpt.get();
                    if (session.getStatus() == TaskStatus.COMPLETED && session.getResultPath() != null) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("sessionId", session.getId());
                        result.put("originalFileName", session.getOriginalFileName());
                        result.put("resultFileName", getFileNameFromPath(session.getResultPath()));
                        result.put("originalImage", "/api/upload/image/" + session.getId() + "/original");
                        result.put("translatedImage", "/api/upload/image/" + session.getId() + "/translated");
                        result.put("downloadUrl", "/api/upload/download/" + session.getId());
                        result.put("createdAt", session.getCreatedAt());
                        result.put("completedAt", session.getCompletedAt());
                        results.add(result);
                    }
                }
            } else {
                // 获取所有已完成的session
                List<TranslationSession> sessions = sessionService.getAllCompletedSessions();
                for (TranslationSession session : sessions) {
                    if (session.getResultPath() != null) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("sessionId", session.getId());
                        result.put("originalFileName", session.getOriginalFileName());
                        result.put("resultFileName", getFileNameFromPath(session.getResultPath()));
                        result.put("originalImage", "/api/upload/image/" + session.getId() + "/original");
                        result.put("translatedImage", "/api/upload/image/" + session.getId() + "/translated");
                        result.put("downloadUrl", "/api/upload/download/" + session.getId());
                        result.put("createdAt", session.getCreatedAt());
                        result.put("completedAt", session.getCompletedAt());
                        results.add(result);
                    }
                }
            }
            
            // 排序（按完成时间倒序）
            results.sort((a, b) -> {
                Object aTime = a.get("completedAt");
                Object bTime = b.get("completedAt");
                if (aTime == null || bTime == null) return 0;
                return ((Comparable) bTime).compareTo(aTime);
            });
            
            // 分页
            int total = results.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<Map<String, Object>> pageResults = fromIndex < total ? 
                    results.subList(fromIndex, toIndex) : new ArrayList<>();
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("size", size);
            pagination.put("total", total);
            pagination.put("totalPages", (int) Math.ceil((double) total / size));
            pagination.put("hasNext", toIndex < total);
            pagination.put("hasPrevious", page > 0);
            
            response.put("success", true);
            response.put("data", pageResults);
            response.put("pagination", pagination);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private String getFileNameFromPath(String path) {
        if (path == null) return null;
        return Paths.get(path).getFileName().toString();
    }
    
    /**
     * 获取所有批量文件夹列表
     */
    @GetMapping("/batch-folders")
    public ResponseEntity<Map<String, Object>> getBatchFolders() {
        try {
            List<File> batchFolders = fileStorageService.getBatchFolders();
            
            List<Map<String, String>> folderList = new ArrayList<>();
            for (File folder : batchFolders) {
                Map<String, String> info = new HashMap<>();
                info.put("name", folder.getName());
                info.put("path", folder.getAbsolutePath());
                info.put("sessionCount", String.valueOf(
                    fileStorageService.getSessionsInBatchFolder(folder.getAbsolutePath()).size()
                ));
                folderList.add(info);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("folders", folderList);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取批量文件夹列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 汇总指定批量文件夹的翻译结果
     */
    @PostMapping("/collect-batch")
    public ResponseEntity<Map<String, Object>> collectBatchResults(
            @RequestBody Map<String, String> request) {
        try {
            String batchFolderPath = request.get("batchFolderPath");
            
            if (batchFolderPath == null || batchFolderPath.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "批量文件夹路径不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = resultCollectionService.collectBatchFolderResults(batchFolderPath);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "汇总批量文件夹失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 汇总所有翻译结果到统一目录
     */
    @PostMapping("/collect-all")
    public ResponseEntity<Map<String, Object>> collectAllResults() {
        try {
            Map<String, Object> result = resultCollectionService.collectAllResults();
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "汇总失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 汇总指定会话的翻译结果
     */
    @PostMapping("/collect/{sessionId}")
    public ResponseEntity<Map<String, Object>> collectSessionResult(@PathVariable String sessionId) {
        try {
            Map<String, Object> result = resultCollectionService.collectSessionResult(sessionId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "汇总失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 从指定会话目录汇总结果到指定目标目录
     */
    @PostMapping("/collect-custom")
    public ResponseEntity<Map<String, Object>> collectCustom(
            @RequestBody Map<String, String> request) {
        try {
            String sessionDirectory = request.get("sessionDirectory");
            String targetDirectory = request.get("targetDirectory");
            
            if (sessionDirectory == null || sessionDirectory.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "会话目录路径不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (targetDirectory == null || targetDirectory.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "目标目录路径不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Map<String, Object> result = resultCollectionService.collectFromSessionDirectory(
                    sessionDirectory, targetDirectory);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "汇总失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有汇总目录列表
     */
    @GetMapping("/collections")
    public ResponseEntity<Map<String, Object>> getCollections() {
        try {
            List<Map<String, Object>> collections = resultCollectionService.getCollectionDirectories();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", collections);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取汇总目录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
