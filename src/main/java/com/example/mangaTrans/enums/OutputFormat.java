package com.example.mangaTrans.enums;

/**
 * 输出格式枚举
 */
public enum OutputFormat {
    SINGLE_IMAGE("单张图片"),
    ZIP("ZIP压缩包"),
    PDF("PDF文件");

    private final String description;

    OutputFormat(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
