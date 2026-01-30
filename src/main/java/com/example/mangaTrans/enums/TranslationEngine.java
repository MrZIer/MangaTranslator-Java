package com.example.mangaTrans.enums;

/**
 * ??????????????
 */
public enum TranslationEngine {
    OPENAI("OpenAI GPT-4o"),
    CLAUDE("Claude 3.5"),
    DEEPSEEK("DeepSeek"),
    ZHIPU("????????");  // ????

    private final String displayName;

    TranslationEngine(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
