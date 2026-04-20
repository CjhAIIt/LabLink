package com.lab.recruitment.controller;

import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.entity.AttendancePhoto;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AttendancePhotoMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.service.UserAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@RestController
@RequestMapping("/attendance-workflow/photos")
public class AttendancePhotoAccessController {

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private AttendancePhotoMapper attendancePhotoMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/{photoId}/view")
    public ResponseEntity<Resource> viewAttendancePhoto(@PathVariable Long photoId) {
        User currentUser = currentUserAccessor.getCurrentUser();

        AttendancePhoto photo = attendancePhotoMapper.selectById(photoId);
        if (photo == null || !Objects.equals(photo.getDeleted(), 0)) {
            return ResponseEntity.notFound().build();
        }

        if (!hasPhotoAccess(photo, currentUser)) {
            return ResponseEntity.status(403).build();
        }

        Path resolvedPath = resolvePhotoPath(photo.getPhotoUrl());
        if (resolvedPath == null || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
            return ResponseEntity.notFound().build();
        }

        try {
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

    private boolean hasPhotoAccess(AttendancePhoto photo, User currentUser) {
        if (photo == null || currentUser == null) {
            return false;
        }
        if (photo.getUploaderId() != null && photo.getUploaderId().equals(currentUser.getId())) {
            return true;
        }
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return true;
        }
        Lab lab = photo.getLabId() == null ? null : labMapper.selectById(photo.getLabId());
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            return false;
        }
        if (currentUserAccessor.isCollegeManager(currentUser)) {
            Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
            return managedCollegeId != null && managedCollegeId.equals(lab.getCollegeId());
        }
        Long managedLabId = userAccessService.resolveManagedLabId(currentUser);
        return managedLabId != null && managedLabId.equals(photo.getLabId());
    }

    private Path resolvePhotoPath(String photoUrl) {
        if (!StringUtils.hasText(photoUrl)) {
            return null;
        }
        Path protectedPath = fileStorageService.resolveProtectedPath(photoUrl);
        if (protectedPath != null) {
            return protectedPath;
        }
        return fileStorageService.resolvePublicPath(photoUrl);
    }
}
