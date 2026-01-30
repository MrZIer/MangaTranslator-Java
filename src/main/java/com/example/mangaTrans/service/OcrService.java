package com.example.mangaTrans.service;

import com.example.mangaTrans.model.TextRegion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR服务（支持内置和外部模式）
 */
@Slf4j
@Service
public class OcrService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient webClient;
    
    @Value("${ocr.service.mode:embedded}")
    private String ocrMode;
    
    @Value("${ocr.service.url:}")
    private String ocrServiceUrl;
    
    @Value("${ocr.service.timeout:30000}")
    private int timeout;
    
    @Value("${ocr.cache.ttl-hours:24}")
    private int cacheTtlHours;
    
    @Autowired(required = false)
    private PythonOcrBridge pythonOcrBridge;
    
    @Autowired(required = false)
    private EmbeddedOcrService embeddedOcrService;
    
    public OcrService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate, 
                      WebClient.Builder webClientBuilder) {
        this.redisTemplate = redisTemplate;
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * 识别图像中的文本
     */
    public List<TextRegion> recognizeText(String imagePath, String fileHash) {
        // 尝试从缓存获取（仅当Redis可用时）
        String cacheKey = "ocr:" + fileHash;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.info("OCR result found in cache for hash: {}", fileHash);
                    return (List<TextRegion>) cached;
                }
            } catch (Exception e) {
                log.warn("Failed to get OCR result from Redis cache: {}", e.getMessage());
            }
        }
        
        // 根据模式选择OCR实现
        List<TextRegion> textRegions;
        
        if ("embedded".equalsIgnoreCase(ocrMode)) {
            log.info("Using embedded OCR mode for image: {}", imagePath);
            textRegions = recognizeWithEmbedded(imagePath);
        } else if ("external".equalsIgnoreCase(ocrMode)) {
            log.info("Using external OCR service for image: {}", imagePath);
            textRegions = recognizeWithExternal(imagePath);
        } else {
            throw new RuntimeException("Invalid OCR mode: " + ocrMode + ". Must be 'embedded' or 'external'");
        }
        
        // 缓存结果（仅当Redis可用时）
        if (redisTemplate != null && textRegions != null && !textRegions.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, textRegions, 
                        Duration.ofHours(cacheTtlHours));
                log.info("Cached OCR result for hash: {}", fileHash);
            } catch (Exception e) {
                log.warn("Failed to cache OCR result to Redis: {}", e.getMessage());
            }
        }
        
        return textRegions;
    }
    
    /**
     * 使用内置OCR（Python桥接或DJL）
     */
    private List<TextRegion> recognizeWithEmbedded(String imagePath) {
        if (pythonOcrBridge != null) {
            try {
                return pythonOcrBridge.detectText(new java.io.File(imagePath));
            } catch (Exception e) {
                log.warn("Python OCR bridge failed, trying embedded service: {}", e.getMessage());
            }
        }
        
        if (embeddedOcrService != null) {
            return embeddedOcrService.detectText(new java.io.File(imagePath));
        }
        
        throw new RuntimeException("No embedded OCR service available. Please configure Python OCR or DJL.");
    }
    
    /**
     * 使用外部OCR服务
     */
    private List<TextRegion> recognizeWithExternal(String imagePath) {
        if (ocrServiceUrl == null || ocrServiceUrl.isEmpty()) {
            throw new RuntimeException("External OCR mode selected but ocr.service.url is not configured");
        }
        
        log.info("Calling external OCR service at: {}", ocrServiceUrl);
        
        try {
            Map<String, String> request = Map.of("image_path", imagePath);
            
            Mono<List<TextRegion>> response = webClient.post()
                    .uri(ocrServiceUrl + "/api/ocr/recognize")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(TextRegion.class)
                    .collectList()
                    .timeout(Duration.ofMillis(timeout));
            
            return response.block();
        } catch (Exception e) {
            log.error("Error calling external OCR service: {}", e.getMessage(), e);
            throw new RuntimeException("External OCR service error: " + e.getMessage());
        }
    }
    
    /**
     * 清除OCR缓存
     */
    public void clearCache(String fileHash) {
        String cacheKey = "ocr:" + fileHash;
        redisTemplate.delete(cacheKey);
        log.info("Cleared OCR cache for hash: {}", fileHash);
    }
}
