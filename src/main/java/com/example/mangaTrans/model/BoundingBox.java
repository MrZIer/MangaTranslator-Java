package com.example.mangaTrans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 边界框坐标信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {
    private Integer x;        // X坐标
    private Integer y;        // Y坐标
    private Integer width;    // 宽度
    private Integer height;   // 高度
}
