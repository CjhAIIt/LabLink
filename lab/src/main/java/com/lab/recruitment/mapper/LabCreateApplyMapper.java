package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.LabCreateApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface LabCreateApplyMapper extends BaseMapper<LabCreateApply> {

    @Select({
            "<script>",
            "SELECT",
            "  a.id, a.applicant_user_id AS applicantUserId, a.college_id AS collegeId, a.lab_name AS labName,",
            "  a.teacher_name AS teacherName, a.location, a.contact_email AS contactEmail,",
            "  a.research_direction AS researchDirection, a.apply_reason AS applyReason, a.status,",
            "  a.college_audit_by AS collegeAuditBy, a.college_audit_time AS collegeAuditTime,",
            "  a.college_audit_comment AS collegeAuditComment, a.school_audit_by AS schoolAuditBy,",
            "  a.school_audit_time AS schoolAuditTime, a.school_audit_comment AS schoolAuditComment,",
            "  a.generated_lab_id AS generatedLabId, a.create_time AS createTime,",
            "  u.real_name AS applicantName, c.college_name AS collegeName",
            "FROM t_lab_create_apply a",
            "LEFT JOIN t_user u ON u.id = a.applicant_user_id AND u.deleted = 0",
            "LEFT JOIN t_college c ON c.id = a.college_id AND c.deleted = 0",
            "WHERE a.deleted = 0",
            "<if test='applicantUserId != null'>",
            "  AND a.applicant_user_id = #{applicantUserId}",
            "</if>",
            "<if test='collegeId != null'>",
            "  AND a.college_id = #{collegeId}",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND a.status = #{status}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (a.lab_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR a.teacher_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR c.college_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY a.create_time DESC, a.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectApplyPage(Page<Map<String, Object>> page,
                                              @Param("applicantUserId") Long applicantUserId,
                                              @Param("collegeId") Long collegeId,
                                              @Param("status") String status,
                                              @Param("keyword") String keyword);
}
