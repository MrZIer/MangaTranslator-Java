package com.example.mangaTrans.controller;

import com.example.mangaTrans.dto.ApiResponse;
import com.example.mangaTrans.dto.HistoryResponse;
import com.example.mangaTrans.entity.TranslationHistory;
import com.example.mangaTrans.service.FileStorageService;
import com.example.mangaTrans.service.FingerprintService;
import com.example.mangaTrans.service.HistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 历史记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {
    
    private final HistoryService historyService;
    private final FingerprintService fingerprintService;
    private final FileStorageService fileStorageService;
    
    /**
     * 获取用户历史记录（分页）
     */
    @GetMapping
    public ApiResponse<Page<HistoryResponse>> getUserHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        try {
            String userFingerprint = fingerprintService.generateFingerprint(request);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<TranslationHistory> historyPage = historyService.getUserHistory(userFingerprint, pageable);
            
            Page<HistoryResponse> responsePage = historyPage.map(history -> 
                    new HistoryResponse(
                            history.getId(),
                            history.getOriginalFileName(),
                            history.getFileSize(),
                            history.getEngine().getDisplayName(),
                            history.getSourceLanguage(),
                            history.getTargetLanguage(),
                            history.getAccessCount(),
                            history.getCreatedAt(),
                            history.getExpiresAt()
                    ));
            
            return ApiResponse.success(responsePage);
            
        } catch (Exception e) {
            log.error("Failed to get history: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get history: " + e.getMessage());
        }
    }
    
    /**
     * 下载历史结果文件
     */
    @GetMapping("/{historyId}/download")
    public ResponseEntity<Resource> downloadResult(@PathVariable String historyId) {
        try {
            Optional<TranslationHistory> optHistory = historyService.findByFileHash(historyId);
            
            if (optHistory.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TranslationHistory history = optHistory.get();
            
            // 增加访问次数
            historyService.incrementAccessCount(historyId);
            
            // 读取文件
            byte[] fileData = fileStorageService.readFile(history.getResultPath());
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + history.getOriginalFileName() + "\"")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("Failed to download result: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 延长历史记录保留期限
     */
    @PostMapping("/{historyId}/extend")
    public ApiResponse<String> extendExpiration(
            @PathVariable String historyId,
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            historyService.extendExpiration(historyId, days);
            return ApiResponse.success("Expiration extended by " + days + " days");
            
        } catch (Exception e) {
            log.error("Failed to extend expiration: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to extend expiration: " + e.getMessage());
        }
    }
}
