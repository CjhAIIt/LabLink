package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.recruitment.entity.LabSpaceFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LabSpaceFolderMapper extends BaseMapper<LabSpaceFolder> {

    @Select({
            "SELECT",
            "  id, lab_id AS labId, parent_id AS parentId, folder_name AS folderName, category,",
            "  sort_order AS sortOrder, access_scope AS accessScope, archived, created_by AS createdBy,",
            "  create_time AS createTime, update_time AS updateTime",
            "FROM t_lab_space_folder",
            "WHERE deleted = 0 AND lab_id = #{labId}",
            "ORDER BY sort_order ASC, create_time ASC, id ASC"
    })
    List<Map<String, Object>> selectFolderList(@Param("labId") Long labId);
}
