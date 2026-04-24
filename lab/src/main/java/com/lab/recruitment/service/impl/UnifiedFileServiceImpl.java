package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.dto.FileRelationBindDTO;
import com.lab.recruitment.entity.BusinessFileRelation;
import com.lab.recruitment.entity.FileObject;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.BusinessFileRelationMapper;
import com.lab.recruitment.mapper.FileObjectMapper;
import com.lab.recruitment.service.UnifiedFileService;
import com.lab.recruitment.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class UnifiedFileServiceImpl implements UnifiedFileService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedFileServiceImpl.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> RESUME_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg");
    private static final Set<String> LOGO_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".svg", ".webp");
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".zip", ".rar", ".jpg", ".jpeg", ".png");
    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".zip", ".rar", ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg");

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileObjectMapper fileObjectMapper;

    @Autowired
    private BusinessFileRelationMapper businessFileRelationMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String scene, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select a file first");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size must not exceed 10MB");
        }

        String originalFilename = trimToNull(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        Set<String> allowedExtensions = resolveAllowedExtensions(scene);
        if (!allowedExtensions.contains(extension)) {
            throw new RuntimeException("Unsupported file type");
        }

        LocalDate today = LocalDate.now();
        String generatedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDir = fileStorageService.resolveProtectedTargetDirectory(today);
        Path targetFile = targetDir.resolve(generatedName);
        String storagePath = fileStorageService.buildProtectedKey(today, generatedName);

        byte[] content;
        try {
            content = file.getBytes();
            Files.write(targetFile, content);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file", ex);
        }

        FileObject fileObject = new FileObject();
        fileObject.setFileName(generatedName);
        fileObject.setOriginalName(originalFilename == null ? generatedName : originalFilename);
        fileObject.setContentType(trimToNull(file.getContentType()));
        fileObject.setFileSize(file.getSize());
        fileObject.setStoragePath(storagePath);
        fileObject.setMd5(DigestUtils.md5DigestAsHex(content));
        fileObject.setUploadedBy(currentUser == null ? null : currentUser.getId());
        try {
            fileObjectMapper.insert(fileObject);
            return buildFilePayload(fileObject);
        } catch (RuntimeException ex) {
            FileObject existingFile = findExistingActiveFile(fileObject.getMd5(), storagePath);
            if (existingFile != null) {
                log.warn("Reusing existing file metadata after upload persistence conflict, scene={}, userId={}, fileId={}, storagePath={}",
                        scene, currentUser == null ? null : currentUser.getId(), existingFile.getId(), existingFile.getStoragePath(), ex);
                return buildFilePayload(existingFile);
            }

            if (supportsLegacyReference(scene)) {
                log.error("Falling back to protected path reference because file metadata persistence failed, scene={}, userId={}, storagePath={}",
                        scene, currentUser == null ? null : currentUser.getId(), storagePath, ex);
                return buildLegacyPayload(generatedName, originalFilename, file.getContentType(), file.getSize(), storagePath,
                        fileObject.getMd5(), currentUser == null ? null : currentUser.getId());
            }

            throw ex;
        }
    }

    @Override
    public Map<String, Object> getFileMeta(Long fileId, User currentUser) {
        return buildFilePayload(getActiveFile(fileId));
    }

    @Override
    public boolean bindFiles(FileRelationBindDTO bindDTO, User currentUser) {
        if (bindDTO == null || !StringUtils.hasText(bindDTO.getBusinessType())
                || bindDTO.getBusinessId() == null || bindDTO.getFileIds() == null || bindDTO.getFileIds().isEmpty()) {
            throw new RuntimeException("Invalid file relation payload");
        }

        List<Long> fileIds = bindDTO.getFileIds();
        for (Long fileId : fileIds) {
            FileObject fileObject = getActiveFile(fileId);
            if (fileObject == null) {
                throw new RuntimeException("File does not exist");
            }

            QueryWrapper<BusinessFileRelation> existsWrapper = new QueryWrapper<>();
            existsWrapper.eq("business_type", bindDTO.getBusinessType().trim())
                    .eq("business_id", bindDTO.getBusinessId())
                    .eq("file_id", fileId)
                    .eq("deleted", 0)
                    .last("LIMIT 1");
            if (businessFileRelationMapper.selectOne(existsWrapper) != null) {
                continue;
            }

            BusinessFileRelation relation = new BusinessFileRelation();
            relation.setBusinessType(bindDTO.getBusinessType().trim());
            relation.setBusinessId(bindDTO.getBusinessId());
            relation.setFileId(fileId);
            relation.setCreatedBy(currentUser == null ? null : currentUser.getId());
            businessFileRelationMapper.insert(relation);
        }
        return true;
    }

    @Override
    public ResponseEntity<Resource> previewFile(Long fileId, String token) {
        return buildFileResponse(fileId, token, false);
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long fileId, String token) {
        return buildFileResponse(fileId, token, true);
    }

    @Override
    public FileObject getActiveFile(Long fileId) {
        if (fileId == null) {
            throw new RuntimeException("File id is required");
        }
        FileObject fileObject = fileObjectMapper.selectById(fileId);
        if (fileObject == null || !Objects.equals(fileObject.getDeleted(), 0)) {
            throw new RuntimeException("File does not exist");
        }
        return fileObject;
    }

    private ResponseEntity<Resource> buildFileResponse(Long fileId, String token, boolean attachment) {
        try {
            FileObject fileObject = getActiveFile(fileId);
            if (!isPublicDisplayFile(fileObject.getId()) && !hasFileAccess(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Path resolvedPath = fileStorageService.resolveProtectedPath(fileObject.getStoragePath());
            if (resolvedPath == null || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(resolvedPath.toUri());
            String contentType = Files.probeContentType(resolvedPath);
            MediaType mediaType = StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
            String disposition = attachment ? "attachment" : "inline";
            String fileName = StringUtils.hasText(fileObject.getOriginalName()) ? fileObject.getOriginalName() : fileObject.getFileName();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename*=UTF-8''" + UriUtils.encode(fileName, StandardCharsets.UTF_8))
                    .body(resource);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> buildFilePayload(FileObject fileObject) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", fileObject.getId());
        payload.put("fileId", fileObject.getId());
        payload.put("fileName", fileObject.getOriginalName());
        payload.put("originalName", fileObject.getOriginalName());
        payload.put("storedName", fileObject.getFileName());
        payload.put("contentType", fileObject.getContentType());
        payload.put("fileSize", fileObject.getFileSize());
        payload.put("storagePath", fileObject.getStoragePath());
        payload.put("md5", fileObject.getMd5());
        payload.put("uploadedBy", fileObject.getUploadedBy());
        payload.put("uploadedAt", fileObject.getUploadedAt());
        payload.put("url", buildFileReference(fileObject.getId()));
        payload.put("path", buildFileReference(fileObject.getId()));
        payload.put("previewUrl", "/api/files/" + fileObject.getId() + "/preview");
        payload.put("downloadUrl", "/api/files/" + fileObject.getId() + "/download");
        return payload;
    }

    private Map<String, Object> buildLegacyPayload(String storedName, String originalName, String contentType,
                                                   Long fileSize, String storagePath, String md5, Long uploadedBy) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", null);
        payload.put("fileId", null);
        payload.put("fileName", StringUtils.hasText(originalName) ? originalName : storedName);
        payload.put("originalName", StringUtils.hasText(originalName) ? originalName : storedName);
        payload.put("storedName", storedName);
        payload.put("contentType", trimToNull(contentType));
        payload.put("fileSize", fileSize);
        payload.put("storagePath", storagePath);
        payload.put("md5", md5);
        payload.put("uploadedBy", uploadedBy);
        payload.put("uploadedAt", null);
        payload.put("url", storagePath);
        payload.put("path", storagePath);
        payload.put("previewUrl", "/api/file/view?path=" + UriUtils.encode(storagePath, StandardCharsets.UTF_8));
        payload.put("downloadUrl", null);
        payload.put("legacyStorage", true);
        return payload;
    }

    private FileObject findExistingActiveFile(String md5, String storagePath) {
        QueryWrapper<FileObject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        if (StringUtils.hasText(md5)) {
            queryWrapper.and(wrapper -> wrapper.eq("md5", md5).or().eq("storage_path", storagePath));
        } else {
            queryWrapper.eq("storage_path", storagePath);
        }
        queryWrapper.orderByDesc("id").last("LIMIT 1");
        return fileObjectMapper.selectOne(queryWrapper);
    }

    private boolean supportsLegacyReference(String scene) {
        String normalizedScene = StringUtils.hasText(scene) ? scene.trim().toLowerCase(Locale.ROOT) : "";
        return "resume".equals(normalizedScene) || "avatar".equals(normalizedScene) || "image".equals(normalizedScene);
    }

    private String buildFileReference(Long fileId) {
        return "file-id:" + fileId;
    }

    private boolean isPublicDisplayFile(Long fileId) {
        if (fileId == null) {
            return false;
        }
        String reference = buildFileReference(fileId);
        Integer labReferences = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_lab WHERE deleted = 0 AND (logo_url = ? OR cover_image_url = ?)",
                Integer.class,
                reference,
                reference
        );
        if (labReferences != null && labReferences > 0) {
            return true;
        }

        Integer graduateReferences = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_outstanding_graduate WHERE deleted = 0 AND (avatar_url = ? OR cover_image_url = ?)",
                Integer.class,
                reference,
                reference
        );
        return graduateReferences != null && graduateReferences > 0;
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
            case "image":
                return IMAGE_EXTENSIONS;
            case "logo":
                return LOGO_EXTENSIONS;
            case "attachment":
            case "attendance":
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
