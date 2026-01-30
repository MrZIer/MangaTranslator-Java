package com.example.mangaTrans.service;

import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 图像处理服务
 */
@Slf4j
@Service
public class ImageProcessingService {
    
    @Value("${image.max-width}")
    private int maxWidth;
    
    @Value("${image.max-height}")
    private int maxHeight;
    
    @Value("${image.output-format}")
    private String outputFormat;
    
    /**
     * 标准化图像（转换为PNG，限制尺寸）
     */
    public BufferedImage normalizeImage(File imageFile) throws IOException {
        log.info("Normalizing image: {}", imageFile.getName());
        
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Failed to read image file");
        }
        
        // 检查是否需要缩放
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        if (width > maxWidth || height > maxHeight) {
            log.info("Resizing image from {}x{} to fit within {}x{}", 
                    width, height, maxWidth, maxHeight);
            
            // 使用高质量缩放算法
            originalImage = Scalr.resize(originalImage, 
                    Scalr.Method.QUALITY, 
                    Scalr.Mode.FIT_TO_WIDTH, 
                    maxWidth, 
                    maxHeight);
        }
        
        return originalImage;
    }
    
    /**
     * 渲染翻译文本到图像
     */
    public BufferedImage renderTranslation(BufferedImage originalImage, 
                                          com.example.mangaTrans.model.TextRegion textRegion,
                                          String fontName) {
        log.info("Rendering translation - Original: '{}', Translated: '{}', BBox: {}", 
            textRegion.getText(), textRegion.getTranslatedText(), textRegion.getBoundingBox());
        
        Graphics2D g2d = originalImage.createGraphics();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 获取文本区域边界
        com.example.mangaTrans.model.BoundingBox bbox = textRegion.getBoundingBox();
        
        // 先用白色填充背景（覆盖原文）
        g2d.setColor(Color.WHITE);
        g2d.fillRect(bbox.getX(), bbox.getY(), bbox.getWidth(), bbox.getHeight());
        
        // 设置字体（自适应大小）
        int fontSize = calculateFontSize(textRegion.getTranslatedText(), 
                bbox.getWidth(), bbox.getHeight());
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        
        // 绘制翻译文本（居中对齐）
        FontMetrics fm = g2d.getFontMetrics();
        String text = textRegion.getTranslatedText();
        
        // 处理多行文本
        String[] lines = wrapText(text, bbox.getWidth(), fm);
        int lineHeight = fm.getHeight();
        int totalHeight = lines.length * lineHeight;
        int startY = bbox.getY() + (bbox.getHeight() - totalHeight) / 2 + fm.getAscent();
        
        for (int i = 0; i < lines.length; i++) {
            int textWidth = fm.stringWidth(lines[i]);
            int x = bbox.getX() + (bbox.getWidth() - textWidth) / 2;
            int y = startY + i * lineHeight;
            g2d.drawString(lines[i], x, y);
        }
        
        g2d.dispose();
        return originalImage;
    }
    
    /**
     * 添加水印
     */
    public BufferedImage addWatermark(BufferedImage image, String watermarkText, float opacity) {
        log.debug("Adding watermark: {}", watermarkText);
        
        Graphics2D g2d = image.createGraphics();
        
        // 设置透明度
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
        g2d.setComposite(alphaChannel);
        
        // 设置字体和颜色
        Font font = new Font("Arial", Font.BOLD, 20);
        g2d.setFont(font);
        g2d.setColor(Color.GRAY);
        
        // 在右下角绘制水印
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(watermarkText);
        int x = image.getWidth() - textWidth - 10;
        int y = image.getHeight() - 10;
        
        g2d.drawString(watermarkText, x, y);
        g2d.dispose();
        
        return image;
    }
    
    /**
     * 将BufferedImage转换为字节数组
     */
    public byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, outputFormat, baos);
        return baos.toByteArray();
    }
    
    /**
     * 计算合适的字体大小
     */
    private int calculateFontSize(String text, int maxWidth, int maxHeight) {
        // 简单的启发式算法，根据文本长度和区域大小计算字体大小
        int textLength = text.length();
        int areaSize = maxWidth * maxHeight;
        
        // 基础字体大小
        int fontSize = (int) Math.sqrt(areaSize / textLength);
        
        // 限制字体大小范围
        fontSize = Math.max(12, Math.min(fontSize, 48));
        
        return fontSize;
    }
    
    /**
     * 文本换行处理
     */
    private String[] wrapText(String text, int maxWidth, FontMetrics fm) {
        if (fm.stringWidth(text) <= maxWidth) {
            return new String[]{text};
        }
        
        // 简单的按空格分割换行
        String[] words = text.split(" ");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine.append(currentLine.length() == 0 ? word : " " + word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
}
