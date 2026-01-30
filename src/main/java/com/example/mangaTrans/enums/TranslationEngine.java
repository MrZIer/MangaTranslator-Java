package com.example.mangaTrans.enums;

/**
 * 翻译引擎枚举
 */
public enum TranslationEngine {
    OPENAI("OpenAI GPT-4o"),
    CLAUDE("Claude 3.5"),
    DEEPSEEK("DeepSeek");

    private final String displayName;

    TranslationEngine(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
