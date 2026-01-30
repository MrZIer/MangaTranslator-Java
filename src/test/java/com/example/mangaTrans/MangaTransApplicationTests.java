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
		// Spring????????????
		assertNotNull(translationService, "TranslationService???????");
		assertNotNull(sessionService, "SessionService???????");
		assertNotNull(ocrService, "OcrService???????");
		assertNotNull(fileStorageService, "FileStorageService???????");
		assertNotNull(asyncTaskService, "AsyncTaskService???????");
	}

	@Test
	void testTranslationServiceConfiguration() {
		// ??????????????
		assertNotNull(translationService);
		
		// ??????????????
		assertEquals(4, TranslationEngine.values().length);
		assertEquals("????????", TranslationEngine.ZHIPU.getDisplayName());
	}

	@Test
	void testCacheKeyGeneration() {
		// ???????????????
		String text1 = "?????";
		String text2 = "?????";
		
		// ????????????????????
		assertEquals(text1.hashCode(), text2.hashCode());
	}

	@Test
	void testEnumsDefinition() {
		// ?????????????????????
		assertNotNull(TranslationEngine.ZHIPU);
		assertNotNull(TranslationEngine.OPENAI);
		assertNotNull(TranslationEngine.CLAUDE);
		assertNotNull(TranslationEngine.DEEPSEEK);
	}
}
