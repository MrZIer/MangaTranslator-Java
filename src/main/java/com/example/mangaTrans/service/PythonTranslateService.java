package com.example.mangaTrans.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Python??????????????
 */
@Slf4j
@Service
public class PythonTranslateService {
    
    @Value("${python.executable:python}")
    private String pythonExecutable;
    
    @Value("${python.script.path:translate.py}")
    private String scriptPath;
    
    @Value("${python.timeout:600}")
    private long timeoutSeconds;
    
    /**
     * ????????
     * 
     * @param inputImagePath ??????????
     * @param outputImagePath ????????????????
     * @return ????????????
     */
    public String translateSingleImage(String inputImagePath, String outputImagePath) throws Exception {
        log.info("Translating single image: {}", inputImagePath);
        
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add(inputImagePath);
        
        if (outputImagePath != null && !outputImagePath.isEmpty()) {
            command.add("--output");
            command.add(outputImagePath);
        }
        
        return executeCommand(command);
    }
    
    /**
     * ?????????
     * 
     * @param inputImagePaths ??????????????
     * @param outputFolder ???????????????????
     * @return ????????????
     */
    public String translateMultipleImages(List<String> inputImagePaths, String outputFolder) throws Exception {
        log.info("Translating {} images", inputImagePaths.size());
        
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.addAll(inputImagePaths);
        
        if (outputFolder != null && !outputFolder.isEmpty()) {
            command.add("--output");
            command.add(outputFolder);
        }
        
        return executeCommand(command);
    }
    
    /**
     * ?????????????
     * 
     * @param inputFolder ?????????????
     * @param outputFolder ???????????????????
     * @return ????????????
     */
    public String translateFolder(String inputFolder, String outputFolder) throws Exception {
        log.info("Translating folder: {}", inputFolder);
        
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add(inputFolder);
        
        if (outputFolder != null && !outputFolder.isEmpty()) {
            command.add("--output");
            command.add(outputFolder);
        }
        
        return executeCommand(command);
    }
    
    /**
     * ?????????????
     */
    private String executeCommand(List<String> command) throws Exception {
        log.info("Executing command: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        // ??????????????????
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        processBuilder.directory(workingDir.toFile());
        
        Process process = processBuilder.start();
        
        // ??????
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Python: {}", line);
                output.append(line).append("\n");
            }
        }
        
        // ??????????
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Translation timeout after " + timeoutSeconds + " seconds");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Translation failed with exit code: " + exitCode + 
                    "\nOutput: " + output.toString());
        }
        
        log.info("Translation completed successfully");
        return output.toString();
    }
    
    /**
     * ???Python??????????
     */
    public boolean isPythonAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            log.error("Python not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * ???translate.py?????????
     */
    public boolean isScriptAvailable() {
        Path scriptFile = Paths.get(scriptPath);
        return Files.exists(scriptFile);
    }
}
