package com.example.mangaTrans.enums;

/**
 * 任务处理状态枚举
 */
public enum TaskStatus {
    UPLOAD("上传完成"),
    PREPROCESS("预处理中"),
    OCR("文字识别中"),
    TRANSLATE("翻译中"),
    RENDER("渲染中"),
    PACKAGE("打包中"),
    COMPLETED("已完成"),
    FAILED("处理失败");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
