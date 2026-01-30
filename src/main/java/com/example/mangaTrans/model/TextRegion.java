package com.example.mangaTrans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本区域信息（OCR识别结果）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextRegion {
    private String text;              // 识别的文本内容
    private Double confidence;        // 置信度 (0-1)
    private BoundingBox boundingBox;  // 位置坐标
    private String language;          // 语言类型 (ja, zh, ko等)
    private String translatedText;    // 翻译后的文本
}
