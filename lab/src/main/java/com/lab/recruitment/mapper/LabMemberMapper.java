package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.LabMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LabMemberMapper extends BaseMapper<LabMember> {

    @Select({
            "<script>",
            "SELECT",
            "  m.id, m.lab_id AS labId, m.user_id AS userId, m.member_role AS memberRole,",
            "  m.join_date AS joinDate, m.quit_date AS quitDate, m.status, m.remark,",
            "  l.lab_name AS labName, u.real_name AS realName, u.student_id AS studentId,",
            "  u.major, u.grade, u.phone, u.email",
            "FROM t_lab_member m",
            "LEFT JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0",
            "LEFT JOIN t_user u ON u.id = m.user_id AND u.deleted = 0",
            "WHERE m.deleted = 0",
            "<if test='labId != null'>",
            "  AND m.lab_id = #{labId}",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND m.status = #{status}",
            "</if>",
            "<if test='memberRole != null and memberRole != \"\"'>",
            "  AND m.member_role = #{memberRole}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (u.real_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.student_id LIKE CONCAT('%', #{keyword}, '%')",
            "       OR l.lab_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY CASE m.status WHEN 'active' THEN 0 ELSE 1 END,",
            "         CASE m.member_role WHEN 'lab_leader' THEN 0 ELSE 1 END,",
            "         m.join_date ASC, m.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectMemberPage(Page<Map<String, Object>> page,
                                               @Param("labId") Long labId,
                                               @Param("status") String status,
                                               @Param("memberRole") String memberRole,
                                               @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT",
            "  m.id, m.lab_id AS labId, m.user_id AS userId, m.member_role AS memberRole,",
            "  m.join_date AS joinDate, m.status, u.username, u.real_name AS realName, u.student_id AS studentId,",
            "  u.major, u.grade",
            "FROM t_lab_member m",
            "LEFT JOIN t_user u ON u.id = m.user_id AND u.deleted = 0",
            "WHERE m.deleted = 0 AND m.lab_id = #{labId} AND m.status = 'active'",
            "ORDER BY CASE m.member_role WHEN 'lab_leader' THEN 0 ELSE 1 END, m.join_date ASC, m.id DESC",
            "</script>"
    })
    List<Map<String, Object>> selectActiveMembersByLabId(@Param("labId") Long labId);
}
