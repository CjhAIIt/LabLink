package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.LabApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LabApplyMapper extends BaseMapper<LabApply> {

    @Select({
            "<script>",
            "SELECT",
            "  a.id, a.lab_id AS labId, a.student_user_id AS studentUserId, a.recruit_plan_id AS recruitPlanId,",
            "  a.apply_reason AS applyReason, a.research_interest AS researchInterest, a.skill_summary AS skillSummary,",
            "  a.status, a.audit_by AS auditBy, a.audit_time AS auditTime, a.audit_comment AS auditComment,",
            "  a.create_time AS createTime, l.lab_name AS labName, rp.title AS planTitle,",
            "  u.real_name AS studentName, u.student_id AS studentId, u.major, u.grade, u.phone, u.email, u.resume AS resume",
            "FROM t_lab_apply a",
            "LEFT JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0",
            "LEFT JOIN t_recruit_plan rp ON rp.id = a.recruit_plan_id AND rp.deleted = 0",
            "LEFT JOIN t_user u ON u.id = a.student_user_id AND u.deleted = 0",
            "WHERE a.deleted = 0",
            "<if test='labId != null'>",
            "  AND a.lab_id = #{labId}",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND a.status = #{status}",
            "</if>",
            "<if test='studentUserId != null'>",
            "  AND a.student_user_id = #{studentUserId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (u.real_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.student_id LIKE CONCAT('%', #{keyword}, '%')",
            "       OR l.lab_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY a.create_time DESC, a.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectApplyPage(Page<Map<String, Object>> page,
                                              @Param("labId") Long labId,
                                              @Param("status") String status,
                                              @Param("studentUserId") Long studentUserId,
                                              @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT",
            "  a.id, a.lab_id AS labId, a.student_user_id AS studentUserId, a.recruit_plan_id AS recruitPlanId,",
            "  a.apply_reason AS applyReason, a.research_interest AS researchInterest, a.skill_summary AS skillSummary,",
            "  a.status, a.audit_by AS auditBy, a.audit_time AS auditTime, a.audit_comment AS auditComment,",
            "  a.create_time AS createTime, l.lab_name AS labName, rp.title AS planTitle",
            "FROM t_lab_apply a",
            "LEFT JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0",
            "LEFT JOIN t_recruit_plan rp ON rp.id = a.recruit_plan_id AND rp.deleted = 0",
            "WHERE a.deleted = 0 AND a.student_user_id = #{studentUserId}",
            "<if test='status != null and status != \"\"'>",
            "  AND a.status = #{status}",
            "</if>",
            "ORDER BY a.create_time DESC, a.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectMyApplyPage(Page<Map<String, Object>> page,
                                                @Param("studentUserId") Long studentUserId,
                                                @Param("status") String status);

    @Select({
            "<script>",
            "SELECT",
            "  a.id, a.status, a.create_time AS createTime, u.real_name AS studentName,",
            "  u.student_id AS studentId, u.major, rp.title AS planTitle",
            "FROM t_lab_apply a",
            "LEFT JOIN t_user u ON u.id = a.student_user_id AND u.deleted = 0",
            "LEFT JOIN t_recruit_plan rp ON rp.id = a.recruit_plan_id AND rp.deleted = 0",
            "WHERE a.deleted = 0",
            "<if test='labId != null'>",
            "  AND a.lab_id = #{labId}",
            "</if>",
            "ORDER BY a.create_time DESC, a.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> selectLatestApplies(@Param("labId") Long labId,
                                                  @Param("limit") int limit);
}
