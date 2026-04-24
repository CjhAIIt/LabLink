package com.lab.recruitment.service.impl;

import com.lab.recruitment.config.PlatformCacheNames;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.StatisticsService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
        Long labId = currentUserAccessor.resolveLabScope(currentUser, null);
        return buildLabOverview(labId);
    }

    @Override
    public Map<String, Object> getLabStatistics(Long labId, User currentUser) {
        Long scopedLabId = currentUserAccessor.isSuperAdmin(currentUser)
                ? labId
                : currentUserAccessor.resolveLabScope(currentUser, labId);
        if (scopedLabId == null) {
            throw new RuntimeException("实验室 ID 不能为空");
        }
        return buildLabOverview(scopedLabId);
    }

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.STAT_DASHBOARD,
            key = "#currentUser.id + ':' + (#startDate == null ? '' : #startDate.toString()) + ':' + (#endDate == null ? '' : #endDate.toString()) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null"
    )
    public Map<String, Object> getDashboard(User currentUser, LocalDate startDate, LocalDate endDate,
                                            Long collegeId, Long labId) {
        StatisticsScope scope = resolveStatisticsScope(currentUser, collegeId, labId);
        DateWindow window = resolveDateWindow(startDate, endDate);
        Map<String, Object> attendance = getAttendanceDimension(currentUser, window.startDate, window.endDate, collegeId, labId);
        Map<String, Object> profiles = getProfileDimension(currentUser, window.startDate, window.endDate, collegeId, labId);
        Map<String, Object> devices = getDeviceDimension(currentUser, window.startDate, window.endDate, collegeId, labId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("labCount", countScopedLabs(scope));
        summary.put("memberCount", countScopedMembers(scope));
        summary.put("deviceCount", devices.get("totalDevices"));
        summary.put("fileCount", countScopedFiles(scope));
        summary.put("attendanceRate", attendance.get("attendanceRate"));
        summary.put("leaveRate", attendance.get("leaveRate"));
        summary.put("profileApprovedRate", profiles.get("approvedRate"));
        summary.put("pendingProfileCount", profiles.get("pendingCount"));

        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("pendingLeaves", countPendingLeaves(scope, window));
        pending.put("pendingProfiles", numberValue(profiles.get("pendingCount")));
        pending.put("pendingMaintenance", countPendingMaintenance(scope, window));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scopeType", scope.scopeType);
        result.put("scopeName", scope.scopeName);
        result.put("collegeId", scope.collegeId);
        result.put("labId", scope.labId);
        result.put("startDate", window.startDate);
        result.put("endDate", window.endDate);
        result.put("summary", summary);
        result.put("pending", pending);
        return result;
    }

    @Override
    public byte[] exportDashboardExcel(User currentUser, LocalDate startDate, LocalDate endDate,
                                       Long collegeId, Long labId) {
        DateWindow window = resolveDateWindow(startDate, endDate);
        Map<String, Object> dashboard = getDashboard(currentUser, window.startDate, window.endDate, collegeId, labId);
        List<Map<String, Object>> labs = getLabDimension(currentUser, window.startDate, window.endDate, collegeId, labId);
        List<Map<String, Object>> members = getMemberDimension(currentUser, window.startDate, window.endDate, collegeId, labId);
        Map<String, Object> attendance = getAttendanceDimension(currentUser, window.startDate, window.endDate, collegeId, labId);
        Map<String, Object> devices = getDeviceDimension(currentUser, window.startDate, window.endDate, collegeId, labId);
        Map<String, Object> profiles = getProfileDimension(currentUser, window.startDate, window.endDate, collegeId, labId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            writeExportSheet(workbook, headerStyle, "Summary", dashboard);
            writeExportSheet(workbook, headerStyle, "Labs", labs);
            writeExportSheet(workbook, headerStyle, "Members", members);
            writeExportSheet(workbook, headerStyle, "Attendance", attendance);
            writeExportSheet(workbook, headerStyle, "Devices", devices);
            writeExportSheet(workbook, headerStyle, "Profiles", profiles);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出统计报表失败", e);
        }
    }

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.STAT_LABS,
            key = "#currentUser.id + ':' + (#startDate == null ? '' : #startDate.toString()) + ':' + (#endDate == null ? '' : #endDate.toString()) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null"
    )
    public List<Map<String, Object>> getLabDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                                     Long collegeId, Long labId) {
        StatisticsScope scope = resolveStatisticsScope(currentUser, collegeId, labId);
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(buildGroup("topLabs", scope.scopeType.equals("lab") ? "当前实验室" : "热门实验室", queryTopLabs(scope)));
        groups.add(buildGroup(
                "orgDistribution",
                scope.scopeType.equals("school") ? "学院分布" : "实验室分布",
                queryLabDistribution(scope)
        ));
        return groups;
    }

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.STAT_MEMBERS,
            key = "#currentUser.id + ':' + (#startDate == null ? '' : #startDate.toString()) + ':' + (#endDate == null ? '' : #endDate.toString()) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null"
    )
    public List<Map<String, Object>> getMemberDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                                        Long collegeId, Long labId) {
        StatisticsScope scope = resolveStatisticsScope(currentUser, collegeId, labId);
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(buildGroup("memberRoles", "成员角色分布", queryMemberRoles(scope)));
        groups.add(buildGroup("majorDistribution", "专业分布", queryMajorDistribution(scope)));
        groups.add(buildGroup(
                "memberOrgDistribution",
                scope.scopeType.equals("school") ? "学院成员分布" : "实验室成员分布",
                queryMemberOrgDistribution(scope)
        ));
        return groups;
    }

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.STAT_ATTENDANCE,
            key = "#currentUser.id + ':' + (#startDate == null ? '' : #startDate.toString()) + ':' + (#endDate == null ? '' : #endDate.toString()) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null"
    )
    public Map<String, Object> getAttendanceDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                                      Long collegeId, Long labId) {
        StatisticsScope scope = resolveStatisticsScope(currentUser, collegeId, labId);
        DateWindow window = resolveDateWindow(startDate, endDate);
        Map<String, Long> counter = queryAttendanceStatusCounter(scope, window);
        long total = 0L;
        for (Long value : counter.values()) {
            total += value == null ? 0L : value;
        }
        long normal = counter.getOrDefault("normal", 0L);
        long late = counter.getOrDefault("late", 0L);
        long leave = counter.getOrDefault("leave", 0L);
        long absent = counter.getOrDefault("absent", 0L);
        long supplementApproved = counter.getOrDefault("makeup_approved", 0L);
        long supplementPending = counter.getOrDefault("makeup_pending", 0L);
        long supplementRejected = counter.getOrDefault("makeup_rejected", 0L);

        double attendanceRate = total <= 0 ? 100D : round((normal + late + supplementApproved) * 100.0D / total);
        double leaveRate = total <= 0 ? 0D : round(leave * 100.0D / total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", window.startDate);
        result.put("endDate", window.endDate);
        result.put("totalRecords", total);
        result.put("attendanceRate", attendanceRate);
        result.put("leaveRate", leaveRate);
        result.put("statusDistribution", toNamedRows(counter, attendanceStatusLabelMap()));
        result.put("monthlyTrend", queryAttendanceMonthlyTrend(scope, window));
        result.put("summary", new LinkedHashMap<String, Object>() {{
            put("normal", normal);
            put("late", late);
            put("leave", leave);
            put("absent", absent);
            put("supplementApproved", supplementApproved);
            put("supplementPending", supplementPending);
            put("supplementRejected", supplementRejected);
        }});
        return result;
    }

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.STAT_DEVICES,
            key = "#currentUser.id + ':' + (#startDate == null ? '' : #startDate.toString()) + ':' + (#endDate == null ? '' : #endDate.toString()) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null"
    )
    public Map<String, Object> getDeviceDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                                  Long collegeId, Long labId) {
        StatisticsScope scope = resolveStatisticsScope(currentUser, collegeId, labId);
        DateWindow window = resolveDateWindow(startDate, endDate);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDevices", countScopedDevices(scope));
        result.put("statusDistribution", queryDeviceStatusDistribution(scope));
        result.put("categoryDistribution", queryDeviceCategoryDistribution(scope));
        result.put("maintenanceDistribution", queryMaintenanceDistribution(scope, window));
        return result;
    }

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.STAT_PROFILES,
            key = "#currentUser.id + ':' + (#startDate == null ? '' : #startDate.toString()) + ':' + (#endDate == null ? '' : #endDate.toString()) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null"
    )
    public Map<String, Object> getProfileDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                                   Long collegeId, Long labId) {
        StatisticsScope scope = resolveStatisticsScope(currentUser, collegeId, labId);
        long totalProfiles = countScopedProfiles(scope);
        long pendingCount = countScopedProfilesByStatus(scope, "PENDING");
        long approvedCount = countScopedProfilesByStatus(scope, "APPROVED") + countScopedProfilesByStatus(scope, "ARCHIVED");
        double approvedRate = totalProfiles <= 0 ? 0D : round(approvedCount * 100.0D / totalProfiles);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProfiles", totalProfiles);
        result.put("pendingCount", pendingCount);
        result.put("approvedRate", approvedRate);
        result.put("statusDistribution", queryProfileStatusDistribution(scope));
        result.put("directionDistribution", queryProfileDirectionDistribution(scope));
        result.put("orgDistribution", queryProfileOrgDistribution(scope));
        return result;
    }

    private StatisticsScope resolveStatisticsScope(User currentUser, Long requestedCollegeId, Long requestedLabId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("当前用户不存在");
        }

        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            if (requestedLabId != null) {
                Long collegeId = requestedCollegeId != null
                        ? requestedCollegeId
                        : currentUserAccessor.resolveCollegeIdByLabId(requestedLabId);
                return new StatisticsScope("lab", resolveLabName(requestedLabId), collegeId, requestedLabId);
            }
            if (requestedCollegeId != null) {
                return new StatisticsScope("college", resolveCollegeName(requestedCollegeId), requestedCollegeId, null);
            }
            return new StatisticsScope("school", "学校", null, null);
        }

        Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
        if (managedCollegeId != null) {
            DataScope scoped = currentUserAccessor.resolveManagementScope(currentUser, requestedCollegeId, requestedLabId);
            if (scoped.getLabId() != null) {
                return new StatisticsScope("lab", resolveLabName(scoped.getLabId()), scoped.getCollegeId(), scoped.getLabId());
            }
            return new StatisticsScope("college", resolveCollegeName(scoped.getCollegeId()), scoped.getCollegeId(), null);
        }

        Long targetLabId = requestedLabId;
        if (targetLabId == null) {
            DataScope baseScope = currentUserAccessor.buildDataScope(currentUser);
            targetLabId = baseScope.getLabId();
        }
        if (targetLabId == null) {
            throw new RuntimeException("当前账号未绑定可统计的数据范围");
        }

        Long scopedLabId = currentUserAccessor.resolveLabScope(currentUser, targetLabId);
        Long scopedCollegeId = currentUserAccessor.resolveCollegeIdByLabId(scopedLabId);
        if (requestedCollegeId != null && !Objects.equals(requestedCollegeId, scopedCollegeId)) {
            throw new RuntimeException("No access to another college");
        }
        return new StatisticsScope("lab", resolveLabName(scopedLabId), scopedCollegeId, scopedLabId);
    }

    private DateWindow resolveDateWindow(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedEnd = endDate == null ? today : endDate;
        LocalDate resolvedStart = startDate == null ? resolvedEnd.minusDays(29) : startDate;
        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new RuntimeException("结束日期不能早于开始日期");
        }
        return new DateWindow(resolvedStart, resolvedEnd);
    }

    private Map<String, Object> buildGroup(String key, String title, List<Map<String, Object>> data) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("key", key);
        group.put("title", title);
        group.put("data", data == null ? new ArrayList<>() : data);
        return group;
    }

    private long numberValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private Map<String, String> attendanceStatusLabelMap() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("normal", "正常");
        labels.put("late", "迟到");
        labels.put("leave", "请假");
        labels.put("absent", "缺勤");
        labels.put("makeup_approved", "补签通过");
        labels.put("makeup_pending", "补签待审");
        labels.put("makeup_rejected", "补签驳回");
        return labels;
    }

    private List<Map<String, Object>> toNamedRows(Map<String, Long> counter, Map<String, String> labels) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            rows.add(statItem(entry.getValue(), counter.getOrDefault(entry.getKey(), 0L)));
        }
        for (Map.Entry<String, Long> entry : counter.entrySet()) {
            if (!labels.containsKey(entry.getKey())) {
                rows.add(statItem(entry.getKey(), entry.getValue()));
            }
        }
        return rows;
    }

    private void appendScopeCondition(StringBuilder where, List<Object> args, StatisticsScope scope,
                                      String labColumn, String collegeColumn) {
        if (scope == null) {
            return;
        }
        if (scope.labId != null && labColumn != null) {
            where.append(" AND ").append(labColumn).append(" = ?");
            args.add(scope.labId);
            return;
        }
        if (scope.collegeId != null && collegeColumn != null) {
            where.append(" AND ").append(collegeColumn).append(" = ?");
            args.add(scope.collegeId);
        }
    }

    private long countScopedLabs(StatisticsScope scope) {
        if (scope.labId != null) {
            return count("SELECT COUNT(*) FROM t_lab WHERE deleted = 0 AND id = ?", scope.labId);
        }
        if (scope.collegeId != null) {
            return count("SELECT COUNT(*) FROM t_lab WHERE deleted = 0 AND college_id = ?", scope.collegeId);
        }
        return count("SELECT COUNT(*) FROM t_lab WHERE deleted = 0");
    }

    private long countScopedMembers(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE m.deleted = 0 AND m.status = 'active'");
        appendScopeCondition(where, args, scope, "m.lab_id", "l.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_lab_member m LEFT JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private long countScopedFiles(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE f.deleted = 0");
        appendScopeCondition(where, args, scope, "f.lab_id", "l.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_lab_space_file f LEFT JOIN t_lab l ON l.id = f.lab_id AND l.deleted = 0" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private long countPendingLeaves(StatisticsScope scope, DateWindow window) {
        List<Object> args = new ArrayList<>();
        args.add(window.startDate);
        args.add(window.endDate.plusDays(1));
        StringBuilder where = new StringBuilder(
                " WHERE al.deleted = 0 AND al.leave_status = 'PENDING' AND al.created_at >= ? AND al.created_at < ?"
        );
        appendScopeCondition(where, args, scope, "al.lab_id", "l.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance_leave al LEFT JOIN t_lab l ON l.id = al.lab_id AND l.deleted = 0" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private long countPendingMaintenance(StatisticsScope scope, DateWindow window) {
        List<Object> args = new ArrayList<>();
        args.add(window.startDate.atStartOfDay());
        args.add(window.endDate.plusDays(1).atStartOfDay());
        StringBuilder where = new StringBuilder(
                " WHERE m.deleted = 0 AND m.maintenance_status <> 'RESOLVED' AND m.create_time >= ? AND m.create_time < ?"
        );
        appendScopeCondition(where, args, scope, "m.lab_id", "l.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_equipment_maintenance m LEFT JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private List<Map<String, Object>> queryTopLabs(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE l.deleted = 0");
        appendScopeCondition(where, args, scope, "l.id", "l.college_id");
        return jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                        "FROM t_lab l " +
                        "LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active'" +
                        where +
                        " GROUP BY l.id, l.lab_name ORDER BY value DESC, name ASC LIMIT 8",
                args.toArray()
        );
    }

    private List<Map<String, Object>> queryLabDistribution(StatisticsScope scope) {
        if (scope.labId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                            "FROM t_lab l " +
                            "LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                            "WHERE l.deleted = 0 AND l.id = ? GROUP BY l.id, l.lab_name",
                    scope.labId
            );
        }
        if (scope.collegeId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                            "FROM t_lab l " +
                            "LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                            "WHERE l.deleted = 0 AND l.college_id = ? " +
                            "GROUP BY l.id, l.lab_name ORDER BY value DESC, name ASC LIMIT 8",
                    scope.collegeId
            );
        }
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(c.college_name, 'Unassigned') AS name, COUNT(l.id) AS value " +
                        "FROM t_lab l " +
                        "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                        "WHERE l.deleted = 0 " +
                        "GROUP BY COALESCE(c.college_name, 'Unassigned') ORDER BY value DESC, name ASC"
        );
    }

    private List<Map<String, Object>> queryMemberRoles(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE m.deleted = 0 AND m.status = 'active'");
        appendScopeCondition(where, args, scope, "m.lab_id", "l.college_id");
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(m.member_role, 'unknown') AS name, COUNT(*) AS value " +
                        "FROM t_lab_member m LEFT JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0" +
                        where +
                        " GROUP BY COALESCE(m.member_role, 'unknown') ORDER BY value DESC, name ASC",
                args.toArray()
        );
    }

    private List<Map<String, Object>> queryMajorDistribution(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE p.deleted = 0 AND p.major IS NOT NULL AND TRIM(p.major) <> ''");
        appendScopeCondition(where, args, scope, "p.lab_id", "p.college_id");
        return jdbcTemplate.queryForList(
                "SELECT p.major AS name, COUNT(*) AS value FROM t_student_profile p" +
                        where +
                        " GROUP BY p.major ORDER BY value DESC, name ASC LIMIT 8",
                args.toArray()
        );
    }

    private List<Map<String, Object>> queryMemberOrgDistribution(StatisticsScope scope) {
        if (scope.labId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                            "FROM t_lab l " +
                            "LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                            "WHERE l.deleted = 0 AND l.id = ? GROUP BY l.id, l.lab_name",
                    scope.labId
            );
        }
        if (scope.collegeId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT l.lab_name AS name, COUNT(m.id) AS value " +
                            "FROM t_lab l " +
                            "LEFT JOIN t_lab_member m ON m.lab_id = l.id AND m.deleted = 0 AND m.status = 'active' " +
                            "WHERE l.deleted = 0 AND l.college_id = ? " +
                            "GROUP BY l.id, l.lab_name ORDER BY value DESC, name ASC LIMIT 8",
                    scope.collegeId
            );
        }
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(c.college_name, 'Unassigned') AS name, COUNT(m.id) AS value " +
                        "FROM t_lab_member m " +
                        "LEFT JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                        "WHERE m.deleted = 0 AND m.status = 'active' " +
                        "GROUP BY COALESCE(c.college_name, 'Unassigned') ORDER BY value DESC, name ASC"
        );
    }

    private Map<String, Long> queryAttendanceStatusCounter(StatisticsScope scope, DateWindow window) {
        List<Object> args = new ArrayList<>();
        args.add(window.startDate);
        args.add(window.endDate);
        StringBuilder where = new StringBuilder(" WHERE r.deleted = 0 AND s.deleted = 0 AND s.session_date BETWEEN ? AND ?");
        appendScopeCondition(where, args, scope, "r.lab_id", "l.college_id");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT LOWER(r.sign_status) AS statusName, COUNT(*) AS value " +
                        "FROM t_attendance_record r " +
                        "INNER JOIN t_attendance_session s ON s.id = r.session_id " +
                        "LEFT JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0" +
                        where +
                        " GROUP BY LOWER(r.sign_status)",
                args.toArray()
        );

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = row.get("statusName") == null
                    ? "unknown"
                    : String.valueOf(row.get("statusName")).trim().toLowerCase(Locale.ROOT);
            result.put(key, numberValue(row.get("value")));
        }
        return result;
    }

    private List<Map<String, Object>> queryAttendanceMonthlyTrend(StatisticsScope scope, DateWindow window) {
        List<Object> args = new ArrayList<>();
        args.add(window.startDate);
        args.add(window.endDate);
        StringBuilder where = new StringBuilder(" WHERE r.deleted = 0 AND s.deleted = 0 AND s.session_date BETWEEN ? AND ?");
        appendScopeCondition(where, args, scope, "r.lab_id", "l.college_id");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(s.session_date, '%Y-%m') AS name, " +
                        "ROUND(CASE WHEN COUNT(*) = 0 THEN 0 ELSE " +
                        "SUM(CASE WHEN LOWER(r.sign_status) IN ('normal', 'late', 'makeup_approved') THEN 1 ELSE 0 END) * 100.0 / COUNT(*) END, 2) AS value " +
                        "FROM t_attendance_record r " +
                        "INNER JOIN t_attendance_session s ON s.id = r.session_id " +
                        "LEFT JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0" +
                        where +
                        " GROUP BY DATE_FORMAT(s.session_date, '%Y-%m') ORDER BY name ASC",
                args.toArray()
        );
        return completeMonthlyTrend(rows, window.startDate, window.endDate);
    }

    private long countScopedDevices(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE e.deleted = 0");
        appendScopeCondition(where, args, scope, "e.lab_id", "l.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_equipment e LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private List<Map<String, Object>> queryDeviceStatusDistribution(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE e.deleted = 0");
        appendScopeCondition(where, args, scope, "e.lab_id", "l.college_id");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT e.status AS statusCode, COUNT(*) AS value " +
                        "FROM t_equipment e LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0" +
                        where +
                        " GROUP BY e.status ORDER BY e.status ASC",
                args.toArray()
        );
        Map<Integer, String> labels = new LinkedHashMap<>();
        labels.put(0, "Idle");
        labels.put(1, "Borrowed");
        labels.put(2, "Maintaining");
        labels.put(3, "Scrapped");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : labels.entrySet()) {
            long value = 0L;
            for (Map<String, Object> row : rows) {
                Integer statusCode = row.get("statusCode") instanceof Number ? ((Number) row.get("statusCode")).intValue() : null;
                if (Objects.equals(statusCode, entry.getKey())) {
                    value = numberValue(row.get("value"));
                    break;
                }
            }
            result.add(statItem(entry.getValue(), value));
        }
        return result;
    }

    private List<Map<String, Object>> queryDeviceCategoryDistribution(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE e.deleted = 0");
        appendScopeCondition(where, args, scope, "e.lab_id", "l.college_id");
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(c.name, e.type, 'Uncategorized') AS name, COUNT(*) AS value " +
                        "FROM t_equipment e " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_equipment_category c ON c.id = e.category_id AND c.deleted = 0" +
                        where +
                        " GROUP BY COALESCE(c.name, e.type, 'Uncategorized') ORDER BY value DESC, name ASC LIMIT 8",
                args.toArray()
        );
    }

    private List<Map<String, Object>> queryMaintenanceDistribution(StatisticsScope scope, DateWindow window) {
        List<Object> args = new ArrayList<>();
        args.add(window.startDate.atStartOfDay());
        args.add(window.endDate.plusDays(1).atStartOfDay());
        StringBuilder where = new StringBuilder(" WHERE m.deleted = 0 AND m.create_time >= ? AND m.create_time < ?");
        appendScopeCondition(where, args, scope, "m.lab_id", "l.college_id");
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(m.maintenance_status, 'PENDING') AS name, COUNT(*) AS value " +
                        "FROM t_equipment_maintenance m " +
                        "LEFT JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0" +
                        where +
                        " GROUP BY COALESCE(m.maintenance_status, 'PENDING') ORDER BY value DESC, name ASC",
                args.toArray()
        );
    }

    private long countScopedProfiles(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE p.deleted = 0");
        appendScopeCondition(where, args, scope, "p.lab_id", "p.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_student_profile p" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private long countScopedProfilesByStatus(StatisticsScope scope, String status) {
        List<Object> args = new ArrayList<>();
        args.add(status);
        StringBuilder where = new StringBuilder(" WHERE p.deleted = 0 AND p.status = ?");
        appendScopeCondition(where, args, scope, "p.lab_id", "p.college_id");
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_student_profile p" + where,
                Long.class,
                args.toArray()
        );
        return value == null ? 0L : value;
    }

    private List<Map<String, Object>> queryProfileStatusDistribution(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE p.deleted = 0");
        appendScopeCondition(where, args, scope, "p.lab_id", "p.college_id");
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(p.status, 'UNKNOWN') AS name, COUNT(*) AS value " +
                        "FROM t_student_profile p" +
                        where +
                        " GROUP BY COALESCE(p.status, 'UNKNOWN') ORDER BY value DESC, name ASC",
                args.toArray()
        );
    }

    private List<Map<String, Object>> queryProfileDirectionDistribution(StatisticsScope scope) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE p.deleted = 0 AND p.direction IS NOT NULL AND TRIM(p.direction) <> ''");
        appendScopeCondition(where, args, scope, "p.lab_id", "p.college_id");
        return jdbcTemplate.queryForList(
                "SELECT p.direction AS name, COUNT(*) AS value " +
                        "FROM t_student_profile p" +
                        where +
                        " GROUP BY p.direction ORDER BY value DESC, name ASC LIMIT 8",
                args.toArray()
        );
    }

    private List<Map<String, Object>> queryProfileOrgDistribution(StatisticsScope scope) {
        if (scope.labId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT l.lab_name AS name, COUNT(p.id) AS value " +
                            "FROM t_lab l LEFT JOIN t_student_profile p ON p.lab_id = l.id AND p.deleted = 0 " +
                            "WHERE l.deleted = 0 AND l.id = ? GROUP BY l.id, l.lab_name",
                    scope.labId
            );
        }
        if (scope.collegeId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT l.lab_name AS name, COUNT(p.id) AS value " +
                            "FROM t_lab l LEFT JOIN t_student_profile p ON p.lab_id = l.id AND p.deleted = 0 " +
                            "WHERE l.deleted = 0 AND l.college_id = ? " +
                            "GROUP BY l.id, l.lab_name ORDER BY value DESC, name ASC LIMIT 8",
                    scope.collegeId
            );
        }
        return jdbcTemplate.queryForList(
                "SELECT COALESCE(c.college_name, 'Unassigned') AS name, COUNT(p.id) AS value " +
                        "FROM t_student_profile p " +
                        "LEFT JOIN t_college c ON c.id = p.college_id AND c.deleted = 0 " +
                        "WHERE p.deleted = 0 " +
                        "GROUP BY COALESCE(c.college_name, 'Unassigned') ORDER BY value DESC, name ASC"
        );
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

    private List<Map<String, Object>> completeMonthlyTrend(List<Map<String, Object>> rawRecords,
                                                           LocalDate startDate,
                                                           LocalDate endDate) {
        Map<String, Object> valueMap = new HashMap<>();
        for (Map<String, Object> rawRecord : rawRecords) {
            valueMap.put(String.valueOf(rawRecord.get("name")), rawRecord.get("value"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        while (!current.isAfter(endMonth)) {
            String month = current.format(YEAR_MONTH_FORMATTER);
            result.add(statItem(month, valueMap.getOrDefault(month, 0)));
            current = current.plusMonths(1);
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private CellStyle createExportHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeExportSheet(Workbook workbook, CellStyle headerStyle, String sheetName, Object source) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        writeExportCell(header, 0, "Section", headerStyle);
        writeExportCell(header, 1, "Name", headerStyle);
        writeExportCell(header, 2, "Value", headerStyle);

        int nextRow = appendExportRows(sheet, 1, sheetName, source);
        if (nextRow == 1) {
            writeExportRow(sheet, nextRow, sheetName, "No data", "");
        }
        autosizeExportColumns(sheet, 3);
    }

    private int appendExportRows(Sheet sheet, int rowIndex, String section, Object source) {
        if (source instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) source;
            if (map.isEmpty()) {
                writeExportRow(sheet, rowIndex++, section, "empty", "");
                return rowIndex;
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String name = stringifyExportValue(entry.getKey());
                Object value = entry.getValue();
                if (isSimpleExportValue(value)) {
                    writeExportRow(sheet, rowIndex++, section, name, value);
                } else {
                    rowIndex = appendExportRows(sheet, rowIndex, joinExportSection(section, name), value);
                }
            }
            return rowIndex;
        }

        if (source instanceof Iterable) {
            int index = 1;
            boolean hasRows = false;
            for (Object item : (Iterable<?>) source) {
                hasRows = true;
                String itemName = deriveExportItemName(item, index);
                if (isSimpleExportValue(item)) {
                    writeExportRow(sheet, rowIndex++, section, itemName, item);
                } else {
                    rowIndex = appendExportRows(sheet, rowIndex, joinExportSection(section, itemName), item);
                }
                index++;
            }
            if (!hasRows) {
                writeExportRow(sheet, rowIndex++, section, "empty", "");
            }
            return rowIndex;
        }

        writeExportRow(sheet, rowIndex++, section, "value", source);
        return rowIndex;
    }

    private void writeExportRow(Sheet sheet, int rowIndex, String section, String name, Object value) {
        Row row = sheet.createRow(rowIndex);
        writeExportCell(row, 0, section, null);
        writeExportCell(row, 1, name, null);
        writeExportCell(row, 2, value, null);
    }

    private void writeExportCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            return;
        }
        if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
            return;
        }
        cell.setCellValue(stringifyExportValue(value));
    }

    private boolean isSimpleExportValue(Object value) {
        return value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof LocalDate
                || value instanceof YearMonth
                || value instanceof java.time.temporal.TemporalAccessor
                || value instanceof java.util.Date
                || value.getClass().isEnum();
    }

    private String deriveExportItemName(Object item, int index) {
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            for (String key : new String[]{"title", "label", "name", "key", "id"}) {
                Object value = map.get(key);
                if (value != null) {
                    String text = stringifyExportValue(value);
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        }
        return "Item " + index;
    }

    private String joinExportSection(String section, String name) {
        if (section == null || section.isEmpty()) {
            return name;
        }
        if (name == null || name.isEmpty()) {
            return section;
        }
        return section + " / " + name;
    }

    private String stringifyExportValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.length() > 32760 ? text.substring(0, 32760) : text;
    }

    private void autosizeExportColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = Math.max(sheet.getColumnWidth(i), 18 * 256);
            sheet.setColumnWidth(i, Math.min(width, 60 * 256));
        }
    }

    private static final class DateWindow {

        private final LocalDate startDate;
        private final LocalDate endDate;

        private DateWindow(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static final class StatisticsScope {

        private final String scopeType;
        private final String scopeName;
        private final Long collegeId;
        private final Long labId;

        private StatisticsScope(String scopeType, String scopeName, Long collegeId, Long labId) {
            this.scopeType = scopeType;
            this.scopeName = scopeName;
            this.collegeId = collegeId;
            this.labId = labId;
        }
    }
}
