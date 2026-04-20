package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.LabSpaceFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LabSpaceFileMapper extends BaseMapper<LabSpaceFile> {

    @Select({
            "<script>",
            "SELECT",
            "  f.id, f.lab_id AS labId, f.folder_id AS folderId, f.file_name AS fileName,",
            "  f.file_url AS fileUrl, f.file_size AS fileSize, f.file_type AS fileType,",
            "  f.archive_flag AS archiveFlag, f.access_scope AS accessScope, f.version_no AS versionNo,",
            "  f.upload_user_id AS uploadUserId, f.create_time AS createTime,",
            "  sf.folder_name AS folderName, u.real_name AS uploadUserName",
            "FROM t_lab_space_file f",
            "LEFT JOIN t_lab_space_folder sf ON sf.id = f.folder_id AND sf.deleted = 0",
            "LEFT JOIN t_user u ON u.id = f.upload_user_id AND u.deleted = 0",
            "WHERE f.deleted = 0 AND f.lab_id = #{labId}",
            "<if test='folderId != null'>",
            "  AND f.folder_id = #{folderId}",
            "</if>",
            "<if test='archiveFlag != null'>",
            "  AND f.archive_flag = #{archiveFlag}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (f.file_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR sf.folder_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY f.create_time DESC, f.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectFilePage(Page<Map<String, Object>> page,
                                             @Param("labId") Long labId,
                                             @Param("folderId") Long folderId,
                                             @Param("archiveFlag") Integer archiveFlag,
                                             @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT",
            "  f.id, f.file_name AS fileName, f.file_url AS fileUrl, f.file_size AS fileSize,",
            "  f.file_type AS fileType, f.archive_flag AS archiveFlag, f.create_time AS createTime,",
            "  sf.folder_name AS folderName, u.real_name AS uploadUserName",
            "FROM t_lab_space_file f",
            "LEFT JOIN t_lab_space_folder sf ON sf.id = f.folder_id AND sf.deleted = 0",
            "LEFT JOIN t_user u ON u.id = f.upload_user_id AND u.deleted = 0",
            "WHERE f.deleted = 0 AND f.lab_id = #{labId}",
            "ORDER BY f.create_time DESC, f.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> selectRecentFiles(@Param("labId") Long labId,
                                                @Param("limit") Integer limit);
}
