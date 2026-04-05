package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.entity.LabSpaceFile;
import com.lab.recruitment.entity.LabSpaceFolder;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.LabSpaceFileMapper;
import com.lab.recruitment.mapper.LabSpaceFolderMapper;
import com.lab.recruitment.service.LabSpaceService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LabSpaceServiceImpl implements LabSpaceService {

    private static final int DEFAULT_FOLDER_SORT_STEP = 10;

    @Autowired
    private LabSpaceFolderMapper labSpaceFolderMapper;

    @Autowired
    private LabSpaceFileMapper labSpaceFileMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public List<Map<String, Object>> getFolderTree(Long requestedLabId, User currentUser) {
        Long labId = resolveLabScope(currentUser, requestedLabId);
        initializeDefaultFolders(labId, null);
        List<Map<String, Object>> folders = labSpaceFolderMapper.selectFolderList(labId);
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> folder : folders) {
            Map<String, Object> node = new LinkedHashMap<>(folder);
            node.put("children", new ArrayList<Map<String, Object>>());
            Long id = toLong(node.get("id"));
            if (id != null) {
                nodeMap.put(id, node);
            }
        }
        for (Map<String, Object> node : nodeMap.values()) {
            Long parentId = toLong(node.get("parentId"));
            if (parentId == null || parentId <= 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
            children.add(node);
        }
        return roots;
    }

    @Override
    @Transactional
    public LabSpaceFolder saveFolder(LabSpaceFolder folder, User currentUser) {
        if (folder == null) {
            throw new RuntimeException("Folder data is required");
        }
        Long labId = resolveLabScope(currentUser, folder.getLabId());
        assertManagePermission(currentUser, labId);

        if (!StringUtils.hasText(folder.getFolderName())) {
            throw new RuntimeException("Folder name is required");
        }

        if (folder.getParentId() != null && folder.getParentId() > 0) {
            LabSpaceFolder parent = labSpaceFolderMapper.selectById(folder.getParentId());
            if (parent == null || parent.getDeleted() != null && parent.getDeleted() == 1) {
                throw new RuntimeException("Parent folder does not exist");
            }
            if (!labId.equals(parent.getLabId())) {
                throw new RuntimeException("Parent folder does not belong to current lab");
            }
        }

        if (folder.getId() == null) {
            folder.setLabId(labId);
            folder.setSortOrder(folder.getSortOrder() == null ? nextSortOrder(labId, folder.getParentId()) : folder.getSortOrder());
            folder.setAccessScope(normalizeAccessScope(folder.getAccessScope()));
            folder.setArchived(folder.getArchived() == null ? 0 : folder.getArchived());
            folder.setCreatedBy(currentUser.getId());
            labSpaceFolderMapper.insert(folder);
            syncFolderToAllLabs(folder, currentUser);
            return folder;
        }

        LabSpaceFolder existing = labSpaceFolderMapper.selectById(folder.getId());
        if (existing == null) {
            throw new RuntimeException("Folder does not exist");
        }
        if (!labId.equals(existing.getLabId())) {
            throw new RuntimeException("No permission to modify this folder");
        }
        existing.setFolderName(folder.getFolderName().trim());
        if (folder.getCategory() != null) {
            existing.setCategory(folder.getCategory().trim());
        }
        if (folder.getParentId() != null) {
            existing.setParentId(folder.getParentId());
        }
        if (folder.getSortOrder() != null) {
            existing.setSortOrder(folder.getSortOrder());
        }
        if (folder.getAccessScope() != null) {
            existing.setAccessScope(normalizeAccessScope(folder.getAccessScope()));
        }
        if (folder.getArchived() != null) {
            existing.setArchived(folder.getArchived());
        }
        labSpaceFolderMapper.updateById(existing);
        return existing;
    }

    @Override
    public Page<Map<String, Object>> getFilePage(Integer pageNum, Integer pageSize, Long requestedLabId, Long folderId,
                                                 Integer archiveFlag, String keyword, User currentUser) {
        Long labId = resolveLabScope(currentUser, requestedLabId);
        if (folderId != null) {
            assertFolderScope(labId, folderId);
        }
        return labSpaceFileMapper.selectFilePage(new Page<>(pageNum, pageSize), labId, folderId,
                archiveFlag, trimToNull(keyword));
    }

    @Override
    @Transactional
    public Map<String, Object> uploadFile(Long requestedLabId, Long folderId, Integer archiveFlag, String accessScope,
                                          MultipartFile file, User currentUser) {
        Long labId = resolveLabScope(currentUser, requestedLabId);
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        assertFolderScope(labId, folderId);

        String originalFilename = trimToNull(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFilename)) {
            throw new RuntimeException("Original file name is missing");
        }

        String extension = getExtension(originalFilename);
        LocalDate today = LocalDate.now();
        String newFilename = UUID.randomUUID() + extension;
        Path targetDir = fileStorageService.resolveTargetDirectory(today);
        Path targetFile = targetDir.resolve(newFilename);

        try {
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }

        LabSpaceFile labSpaceFile = new LabSpaceFile();
        labSpaceFile.setLabId(labId);
        labSpaceFile.setFolderId(folderId);
        labSpaceFile.setFileName(originalFilename);
        labSpaceFile.setFileUrl(fileStorageService.buildPublicUrl(today, newFilename));
        labSpaceFile.setFileSize(file.getSize());
        labSpaceFile.setFileType(resolveFileType(extension, file.getContentType()));
        labSpaceFile.setArchiveFlag(archiveFlag == null ? 0 : archiveFlag);
        labSpaceFile.setAccessScope(normalizeAccessScope(accessScope));
        labSpaceFile.setVersionNo(1);
        labSpaceFile.setUploadUserId(currentUser.getId());
        labSpaceFileMapper.insert(labSpaceFile);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", labSpaceFile.getId());
        result.put("fileName", labSpaceFile.getFileName());
        result.put("fileUrl", labSpaceFile.getFileUrl());
        result.put("fileSize", labSpaceFile.getFileSize());
        result.put("fileType", labSpaceFile.getFileType());
        result.put("archiveFlag", labSpaceFile.getArchiveFlag());
        return result;
    }

    @Override
    @Transactional
    public boolean updateArchiveFlag(Long fileId, Integer archiveFlag, User currentUser) {
        if (fileId == null) {
            throw new RuntimeException("File id is required");
        }
        LabSpaceFile file = labSpaceFileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("File does not exist");
        }
        Long labId = resolveLabScope(currentUser, file.getLabId());
        assertManagePermission(currentUser, labId);
        file.setArchiveFlag(archiveFlag == null ? 0 : archiveFlag);
        return labSpaceFileMapper.updateById(file) > 0;
    }

    @Override
    public List<Map<String, Object>> getRecentFiles(Long requestedLabId, Integer limit, User currentUser) {
        Long labId = resolveLabScope(currentUser, requestedLabId);
        return labSpaceFileMapper.selectRecentFiles(labId, Math.max(limit == null ? 5 : limit, 1));
    }

    @Override
    @Transactional
    public void initializeDefaultFolders(Long labId, Long createdBy) {
        if (labId == null) {
            return;
        }

        List<String[]> defaults = List.of(
                new String[]{"基础档案", "profile"},
                new String[]{"招新归档", "recruit"},
                new String[]{"成员档案", "member"},
                new String[]{"考勤资料", "attendance"},
                new String[]{"项目文档", "project"},
                new String[]{"会议与活动", "meeting"},
                new String[]{"成果档案", "achievement"},
                new String[]{"模板与规范", "template"}
        );

        QueryWrapper<LabSpaceFolder> existingWrapper = new QueryWrapper<>();
        existingWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("parent_id", 0);
        List<LabSpaceFolder> existing = labSpaceFolderMapper.selectList(existingWrapper);

        int sortOrder = DEFAULT_FOLDER_SORT_STEP;
        for (String[] item : defaults) {
            boolean present = existing.stream().anyMatch(folder ->
                    item[0].equals(folder.getFolderName())
                            && ((folder.getCategory() == null && item[1] == null) || (folder.getCategory() != null && folder.getCategory().equals(item[1]))));
            if (present) {
                sortOrder += DEFAULT_FOLDER_SORT_STEP;
                continue;
            }
            LabSpaceFolder folder = new LabSpaceFolder();
            folder.setLabId(labId);
            folder.setParentId(0L);
            folder.setFolderName(item[0]);
            folder.setCategory(item[1]);
            folder.setSortOrder(sortOrder);
            folder.setAccessScope("lab");
            folder.setArchived(0);
            folder.setCreatedBy(createdBy);
            labSpaceFolderMapper.insert(folder);
            sortOrder += DEFAULT_FOLDER_SORT_STEP;
        }
    }

    private Long resolveLabScope(User currentUser, Long requestedLabId) {
        if (currentUser == null) {
            throw new RuntimeException("Current user is required");
        }
        if (!currentUserAccessor.isAdmin(currentUser) && !currentUserAccessor.isTeacherIdentity(currentUser)) {
            if (currentUser.getLabId() == null) {
                throw new RuntimeException("Current account has not joined any lab");
            }
            if (requestedLabId != null && !currentUser.getLabId().equals(requestedLabId)) {
                throw new RuntimeException("No permission to access another lab");
            }
            return currentUser.getLabId();
        }
        return currentUserAccessor.resolveLabScope(currentUser, requestedLabId);
    }

    private void assertManagePermission(User currentUser, Long labId) {
        if (currentUserAccessor.isSuperAdmin(currentUser) || currentUserAccessor.isCollegeManager(currentUser)) {
            currentUserAccessor.assertLabScope(currentUser, labId);
            return;
        }
        if (!currentUserAccessor.isLabManager(currentUser)) {
            throw new RuntimeException("Only lab managers can modify lab space");
        }
        currentUserAccessor.assertLabScope(currentUser, labId);
    }

    private void syncFolderToAllLabs(LabSpaceFolder source, User currentUser) {
        if (source == null || source.getLabId() == null || source.getId() == null) {
            return;
        }
        Long parentId = source.getParentId();
        if (parentId != null && parentId > 0) {
            return;
        }
        QueryWrapper<Lab> labQuery = new QueryWrapper<>();
        labQuery.select("id").eq("deleted", 0);
        List<Lab> labs = labMapper.selectList(labQuery);
        if (labs == null || labs.isEmpty()) {
            return;
        }
        for (Lab lab : labs) {
            if (lab == null || lab.getId() == null) {
                continue;
            }
            Long targetLabId = lab.getId();
            if (targetLabId.equals(source.getLabId())) {
                continue;
            }
            initializeDefaultFolders(targetLabId, null);
            QueryWrapper<LabSpaceFolder> existsWrapper = new QueryWrapper<>();
            existsWrapper.eq("lab_id", targetLabId)
                    .eq("deleted", 0)
                    .eq("parent_id", 0)
                    .eq("folder_name", source.getFolderName());
            if (StringUtils.hasText(source.getCategory())) {
                existsWrapper.eq("category", source.getCategory());
            } else {
                existsWrapper.isNull("category");
            }
            LabSpaceFolder exists = labSpaceFolderMapper.selectOne(existsWrapper);
            if (exists != null) {
                continue;
            }
            LabSpaceFolder cloned = new LabSpaceFolder();
            cloned.setLabId(targetLabId);
            cloned.setParentId(0L);
            cloned.setFolderName(source.getFolderName());
            cloned.setCategory(source.getCategory());
            cloned.setSortOrder(source.getSortOrder() == null ? nextSortOrder(targetLabId, 0L) : source.getSortOrder());
            cloned.setAccessScope(normalizeAccessScope(source.getAccessScope()));
            cloned.setArchived(source.getArchived() == null ? 0 : source.getArchived());
            cloned.setCreatedBy(currentUser == null ? null : currentUser.getId());
            labSpaceFolderMapper.insert(cloned);
        }
    }

    private void assertFolderScope(Long labId, Long folderId) {
        if (folderId == null) {
            throw new RuntimeException("Folder id is required");
        }
        LabSpaceFolder folder = labSpaceFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("Folder does not exist");
        }
        if (!labId.equals(folder.getLabId())) {
            throw new RuntimeException("Folder does not belong to current lab");
        }
    }

    private Integer nextSortOrder(Long labId, Long parentId) {
        QueryWrapper<LabSpaceFolder> wrapper = new QueryWrapper<>();
        wrapper.eq("lab_id", labId).eq("deleted", 0);
        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }
        wrapper.orderByDesc("sort_order").last("LIMIT 1");
        LabSpaceFolder latest = labSpaceFolderMapper.selectOne(wrapper);
        return latest == null || latest.getSortOrder() == null
                ? DEFAULT_FOLDER_SORT_STEP
                : latest.getSortOrder() + DEFAULT_FOLDER_SORT_STEP;
    }

    private String normalizeAccessScope(String accessScope) {
        String normalized = trimToNull(accessScope);
        if (!StringUtils.hasText(normalized)) {
            return "lab";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String resolveFileType(String extension, String contentType) {
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        if (!StringUtils.hasText(extension)) {
            return "application/octet-stream";
        }
        return extension.substring(1).toLowerCase(Locale.ROOT);
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
