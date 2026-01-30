package com.example.mangaTrans.service;

import com.example.mangaTrans.model.TextRegion;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@Service
public class PythonOcrBridge {

    @Value("${ocr.model.path:./models/manga_ocr}")
    private String modelPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通过Python子进程调用manga_ocr
     */
    public List<TextRegion> detectText(File imageFile) {
        try {
            // 构建Python命令
            ProcessBuilder pb = new ProcessBuilder(
                    "python", "ocr_wrapper.py",
                    "--image", imageFile.getAbsolutePath(),
                    "--model", modelPath
            );
            pb.redirectErrorStream(true);

            // 执行命令
            Process process = pb.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python OCR process failed with code: " + exitCode);
            }

            // 解析JSON结果
            return objectMapper.readValue(output.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TextRegion.class));

        } catch (Exception e) {
            log.error("Python OCR bridge failed", e);
            throw new RuntimeException("OCR processing failed: " + e.getMessage());
        }
    }
}