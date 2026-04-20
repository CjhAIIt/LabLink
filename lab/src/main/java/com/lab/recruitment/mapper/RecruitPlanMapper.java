package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.RecruitPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecruitPlanMapper extends BaseMapper<RecruitPlan> {

    @Select({
            "<script>",
            "SELECT",
            "  rp.id, rp.lab_id AS labId, rp.title, rp.start_time AS startTime, rp.end_time AS endTime,",
            "  rp.quota, rp.requirement, rp.status, rp.created_by AS createdBy, rp.create_time AS createTime,",
            "  l.lab_name AS labName, l.location, c.college_name AS collegeName",
            "FROM t_recruit_plan rp",
            "LEFT JOIN t_lab l ON l.id = rp.lab_id AND l.deleted = 0",
            "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0",
            "WHERE rp.deleted = 0",
            "<if test='labId != null'>",
            "  AND rp.lab_id = #{labId}",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND rp.status = #{status}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (rp.title LIKE CONCAT('%', #{keyword}, '%')",
            "       OR l.lab_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR c.college_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY CASE rp.status WHEN 'open' THEN 0 WHEN 'draft' THEN 1 ELSE 2 END, rp.start_time DESC, rp.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectPlanPage(Page<Map<String, Object>> page,
                                             @Param("labId") Long labId,
                                             @Param("status") String status,
                                             @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT",
            "  rp.id, rp.lab_id AS labId, rp.title, rp.start_time AS startTime, rp.end_time AS endTime,",
            "  rp.quota, rp.requirement, rp.status, l.lab_name AS labName, l.location,",
            "  c.college_name AS collegeName, l.require_skill AS requireSkill",
            "FROM t_recruit_plan rp",
            "LEFT JOIN t_lab l ON l.id = rp.lab_id AND l.deleted = 0",
            "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0",
            "WHERE rp.deleted = 0",
            "  AND rp.status = 'open'",
            "  AND (rp.start_time IS NULL OR rp.start_time &lt;= NOW())",
            "  AND (rp.end_time IS NULL OR rp.end_time &gt;= NOW())",
            "<if test='labId != null'>",
            "  AND rp.lab_id = #{labId}",
            "</if>",
            "ORDER BY rp.start_time DESC, rp.id DESC",
            "</script>"
    })
    List<Map<String, Object>> selectActivePlanList(@Param("labId") Long labId);
}
