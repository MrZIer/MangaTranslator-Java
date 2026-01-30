package com.example.mangaTrans.service;

import com.example.mangaTrans.enums.TranslationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 翻译服务（支持多引擎）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {
    
    private final WebClient.Builder webClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${translation.default-engine}")
    private String defaultEngine;
    
    @Value("${translation.cache.ttl-days}")
    private int cacheTtlDays;
    
    // OpenAI配置
    @Value("${translation.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${translation.openai.model}")
    private String openaiModel;
    
    @Value("${translation.openai.base-url}")
    private String openaiBaseUrl;
    
    // Claude配置
    @Value("${translation.claude.api-key}")
    private String claudeApiKey;
    
    @Value("${translation.claude.model}")
    private String claudeModel;
    
    @Value("${translation.claude.base-url}")
    private String claudeBaseUrl;
    
    // DeepSeek配置
    @Value("${translation.deepseek.api-key}")
    private String deepseekApiKey;
    
    @Value("${translation.deepseek.model}")
    private String deepseekModel;
    
    @Value("${translation.deepseek.base-url}")
    private String deepseekBaseUrl;
    
    /**
     * 翻译文本
     */
    public String translate(String text, String targetLanguage, TranslationEngine engine, 
                           Map<String, String> glossary) {
        // 生成缓存键
        String cacheKey = generateCacheKey(text, targetLanguage, engine);
        
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Translation found in cache");
            return (String) cached;
        }
        
        // 调用翻译API
        log.info("Translating text with engine: {}", engine);
        String translatedText;
        
        try {
            switch (engine) {
                case OPENAI:
                    translatedText = translateWithOpenAI(text, targetLanguage, glossary);
                    break;
                case CLAUDE:
                    translatedText = translateWithClaude(text, targetLanguage, glossary);
                    break;
                case DEEPSEEK:
                    translatedText = translateWithDeepSeek(text, targetLanguage, glossary);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported translation engine: " + engine);
            }
            
            // 缓存结果
            if (translatedText != null && !translatedText.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, translatedText, 
                        Duration.ofDays(cacheTtlDays));
            }
            
            return translatedText;
        } catch (Exception e) {
            log.error("Translation error: {}", e.getMessage(), e);
            throw new RuntimeException("Translation failed: " + e.getMessage());
        }
    }
    
    /**
     * 使用OpenAI翻译
     */
    private String translateWithOpenAI(String text, String targetLanguage, Map<String, String> glossary) {
        String prompt = buildTranslationPrompt(text, targetLanguage, glossary);
        
        WebClient webClient = webClientBuilder.baseUrl(openaiBaseUrl).build();
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", openaiModel);
        request.put("messages", new Object[]{
                Map.of("role", "system", "content", "You are a professional manga translator."),
                Map.of("role", "user", "content", prompt)
        });
        request.put("temperature", 0.3);
        
        Mono<Map> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + openaiApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class);
        
        Map responseBody = response.block();
        if (responseBody != null && responseBody.containsKey("choices")) {
            Object choices = responseBody.get("choices");
            if (choices instanceof java.util.List && !((java.util.List<?>) choices).isEmpty()) {
                Map choice = (Map) ((java.util.List<?>) choices).get(0);
                Map message = (Map) choice.get("message");
                return (String) message.get("content");
            }
        }
        
        throw new RuntimeException("Invalid OpenAI response");
    }
    
    /**
     * 使用Claude翻译
     */
    private String translateWithClaude(String text, String targetLanguage, Map<String, String> glossary) {
        String prompt = buildTranslationPrompt(text, targetLanguage, glossary);
        
        WebClient webClient = webClientBuilder.baseUrl(claudeBaseUrl).build();
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", claudeModel);
        request.put("max_tokens", 1024);
        request.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
        });
        
        Mono<Map> response = webClient.post()
                .uri("/messages")
                .header("x-api-key", claudeApiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class);
        
        Map responseBody = response.block();
        if (responseBody != null && responseBody.containsKey("content")) {
            Object content = responseBody.get("content");
            if (content instanceof java.util.List && !((java.util.List<?>) content).isEmpty()) {
                Map contentItem = (Map) ((java.util.List<?>) content).get(0);
                return (String) contentItem.get("text");
            }
        }
        
        throw new RuntimeException("Invalid Claude response");
    }
    
    /**
     * 使用DeepSeek翻译
     */
    private String translateWithDeepSeek(String text, String targetLanguage, Map<String, String> glossary) {
        // DeepSeek API与OpenAI兼容
        String prompt = buildTranslationPrompt(text, targetLanguage, glossary);
        
        WebClient webClient = webClientBuilder.baseUrl(deepseekBaseUrl).build();
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", deepseekModel);
        request.put("messages", new Object[]{
                Map.of("role", "system", "content", "You are a professional manga translator."),
                Map.of("role", "user", "content", prompt)
        });
        request.put("temperature", 0.3);
        
        Mono<Map> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + deepseekApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class);
        
        Map responseBody = response.block();
        if (responseBody != null && responseBody.containsKey("choices")) {
            Object choices = responseBody.get("choices");
            if (choices instanceof java.util.List && !((java.util.List<?>) choices).isEmpty()) {
                Map choice = (Map) ((java.util.List<?>) choices).get(0);
                Map message = (Map) choice.get("message");
                return (String) message.get("content");
            }
        }
        
        throw new RuntimeException("Invalid DeepSeek response");
    }
    
    /**
     * 构建翻译提示词
     */
    private String buildTranslationPrompt(String text, String targetLanguage, Map<String, String> glossary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Translate the following manga text to ").append(targetLanguage).append(":\n\n");
        prompt.append(text).append("\n\n");
        
        if (glossary != null && !glossary.isEmpty()) {
            prompt.append("Use the following glossary for consistency:\n");
            glossary.forEach((key, value) -> 
                    prompt.append("- ").append(key).append(" → ").append(value).append("\n"));
            prompt.append("\n");
        }
        
        prompt.append("Rules:\n");
        prompt.append("1. Keep the translation natural and context-appropriate\n");
        prompt.append("2. Preserve the tone and style of the original text\n");
        prompt.append("3. Only return the translated text, no explanations\n");
        
        return prompt.toString();
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(String text, String targetLanguage, TranslationEngine engine) {
        String raw = text + "|" + targetLanguage + "|" + engine.name();
        String hash = DigestUtils.sha256Hex(raw);
        return "translation:" + hash;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache(String text, String targetLanguage, TranslationEngine engine) {
        String cacheKey = generateCacheKey(text, targetLanguage, engine);
        redisTemplate.delete(cacheKey);
    }
}
