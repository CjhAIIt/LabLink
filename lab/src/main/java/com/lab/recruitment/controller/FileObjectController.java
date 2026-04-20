package com.lab.recruitment.controller;

import com.lab.recruitment.dto.FileRelationBindDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UnifiedFileService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileObjectController {

    @Autowired
    private UnifiedFileService unifiedFileService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                              @RequestParam(value = "scene", required = false) String scene) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(unifiedFileService.uploadFile(file, scene, currentUser));
    }

    @GetMapping("/{fileId}/meta")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> meta(@PathVariable Long fileId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(unifiedFileService.getFileMeta(fileId, currentUser));
    }

    @PostMapping("/relations")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> bindRelations(@Validated @org.springframework.web.bind.annotation.RequestBody FileRelationBindDTO bindDTO) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(unifiedFileService.bindFiles(bindDTO, currentUser));
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long fileId,
                                            @RequestParam(value = "token", required = false) String token) {
        return unifiedFileService.previewFile(fileId, token);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                             @RequestParam(value = "token", required = false) String token) {
        return unifiedFileService.downloadFile(fileId, token);
    }
}
