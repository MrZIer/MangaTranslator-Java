package com.example.mangaTrans.controller;

import com.example.mangaTrans.dto.ApiResponse;
import com.example.mangaTrans.service.FileStorageService;
import com.example.mangaTrans.service.PythonTranslateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ?????????????
 * ??????Python???????????????????
 */
@Slf4j
@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslateController {
    
    private final PythonTranslateService pythonTranslateService;
    private final FileStorageService fileStorageService;
    
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/bmp");
    
    /**
     * ??????? - ???Python????
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("pythonAvailable", pythonTranslateService.isPythonAvailable());
        status.put("scriptAvailable", pythonTranslateService.isScriptAvailable());
        status.put("status", "ok");
        return ApiResponse.success("Service is healthy", status);
    }
    
    /**
     * ????????????
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> translateImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "outputFolder", required = false) String outputFolder) {
        
        try {
            // ??????
            if (files == null || files.length == 0) {
                return ApiResponse.error("No files provided");
            }
            
            for (MultipartFile file : files) {
                if (!ALLOWED_TYPES.contains(file.getContentType())) {
                    return ApiResponse.error("Invalid file type: " + file.getOriginalFilename());
                }
            }
            
            // ???????ID
            String taskId = UUID.randomUUID().toString();
            
            // ????????????
            List<String> savedPaths = new ArrayList<>();
            for (MultipartFile file : files) {
                String savedPath = fileStorageService.saveUploadedFile(file, taskId, file.getOriginalFilename());
                savedPaths.add(savedPath);
            }
            
            log.info("Saved {} files for translation", savedPaths.size());
            
            // ??????????????????????
            Path outputDirPath = outputFolder != null ? Paths.get(outputFolder) : 
                    Paths.get(fileStorageService.getTranslatedPath(taskId));
            Files.createDirectories(outputDirPath);
            String outputDir = outputDirPath.toString();
            
            log.info("Output directory: {}", outputDir);
            
            // ????Python??????
            String result;
            if (savedPaths.size() == 1) {
                // ?????? - ??????????????????????
                String outputPath = outputDirPath.resolve(
                        new File(savedPaths.get(0)).getName()).toString();
                log.info("Translating single image to: {}", outputPath);
                result = pythonTranslateService.translateSingleImage(savedPaths.get(0), outputPath);
            } else {
                // ?????? - ???????????????
                log.info("Translating {} images to directory: {}", savedPaths.size(), outputDir);
                result = pythonTranslateService.translateMultipleImages(savedPaths, outputDir);
            }
            
            // ?????????
            File outputDirectory = new File(outputDir);
            List<String> outputFiles = new ArrayList<>();
            if (outputDirectory.exists() && outputDirectory.isDirectory()) {
                File[] resultFiles = outputDirectory.listFiles((dir, name) -> 
                        name.endsWith(".jpg") || name.endsWith(".png"));
                if (resultFiles != null) {
                    for (File f : resultFiles) {
                        outputFiles.add(f.getName());
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("inputCount", files.length);
            response.put("outputCount", outputFiles.size());
            response.put("outputFiles", outputFiles);
            response.put("outputDirectory", outputDir);
            
            return ApiResponse.success("Translation completed successfully", response);
            
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage(), e);
            return ApiResponse.error("Translation failed: " + e.getMessage());
        }
    }
    
    /**
     * ?????????????????????
     */
    @PostMapping("/folder")
    public ApiResponse<Map<String, Object>> translateFolder(
            @RequestParam("folderPath") String folderPath,
            @RequestParam(value = "outputFolder", required = false) String outputFolder) {
        
        try {
            // ????????
            Path folder = Paths.get(folderPath);
            if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                return ApiResponse.error("Invalid folder path: " + folderPath);
            }
            
            // ????Python??????
            String result = pythonTranslateService.translateFolder(folderPath, outputFolder);
            
            // ???????????
            String outputDir = outputFolder != null ? outputFolder : 
                    folder.resolve("translated").toString();
            
            // ?????????
            File outputDirectory = new File(outputDir);
            int outputCount = 0;
            if (outputDirectory.exists() && outputDirectory.isDirectory()) {
                File[] resultFiles = outputDirectory.listFiles((dir, name) -> 
                        name.endsWith(".jpg") || name.endsWith(".png"));
                outputCount = resultFiles != null ? resultFiles.length : 0;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("inputFolder", folderPath);
            response.put("outputFolder", outputDir);
            response.put("outputCount", outputCount);
            response.put("result", "Translation completed successfully");
            
            return ApiResponse.success("Folder translation completed", response);
            
        } catch (Exception e) {
            log.error("Folder translation failed: {}", e.getMessage(), e);
            return ApiResponse.error("Folder translation failed: " + e.getMessage());
        }
    }
    
    /**
     * ?????????
     */
    @GetMapping("/download/{taskId}/{filename}")
    public ResponseEntity<Resource> downloadResult(
            @PathVariable String taskId,
            @PathVariable String filename) {
        
        try {
            Path filePath = Paths.get(fileStorageService.getTranslatedPath(taskId)).resolve(filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(filePath.toFile());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("Download failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
