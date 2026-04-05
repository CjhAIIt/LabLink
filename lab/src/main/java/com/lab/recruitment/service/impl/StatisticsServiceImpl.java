package com.lab.recruitment.service.impl;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.StatisticsService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Override
    public Map<String, Object> getOverview(User currentUser) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return buildSchoolOverview();
        }
        Long collegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
        if (collegeId != null) {
            return buildCollegeOverview(collegeId);
        }
        Long labId = currentUserAccessor.resolveLabScope(currentUser, currentUser.getLabId());
        return buildLabOverview(labId);
    }

    @Override
    public Map<String, Object> getLabStatistics(Long labId, User currentUser) {
        Long scopedLabId = currentUserAccessor.isSuperAdmin(currentUser)
                ? labId
                : currentUserAccessor.resolveLabScope(currentUser, labId);
        if (scopedLabId == null) {
            throw new RuntimeException("Lab id is required");
        }
        return buildLabOverview(scopedLabId);
    }

    private Map<String, Object> buildSchoolOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("scopeType", "school");
        data.put("scopeName", "全校实验室");
        data.put("collegeCount", count("SELECT COUNT(*) FROM t_college WHERE deleted = 0"));
        data.put("labCount", count("SELECT COUNT(*) FROM t_lab WHERE deleted = 0"));
        data.put("openPlanCount", count("SELECT COUNT(*) FROM t_recruit_plan WHERE deleted = 0 AND status = 'open'"));
        data.put("applyCount", count("SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0"));
        data.put("approvedCount", count("SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0 AND status = 'approved'"));
        data.put("memberCount", count("SELECT COUNT(*) FROM t_lab_member WHERE deleted = 0 AND status = 'active'"));
        data.put("formalMemberCount", data.get("memberCount"));
        data.put("teacherCount", count(
                "SELECT COUNT(DISTINCT teacher_name) FROM t_lab WHERE deleted = 0 AND teacher_name IS NOT NULL AND TRIM(teacher_name) <> ''"));
        data.put("noticeCount", count("SELECT COUNT(*) FROM t_notice WHERE deleted = 0"));
        data.put("fileCount", count("SELECT COUNT(*) FROM t_lab_space_file WHERE deleted = 0"));
        data.put("archivedFileCount", count("SELECT COUNT(*) FROM t_lab_space_file WHERE deleted = 0 AND archive_flag = 1"));
        data.put("attendanceRate", calculateAttendanceRate(fetchAttendanceStatusCounter(null)));
        data.put("applyStatus", jdbcTemplate.queryForList(
                "SELECT status AS name, COUNT(*) AS value FROM t_lab_apply WHERE deleted = 0 GROUP BY status"));
        data.put("collegeDistribution", jdbcTemplate.queryForList(
                "SELECT COALESCE(c.college_name, '未分配学院') AS name, COUNT(l.id) AS value " +
                        "FROM t_lab l LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                        "WHERE l.deleted = 0 GROUP BY COALESCE(c.college_name, '未分配学院') ORDER BY value DESC"));
        data.put("hotLabs", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                        "FROM t_lab l LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                        "WHERE l.deleted = 0 GROUP BY l.id, l.lab_name ORDER BY value DESC LIMIT 8"));
        data.put("labMemberDistribution", data.get("hotLabs"));
        data.put("memberTypeDistribution", buildSchoolMemberTypeDistribution());
        data.put("monthlyApplyTrend", completeMonthlyTrend(jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(create_time, '%Y-%m') AS name, COUNT(*) AS value " +
                        "FROM t_lab_apply WHERE deleted = 0 " +
                        "AND create_time >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 5 MONTH) " +
                        "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY name ASC"), 6));
        data.put("monthlyAttendanceTrend", completeMonthlyTrend(queryAttendanceMonthlyTrend(null), 6));
        data.put("teacherGuidanceRanking", jdbcTemplate.queryForList(
                "SELECT teacher_name AS name, COUNT(*) AS value " +
                        "FROM t_lab WHERE deleted = 0 AND teacher_name IS NOT NULL AND TRIM(teacher_name) <> '' " +
                        "GROUP BY teacher_name ORDER BY value DESC, name ASC LIMIT 8"));
        data.put("recentApplies", jdbcTemplate.queryForList(
                "SELECT a.id, a.status, a.create_time AS createTime, u.real_name AS studentName, " +
                        "u.student_id AS studentId, l.lab_name AS labName " +
                        "FROM t_lab_apply a " +
                        "LEFT JOIN t_user u ON u.id = a.student_user_id AND u.deleted = 0 " +
                        "LEFT JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 ORDER BY a.create_time DESC LIMIT 5"));
        data.put("recruitConversionRanking", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, " +
                        "ROUND(CASE WHEN COUNT(a.id) = 0 THEN 0 ELSE " +
                        "SUM(CASE WHEN a.status = 'approved' THEN 1 ELSE 0 END) * 100.0 / COUNT(a.id) END, 2) AS value " +
                        "FROM t_lab l LEFT JOIN t_lab_apply a ON a.lab_id = l.id AND a.deleted = 0 " +
                        "WHERE l.deleted = 0 GROUP BY l.id, l.lab_name ORDER BY value DESC, name ASC LIMIT 8"));
        data.put("activityRanking", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, ROUND(" +
                        "COALESCE(m.member_count, 0) * 8 + " +
                        "COALESCE(f.file_count, 0) * 5 + " +
                        "COALESCE(n.notice_count, 0) * 3 + " +
                        "COALESCE(a.attended_count, 0) * 2, 2) AS value " +
                        "FROM t_lab l " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS member_count FROM t_lab_member " +
                        "           WHERE deleted = 0 AND status = 'active' GROUP BY lab_id) m ON m.lab_id = l.id " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS file_count FROM t_lab_space_file " +
                        "           WHERE deleted = 0 GROUP BY lab_id) f ON f.lab_id = l.id " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS notice_count FROM t_notice " +
                        "           WHERE deleted = 0 GROUP BY lab_id) n ON n.lab_id = l.id " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS attended_count FROM t_lab_attendance " +
                        "           WHERE deleted = 0 AND status IN (1, 2, 5) GROUP BY lab_id) a ON a.lab_id = l.id " +
                        "WHERE l.deleted = 0 ORDER BY value DESC, name ASC LIMIT 8"));
        data.put("collegeComparison", jdbcTemplate.queryForList(
                "SELECT COALESCE(c.college_name, '未分配学院') AS name, ROUND(" +
                        "COUNT(DISTINCT l.id) * 10 + " +
                        "COUNT(DISTINCT m.id) * 2 + " +
                        "SUM(CASE WHEN a.status = 'approved' THEN 6 ELSE 0 END) + " +
                        "COUNT(DISTINCT f.id) * 1.5, 2) AS value " +
                        "FROM t_college c " +
                        "LEFT JOIN t_lab l ON l.college_id = c.id AND l.deleted = 0 " +
                        "LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                        "LEFT JOIN t_lab_apply a ON a.lab_id = l.id AND a.deleted = 0 " +
                        "LEFT JOIN t_lab_space_file f ON f.lab_id = l.id AND f.deleted = 0 " +
                        "WHERE c.deleted = 0 GROUP BY c.id, c.college_name ORDER BY value DESC"));
        List<Map<String, Object>> pendingApprovals = buildSchoolPendingApprovals();
        data.put("pendingApprovals", pendingApprovals);
        data.put("pendingApprovalTotal", sumPendingValues(pendingApprovals));
        return data;
    }

    private Map<String, Object> buildCollegeOverview(Long collegeId) {
        Map<String, Object> data = new HashMap<>();
        data.put("scopeType", "college");
        data.put("scopeName", resolveCollegeName(collegeId));
        data.put("labCount", count("SELECT COUNT(*) FROM t_lab WHERE deleted = 0 AND college_id = ?", collegeId));
        data.put("openPlanCount", count(
                "SELECT COUNT(*) FROM t_recruit_plan rp " +
                        "INNER JOIN t_lab l ON l.id = rp.lab_id AND l.deleted = 0 " +
                        "WHERE rp.deleted = 0 AND rp.status = 'open' AND l.college_id = ?",
                collegeId));
        data.put("applyCount", count(
                "SELECT COUNT(*) FROM t_lab_apply a " +
                        "INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND l.college_id = ?",
                collegeId));
        data.put("approvedCount", count(
                "SELECT COUNT(*) FROM t_lab_apply a " +
                        "INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND a.status = 'approved' AND l.college_id = ?",
                collegeId));
        data.put("memberCount", count(
                "SELECT COUNT(*) FROM t_lab_member m " +
                        "INNER JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0 " +
                        "WHERE m.deleted = 0 AND m.status = 'active' AND l.college_id = ?",
                collegeId));
        data.put("formalMemberCount", data.get("memberCount"));
        data.put("teacherCount", count(
                "SELECT COUNT(DISTINCT teacher_name) FROM t_lab " +
                        "WHERE deleted = 0 AND college_id = ? AND teacher_name IS NOT NULL AND TRIM(teacher_name) <> ''",
                collegeId));
        data.put("noticeCount", count(
                "SELECT COUNT(*) FROM t_notice n " +
                        "WHERE n.deleted = 0 AND (n.college_id = ? OR n.lab_id IN (" +
                        "SELECT id FROM t_lab WHERE deleted = 0 AND college_id = ?))",
                collegeId, collegeId));
        data.put("fileCount", count(
                "SELECT COUNT(*) FROM t_lab_space_file f " +
                        "INNER JOIN t_lab l ON l.id = f.lab_id AND l.deleted = 0 " +
                        "WHERE f.deleted = 0 AND l.college_id = ?",
                collegeId));
        data.put("archivedFileCount", count(
                "SELECT COUNT(*) FROM t_lab_space_file f " +
                        "INNER JOIN t_lab l ON l.id = f.lab_id AND l.deleted = 0 " +
                        "WHERE f.deleted = 0 AND f.archive_flag = 1 AND l.college_id = ?",
                collegeId));
        data.put("attendanceRate", calculateAttendanceRate(fetchAttendanceStatusCounterByCollege(collegeId)));
        data.put("applyStatus", jdbcTemplate.queryForList(
                "SELECT a.status AS name, COUNT(*) AS value FROM t_lab_apply a " +
                        "INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND l.college_id = ? GROUP BY a.status",
                collegeId));
        data.put("hotLabs", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                        "FROM t_lab l LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                        "WHERE l.deleted = 0 AND l.college_id = ? GROUP BY l.id, l.lab_name ORDER BY value DESC LIMIT 8",
                collegeId));
        data.put("labMemberDistribution", data.get("hotLabs"));
        data.put("monthlyApplyTrend", completeMonthlyTrend(jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(a.create_time, '%Y-%m') AS name, COUNT(*) AS value " +
                        "FROM t_lab_apply a INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND l.college_id = ? " +
                        "AND a.create_time >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 5 MONTH) " +
                        "GROUP BY DATE_FORMAT(a.create_time, '%Y-%m') ORDER BY name ASC",
                collegeId), 6));
        data.put("monthlyAttendanceTrend", completeMonthlyTrend(queryAttendanceMonthlyTrendByCollege(collegeId), 6));
        data.put("teacherGuidanceRanking", jdbcTemplate.queryForList(
                "SELECT teacher_name AS name, COUNT(*) AS value " +
                        "FROM t_lab WHERE deleted = 0 AND college_id = ? " +
                        "AND teacher_name IS NOT NULL AND TRIM(teacher_name) <> '' " +
                        "GROUP BY teacher_name ORDER BY value DESC, name ASC LIMIT 8",
                collegeId));
        data.put("recentApplies", jdbcTemplate.queryForList(
                "SELECT a.id, a.status, a.create_time AS createTime, u.real_name AS studentName, " +
                        "u.student_id AS studentId, l.lab_name AS labName " +
                        "FROM t_lab_apply a " +
                        "LEFT JOIN t_user u ON u.id = a.student_user_id AND u.deleted = 0 " +
                        "INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND l.college_id = ? " +
                        "ORDER BY a.create_time DESC LIMIT 5",
                collegeId));
        data.put("recruitConversionRanking", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, " +
                        "ROUND(CASE WHEN COUNT(a.id) = 0 THEN 0 ELSE " +
                        "SUM(CASE WHEN a.status = 'approved' THEN 1 ELSE 0 END) * 100.0 / COUNT(a.id) END, 2) AS value " +
                        "FROM t_lab l LEFT JOIN t_lab_apply a ON a.lab_id = l.id AND a.deleted = 0 " +
                        "WHERE l.deleted = 0 AND l.college_id = ? " +
                        "GROUP BY l.id, l.lab_name ORDER BY value DESC, name ASC LIMIT 8",
                collegeId));
        data.put("activityRanking", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, ROUND(" +
                        "COALESCE(m.member_count, 0) * 8 + " +
                        "COALESCE(f.file_count, 0) * 5 + " +
                        "COALESCE(n.notice_count, 0) * 3 + " +
                        "COALESCE(a.attended_count, 0) * 2, 2) AS value " +
                        "FROM t_lab l " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS member_count FROM t_lab_member " +
                        "           WHERE deleted = 0 AND status = 'active' GROUP BY lab_id) m ON m.lab_id = l.id " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS file_count FROM t_lab_space_file " +
                        "           WHERE deleted = 0 GROUP BY lab_id) f ON f.lab_id = l.id " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS notice_count FROM t_notice " +
                        "           WHERE deleted = 0 GROUP BY lab_id) n ON n.lab_id = l.id " +
                        "LEFT JOIN (SELECT lab_id, COUNT(*) AS attended_count FROM t_lab_attendance " +
                        "           WHERE deleted = 0 AND status IN (1, 2, 5) GROUP BY lab_id) a ON a.lab_id = l.id " +
                        "WHERE l.deleted = 0 AND l.college_id = ? " +
                        "ORDER BY value DESC, name ASC LIMIT 8",
                collegeId));
        List<Map<String, Object>> pendingApprovals = buildCollegePendingApprovals(collegeId);
        data.put("pendingApprovals", pendingApprovals);
        data.put("pendingApprovalTotal", sumPendingValues(pendingApprovals));
        return data;
    }

    private Map<String, Object> buildLabOverview(Long labId) {
        Map<String, Object> data = new HashMap<>();
        data.put("scopeType", "lab");
        data.put("scopeName", resolveLabName(labId));
        data.put("memberCount", count("SELECT COUNT(*) FROM t_lab_member WHERE deleted = 0 AND status = 'active' AND lab_id = ?", labId));
        data.put("planCount", count("SELECT COUNT(*) FROM t_recruit_plan WHERE deleted = 0 AND lab_id = ?", labId));
        data.put("openPlanCount", count("SELECT COUNT(*) FROM t_recruit_plan WHERE deleted = 0 AND lab_id = ? AND status = 'open'", labId));
        data.put("applyCount", count("SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0 AND lab_id = ?", labId));
        data.put("pendingCount", count(
                "SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? AND status IN ('submitted', 'leader_approved')",
                labId));
        data.put("approvedCount", count("SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? AND status = 'approved'", labId));
        data.put("noticeCount", count("SELECT COUNT(*) FROM t_notice WHERE deleted = 0 AND lab_id = ?", labId));
        data.put("fileCount", count("SELECT COUNT(*) FROM t_lab_space_file WHERE deleted = 0 AND lab_id = ?", labId));
        data.put("archivedFileCount", count(
                "SELECT COUNT(*) FROM t_lab_space_file WHERE deleted = 0 AND lab_id = ? AND archive_flag = 1", labId));
        Map<Integer, Long> attendanceCounter = fetchAttendanceStatusCounter(labId);
        data.put("attendanceRate", calculateAttendanceRate(attendanceCounter));
        data.put("attendanceSummary", buildAttendanceSummary(attendanceCounter));
        data.put("applyStatus", jdbcTemplate.queryForList(
                "SELECT status AS name, COUNT(*) AS value FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? GROUP BY status", labId));
        data.put("memberRoles", jdbcTemplate.queryForList(
                "SELECT member_role AS name, COUNT(*) AS value FROM t_lab_member " +
                        "WHERE deleted = 0 AND status = 'active' AND lab_id = ? GROUP BY member_role", labId));
        data.put("monthlyApplyTrend", completeMonthlyTrend(jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(create_time, '%Y-%m') AS name, COUNT(*) AS value " +
                        "FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? " +
                        "AND create_time >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 5 MONTH) " +
                        "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY name ASC", labId), 6));
        data.put("monthlyAttendanceTrend", completeMonthlyTrend(queryAttendanceMonthlyTrend(labId), 6));
        data.put("recentApplies", jdbcTemplate.queryForList(
                "SELECT a.id, a.status, a.create_time AS createTime, u.real_name AS studentName, u.student_id AS studentId " +
                        "FROM t_lab_apply a LEFT JOIN t_user u ON u.id = a.student_user_id AND u.deleted = 0 " +
                        "WHERE a.deleted = 0 AND a.lab_id = ? ORDER BY a.create_time DESC LIMIT 5", labId));
        data.put("recentFiles", jdbcTemplate.queryForList(
                "SELECT file_name AS name, create_time AS createTime, archive_flag AS archiveFlag " +
                        "FROM t_lab_space_file WHERE deleted = 0 AND lab_id = ? ORDER BY create_time DESC LIMIT 5", labId));
        data.put("activityRanking", jdbcTemplate.queryForList(
                "SELECT '文件上传' AS name, COUNT(*) AS value FROM t_lab_space_file WHERE deleted = 0 AND lab_id = ? " +
                        "UNION ALL " +
                        "SELECT '公告发布' AS name, COUNT(*) AS value FROM t_notice WHERE deleted = 0 AND lab_id = ? " +
                        "UNION ALL " +
                        "SELECT '已通过申请' AS name, COUNT(*) AS value FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? AND status = 'approved'",
                labId, labId, labId));
        List<Map<String, Object>> pendingApprovals = buildLabPendingApprovals(labId);
        data.put("pendingApprovals", pendingApprovals);
        data.put("pendingApprovalTotal", sumPendingValues(pendingApprovals));
        return data;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private Map<Integer, Long> fetchAttendanceStatusCounter(Long labId) {
        String sql = labId == null
                ? "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 GROUP BY status"
                : "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND lab_id = ? GROUP BY status";
        List<Map<String, Object>> rows = labId == null
                ? jdbcTemplate.queryForList(sql)
                : jdbcTemplate.queryForList(sql, labId);
        Map<Integer, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer status = row.get("status") instanceof Number ? ((Number) row.get("status")).intValue() : null;
            Long value = row.get("value") instanceof Number ? ((Number) row.get("value")).longValue() : 0L;
            if (status != null) {
                result.put(status, value);
            }
        }
        return result;
    }

    private Map<Integer, Long> fetchAttendanceStatusCounterByCollege(Long collegeId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT a.status, COUNT(*) AS value FROM t_lab_attendance a " +
                        "INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND l.college_id = ? GROUP BY a.status",
                collegeId
        );
        Map<Integer, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer status = row.get("status") instanceof Number ? ((Number) row.get("status")).intValue() : null;
            Long value = row.get("value") instanceof Number ? ((Number) row.get("value")).longValue() : 0L;
            if (status != null) {
                result.put(status, value);
            }
        }
        return result;
    }

    private List<Map<String, Object>> queryAttendanceMonthlyTrend(Long labId) {
        String baseSql =
                "SELECT SUBSTRING(attendance_date, 1, 7) AS name, ROUND(" +
                        "CASE WHEN SUM(CASE WHEN status IN (1, 2, 4, 5, 0) THEN 1 ELSE 0 END) = 0 THEN 0 " +
                        "ELSE SUM(CASE WHEN status IN (1, 2, 5) THEN 1 ELSE 0 END) * 100.0 / " +
                        "SUM(CASE WHEN status IN (1, 2, 4, 5, 0) THEN 1 ELSE 0 END) END, 2) AS value " +
                        "FROM t_lab_attendance WHERE deleted = 0 ";
        if (labId != null) {
            return jdbcTemplate.queryForList(
                    baseSql +
                            "AND lab_id = ? AND attendance_date >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01') " +
                            "GROUP BY SUBSTRING(attendance_date, 1, 7) ORDER BY name ASC",
                    labId
            );
        }
        return jdbcTemplate.queryForList(
                baseSql +
                "AND attendance_date >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01') " +
                        "GROUP BY SUBSTRING(attendance_date, 1, 7) ORDER BY name ASC"
        );
    }

    private List<Map<String, Object>> queryAttendanceMonthlyTrendByCollege(Long collegeId) {
        return jdbcTemplate.queryForList(
                "SELECT SUBSTRING(a.attendance_date, 1, 7) AS name, ROUND(" +
                        "CASE WHEN SUM(CASE WHEN a.status IN (1, 2, 4, 5, 0) THEN 1 ELSE 0 END) = 0 THEN 0 " +
                        "ELSE SUM(CASE WHEN a.status IN (1, 2, 5) THEN 1 ELSE 0 END) * 100.0 / " +
                        "SUM(CASE WHEN a.status IN (1, 2, 4, 5, 0) THEN 1 ELSE 0 END) END, 2) AS value " +
                        "FROM t_lab_attendance a " +
                        "INNER JOIN t_lab l ON l.id = a.lab_id AND l.deleted = 0 " +
                        "WHERE a.deleted = 0 AND l.college_id = ? " +
                        "AND a.attendance_date >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01') " +
                        "GROUP BY SUBSTRING(a.attendance_date, 1, 7) ORDER BY name ASC",
                collegeId
        );
    }

    private List<Map<String, Object>> buildSchoolMemberTypeDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(statItem("普通学生", count(
                "SELECT COUNT(*) FROM t_user WHERE deleted = 0 AND status = 1 AND role = 'student' AND lab_id IS NULL")));
        distribution.add(statItem("实验室成员", count(
                "SELECT COUNT(*) FROM t_lab_member WHERE deleted = 0 AND status = 'active' AND member_role = 'member'")));
        distribution.add(statItem("学生负责人", count(
                "SELECT COUNT(*) FROM t_lab_member WHERE deleted = 0 AND status = 'active' AND member_role = 'lab_leader'")));
        distribution.add(statItem("指导老师", count(
                "SELECT COUNT(DISTINCT teacher_name) FROM t_lab WHERE deleted = 0 AND teacher_name IS NOT NULL AND TRIM(teacher_name) <> ''")));
        return distribution;
    }

    private Map<String, Object> buildAttendanceSummary(Map<Integer, Long> counter) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("presentCount", counter.getOrDefault(1, 0L));
        summary.put("lateCount", counter.getOrDefault(2, 0L));
        summary.put("leaveCount", counter.getOrDefault(3, 0L));
        summary.put("absentCount", counter.getOrDefault(4, 0L));
        summary.put("makeupCount", counter.getOrDefault(5, 0L));
        summary.put("exemptCount", counter.getOrDefault(6, 0L));
        summary.put("attendanceRate", calculateAttendanceRate(counter));
        return summary;
    }

    private double calculateAttendanceRate(Map<Integer, Long> counter) {
        long attended = counter.getOrDefault(1, 0L) + counter.getOrDefault(2, 0L) + counter.getOrDefault(5, 0L);
        long denominator = attended + counter.getOrDefault(4, 0L) + counter.getOrDefault(0, 0L);
        if (denominator <= 0) {
            return 100D;
        }
        return round(attended * 100.0D / denominator);
    }

    private Map<String, Object> statItem(String name, Object value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private Map<String, Object> pendingItem(String label, long value, String description, String route) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("description", description);
        item.put("route", route);
        return item;
    }

    private List<Map<String, Object>> buildSchoolPendingApprovals() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(pendingItem(
                "教师注册待学院初审",
                count("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND status = 'submitted'"),
                "等待学院管理员完成教师注册初审",
                "/admin/teacher-register-applies"));
        items.add(pendingItem(
                "教师注册待学校终审",
                count("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND status = 'college_approved'"),
                "等待学校管理员完成教师注册终审",
                "/admin/teacher-register-applies"));
        items.add(pendingItem(
                "实验室创建待学院初审",
                count("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND status = 'submitted'"),
                "等待学院管理员处理实验室创建申请",
                "/admin/create-applies"));
        items.add(pendingItem(
                "实验室创建待学校终审",
                count("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND status = 'college_approved'"),
                "等待学校管理员完成实验室创建终审",
                "/admin/create-applies"));
        return items;
    }

    private List<Map<String, Object>> buildCollegePendingApprovals(Long collegeId) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(pendingItem(
                "教师注册待初审",
                count("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND college_id = ? AND status = 'submitted'",
                        collegeId),
                "本学院等待初审的教师注册申请",
                "/admin/teacher-register-applies"));
        items.add(pendingItem(
                "实验室创建待初审",
                count("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND college_id = ? AND status = 'submitted'",
                        collegeId),
                "本学院等待初审的实验室创建申请",
                "/admin/create-applies"));
        items.add(pendingItem(
                "教师注册待学校终审",
                count("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0 AND college_id = ? AND status = 'college_approved'",
                        collegeId),
                "已完成初审，等待学校终审的教师注册申请",
                "/admin/teacher-register-applies"));
        items.add(pendingItem(
                "实验室创建待学校终审",
                count("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0 AND college_id = ? AND status = 'college_approved'",
                        collegeId),
                "已完成初审，等待学校终审的实验室创建申请",
                "/admin/create-applies"));
        return items;
    }

    private List<Map<String, Object>> buildLabPendingApprovals(Long labId) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(pendingItem(
                "成员申请待审核",
                count("SELECT COUNT(*) FROM t_lab_apply WHERE deleted = 0 AND lab_id = ? AND status IN ('submitted', 'leader_approved')",
                        labId),
                "等待实验室管理员处理的入组申请",
                "/admin/applications"));
        items.add(pendingItem(
                "退出申请待处理",
                count("SELECT COUNT(*) FROM t_lab_exit_application WHERE deleted = 0 AND status = 0 AND lab_id = ?", labId),
                "等待实验室管理员审核的退出申请",
                "/admin/workspace"));
        items.add(pendingItem(
                "资料待归档",
                count("SELECT COUNT(*) FROM t_lab_space_file WHERE deleted = 0 AND lab_id = ? AND (archive_flag = 0 OR archive_flag IS NULL)",
                        labId),
                "建议尽快完成资料归档与分类",
                "/admin/workspace"));
        return items;
    }

    private long sumPendingValues(List<Map<String, Object>> items) {
        long total = 0L;
        for (Map<String, Object> item : items) {
            Object value = item.get("value");
            if (value instanceof Number) {
                total += ((Number) value).longValue();
            }
        }
        return total;
    }

    private String resolveCollegeName(Long collegeId) {
        if (collegeId == null) {
            return "当前学院";
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT college_name AS name FROM t_college WHERE deleted = 0 AND id = ? LIMIT 1",
                collegeId
        );
        if (rows.isEmpty() || rows.get(0).get("name") == null) {
            return "当前学院";
        }
        return String.valueOf(rows.get(0).get("name"));
    }

    private String resolveLabName(Long labId) {
        if (labId == null) {
            return "当前实验室";
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT lab_name AS name FROM t_lab WHERE deleted = 0 AND id = ? LIMIT 1",
                labId
        );
        if (rows.isEmpty() || rows.get(0).get("name") == null) {
            return "当前实验室";
        }
        return String.valueOf(rows.get(0).get("name"));
    }

    private List<Map<String, Object>> completeMonthlyTrend(List<Map<String, Object>> rawRecords, int monthCount) {
        Map<String, Object> valueMap = new HashMap<>();
        for (Map<String, Object> rawRecord : rawRecords) {
            valueMap.put(String.valueOf(rawRecord.get("name")), rawRecord.get("value"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        for (int offset = monthCount - 1; offset >= 0; offset--) {
            String month = currentMonth.minusMonths(offset).format(YEAR_MONTH_FORMATTER);
            Object value = valueMap.getOrDefault(month, 0);
            result.add(statItem(month, value));
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
