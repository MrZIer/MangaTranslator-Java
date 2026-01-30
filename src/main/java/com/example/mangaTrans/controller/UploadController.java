package com.example.mangaTrans.controller;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.service.AsyncTaskService;
import com.example.mangaTrans.service.FileStorageService;
import com.example.mangaTrans.service.SessionService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceLanguage", defaultValue = "JA") String sourceLanguage,
            @RequestParam(value = "targetLanguage", defaultValue = "ZH") String targetLanguage,
            @RequestParam(value = "engine", defaultValue = "ZHIPU") String engineStr,
            @RequestParam(value = "outputFormat", defaultValue = "PNG") String formatStr) {

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
            TranslationSession session = sessionService.createSession(file, "default-user");
            
            // 检查是否是已存在的session
            boolean isExistingSession = session.getUploadPath() != null;
            
            if (isExistingSession) {
                // 如果是已存在的session，直接返回
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", session.getId());
                data.put("fileName", session.getOriginalFileName());
                data.put("status", session.getStatus().name());
                data.put("progress", session.getProgress());
                
                response.put("success", true);
                response.put("data", data);
                
                if (session.getStatus() == TaskStatus.COMPLETED) {
                    response.put("message", "该文件已翻译完成，直接返回结果");
                } else if (session.getStatus() == TaskStatus.FAILED) {
                    response.put("message", "该文件之前翻译失败，请重新上传");
                } else {
                    response.put("message", "该文件正在翻译中，请稍候");
                }
                return ResponseEntity.ok(response);
            }
            
            // 新session：设置翻译参数
            session.setEngine(engine);
            session.setSourceLanguage(sourceLanguage);
            session.setTargetLanguage(targetLanguage);
            session.setOutputFormat(outputFormat);
            
            // 保存文件
            String savedPath = fileStorageService.saveUploadedFile(file, session.getId());
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
}
