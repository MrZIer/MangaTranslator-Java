package com.example.mangaTrans.service;

import com.example.mangaTrans.entity.TranslationSession;
import com.example.mangaTrans.enums.TaskStatus;
import com.example.mangaTrans.enums.TranslationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * 异步任务处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {
    
    private final SessionService sessionService;
    private final FileStorageService fileStorageService;
    private final HistoryService historyService;
    
    /**
     * 异步处理翻译任务 - 直接调用 translate.py
     */
    @Async
    public void processTranslationTask(String sessionId, TranslationEngine engine, 
                                      String targetLanguage) {
        log.info("Starting Python translation task for session: {}", sessionId);
        
        try {
            // 获取会话信息
            TranslationSession session = sessionService.getSession(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
            
            // 1. 准备输入输出路径
            sessionService.updateSessionProgress(sessionId, TaskStatus.PREPROCESS, 10, "准备翻译环境");
            
            File uploadFile = fileStorageService.getFilePath(session.getUploadPath()).toFile();
            String inputPath = uploadFile.getAbsolutePath();
            
            // 输出文件路径
            String outputFileName = "translated_" + session.getOriginalFileName();
            File outputFile = fileStorageService.getResultDirectory(sessionId)
                    .resolve(outputFileName).toFile();
            String outputPath = outputFile.getAbsolutePath();
            
            log.info("Input: {}", inputPath);
            log.info("Output: {}", outputPath);
            
            // 2. 调用 Python translate.py 完成完整翻译流程
            sessionService.updateSessionProgress(sessionId, TaskStatus.TRANSLATE, 20, "Python翻译中...");
            
            ProcessBuilder pb = new ProcessBuilder(
                    "python", 
                    "translate.py", 
                    inputPath,
                    "--output",
                    outputPath
            );
            
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            
            log.info("Executing: python translate.py \"{}\" --output \"{}\"", inputPath, outputPath);
            
            Process process = pb.start();
            
            // 读取输出并监控进度
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.info("Python: {}", line);
                    
                    // 根据输出更新进度
                    if (line.contains("检测文本区域") || line.contains("Detecting")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.PREPROCESS, 40, "检测文本区域");
                    } else if (line.contains("识别文本") || line.contains("OCR")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.OCR, 55, "识别文本");
                    } else if (line.contains("翻译") || line.contains("Translat")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.TRANSLATE, 70, "翻译中");
                    } else if (line.contains("渲染") || line.contains("替换") || line.contains("Render")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.RENDER, 85, "渲染译文");
                    }
                }
            }
            
            // 等待完成（最多5分钟）
            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroy();
                throw new RuntimeException("Python translation timeout (300s)");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                log.error("Python translation failed with exit code: {}", exitCode);
                log.error("Output: {}", output);
                throw new RuntimeException("Translation failed: " + output);
            }
            
            // 3. 验证输出文件
            if (!outputFile.exists()) {
                throw new RuntimeException("Output file not found: " + outputPath);
            }
            
            log.info("Python translation completed: {}", outputPath);
            
            // 4. 完成
            sessionService.updateSessionProgress(sessionId, TaskStatus.COMPLETED, 100, "翻译完成");
            session.setResultPath(outputPath);
            
            // 保存到历史记录
            historyService.createHistory(session);
            
            log.info("Translation task completed for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Translation task failed for session {}: {}", sessionId, e.getMessage(), e);
            sessionService.markSessionFailed(sessionId, e.getMessage());
        }
    }
}
