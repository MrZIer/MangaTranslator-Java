package com.example.mangaTrans.service;

import com.example.mangaTrans.enums.TranslationEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TranslationService {

    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${translation.cache.ttl-days:7}")
    private int cacheTtlDays;

    // 智谱清言配置
    @Value("${translation.zhipu.api-key}")
    private String zhipuApiKey;
    @Value("${translation.zhipu.model:glm-4-flash}")
    private String zhipuModel;
    @Value("${translation.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String zhipuBaseUrl;

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

    public TranslationService(WebClient.Builder webClientBuilder, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClientBuilder.build();
        this.redisTemplate = redisTemplate;
    }

    public String translate(String text, String targetLanguage, TranslationEngine engine) {
        // 检查缓存
        String cacheKey = generateCacheKey(text, targetLanguage, engine);
        String cached = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Translation cache hit for key: {}", cacheKey);
            return cached;
        }

        // 根据引擎选择翻译方法
        String translated = switch (engine) {
            case OPENAI -> translateWithOpenAI(text, targetLanguage);
            case CLAUDE -> translateWithClaude(text, targetLanguage);
            case DEEPSEEK -> translateWithDeepSeek(text, targetLanguage);
            case ZHIPU -> translateWithZhipu(text, targetLanguage);  // 新增
        };

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, translated, cacheTtlDays, TimeUnit.DAYS);

        return translated;
    }

    /**
     * 使用智谱清言翻译
     */
    private String translateWithZhipu(String text, String targetLanguage) {
        log.info("Translating with Zhipu API: {} characters", text.length());

        String prompt = String.format(
                "你是一个专业的漫画翻译助手。请将以下日文漫画文本翻译成%s，保持原文的语气和风格。\n\n原文：%s\n\n只返回翻译结果，不要包含任何解释。",
                getLanguageName(targetLanguage),
                text
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", zhipuModel);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2000);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(zhipuBaseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + zhipuApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new RuntimeException("Invalid response from Zhipu API");

        } catch (Exception e) {
            log.error("Zhipu translation failed", e);
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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = text + ":" + targetLanguage + ":" + engine.name();
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return "translation:" + bytesToHex(hash);
        } catch (Exception e) {
            return "translation:" + text.hashCode() + ":" + targetLanguage + ":" + engine.name();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String getLanguageName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "zh", "zh-cn", "chinese" -> "简体中文";
            case "zh-tw", "traditional-chinese" -> "繁体中文";
            case "en", "english" -> "English";
            case "ja", "japanese" -> "日本語";
            case "ko", "korean" -> "한국어";
            default -> languageCode;
        };
    }

    // ...existing code... (保留其他方法)
}
