package com.example.mangaTrans.service;

import com.example.mangaTrans.model.TextRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OCR服务（调用manga_ocr微服务）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {
    
    private final WebClient.Builder webClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${ocr.service.url}")
    private String ocrServiceUrl;
    
    @Value("${ocr.service.timeout}")
    private int timeout;
    
    @Value("${ocr.cache.ttl-hours}")
    private int cacheTtlHours;
    
    /**
     * 识别图像中的文本
     */
    public List<TextRegion> recognizeText(String imagePath, String fileHash) {
        // 尝试从缓存获取
        String cacheKey = "ocr:" + fileHash;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("OCR result found in cache for hash: {}", fileHash);
            return (List<TextRegion>) cached;
        }
        
        // 调用OCR服务
        log.info("Calling OCR service for image: {}", imagePath);
        
        WebClient webClient = webClientBuilder.baseUrl(ocrServiceUrl).build();
        
        try {
            Map<String, String> request = Map.of("image_path", imagePath);
            
            Mono<List<TextRegion>> response = webClient.post()
                    .uri("/api/ocr/recognize")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(TextRegion.class)
                    .collectList()
                    .timeout(Duration.ofMillis(timeout));
            
            List<TextRegion> textRegions = response.block();
            
            // 缓存结果
            if (textRegions != null && !textRegions.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, textRegions, 
                        Duration.ofHours(cacheTtlHours));
                log.info("Cached OCR result for hash: {}", fileHash);
            }
            
            return textRegions;
        } catch (Exception e) {
            log.error("Error calling OCR service: {}", e.getMessage(), e);
            throw new RuntimeException("OCR service error: " + e.getMessage());
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
