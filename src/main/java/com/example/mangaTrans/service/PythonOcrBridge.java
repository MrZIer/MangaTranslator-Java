package com.example.mangaTrans.service;

import com.example.mangaTrans.model.TextRegion;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class PythonOcrBridge {

    @Value("${ocr.model.path:./models/manga_ocr}")
    private String modelPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ???Python????????manga_ocr
     */
    public List<TextRegion> detectText(File imageFile) {
        try {
            log.info("Starting Python OCR bridge for image: {}", imageFile.getName());
            
            // ????Python???? - ?????? python_ocr_bridge.py
            ProcessBuilder pb = new ProcessBuilder(
                    "python", "python_ocr_bridge.py",
                    imageFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            // ???????
            Process process = pb.start();
            
            // ?????? - ???ByteArrayOutputStream???????????
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            java.io.ByteArrayOutputStream errorStream = new java.io.ByteArrayOutputStream();
            
            // ?????????
            byte[] buffer = new byte[8192];
            int bytesRead;
            java.io.InputStream inputStream = process.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            // ??????????????????????????
            java.io.InputStream errorInputStream = process.getErrorStream();
            while ((bytesRead = errorInputStream.read(buffer)) != -1) {
                errorStream.write(buffer, 0, bytesRead);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorOutput = errorStream.toString(StandardCharsets.UTF_8);
                log.error("Python process failed with error: {}", errorOutput);
                throw new RuntimeException("Python OCR process failed with exit code: " + exitCode);
            }

            // ????JSON??? - ?????????????UTF-8?????
            String fullOutput = outputStream.toString(StandardCharsets.UTF_8.name());
            log.info("Received output from Python (length: {} bytes)", outputStream.size());
            
            // ????JSON??????????{?????
            int jsonStart = fullOutput.indexOf("{");
            if (jsonStart == -1) {
                log.error("No JSON found in output: {}", fullOutput.substring(0, Math.min(200, fullOutput.length())));
                throw new RuntimeException("No JSON found in Python output");
            }
            
            // ???JSON????
            String jsonOutput = fullOutput.substring(jsonStart);
            
            // ????JSON????????
            int jsonEnd = jsonOutput.lastIndexOf("}");
            if (jsonEnd != -1) {
                jsonOutput = jsonOutput.substring(0, jsonEnd + 1);
            }
            
            log.info("Parsing JSON (size: {} chars)", jsonOutput.length());
            
            // ????UTF-8?????????
            OcrResult result = objectMapper.readValue(jsonOutput, OcrResult.class);
            
            if (!result.isSuccess()) {
                throw new RuntimeException("OCR failed: " + result.getError());
            }
            
            return result.getRegions();

        } catch (Exception e) {
            log.error("Python OCR bridge failed", e);
            throw new RuntimeException("OCR processing failed: " + e.getMessage());
        }
    }
    
    /**
     * OCR????????
     */
    private static class OcrResult {
        private boolean success;
        private String error;
        private String text;
        private List<TextRegion> regions;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public List<TextRegion> getRegions() {
            return regions;
        }
        
        public void setRegions(List<TextRegion> regions) {
            this.regions = regions;
        }
    }
}