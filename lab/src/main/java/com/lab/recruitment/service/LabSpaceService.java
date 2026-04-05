package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.LabSpaceFolder;
import com.lab.recruitment.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface LabSpaceService {

    List<Map<String, Object>> getFolderTree(Long requestedLabId, User currentUser);

    LabSpaceFolder saveFolder(LabSpaceFolder folder, User currentUser);

    Page<Map<String, Object>> getFilePage(Integer pageNum, Integer pageSize, Long requestedLabId, Long folderId,
                                          Integer archiveFlag, String keyword, User currentUser);

    Map<String, Object> uploadFile(Long requestedLabId, Long folderId, Integer archiveFlag, String accessScope,
                                   MultipartFile file, User currentUser);

    boolean updateArchiveFlag(Long fileId, Integer archiveFlag, User currentUser);

    List<Map<String, Object>> getRecentFiles(Long requestedLabId, Integer limit, User currentUser);

    void initializeDefaultFolders(Long labId, Long createdBy);
}
