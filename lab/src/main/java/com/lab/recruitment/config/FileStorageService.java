package com.lab.recruitment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path uploadRoot;

    public FileStorageService(@Value("${file.upload-path:./uploads/}") String uploadPath) {
        this.uploadRoot = initializeUploadRoot(uploadPath);
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public Path resolveTargetDirectory(LocalDate date) {
        Path targetDir = uploadRoot.resolve(DATE_PATH_FORMATTER.format(date).replace("/", File.separator));
        ensureDirectory(targetDir);
        return targetDir;
    }

    public String buildPublicUrl(LocalDate date, String fileName) {
        return "/uploads/" + DATE_PATH_FORMATTER.format(date) + "/" + fileName;
    }

    public String getResourceLocation() {
        String resourceLocation = uploadRoot.toUri().toString();
        return resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/";
    }

    public Path resolvePublicPath(String publicPath) {
        String normalizedPath = publicPath == null ? "" : publicPath.trim();
        if (normalizedPath.isEmpty()) {
            return null;
        }

        int queryIndex = normalizedPath.indexOf('?');
        if (queryIndex >= 0) {
            normalizedPath = normalizedPath.substring(0, queryIndex);
        }

        int hashIndex = normalizedPath.indexOf('#');
        if (hashIndex >= 0) {
            normalizedPath = normalizedPath.substring(0, hashIndex);
        }

        if (normalizedPath.startsWith("/uploads/")) {
            normalizedPath = normalizedPath.substring("/uploads/".length());
        } else if (normalizedPath.startsWith("uploads/")) {
            normalizedPath = normalizedPath.substring("uploads/".length());
        } else {
            return null;
        }

        try {
            Path resolvedPath = uploadRoot.resolve(normalizedPath).normalize();
            return resolvedPath.startsWith(uploadRoot) ? resolvedPath : null;
        } catch (InvalidPathException ex) {
            log.warn("Invalid public upload path: {}", publicPath, ex);
            return null;
        }
    }

    private Path initializeUploadRoot(String uploadPath) {
        Path configuredPath = resolveConfiguredPath(uploadPath);
        if (ensureDirectory(configuredPath)) {
            return configuredPath;
        }

        Path fallbackPath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        ensureDirectory(fallbackPath);
        log.warn("Upload directory fallback in use: {}", fallbackPath);
        return fallbackPath;
    }

    private Path resolveConfiguredPath(String uploadPath) {
        String rawPath = uploadPath == null ? "" : uploadPath.trim();
        if (rawPath.isEmpty()) {
            rawPath = "./uploads/";
        }
        if (rawPath.startsWith("file:")) {
            rawPath = rawPath.substring(5);
        }

        try {
            Path candidatePath;
            if (rawPath.startsWith("./") || rawPath.startsWith(".\\")) {
                candidatePath = Paths.get(System.getProperty("user.dir")).resolve(rawPath.substring(2));
            } else {
                candidatePath = Paths.get(rawPath);
                if (!candidatePath.isAbsolute()) {
                    candidatePath = Paths.get(System.getProperty("user.dir")).resolve(rawPath);
                }
            }
            return candidatePath.toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            log.warn("Invalid upload path '{}', falling back to project uploads directory", uploadPath, ex);
            return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        }
    }

    private boolean ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException ex) {
            log.warn("Unable to create upload directory {}", path, ex);
            return false;
        }
    }
}
