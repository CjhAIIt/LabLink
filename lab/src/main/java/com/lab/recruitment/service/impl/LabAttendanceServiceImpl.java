package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.LabAttendance;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabAttendanceMapper;
import com.lab.recruitment.service.LabAttendanceService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.vo.LabAttendanceMemberVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabAttendanceServiceImpl extends ServiceImpl<LabAttendanceMapper, LabAttendance> implements LabAttendanceService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PRESENT = 1;
    private static final int STATUS_LATE = 2;
    private static final int STATUS_LEAVE = 3;
    private static final int STATUS_ABSENT = 4;
    private static final int STATUS_MAKEUP = 5;
    private static final int STATUS_EXEMPT = 6;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<LabAttendanceMemberVO> getDailyAttendance(Long labId, String attendanceDate) {
        QueryWrapper<User> memberQuery = new QueryWrapper<>();
        memberQuery.eq("lab_id", labId)
                .eq("role", "student")
                .orderByAsc("student_id")
                .orderByAsc("id");
        List<User> members = userService.list(memberQuery);

        QueryWrapper<LabAttendance> attendanceQuery = new QueryWrapper<>();
        attendanceQuery.eq("lab_id", labId)
                .eq("attendance_date", attendanceDate)
                .eq("deleted", 0);
        List<LabAttendance> attendanceList = this.list(attendanceQuery);
        Map<Long, LabAttendance> attendanceMap = new HashMap<>();
        for (LabAttendance attendance : attendanceList) {
            attendanceMap.put(attendance.getUserId(), attendance);
        }

        List<LabAttendanceMemberVO> result = new ArrayList<>();
        for (User member : members) {
            LabAttendance attendance = attendanceMap.get(member.getId());
            LabAttendanceMemberVO item = new LabAttendanceMemberVO();
            item.setUserId(member.getId());
            item.setRealName(member.getRealName());
            item.setStudentId(member.getStudentId());
            item.setMajor(member.getMajor());
            item.setAttendanceDate(attendanceDate);
            if (attendance != null) {
                item.setStatus(attendance.getStatus());
                item.setReason(attendance.getReason());
                item.setConfirmTime(attendance.getConfirmTime());
            } else {
                item.setStatus(0);
            }
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean confirmAttendance(Long labId, Long adminId, Long userId, String attendanceDate, Integer status, String reason) {
        if (!isValidStatus(status)) {
            throw new RuntimeException("Attendance status is invalid");
        }
        if (requiresReason(status) && !StringUtils.hasText(reason)) {
            throw new RuntimeException("Please provide a reason for this attendance status");
        }

        User member = userService.getById(userId);
        if (member == null || !"student".equals(member.getRole())) {
            throw new RuntimeException("Lab member not found");
        }
        if (member.getLabId() == null || !member.getLabId().equals(labId)) {
            throw new RuntimeException("This student does not belong to your lab");
        }

        QueryWrapper<LabAttendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("attendance_date", attendanceDate)
                .eq("deleted", 0);

        LabAttendance attendance = this.getOne(queryWrapper);
        if (attendance == null) {
            attendance = new LabAttendance();
            attendance.setLabId(labId);
            attendance.setUserId(userId);
            attendance.setAttendanceDate(attendanceDate);
        }

        attendance.setStatus(status);
        attendance.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        attendance.setConfirmedBy(adminId);
        attendance.setConfirmTime(LocalDateTime.now());
        return attendance.getId() == null ? this.save(attendance) : this.updateById(attendance);
    }

    @Override
    public Page<LabAttendance> getMyAttendancePage(Integer pageNum, Integer pageSize, Long userId) {
        Page<LabAttendance> page = new Page<>(pageNum, pageSize);
        QueryWrapper<LabAttendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("attendance_date")
                .orderByDesc("confirm_time")
                .orderByDesc("id");
        return this.page(page, queryWrapper);
    }

    @Override
    @Transactional
    public boolean studentSignIn(Long labId, Long userId, String attendanceDate, Integer status, String reason) {
        if (status == null || (status != STATUS_PRESENT && status != STATUS_LATE && status != STATUS_MAKEUP)) {
            throw new RuntimeException("Students can only sign in as present, late or makeup");
        }

        User member = userService.getById(userId);
        if (member == null || member.getLabId() == null || !member.getLabId().equals(labId)) {
            throw new RuntimeException("Current user does not belong to this lab");
        }

        QueryWrapper<LabAttendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("attendance_date", attendanceDate)
                .eq("deleted", 0);

        LabAttendance attendance = this.getOne(queryWrapper);
        if (attendance != null && attendance.getStatus() != null
                && (attendance.getStatus() == STATUS_LEAVE || attendance.getStatus() == STATUS_EXEMPT)) {
            throw new RuntimeException("Current attendance record has been fixed by a manager");
        }

        if (attendance == null) {
            attendance = new LabAttendance();
            attendance.setLabId(labId);
            attendance.setUserId(userId);
            attendance.setAttendanceDate(attendanceDate);
        }

        attendance.setStatus(status);
        attendance.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        attendance.setConfirmedBy(userId);
        attendance.setConfirmTime(LocalDateTime.now());
        return attendance.getId() == null ? this.save(attendance) : this.updateById(attendance);
    }

    @Override
    public Map<String, Object> getAttendanceSummary(Long labId, Long userId) {
        if (labId == null && userId == null) {
            throw new RuntimeException("Attendance summary scope is required");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        String weekStart = today.minusDays(6).toString();
        String monthStart = today.minusDays(29).toString();
        String todayString = today.toString();

        if (userId != null) {
            summary.putAll(buildStatusCountMap(
                    "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND user_id = ? GROUP BY status",
                    userId));
            summary.put("weeklyRate", calculateRate(fetchStatusCounter(
                    "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND user_id = ? AND attendance_date BETWEEN ? AND ? GROUP BY status",
                    userId, weekStart, todayString)));
            summary.put("monthlyRate", calculateRate(fetchStatusCounter(
                    "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND user_id = ? AND attendance_date BETWEEN ? AND ? GROUP BY status",
                    userId, monthStart, todayString)));
            summary.put("recentRecords", jdbcTemplate.queryForList(
                    "SELECT attendance_date AS attendanceDate, status, reason, confirm_time AS confirmTime " +
                            "FROM t_lab_attendance WHERE deleted = 0 AND user_id = ? " +
                            "ORDER BY attendance_date DESC, id DESC LIMIT 7",
                    userId));
            return summary;
        }

        summary.putAll(buildStatusCountMap(
                "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND lab_id = ? GROUP BY status",
                labId));
        summary.put("weeklyRate", calculateRate(fetchStatusCounter(
                "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND lab_id = ? AND attendance_date BETWEEN ? AND ? GROUP BY status",
                labId, weekStart, todayString)));
        summary.put("monthlyRate", calculateRate(fetchStatusCounter(
                "SELECT status, COUNT(*) AS value FROM t_lab_attendance WHERE deleted = 0 AND lab_id = ? AND attendance_date BETWEEN ? AND ? GROUP BY status",
                labId, monthStart, todayString)));
        summary.put("dailyTrend", buildDailyTrend(labId, today));
        summary.put("abnormalMembers", jdbcTemplate.queryForList(
                "SELECT u.real_name AS name, COUNT(*) AS value " +
                        "FROM t_lab_attendance a " +
                        "LEFT JOIN t_user u ON u.id = a.user_id AND u.deleted = 0 " +
                        "WHERE a.deleted = 0 AND a.lab_id = ? AND a.attendance_date BETWEEN ? AND ? " +
                        "AND a.status IN (?, ?) " +
                        "GROUP BY a.user_id, u.real_name " +
                        "ORDER BY value DESC, name ASC LIMIT 6",
                labId, monthStart, todayString, STATUS_LATE, STATUS_ABSENT));
        return summary;
    }

    private boolean isValidStatus(Integer status) {
        return status != null && status >= STATUS_PRESENT && status <= STATUS_EXEMPT;
    }

    private boolean requiresReason(Integer status) {
        return status != null && (status == STATUS_LEAVE || status == STATUS_ABSENT);
    }

    private Map<Integer, Long> fetchStatusCounter(String sql, Object... args) {
        Map<Integer, Long> counter = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        for (Map<String, Object> row : rows) {
            Integer status = row.get("status") instanceof Number ? ((Number) row.get("status")).intValue() : null;
            Long value = row.get("value") instanceof Number ? ((Number) row.get("value")).longValue() : 0L;
            if (status != null) {
                counter.put(status, value);
            }
        }
        return counter;
    }

    private Map<String, Object> buildStatusCountMap(String sql, Object... args) {
        Map<Integer, Long> counter = fetchStatusCounter(sql, args);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("presentCount", counter.getOrDefault(STATUS_PRESENT, 0L));
        result.put("lateCount", counter.getOrDefault(STATUS_LATE, 0L));
        result.put("leaveCount", counter.getOrDefault(STATUS_LEAVE, 0L));
        result.put("absentCount", counter.getOrDefault(STATUS_ABSENT, 0L));
        result.put("makeupCount", counter.getOrDefault(STATUS_MAKEUP, 0L));
        result.put("exemptCount", counter.getOrDefault(STATUS_EXEMPT, 0L));
        result.put("pendingCount", counter.getOrDefault(STATUS_PENDING, 0L));
        long totalCount = 0L;
        for (Long value : counter.values()) {
            totalCount += value == null ? 0L : value;
        }
        result.put("totalCount", totalCount);
        result.put("attendanceRate", calculateRate(counter));
        return result;
    }

    private List<Map<String, Object>> buildDailyTrend(Long labId, LocalDate today) {
        String start = today.minusDays(6).toString();
        String end = today.toString();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT attendance_date AS attendanceDate, " +
                        "SUM(CASE WHEN status IN (?, ?, ?) THEN 1 ELSE 0 END) AS attendedCount, " +
                        "SUM(CASE WHEN status NOT IN (?, ?) THEN 1 ELSE 0 END) AS requiredCount " +
                        "FROM t_lab_attendance WHERE deleted = 0 AND lab_id = ? AND attendance_date BETWEEN ? AND ? " +
                        "GROUP BY attendance_date ORDER BY attendance_date ASC",
                STATUS_PRESENT, STATUS_LATE, STATUS_MAKEUP, STATUS_LEAVE, STATUS_EXEMPT, labId, start, end);

        Map<String, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            rowMap.put(String.valueOf(row.get("attendanceDate")), row);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            String date = today.minusDays(offset).toString();
            Map<String, Object> row = rowMap.get(date);
            long attended = row == null || !(row.get("attendedCount") instanceof Number)
                    ? 0L : ((Number) row.get("attendedCount")).longValue();
            long required = row == null || !(row.get("requiredCount") instanceof Number)
                    ? 0L : ((Number) row.get("requiredCount")).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", date);
            item.put("value", required <= 0 ? 0D : round(attended * 100.0D / required));
            result.add(item);
        }
        return result;
    }

    private double calculateRate(Map<Integer, Long> counter) {
        long attended = counter.getOrDefault(STATUS_PRESENT, 0L)
                + counter.getOrDefault(STATUS_LATE, 0L)
                + counter.getOrDefault(STATUS_MAKEUP, 0L);
        long denominator = attended
                + counter.getOrDefault(STATUS_ABSENT, 0L)
                + counter.getOrDefault(STATUS_PENDING, 0L);
        if (denominator <= 0) {
            return 100D;
        }
        return round(attended * 100.0D / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
