package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.AiInterviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface AiInterviewRecordMapper extends BaseMapper<AiInterviewRecord> {

    @Select("SELECT COUNT(*) FROM t_ai_interview_record WHERE student_id = #{studentId} AND status IN ('进行中','已完成') AND deleted = 0")
    int countUsedChances(@Param("studentId") Long studentId);

    @Select({
            "<script>",
            "SELECT",
            "  r.id AS id,",
            "  r.student_id AS studentId,",
            "  r.student_id AS studentUserId,",
            "  COALESCE(NULLIF(r.student_name, ''), u.real_name, '') AS studentName,",
            "  u.student_id AS studentNo,",
            "  u.college AS college,",
            "  u.major AS major,",
            "  u.grade AS grade,",
            "  l.id AS labId,",
            "  l.lab_name AS labName,",
            "  r.module_id AS moduleId,",
            "  r.module_name AS moduleName,",
            "  r.attempt_no AS attemptNo,",
            "  r.score AS score,",
            "  r.tags_json AS tagsJson,",
            "  r.summary AS summary,",
            "  r.strengths AS strengths,",
            "  r.weaknesses AS weaknesses,",
            "  r.suggestions AS suggestions,",
            "  r.conversation_json AS conversationJson,",
            "  r.status AS status,",
            "  r.start_time AS startTime,",
            "  r.end_time AS endTime,",
            "  r.create_time AS createTime,",
            "  CASE WHEN r.score IS NOT NULL AND r.score &gt;= 80 THEN 1 ELSE 0 END AS recommendNext",
            "FROM t_ai_interview_record r",
            "LEFT JOIN t_user u ON u.id = r.student_id AND u.deleted = 0",
            "LEFT JOIN (",
            "  SELECT user_id, MIN(lab_id) AS lab_id",
            "  FROM t_lab_member",
            "  WHERE deleted = 0 AND status = 'active'",
            "  GROUP BY user_id",
            ") lm ON lm.user_id = r.student_id",
            "LEFT JOIN t_lab_apply la ON la.id = (",
            "  SELECT la2.id",
            "  FROM t_lab_apply la2",
            "  WHERE la2.student_user_id = r.student_id AND la2.deleted = 0",
            "  ORDER BY la2.create_time DESC, la2.id DESC",
            "  LIMIT 1",
            ")",
            "LEFT JOIN t_lab l ON l.id = COALESCE(lm.lab_id, la.lab_id) AND l.deleted = 0",
            "WHERE r.deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (r.student_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.real_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.student_id LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.college LIKE CONCAT('%', #{keyword}, '%')",
            "       OR l.lab_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='moduleId != null'>",
            "  AND r.module_id = #{moduleId}",
            "</if>",
            "<if test='minScore != null'>",
            "  AND r.score &gt;= #{minScore}",
            "</if>",
            "<if test='maxScore != null'>",
            "  AND r.score &lt;= #{maxScore}",
            "</if>",
            "<if test='startDate != null and startDate != \"\"'>",
            "  AND r.start_time &gt;= #{startDate}",
            "</if>",
            "<if test='endDate != null and endDate != \"\"'>",
            "  AND r.start_time &lt;= #{endDate}",
            "</if>",
            "<if test='labId != null'>",
            "  AND l.id = #{labId}",
            "</if>",
            "<if test='collegeId != null'>",
            "  AND l.college_id = #{collegeId}",
            "</if>",
            "<if test='studentUserId != null'>",
            "  AND r.student_id = #{studentUserId}",
            "</if>",
            "ORDER BY r.create_time DESC, r.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectRecordViewPage(Page<Map<String, Object>> page,
                                                   @Param("keyword") String keyword,
                                                   @Param("moduleId") Long moduleId,
                                                   @Param("minScore") Integer minScore,
                                                   @Param("maxScore") Integer maxScore,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("labId") Long labId,
                                                   @Param("collegeId") Long collegeId,
                                                   @Param("studentUserId") Long studentUserId);
}
