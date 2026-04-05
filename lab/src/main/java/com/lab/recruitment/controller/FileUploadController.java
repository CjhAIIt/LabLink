package com.lab.recruitment.controller;

import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileUploadController {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> RESUME_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".zip", ".rar");
    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".zip", ".rar", ".jpg", ".jpeg", ".png");

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "scene", required = false) String scene) {
        try {
            if (file.isEmpty()) {
                return Result.error("请先选择文件");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return Result.error("文件大小不能超过 10MB");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            Set<String> allowedExtensions = resolveAllowedExtensions(scene);
            if (!allowedExtensions.contains(extension)) {
                return Result.error("不支持的文件类型");
            }

            LocalDate today = LocalDate.now();
            Path targetDir = fileStorageService.resolveTargetDirectory(today);
            String newFilename = UUID.randomUUID() + extension;
            Path targetFile = targetDir.resolve(newFilename);
            file.transferTo(targetFile.toFile());

            String publicUrl = fileStorageService.buildPublicUrl(today, newFilename);
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", originalFilename);
            result.put("url", publicUrl);
            result.put("path", publicUrl);
            result.put("fileSize", file.getSize());

            return Result.success(result);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(@RequestParam("path") String publicPath) {
        try {
            Path resolvedPath = fileStorageService.resolvePublicPath(publicPath);
            if (resolvedPath == null || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(resolvedPath.toUri());
            String fileName = resolvedPath.getFileName().toString();
            String contentType = Files.probeContentType(resolvedPath);
            MediaType mediaType = StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename*=UTF-8''" + UriUtils.encode(fileName, StandardCharsets.UTF_8))
                    .body(resource);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Set<String> resolveAllowedExtensions(String scene) {
        String normalizedScene = StringUtils.hasText(scene) ? scene.trim().toLowerCase(Locale.ROOT) : "";
        switch (normalizedScene) {
            case "resume":
                return RESUME_EXTENSIONS;
            case "avatar":
                return IMAGE_EXTENSIONS;
            case "attachment":
                return ATTACHMENT_EXTENSIONS;
            default:
                return DEFAULT_EXTENSIONS;
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }
}
