package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.entity.AttendanceSession;
import com.lab.recruitment.entity.AttendanceSessionRecord;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabAttendance;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AttendanceSessionMapper;
import com.lab.recruitment.mapper.AttendanceSessionRecordMapper;
import com.lab.recruitment.mapper.LabAttendanceMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AttendanceSessionService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSessionServiceImpl.class);

    private static final int ATTENDANCE_STATUS_SIGNED = 1;
    private static final int ATTENDANCE_STATUS_LATE = 2;
    private static final int ATTENDANCE_STATUS_LEAVE = 3;
    private static final int ATTENDANCE_STATUS_ABSENT = 4;
    private static final int ATTENDANCE_STATUS_MAKEUP = 5;
    private static final int ATTENDANCE_STATUS_EXEMPT = 6;

    private static final String TAG_LEAVE = "leave";
    private static final String TAG_FORGOT = "forgot";

    private static final String SESSION_STATUS_ACTIVE = "active";
    private static final String SESSION_STATUS_EXPIRED = "expired";
    private static final String SESSION_STATUS_CANCELLED = "cancelled";

    private static final String SIGN_METHOD_CODE = "code";
    private static final String SIGN_METHOD_QR = "qr";

    private static final String MEMBER_STATUS_ACTIVE = "active";
    private static final int DEFAULT_DURATION_SECONDS = 60;
    private static final int SESSION_CREATION_LOCK_TIMEOUT_SECONDS = 5;
    private static final DateTimeFormatter SESSION_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int LIGHTWEIGHT_SESSION_COLUMN_COUNT = 7;
    private static final int LIGHTWEIGHT_ATTENDANCE_COLUMN_COUNT = 4;
    private static final int LIGHTWEIGHT_RECORD_COLUMN_COUNT = 8;

    private volatile boolean lightweightSchemaReady;
    private volatile boolean lightweightSchemaMissingLogged;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceSessionRecordMapper attendanceSessionRecordMapper;

    @Autowired
    private LabAttendanceMapper labAttendanceMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Map<String, Object> createSession(Long labId, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        Long scopedLabId = resolveManageableLabId(currentUser, labId);
        if (!acquireSessionCreationLock(scopedLabId)) {
            throw new RuntimeException("Failed to acquire attendance session lock");
        }
        try {
            ensureExpiredSessionsFinalizedForLab(scopedLabId);

            AttendanceSession existing = findActiveLightweightSession(scopedLabId);
            if (existing != null) {
                return buildSessionPayload(existing, true);
            }

            LocalDateTime now = LocalDateTime.now();
            clearAttendanceSummaryForDate(scopedLabId, now.toLocalDate().toString());

            AttendanceSession session = new AttendanceSession();
            session.setLabId(scopedLabId);
            session.setSessionNo(generateSessionNo());
            session.setSignCode(generateSignCode(6));
            session.setStatus(SESSION_STATUS_ACTIVE);
            session.setStartTime(now);
            session.setExpireTime(now.plusSeconds(DEFAULT_DURATION_SECONDS));
            session.setDurationSeconds(DEFAULT_DURATION_SECONDS);
            session.setCreatedBy(currentUser.getId());
            session.setQrCodeContent(buildQrCodeContent(null));
            try {
                attendanceSessionMapper.insert(session);
            } catch (DuplicateKeyException exception) {
                AttendanceSession concurrentSession = findActiveLightweightSession(scopedLabId);
                if (concurrentSession != null) {
                    return buildSessionPayload(concurrentSession, true);
                }
                throw exception;
            }

            session.setQrCodeContent(buildQrCodeContent(session.getId()));
            attendanceSessionMapper.updateById(session);
            return buildSessionPayload(session, true);
        } finally {
            releaseSessionCreationLock(scopedLabId);
        }
    }

    @Override
    public Map<String, Object> getActiveSession(Long labId, User currentUser, boolean includeSensitiveData) {
        ensureLightweightAttendanceSchemaAvailable();
        Long scopedLabId = resolveReadableLabId(currentUser, labId);
        ensureExpiredSessionsFinalizedForLab(scopedLabId);
        AttendanceSession session = findActiveLightweightSession(scopedLabId);
        if (session == null && includeSensitiveData) {
            session = findLatestLightweightSession(scopedLabId);
        }
        if (session == null) {
            return null;
        }
        return buildSessionPayload(session, includeSensitiveData);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelSession(Long sessionId, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getManagedLightweightSession(sessionId, currentUser);
        return finalizeSession(session, SESSION_STATUS_CANCELLED, currentUser.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> finalizeSession(Long sessionId, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getManagedLightweightSession(sessionId, currentUser);
        return finalizeSession(session, SESSION_STATUS_EXPIRED, currentUser.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> refreshSessionStatus(Long sessionId, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getReadableLightweightSession(sessionId, currentUser);
        if (isExpired(session)) {
            return finalizeSession(session, SESSION_STATUS_EXPIRED, null);
        }
        return buildSessionPayload(session, !currentUserAccessor.isStudentIdentity(currentUser));
    }

    @Override
    @Transactional
    public Map<String, Object> expireAndFinalizeSession(Long sessionId, Long operatorId) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getLightweightSession(sessionId);
        return finalizeSession(session, SESSION_STATUS_EXPIRED, operatorId);
    }

    @Override
    @Transactional
    public Map<String, Object> signByCode(Long userId, String signCode, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        String normalizedCode = trimToNull(signCode);
        if (normalizedCode == null) {
            throw new RuntimeException("Sign code is required");
        }

        AttendanceSession session = findActiveSessionByCode(normalizedCode);
        if (session == null) {
            AttendanceSession historySession = findLatestLightweightSessionByCode(normalizedCode);
            if (historySession != null && isExpired(historySession)) {
                finalizeSession(historySession, SESSION_STATUS_EXPIRED, null);
                throw new RuntimeException("Session expired");
            }
            throw new RuntimeException("Invalid sign code");
        }
        return signInternal(userId, currentUser, session, SIGN_METHOD_CODE);
    }

    @Override
    @Transactional
    public Map<String, Object> signBySession(Long userId, Long sessionId, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getLightweightSession(sessionId);
        return signInternal(userId, currentUser, session, SIGN_METHOD_QR);
    }

    @Override
    public Map<String, Object> listSessionRecords(Long sessionId, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getReadableLightweightSession(sessionId, currentUser);
        if (isExpired(session)) {
            finalizeSession(session, SESSION_STATUS_EXPIRED, null);
            session = getLightweightSession(sessionId);
        }

        List<Map<String, Object>> records = listRealtimeRecords(session.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("session", buildSessionPayload(session, true));
        result.put("records", records);
        result.put("stat", buildSessionStat(session, records));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> finalizeAttendanceResult(Long sessionId, Long operatorId) {
        ensureLightweightAttendanceSchemaAvailable();
        AttendanceSession session = getLightweightSession(sessionId);
        upsertAttendanceResults(session, operatorId, LocalDateTime.now());
        return buildSessionPayload(session, true);
    }

    @Override
    @Transactional
    public Map<String, Object> tagAbsentMember(Long attendanceId, String tagType, String reason, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        LabAttendance attendance = labAttendanceMapper.selectById(attendanceId);
        if (attendance == null || !Objects.equals(attendance.getDeleted(), 0)) {
            throw new RuntimeException("Attendance record does not exist");
        }

        Long scopedLabId = resolveManageableLabId(currentUser, attendance.getLabId());
        if (!Objects.equals(scopedLabId, attendance.getLabId())) {
            throw new RuntimeException("No permission to update this attendance record");
        }

        String normalizedTagType = normalizeTagType(tagType);
        if (!Objects.equals(attendance.getStatus(), ATTENDANCE_STATUS_ABSENT)
                && !Objects.equals(attendance.getStatus(), ATTENDANCE_STATUS_LEAVE)) {
            throw new RuntimeException("Only absent members can be tagged");
        }

        attendance.setStatus(TAG_LEAVE.equals(normalizedTagType) ? ATTENDANCE_STATUS_LEAVE : ATTENDANCE_STATUS_ABSENT);
        attendance.setTagType(normalizedTagType);
        attendance.setReason(trimToNull(reason));
        attendance.setConfirmedBy(currentUser.getId());
        attendance.setConfirmTime(LocalDateTime.now());
        labAttendanceMapper.updateById(attendance);
        return buildAttendanceRow(attendance, resolveUser(attendance.getUserId()), resolveLab(attendance.getLabId()));
    }

    @Override
    public Map<String, Object> getAttendanceStat(Long labId, String date, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        Long scopedLabId = resolveManageableLabId(currentUser, labId);
        ensureExpiredSessionsFinalizedForLab(scopedLabId);
        String targetDate = normalizeDate(date, LocalDate.now());
        List<LabAttendance> attendanceList = listAttendanceByDate(scopedLabId, targetDate);
        long totalCount = attendanceList.isEmpty() ? countActiveMembers(scopedLabId) : attendanceList.size();

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("labId", scopedLabId);
        stat.put("date", targetDate);
        stat.put("totalCount", totalCount);
        stat.put("signedCount", countSignedAttendance(attendanceList));
        stat.put("leaveCount", countLeaveAttendance(attendanceList));
        stat.put("forgotCount", countForgotAttendance(attendanceList));
        stat.put("absentCount", countAbsentAttendance(attendanceList));
        return stat;
    }

    @Override
    public Map<String, Object> getAttendanceList(Long labId, String date, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        Long scopedLabId = resolveManageableLabId(currentUser, labId);
        ensureExpiredSessionsFinalizedForLab(scopedLabId);
        String targetDate = normalizeDate(date, LocalDate.now());

        List<LabAttendance> attendanceList = listAttendanceByDate(scopedLabId, targetDate);
        Map<Long, LabAttendance> attendanceMap = attendanceList.stream()
                .collect(Collectors.toMap(LabAttendance::getUserId, item -> item, (left, right) -> right, LinkedHashMap::new));

        List<Map<String, Object>> activeMembers = labMemberMapper.selectActiveMembersByLabId(scopedLabId);
        Set<Long> userIds = new LinkedHashSet<>();
        for (Map<String, Object> member : activeMembers) {
            Long userId = getLongValue(member.get("userId"));
            if (userId != null) {
                userIds.add(userId);
            }
        }
        for (LabAttendance attendance : attendanceList) {
            if (attendance.getUserId() != null) {
                userIds.add(attendance.getUserId());
            }
        }

        Lab lab = resolveLab(scopedLabId);
        Map<Long, Map<String, Object>> activeMemberMap = activeMembers.stream()
                .filter(item -> getLongValue(item.get("userId")) != null)
                .collect(Collectors.toMap(
                        item -> getLongValue(item.get("userId")),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long userId : userIds) {
            User user = resolveUser(userId);
            LabAttendance attendance = attendanceMap.get(userId);
            Map<String, Object> member = activeMemberMap.get(userId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attendanceId", attendance == null ? null : attendance.getId());
            row.put("userId", userId);
            row.put("realName", user == null ? memberValue(member, "realName") : user.getRealName());
            row.put("studentId", user == null ? memberValue(member, "studentId") : user.getStudentId());
            row.put("college", user == null ? null : user.getCollege());
            row.put("labName", lab.getLabName());
            row.put("date", targetDate);
            row.put("status", attendance == null ? null : attendance.getStatus());
            row.put("statusLabel", statusLabel(attendance == null ? null : attendance.getStatus()));
            row.put("checkinTime", attendance == null ? null : attendance.getCheckinTime());
            row.put("tagType", attendance == null ? null : attendance.getTagType());
            row.put("tagLabel", tagLabel(attendance == null ? null : attendance.getTagType()));
            row.put("reason", attendance == null ? null : attendance.getReason());
            row.put("confirmTime", attendance == null ? null : attendance.getConfirmTime());
            rows.add(row);
        }

        rows.sort(Comparator
                .comparing((Map<String, Object> item) -> stringValue(item.get("studentId")))
                .thenComparing(item -> stringValue(item.get("realName"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labId", scopedLabId);
        result.put("date", targetDate);
        result.put("rows", rows);
        result.put("stat", getAttendanceStat(scopedLabId, targetDate, currentUser));
        return result;
    }

    @Override
    @Transactional
    public byte[] exportAttendanceExcel(Long labId, String startDate, String endDate, User currentUser) {
        ensureLightweightAttendanceSchemaAvailable();
        Long scopedLabId = resolveManageableLabId(currentUser, labId);
        ensureExpiredSessionsFinalizedForLab(scopedLabId);

        LocalDate start = parseDate(startDate, LocalDate.now());
        LocalDate end = parseDate(endDate, start);
        if (start.isAfter(end)) {
            throw new RuntimeException("Start date cannot be later than end date");
        }

        List<LabAttendance> attendanceList = listAttendanceByDateRange(scopedLabId, start.toString(), end.toString());
        Lab lab = resolveLab(scopedLabId);
        Map<Long, User> userMap = loadUsers(attendanceList.stream()
                .map(LabAttendance::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Attendance");
            writeExportHeader(workbook, sheet);

            int rowIndex = 1;
            for (LabAttendance attendance : attendanceList) {
                User user = userMap.get(attendance.getUserId());
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, user == null ? "" : user.getRealName());
                writeCell(row, 1, user == null ? "" : user.getStudentId());
                writeCell(row, 2, user == null ? "" : user.getCollege());
                writeCell(row, 3, lab.getLabName());
                writeCell(row, 4, attendance.getAttendanceDate());
                writeCell(row, 5, statusLabel(attendance.getStatus()));
                writeCell(row, 6, formatDateTime(attendance.getCheckinTime()));
                writeCell(row, 7, tagLabel(attendance.getTagType()));
                writeCell(row, 8, attendance.getReason());
                attendance.setExportFlag(1);
                labAttendanceMapper.updateById(attendance);
            }

            for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
                sheet.setColumnWidth(columnIndex, columnIndex == 8 ? 28 * 256 : 18 * 256);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to export attendance excel");
        }
    }

    @Scheduled(fixedDelay = 10000L)
    public void finalizeExpiredSessionsJob() {
        if (!isLightweightAttendanceSchemaReady()) {
            if (!lightweightSchemaMissingLogged) {
                log.warn("Skip lightweight attendance session finalizer because required schema is not ready");
                lightweightSchemaMissingLogged = true;
            }
            return;
        }
        lightweightSchemaMissingLogged = false;

        QueryWrapper<AttendanceSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("status", SESSION_STATUS_ACTIVE)
                .isNotNull("session_no")
                .le("expire_time", LocalDateTime.now())
                .orderByAsc("id")
                .last("LIMIT 100");
        List<AttendanceSession> sessionList = attendanceSessionMapper.selectList(queryWrapper);
        for (AttendanceSession session : sessionList) {
            try {
                expireAndFinalizeSession(session.getId(), null);
            } catch (Exception exception) {
                log.warn("Failed to finalize attendance session {}, reason={}", session.getId(), exception.getMessage());
            }
        }
    }

    private boolean isLightweightAttendanceSchemaReady() {
        if (lightweightSchemaReady) {
            return true;
        }
        try {
            Integer sessionColumnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_attendance_session' " +
                            "AND COLUMN_NAME IN ('session_no', 'sign_code', 'qr_code_content', 'start_time', 'expire_time', 'duration_seconds', 'created_by')",
                    Integer.class
            );
            Integer attendanceColumnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_lab_attendance' " +
                            "AND COLUMN_NAME IN ('session_id', 'checkin_time', 'tag_type', 'export_flag')",
                    Integer.class
            );
            Integer recordColumnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_attendance_session_record' " +
                            "AND COLUMN_NAME IN ('session_id', 'lab_id', 'user_id', 'sign_time', 'sign_method', 'sign_code_snapshot', 'create_time', 'deleted')",
                    Integer.class
            );
            boolean ready = Objects.equals(sessionColumnCount, LIGHTWEIGHT_SESSION_COLUMN_COUNT)
                    && Objects.equals(attendanceColumnCount, LIGHTWEIGHT_ATTENDANCE_COLUMN_COUNT)
                    && Objects.equals(recordColumnCount, LIGHTWEIGHT_RECORD_COLUMN_COUNT);
            if (ready) {
                lightweightSchemaReady = true;
            }
            return ready;
        } catch (Exception exception) {
            log.warn("Failed to inspect lightweight attendance schema, reason={}", exception.getMessage());
            return false;
        }
    }

    private void ensureLightweightAttendanceSchemaAvailable() {
        if (isLightweightAttendanceSchemaReady()) {
            return;
        }
        throw new RuntimeException("Attendance schema is not ready. Please enable schema migration and restart the service.");
    }

    private Map<String, Object> signInternal(Long userId, User currentUser, AttendanceSession session, String signMethod) {
        ensureStudentMemberOfLab(currentUser, session.getLabId());
        if (isExpired(session)) {
            finalizeSession(session, SESSION_STATUS_EXPIRED, null);
            throw new RuntimeException("Session expired");
        }
        if (!SESSION_STATUS_ACTIVE.equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Session expired");
        }

        AttendanceSessionRecord existingRecord = findSessionRecord(session.getId(), userId);
        if (existingRecord != null) {
            throw new RuntimeException("Already signed in this session");
        }

        AttendanceSessionRecord record = new AttendanceSessionRecord();
        record.setSessionId(session.getId());
        record.setLabId(session.getLabId());
        record.setUserId(userId);
        record.setSignTime(LocalDateTime.now());
        record.setSignMethod(signMethod);
        record.setSignCodeSnapshot(session.getSignCode());
        try {
            attendanceSessionRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            throw new RuntimeException("Already signed in this session");
        }
        upsertRealtimeSignedAttendance(session, userId, record.getSignTime());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("signTime", record.getSignTime());
        result.put("signMethod", record.getSignMethod());
        result.put("message", "Sign in success");
        return result;
    }

    private void upsertRealtimeSignedAttendance(AttendanceSession session, Long userId, LocalDateTime signTime) {
        if (session == null || session.getLabId() == null || userId == null) {
            return;
        }
        String attendanceDate = resolveAttendanceDate(session);
        LocalDateTime now = signTime == null ? LocalDateTime.now() : signTime;
        LabAttendance attendance = findAttendance(session.getLabId(), userId, attendanceDate);
        if (attendance == null) {
            attendance = new LabAttendance();
            attendance.setLabId(session.getLabId());
            attendance.setUserId(userId);
            attendance.setAttendanceDate(attendanceDate);
            attendance.setExportFlag(0);
        }
        attendance.setSessionId(session.getId());
        attendance.setStatus(ATTENDANCE_STATUS_SIGNED);
        attendance.setCheckinTime(now);
        attendance.setTagType(null);
        attendance.setReason(null);
        attendance.setConfirmedBy(userId);
        attendance.setConfirmTime(now);
        attendance.setDeleted(0);

        if (attendance.getId() == null) {
            try {
                labAttendanceMapper.insert(attendance);
            } catch (DuplicateKeyException exception) {
                restoreSoftDeletedAttendance(attendance, now);
            }
            return;
        }
        labAttendanceMapper.updateById(attendance);
    }

    @Transactional
    protected Map<String, Object> finalizeSession(AttendanceSession session, String targetStatus, Long operatorId) {
        if (session == null || session.getId() == null) {
            throw new RuntimeException("Attendance session does not exist");
        }
        AttendanceSession latestSession = attendanceSessionMapper.selectById(session.getId());
        if (latestSession == null || !Objects.equals(latestSession.getDeleted(), 0)) {
            throw new RuntimeException("Attendance session does not exist");
        }

        if (SESSION_STATUS_EXPIRED.equalsIgnoreCase(latestSession.getStatus())
                || SESSION_STATUS_CANCELLED.equalsIgnoreCase(latestSession.getStatus())) {
            return buildSessionPayload(latestSession, true);
        }

        LocalDateTime now = LocalDateTime.now();
        upsertAttendanceResults(latestSession, operatorId, now);
        latestSession.setStatus(targetStatus);
        if (SESSION_STATUS_CANCELLED.equals(targetStatus)
                && (latestSession.getExpireTime() == null || latestSession.getExpireTime().isAfter(now))) {
            latestSession.setExpireTime(now);
        }
        attendanceSessionMapper.updateById(latestSession);
        return buildSessionPayload(latestSession, true);
    }

    private void upsertAttendanceResults(AttendanceSession session, Long operatorId, LocalDateTime now) {
        String attendanceDate = resolveAttendanceDate(session);
        List<Map<String, Object>> memberList = labMemberMapper.selectActiveMembersByLabId(session.getLabId());
        Map<Long, AttendanceSessionRecord> signedRecordMap = listSessionRecordEntities(session.getId()).stream()
                .collect(Collectors.toMap(AttendanceSessionRecord::getUserId, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (Map<String, Object> member : memberList) {
            Long userId = getLongValue(member.get("userId"));
            if (userId == null) {
                continue;
            }

            AttendanceSessionRecord sessionRecord = signedRecordMap.get(userId);
            LabAttendance attendance = findAttendance(session.getLabId(), userId, attendanceDate);
            if (attendance == null) {
                attendance = new LabAttendance();
                attendance.setLabId(session.getLabId());
                attendance.setUserId(userId);
                attendance.setAttendanceDate(attendanceDate);
                attendance.setExportFlag(0);
            }

            attendance.setSessionId(session.getId());
            if (sessionRecord != null) {
                attendance.setStatus(ATTENDANCE_STATUS_SIGNED);
                attendance.setCheckinTime(sessionRecord.getSignTime());
                attendance.setTagType(null);
                attendance.setReason(null);
                attendance.setConfirmedBy(operatorId);
                attendance.setConfirmTime(now);
            } else {
                attendance.setStatus(ATTENDANCE_STATUS_ABSENT);
                attendance.setCheckinTime(null);
                attendance.setTagType(null);
                attendance.setReason(null);
                attendance.setConfirmedBy(operatorId);
                attendance.setConfirmTime(now);
            }
            attendance.setDeleted(0);

            if (attendance.getId() == null) {
                try {
                    labAttendanceMapper.insert(attendance);
                } catch (DuplicateKeyException exception) {
                    restoreSoftDeletedAttendance(attendance, now);
                }
            } else if (Objects.equals(attendance.getDeleted(), 1)) {
                updateAttendanceIncludingDeleted(attendance, now);
            } else {
                labAttendanceMapper.updateById(attendance);
            }
        }
    }

    private AttendanceSession findActiveLightweightSession(Long labId) {
        QueryWrapper<AttendanceSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("lab_id", labId)
                .eq("status", SESSION_STATUS_ACTIVE)
                .isNotNull("session_no")
                .orderByDesc("id")
                .last("LIMIT 1");
        AttendanceSession session = attendanceSessionMapper.selectOne(queryWrapper);
        if (session != null && isExpired(session)) {
            finalizeSession(session, SESSION_STATUS_EXPIRED, null);
            return null;
        }
        return session;
    }

    private AttendanceSession findLatestLightweightSession(Long labId) {
        QueryWrapper<AttendanceSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("lab_id", labId)
                .isNotNull("session_no")
                .orderByDesc("id")
                .last("LIMIT 1");
        return attendanceSessionMapper.selectOne(queryWrapper);
    }

    private AttendanceSession findActiveSessionByCode(String signCode) {
        QueryWrapper<AttendanceSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("status", SESSION_STATUS_ACTIVE)
                .eq("sign_code", signCode)
                .isNotNull("session_no")
                .orderByDesc("id")
                .last("LIMIT 1");
        return attendanceSessionMapper.selectOne(queryWrapper);
    }

    private AttendanceSession findLatestLightweightSessionByCode(String signCode) {
        QueryWrapper<AttendanceSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("sign_code", signCode)
                .isNotNull("session_no")
                .orderByDesc("id")
                .last("LIMIT 1");
        return attendanceSessionMapper.selectOne(queryWrapper);
    }

    private AttendanceSession getLightweightSession(Long sessionId) {
        if (sessionId == null) {
            throw new RuntimeException("Session id is required");
        }
        AttendanceSession session = attendanceSessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getDeleted(), 0) || !StringUtils.hasText(session.getSessionNo())) {
            throw new RuntimeException("Attendance session does not exist");
        }
        return session;
    }

    private AttendanceSession getManagedLightweightSession(Long sessionId, User currentUser) {
        AttendanceSession session = getLightweightSession(sessionId);
        resolveManageableLabId(currentUser, session.getLabId());
        return session;
    }

    private AttendanceSession getReadableLightweightSession(Long sessionId, User currentUser) {
        AttendanceSession session = getLightweightSession(sessionId);
        Long scopedLabId = resolveReadableLabId(currentUser, session.getLabId());
        if (!Objects.equals(scopedLabId, session.getLabId())) {
            throw new RuntimeException("No permission to access this attendance session");
        }
        return session;
    }

    private Long resolveManageableLabId(User currentUser, Long requestedLabId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("Current user is required");
        }
        return currentUserAccessor.resolveLabScope(currentUser, requestedLabId);
    }

    private Long resolveReadableLabId(User currentUser, Long requestedLabId) {
        if (currentUserAccessor.isStudentIdentity(currentUser)) {
            Long ownLabId = resolveCurrentUserLabId(currentUser);
            if (ownLabId == null) {
                throw new RuntimeException("You are not an active member of any lab");
            }
            if (requestedLabId != null && !Objects.equals(ownLabId, requestedLabId)) {
                throw new RuntimeException("No permission to access another lab");
            }
            return ownLabId;
        }
        return currentUserAccessor.resolveLabScope(currentUser, requestedLabId);
    }

    private Long resolveCurrentUserLabId(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        Long managedLabId = userAccessService.resolveManagedLabId(currentUser);
        if (managedLabId != null) {
            return managedLabId;
        }
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentUser.getId())
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .orderByAsc("id")
                .last("LIMIT 1");
        LabMember labMember = labMemberMapper.selectOne(queryWrapper);
        return labMember == null ? null : labMember.getLabId();
    }

    private void ensureStudentMemberOfLab(User currentUser, Long labId) {
        Long ownLabId = resolveCurrentUserLabId(currentUser);
        if (ownLabId == null || !Objects.equals(ownLabId, labId)) {
            throw new RuntimeException("You do not belong to this lab");
        }
    }

    private void ensureExpiredSessionsFinalizedForLab(Long labId) {
        if (labId == null) {
            return;
        }
        QueryWrapper<AttendanceSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .eq("lab_id", labId)
                .eq("status", SESSION_STATUS_ACTIVE)
                .isNotNull("session_no")
                .le("expire_time", LocalDateTime.now());
        List<AttendanceSession> expiredSessions = attendanceSessionMapper.selectList(queryWrapper);
        for (AttendanceSession session : expiredSessions) {
            finalizeSession(session, SESSION_STATUS_EXPIRED, null);
        }
    }

    private void clearAttendanceSummaryForDate(Long labId, String attendanceDate) {
        if (labId == null || !StringUtils.hasText(attendanceDate)) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE t_lab_attendance " +
                        "SET deleted = 1, update_time = NOW() " +
                        "WHERE deleted = 0 AND lab_id = ? AND attendance_date = ?",
                labId,
                attendanceDate
        );
    }

    private List<Map<String, Object>> listRealtimeRecords(Long sessionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT r.id, r.user_id AS userId, u.real_name AS realName, u.student_id AS studentId, " +
                        "r.sign_time AS signTime, r.sign_method AS signMethod " +
                        "FROM t_attendance_session_record r " +
                        "LEFT JOIN t_user u ON u.id = r.user_id AND u.deleted = 0 " +
                        "WHERE r.deleted = 0 AND r.session_id = ? " +
                        "ORDER BY r.sign_time ASC, r.id ASC",
                sessionId
        );
        for (Map<String, Object> row : rows) {
            row.put("status", ATTENDANCE_STATUS_SIGNED);
            row.put("statusLabel", statusLabel(ATTENDANCE_STATUS_SIGNED));
        }
        return rows;
    }

    private Map<String, Object> buildSessionStat(AttendanceSession session, List<Map<String, Object>> records) {
        long totalCount = countActiveMembers(session.getLabId());
        if (SESSION_STATUS_EXPIRED.equalsIgnoreCase(session.getStatus())
                || SESSION_STATUS_CANCELLED.equalsIgnoreCase(session.getStatus())) {
            List<LabAttendance> attendanceList = listAttendanceByDate(session.getLabId(), resolveAttendanceDate(session));
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("totalCount", attendanceList.isEmpty() ? totalCount : attendanceList.size());
            stat.put("signedCount", countSignedAttendance(attendanceList));
            stat.put("leaveCount", countLeaveAttendance(attendanceList));
            stat.put("forgotCount", countForgotAttendance(attendanceList));
            stat.put("absentCount", countAbsentAttendance(attendanceList));
            return stat;
        }

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("totalCount", totalCount);
        stat.put("signedCount", records.size());
        stat.put("leaveCount", 0L);
        stat.put("forgotCount", 0L);
        stat.put("absentCount", Math.max(totalCount - records.size(), 0L));
        return stat;
    }

    private Map<String, Object> buildSessionPayload(AttendanceSession session, boolean includeSensitiveData) {
        Map<String, Object> result = new LinkedHashMap<>();
        long remainingSeconds = remainingSeconds(session);
        result.put("id", session.getId());
        result.put("labId", session.getLabId());
        result.put("sessionNo", session.getSessionNo());
        result.put("status", session.getStatus());
        result.put("startTime", session.getStartTime());
        result.put("expireTime", session.getExpireTime());
        result.put("durationSeconds", session.getDurationSeconds());
        result.put("remainingSeconds", remainingSeconds);
        if (includeSensitiveData && SESSION_STATUS_ACTIVE.equalsIgnoreCase(session.getStatus()) && remainingSeconds > 0) {
            result.put("signCode", session.getSignCode());
            result.put("qrCodeContent", session.getQrCodeContent());
        }
        return result;
    }

    private Map<String, Object> buildAttendanceRow(LabAttendance attendance, User user, Lab lab) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attendanceId", attendance == null ? null : attendance.getId());
        result.put("userId", attendance == null ? null : attendance.getUserId());
        result.put("realName", user == null ? null : user.getRealName());
        result.put("studentId", user == null ? null : user.getStudentId());
        result.put("college", user == null ? null : user.getCollege());
        result.put("labName", lab == null ? null : lab.getLabName());
        result.put("date", attendance == null ? null : attendance.getAttendanceDate());
        result.put("status", attendance == null ? null : attendance.getStatus());
        result.put("statusLabel", attendance == null ? null : statusLabel(attendance.getStatus()));
        result.put("checkinTime", attendance == null ? null : attendance.getCheckinTime());
        result.put("tagType", attendance == null ? null : attendance.getTagType());
        result.put("tagLabel", attendance == null ? null : tagLabel(attendance.getTagType()));
        result.put("reason", attendance == null ? null : attendance.getReason());
        result.put("confirmTime", attendance == null ? null : attendance.getConfirmTime());
        return result;
    }

    private List<AttendanceSessionRecord> listSessionRecordEntities(Long sessionId) {
        QueryWrapper<AttendanceSessionRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                .eq("deleted", 0)
                .orderByAsc("sign_time")
                .orderByAsc("id");
        return attendanceSessionRecordMapper.selectList(queryWrapper);
    }

    private AttendanceSessionRecord findSessionRecord(Long sessionId, Long userId) {
        QueryWrapper<AttendanceSessionRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return attendanceSessionRecordMapper.selectOne(queryWrapper);
    }

    private LabAttendance findAttendance(Long labId, Long userId, String attendanceDate) {
        QueryWrapper<LabAttendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("attendance_date", attendanceDate)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return labAttendanceMapper.selectOne(queryWrapper);
    }

    private void restoreSoftDeletedAttendance(LabAttendance attendance, LocalDateTime now) {
        List<Long> existingIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_lab_attendance " +
                        "WHERE lab_id = ? AND user_id = ? AND attendance_date = ? " +
                        "ORDER BY deleted ASC, id DESC LIMIT 1",
                Long.class,
                attendance.getLabId(),
                attendance.getUserId(),
                attendance.getAttendanceDate()
        );
        if (existingIds.isEmpty()) {
            throw new DuplicateKeyException("Duplicate attendance key but existing attendance row was not found");
        }
        attendance.setId(existingIds.get(0));
        updateAttendanceIncludingDeleted(attendance, now);
    }

    private void updateAttendanceIncludingDeleted(LabAttendance attendance, LocalDateTime now) {
        int updated = jdbcTemplate.update(
                "UPDATE t_lab_attendance SET " +
                        "session_id = ?, checkin_time = ?, status = ?, tag_type = ?, reason = ?, " +
                        "confirmed_by = ?, confirm_time = ?, export_flag = ?, deleted = 0, update_time = ? " +
                        "WHERE id = ?",
                attendance.getSessionId(),
                attendance.getCheckinTime(),
                attendance.getStatus(),
                attendance.getTagType(),
                attendance.getReason(),
                attendance.getConfirmedBy(),
                attendance.getConfirmTime(),
                attendance.getExportFlag(),
                now,
                attendance.getId()
        );
        if (updated == 0) {
            throw new RuntimeException("Failed to restore attendance row");
        }
    }

    private List<LabAttendance> listAttendanceByDate(Long labId, String attendanceDate) {
        QueryWrapper<LabAttendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("attendance_date", attendanceDate)
                .eq("deleted", 0)
                .orderByAsc("user_id");
        return labAttendanceMapper.selectList(queryWrapper);
    }

    private List<LabAttendance> listAttendanceByDateRange(Long labId, String startDate, String endDate) {
        QueryWrapper<LabAttendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .between("attendance_date", startDate, endDate)
                .orderByAsc("attendance_date")
                .orderByAsc("user_id");
        return labAttendanceMapper.selectList(queryWrapper);
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", userIds)
                .eq("deleted", 0);
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private long countActiveMembers(Long labId) {
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE);
        return labMemberMapper.selectCount(queryWrapper);
    }

    private long countByStatus(List<LabAttendance> attendanceList, Integer status) {
        return attendanceList.stream()
                .filter(item -> Objects.equals(item.getStatus(), status))
                .count();
    }

    private long countSignedAttendance(List<LabAttendance> attendanceList) {
        return attendanceList.stream()
                .filter(this::isSignedAttendance)
                .count();
    }

    private long countLeaveAttendance(List<LabAttendance> attendanceList) {
        return attendanceList.stream()
                .filter(item -> Objects.equals(item.getStatus(), ATTENDANCE_STATUS_LEAVE) || hasTag(item, TAG_LEAVE))
                .count();
    }

    private long countForgotAttendance(List<LabAttendance> attendanceList) {
        return attendanceList.stream()
                .filter(item -> hasTag(item, TAG_FORGOT))
                .count();
    }

    private long countAbsentAttendance(List<LabAttendance> attendanceList) {
        return attendanceList.stream()
                .filter(item -> Objects.equals(item.getStatus(), ATTENDANCE_STATUS_ABSENT))
                .filter(item -> !hasTag(item, TAG_LEAVE) && !hasTag(item, TAG_FORGOT))
                .count();
    }

    private boolean isSignedAttendance(LabAttendance attendance) {
        if (attendance == null) {
            return false;
        }
        Integer status = attendance.getStatus();
        return Objects.equals(status, ATTENDANCE_STATUS_SIGNED)
                || Objects.equals(status, ATTENDANCE_STATUS_LATE)
                || Objects.equals(status, ATTENDANCE_STATUS_MAKEUP);
    }

    private boolean hasTag(LabAttendance attendance, String tagType) {
        return attendance != null
                && StringUtils.hasText(attendance.getTagType())
                && tagType.equalsIgnoreCase(attendance.getTagType());
    }

    private boolean isExpired(AttendanceSession session) {
        if (session == null) {
            return true;
        }
        if (SESSION_STATUS_CANCELLED.equalsIgnoreCase(session.getStatus())
                || SESSION_STATUS_EXPIRED.equalsIgnoreCase(session.getStatus())) {
            return true;
        }
        return session.getExpireTime() != null && !session.getExpireTime().isAfter(LocalDateTime.now());
    }

    private long remainingSeconds(AttendanceSession session) {
        if (session == null || session.getExpireTime() == null || !SESSION_STATUS_ACTIVE.equalsIgnoreCase(session.getStatus())) {
            return 0L;
        }
        return Math.max(Duration.between(LocalDateTime.now(), session.getExpireTime()).getSeconds(), 0L);
    }

    private String normalizeTagType(String tagType) {
        String normalized = trimToNull(tagType);
        if (normalized == null) {
            throw new RuntimeException("Tag type is required");
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!TAG_LEAVE.equals(normalized) && !TAG_FORGOT.equals(normalized)) {
            throw new RuntimeException("Unsupported tag type");
        }
        return normalized;
    }

    private String generateSessionNo() {
        return "AS" + LocalDateTime.now().format(SESSION_NO_TIME_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private boolean acquireSessionCreationLock(Long labId) {
        Integer lockResult = jdbcTemplate.queryForObject(
                "SELECT GET_LOCK(?, ?)",
                Integer.class,
                buildSessionCreationLockName(labId),
                SESSION_CREATION_LOCK_TIMEOUT_SECONDS
        );
        return Objects.equals(lockResult, 1);
    }

    private void releaseSessionCreationLock(Long labId) {
        if (labId == null) {
            return;
        }
        try {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, buildSessionCreationLockName(labId));
        } catch (Exception exception) {
            log.warn("Failed to release attendance session lock for lab {}, reason={}", labId, exception.getMessage());
        }
    }

    private String buildSessionCreationLockName(Long labId) {
        return "attendance_session_create_" + labId;
    }

    private String generateSignCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    private String buildQrCodeContent(Long sessionId) {
        if (sessionId == null) {
            return "/m/student/attendance";
        }
        return "/m/student/attendance?sessionId=" + sessionId;
    }

    private String resolveAttendanceDate(AttendanceSession session) {
        if (session.getStartTime() != null) {
            return session.getStartTime().toLocalDate().toString();
        }
        if (session.getExpireTime() != null) {
            return session.getExpireTime().toLocalDate().toString();
        }
        return LocalDate.now().toString();
    }

    private String normalizeDate(String date, LocalDate defaultDate) {
        return parseDate(date, defaultDate).toString();
    }

    private LocalDate parseDate(String date, LocalDate defaultDate) {
        String normalized = trimToNull(date);
        return normalized == null ? defaultDate : LocalDate.parse(normalized);
    }

    private Lab resolveLab(Long labId) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null || !Objects.equals(lab.getDeleted(), 0)) {
            throw new RuntimeException("Lab does not exist");
        }
        return lab;
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getDeleted(), 0)) {
            return null;
        }
        return user;
    }

    private void writeExportHeader(XSSFWorkbook workbook, XSSFSheet sheet) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor((short) 22);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);

        String[] headers = {"姓名", "学号", "学院", "实验室", "日期", "状态", "签到时间", "标签类型", "备注"};
        Row headerRow = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(style);
        }
    }

    private void writeCell(Row row, int columnIndex, String value) {
        row.createCell(columnIndex).setCellValue(value == null ? "" : value);
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case ATTENDANCE_STATUS_SIGNED:
                return "已签到";
            case ATTENDANCE_STATUS_LATE:
                return "迟到";
            case ATTENDANCE_STATUS_LEAVE:
                return "请假";
            case ATTENDANCE_STATUS_ABSENT:
                return "缺勤";
            case ATTENDANCE_STATUS_MAKEUP:
                return "补签";
            case ATTENDANCE_STATUS_EXEMPT:
                return "免签到";
            default:
                return "未知";
        }
    }

    private String tagLabel(String tagType) {
        if (!StringUtils.hasText(tagType)) {
            return "";
        }
        if (TAG_LEAVE.equalsIgnoreCase(tagType)) {
            return "请假";
        }
        if (TAG_FORGOT.equalsIgnoreCase(tagType)) {
            return "忘记签到";
        }
        return tagType;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String memberValue(Map<String, Object> member, String key) {
        return member == null || member.get(key) == null ? null : String.valueOf(member.get(key));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
