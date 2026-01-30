package com.example.mangaTrans.controller;

import com.example.mangaTrans.dto.ApiResponse;
import com.example.mangaTrans.dto.SessionResponse;
import com.example.mangaTrans.dto.UploadRequest;
import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.service.AsyncTaskService;
import com.example.mangaTrans.service.FileStorageService;
import com.example.mangaTrans.service.FingerprintService;
import com.example.mangaTrans.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {
    
    private final SessionService sessionService;
    private final FingerprintService fingerprintService;
    private final FileStorageService fileStorageService;
    private final AsyncTaskService asyncTaskService;
    
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/jpg", "application/zip");
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    /**
     * 上传文件并开始翻译
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SessionResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute UploadRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ApiResponse.error("File is empty");
            }
            
            if (file.getSize() > MAX_FILE_SIZE) {
                return ApiResponse.error("File size exceeds 10MB limit");
            }
            
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                return ApiResponse.error("Invalid file type. Only PNG, JPG, and ZIP are allowed");
            }
            
            // 生成用户指纹
            String userFingerprint = fingerprintService.generateFingerprint(httpRequest);
            
            // 创建会话
            TranslationSession session = sessionService.createSession(file, userFingerprint);
            
            // 保存上传文件
            String uploadPath = fileStorageService.saveUploadedFile(file, session.getId());
            session.setUploadPath(uploadPath);
            session.setEngine(request.getEngine());
            session.setSourceLanguage(request.getSourceLanguage());
            session.setTargetLanguage(request.getTargetLanguage());
            session.setOutputFormat(request.getOutputFormat());
            
            // 启动异步处理任务
            asyncTaskService.processTranslationTask(
                    session.getId(), 
                    request.getEngine(), 
                    request.getTargetLanguage());
            
            SessionResponse response = new SessionResponse(
                    session.getId(),
                    session.getOriginalFileName(),
                    session.getStatus(),
                    session.getProgress(),
                    session.getCurrentStage(),
                    null
            );
            
            log.info("File uploaded successfully, session: {}", session.getId());
            return ApiResponse.success("File uploaded successfully", response);
            
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ApiResponse.error("Upload failed: " + e.getMessage());
        }
    }
}
