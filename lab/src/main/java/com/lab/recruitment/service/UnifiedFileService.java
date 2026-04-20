package com.lab.recruitment.service;

import com.lab.recruitment.dto.FileRelationBindDTO;
import com.lab.recruitment.entity.FileObject;
import com.lab.recruitment.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UnifiedFileService {

    Map<String, Object> uploadFile(MultipartFile file, String scene, User currentUser);

    Map<String, Object> getFileMeta(Long fileId, User currentUser);

    boolean bindFiles(FileRelationBindDTO bindDTO, User currentUser);

    ResponseEntity<Resource> previewFile(Long fileId, String token);

    ResponseEntity<Resource> downloadFile(Long fileId, String token);

    FileObject getActiveFile(Long fileId);
}
