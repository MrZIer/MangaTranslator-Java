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
            
            // 输出文件路径：使用 原文件名_cn.jpg 格式，输出到translator_result目录
            String originalFileName = session.getOriginalFileName();
            String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
            String outputFileName = baseName + "_cn" + extension;
            
            // 使用新的标准化目录结构：translator_result
            String translatorResultPath = fileStorageService.getTranslatedPath(session.getSessionDirectory());
            File outputFile = new File(translatorResultPath, outputFileName);
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
                    
                    // 根据输出更新进度 - 更细粒度的进度追踪
                    if (line.contains("检测文本区域") || line.contains("Detecting") || line.contains("detect")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.PREPROCESS, 35, "检测文本区域");
                    } else if (line.contains("文本区域检测完成") || line.contains("Detection complete")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.PREPROCESS, 45, "文本区域检测完成");
                    } else if (line.contains("识别文本") || line.contains("OCR") || line.contains("recogniz")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.OCR, 55, "识别文本中");
                    } else if (line.contains("文本识别完成") || line.contains("OCR complete")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.OCR, 65, "文本识别完成");
                    } else if (line.contains("翻译") || line.contains("Translat") || line.contains("translat")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.TRANSLATE, 75, "翻译文本中");
                    } else if (line.contains("翻译完成") || line.contains("Translation complete")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.TRANSLATE, 82, "翻译完成");
                    } else if (line.contains("渲染") || line.contains("替换") || line.contains("Render") || line.contains("render") || line.contains("inpaint")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.RENDER, 88, "渲染译文中");
                    } else if (line.contains("保存") || line.contains("Saving") || line.contains("save") || line.contains("完成")) {
                        sessionService.updateSessionProgress(sessionId, TaskStatus.RENDER, 95, "保存结果中");
                    }
                }
            }
            
            log.info("Python process output reading completed, waiting for process to finish...");
            sessionService.updateSessionProgress(sessionId, TaskStatus.PACKAGE, 97, "等待Python进程完成");
            
            // 等待完成（最多5分钟）
            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroy();
                sessionService.updateSessionProgress(sessionId, TaskStatus.FAILED, 0, "翻译超时");
                throw new RuntimeException("Python translation timeout (300s)");
            }
            
            int exitCode = process.exitValue();
            log.info("Python process finished with exit code: {}", exitCode);
            sessionService.updateSessionProgress(sessionId, TaskStatus.PACKAGE, 98, "验证结果");
            
            if (exitCode != 0) {
                log.error("Python translation failed with exit code: {}", exitCode);
                log.error("Output: {}", output);
                throw new RuntimeException("Translation failed: " + output);
            }
            
            // 3. 验证输出文件
            if (!outputFile.exists()) {
                sessionService.updateSessionProgress(sessionId, TaskStatus.FAILED, 0, "输出文件未找到");
                throw new RuntimeException("Output file not found: " + outputPath);
            }
            
            log.info("Python translation completed: {}", outputPath);
            sessionService.updateSessionProgress(sessionId, TaskStatus.PACKAGE, 99, "最终确认");
            
            // 4. 设置结果路径并保存
            session.setResultPath(outputPath);
            session = sessionService.saveSession(session);
            log.info("Saved result path to session: {}", outputPath);
            
            // 5. 更新为完成状态
            sessionService.updateSessionProgress(sessionId, TaskStatus.COMPLETED, 100, "翻译完成");
            
            // 保存到历史记录
            historyService.createHistory(session);
            
            log.info("Translation task completed for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Translation task failed for session {}: {}", sessionId, e.getMessage(), e);
            sessionService.markSessionFailed(sessionId, e.getMessage());
        }
    }
}
