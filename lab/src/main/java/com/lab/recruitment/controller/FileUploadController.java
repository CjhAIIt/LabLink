package com.lab.recruitment.controller;

import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private JwtUtils jwtUtils;

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
            Path targetDir = fileStorageService.resolveProtectedTargetDirectory(today);
            String newFilename = UUID.randomUUID() + extension;
            Path targetFile = targetDir.resolve(newFilename);
            file.transferTo(targetFile.toFile());

            String protectedKey = fileStorageService.buildProtectedKey(today, newFilename);
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", originalFilename);
            result.put("url", protectedKey);
            result.put("path", protectedKey);
            result.put("fileSize", file.getSize());

            return Result.success(result);
        } catch (IOException e) {
            return Result.error("上传失败，请稍后重试");
        }
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(@RequestParam("path") String path,
                                             @RequestParam(value = "token", required = false) String token) {
        try {
            if (!hasFileAccess(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Path resolvedPath = resolveStoragePath(path);
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

    private Path resolveStoragePath(String rawPath) {
        Path protectedPath = fileStorageService.resolveProtectedPath(rawPath);
        if (protectedPath != null) {
            return protectedPath;
        }
        return fileStorageService.resolvePublicPath(rawPath);
    }

    private boolean hasFileAccess(String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && StringUtils.hasText(authentication.getName())
                && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return true;
        }

        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            jwtUtils.parseToken(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
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
