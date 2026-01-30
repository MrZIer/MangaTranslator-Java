package com.example.mangaTrans.dto;

import com.example.mangaTrans.enums.OutputFormat;
import com.example.mangaTrans.enums.TranslationEngine;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件上传请求DTO
 */
@Data
public class UploadRequest {
    
    @NotNull(message = "Translation engine is required")
    private TranslationEngine engine = TranslationEngine.OPENAI;
    
    private String sourceLanguage = "ja";
    
    @NotNull(message = "Target language is required")
    private String targetLanguage = "zh";
    
    private OutputFormat outputFormat = OutputFormat.SINGLE_IMAGE;
}
