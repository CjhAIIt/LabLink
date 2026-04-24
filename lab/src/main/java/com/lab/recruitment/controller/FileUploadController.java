package com.lab.recruitment.controller;

import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UnifiedFileService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.JwtUtils;
import com.lab.recruitment.utils.Result;
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
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UnifiedFileService unifiedFileService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "scene", required = false) String scene) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(unifiedFileService.uploadFile(file, scene, currentUser));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(@RequestParam("path") String path,
                                             @RequestParam(value = "token", required = false) String token) {
        try {
            Path resolvedPath = resolveStoragePath(path);
            if (resolvedPath == null || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
                return ResponseEntity.notFound().build();
            }

            if (!isPublicUploadPath(resolvedPath) && !hasFileAccess(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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

    private boolean isPublicUploadPath(Path resolvedPath) {
        Path uploadRoot = fileStorageService.getUploadRoot();
        return uploadRoot != null
                && resolvedPath.normalize().startsWith(uploadRoot.normalize());
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
}
