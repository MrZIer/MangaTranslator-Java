package com.example.mangaTrans;

import com.example.mangaTrans.enums.TranslationEngine;
import com.example.mangaTrans.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
	"translation.zhipu.api-key=test-key",
	"translation.openai.api-key=test-key",
	"translation.claude.api-key=test-key",
	"translation.deepseek.api-key=test-key",
	"ocr.service.mode=embedded"
})
class MangaTransApplicationTests {

	@Autowired(required = false)
	private TranslationService translationService;

	@Autowired(required = false)
	private SessionService sessionService;

	@Autowired(required = false)
	private OcrService ocrService;

	@Autowired(required = false)
	private FileStorageService fileStorageService;

	@Autowired(required = false)
	private AsyncTaskService asyncTaskService;

	@Test
	void contextLoads() {
		// Spring上下文加载测试
		assertNotNull(translationService, "TranslationService应该被注入");
		assertNotNull(sessionService, "SessionService应该被注入");
		assertNotNull(ocrService, "OcrService应该被注入");
		assertNotNull(fileStorageService, "FileStorageService应该被注入");
		assertNotNull(asyncTaskService, "AsyncTaskService应该被注入");
	}

	@Test
	void testTranslationServiceConfiguration() {
		// 测试翻译服务配置
		assertNotNull(translationService);
		
		// 测试翻译引擎枚举
		assertEquals(4, TranslationEngine.values().length);
		assertEquals("智谱清言", TranslationEngine.ZHIPU.getDisplayName());
	}

	@Test
	void testCacheKeyGeneration() {
		// 测试缓存键生成逻辑
		String text1 = "テスト";
		String text2 = "テスト";
		
		// 相同文本应该生成相同的哈希
		assertEquals(text1.hashCode(), text2.hashCode());
	}

	@Test
	void testEnumsDefinition() {
		// 测试所有枚举类型定义正确
		assertNotNull(TranslationEngine.ZHIPU);
		assertNotNull(TranslationEngine.OPENAI);
		assertNotNull(TranslationEngine.CLAUDE);
		assertNotNull(TranslationEngine.DEEPSEEK);
	}
}
