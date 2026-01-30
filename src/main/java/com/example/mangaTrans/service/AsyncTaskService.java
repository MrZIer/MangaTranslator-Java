package com.example.mangaTrans.service;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.enums.TaskStatus;
import com.example.mangaTrans.enums.TranslationEngine;
import com.example.mangaTrans.model.TextRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步任务处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {
    
    private final SessionService sessionService;
    private final FileStorageService fileStorageService;
    private final ImageProcessingService imageProcessingService;
    private final OcrService ocrService;
    private final TranslationService translationService;
    private final HistoryService historyService;
    
    @Value("${watermark.enabled}")
    private boolean watermarkEnabled;
    
    @Value("${watermark.text}")
    private String watermarkText;
    
    @Value("${watermark.opacity}")
    private float watermarkOpacity;
    
    /**
     * 异步处理翻译任务
     */
    @Async
    public void processTranslationTask(String sessionId, TranslationEngine engine, 
                                      String targetLanguage) {
        log.info("Starting async translation task for session: {}", sessionId);
        
        try {
            // 获取会话信息
            TranslationSession session = sessionService.getSession(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
            
            // 1. 预处理阶段
            sessionService.updateSessionProgress(sessionId, TaskStatus.PREPROCESS, 10, "图像预处理中");
            BufferedImage normalizedImage = preprocessImage(session.getUploadPath());
            String processedPath = saveProcessedImage(normalizedImage, sessionId);
            
            // 2. OCR识别阶段
            sessionService.updateSessionProgress(sessionId, TaskStatus.OCR, 30, "文本识别中");
            List<TextRegion> textRegions = ocrService.recognizeText(processedPath, session.getFileHash());
            
            if (textRegions == null || textRegions.isEmpty()) {
                throw new RuntimeException("No text detected in image");
            }
            
            // 3. 翻译阶段
            sessionService.updateSessionProgress(sessionId, TaskStatus.TRANSLATE, 50, "翻译中");
            Map<String, String> glossary = session.getGlossary() != null ? 
                    session.getGlossary() : new HashMap<>();
            
            for (int i = 0; i < textRegions.size(); i++) {
                TextRegion region = textRegions.get(i);
                String translatedText = translationService.translate(
                        region.getText(), 
                        targetLanguage, 
                        engine, 
                        glossary);
                region.setTranslatedText(translatedText);
                
                // 更新术语表
                if (!glossary.containsKey(region.getText())) {
                    glossary.put(region.getText(), translatedText);
                }
                
                // 更新进度
                int progress = 50 + (i * 20 / textRegions.size());
                sessionService.updateSessionProgress(sessionId, TaskStatus.TRANSLATE, 
                        progress, "翻译进度: " + (i + 1) + "/" + textRegions.size());
            }
            
            // 4. 渲染阶段
            sessionService.updateSessionProgress(sessionId, TaskStatus.RENDER, 70, "渲染译文中");
            BufferedImage renderedImage = renderTranslations(normalizedImage, textRegions, targetLanguage);
            
            // 添加水印（如果启用）
            if (watermarkEnabled) {
                renderedImage = imageProcessingService.addWatermark(renderedImage, 
                        watermarkText, watermarkOpacity);
            }
            
            // 5. 打包阶段
            sessionService.updateSessionProgress(sessionId, TaskStatus.PACKAGE, 90, "打包结果中");
            byte[] resultBytes = imageProcessingService.imageToBytes(renderedImage);
            String resultPath = fileStorageService.saveResultFile(resultBytes, sessionId, 
                    "translated_" + session.getOriginalFileName());
            
            // 6. 完成并保存历史记录
            session.setResultPath(resultPath);
            session.setTextRegions(textRegions);
            session.setGlossary(glossary);
            sessionService.updateSessionProgress(sessionId, TaskStatus.COMPLETED, 100, "翻译完成");
            
            // 保存到历史记录
            historyService.createHistory(session);
            
            log.info("Translation task completed for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Translation task failed for session {}: {}", sessionId, e.getMessage(), e);
            sessionService.markSessionFailed(sessionId, e.getMessage());
        }
    }
    
    /**
     * 预处理图像
     */
    private BufferedImage preprocessImage(String uploadPath) throws Exception {
        File imageFile = fileStorageService.getFilePath(uploadPath).toFile();
        return imageProcessingService.normalizeImage(imageFile);
    }
    
    /**
     * 保存处理后的图像
     */
    private String saveProcessedImage(BufferedImage image, String sessionId) throws Exception {
        byte[] imageBytes = imageProcessingService.imageToBytes(image);
        return fileStorageService.saveProcessedFile(imageBytes, sessionId, "normalized.png");
    }
    
    /**
     * 渲染翻译文本
     */
    private BufferedImage renderTranslations(BufferedImage image, List<TextRegion> textRegions, 
                                            String targetLanguage) {
        BufferedImage result = image;
        
        // 根据目标语言选择合适的字体
        String fontName = selectFont(targetLanguage);
        
        for (TextRegion region : textRegions) {
            if (region.getTranslatedText() != null && !region.getTranslatedText().isEmpty()) {
                result = imageProcessingService.renderTranslation(result, region, fontName);
            }
        }
        
        return result;
    }
    
    /**
     * 根据语言选择字体
     */
    private String selectFont(String language) {
        switch (language.toLowerCase()) {
            case "ja":
            case "japanese":
                return "Noto Sans JP";
            case "zh":
            case "chinese":
                return "Noto Sans SC";
            case "ko":
            case "korean":
                return "Noto Sans KR";
            default:
                return "Arial";
        }
    }
}
