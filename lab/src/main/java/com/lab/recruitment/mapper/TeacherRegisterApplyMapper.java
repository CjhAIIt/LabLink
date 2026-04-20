package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.TeacherRegisterApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface TeacherRegisterApplyMapper extends BaseMapper<TeacherRegisterApply> {

    @Select({
            "<script>",
            "SELECT",
            "  a.id, a.teacher_no AS teacherNo, a.real_name AS realName, a.college_id AS collegeId,",
            "  a.title, a.phone, a.email, a.apply_reason AS applyReason, a.status,",
            "  a.college_audit_by AS collegeAuditBy, a.college_audit_time AS collegeAuditTime,",
            "  a.college_audit_comment AS collegeAuditComment, a.school_audit_by AS schoolAuditBy,",
            "  a.school_audit_time AS schoolAuditTime, a.school_audit_comment AS schoolAuditComment,",
            "  a.generated_user_id AS generatedUserId, a.create_time AS createTime,",
            "  c.college_name AS collegeName, u.real_name AS generatedUserName",
            "FROM t_teacher_register_apply a",
            "LEFT JOIN t_college c ON c.id = a.college_id AND c.deleted = 0",
            "LEFT JOIN t_user u ON u.id = a.generated_user_id AND u.deleted = 0",
            "WHERE a.deleted = 0",
            "<if test='collegeId != null'>",
            "  AND a.college_id = #{collegeId}",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND a.status = #{status}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (a.teacher_no LIKE CONCAT('%', #{keyword}, '%')",
            "       OR a.real_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR a.email LIKE CONCAT('%', #{keyword}, '%')",
            "       OR c.college_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY a.create_time DESC, a.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectApplyPage(Page<Map<String, Object>> page,
                                              @Param("collegeId") Long collegeId,
                                              @Param("status") String status,
                                              @Param("keyword") String keyword);
}
