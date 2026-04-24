package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.config.FileStorageService;
import com.lab.recruitment.dto.AttendanceLeaveApplyDTO;
import com.lab.recruitment.dto.AttendanceLeaveReviewDTO;
import com.lab.recruitment.dto.AttendanceMakeupRequestDTO;
import com.lab.recruitment.dto.AttendanceDutyUpsertDTO;
import com.lab.recruitment.dto.AttendanceRecordReviewDTO;
import com.lab.recruitment.dto.AttendanceScheduleDTO;
import com.lab.recruitment.dto.AttendanceSignInDTO;
import com.lab.recruitment.dto.AttendanceTaskUpsertDTO;
import com.lab.recruitment.entity.AttendanceChangeLog;
import com.lab.recruitment.entity.AttendanceLeave;
import com.lab.recruitment.entity.AttendancePhoto;
import com.lab.recruitment.entity.AttendanceRecord;
import com.lab.recruitment.entity.AttendanceSchedule;
import com.lab.recruitment.entity.AttendanceSession;
import com.lab.recruitment.entity.AttendanceTask;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AttendanceChangeLogMapper;
import com.lab.recruitment.mapper.AttendanceLeaveMapper;
import com.lab.recruitment.mapper.AttendancePhotoMapper;
import com.lab.recruitment.mapper.AttendanceRecordMapper;
import com.lab.recruitment.mapper.AttendanceScheduleMapper;
import com.lab.recruitment.mapper.AttendanceSessionMapper;
import com.lab.recruitment.mapper.AttendanceTaskMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.service.AttendanceWorkflowService;
import com.lab.recruitment.service.IdempotencyLockService;
import com.lab.recruitment.service.StatisticsRefreshService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.UserAccessProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AttendanceWorkflowServiceImpl implements AttendanceWorkflowService {

    private static final String TASK_STATUS_DRAFT = "draft";
    private static final String TASK_STATUS_PUBLISHED = "published";
    private static final String SESSION_STATUS_PENDING = "pending";
    private static final String SESSION_STATUS_ACTIVE = "active";
    private static final String SESSION_STATUS_CLOSED = "closed";
    private static final String RECORD_STATUS_NORMAL = "normal";
    private static final String RECORD_STATUS_LATE = "late";
    private static final String RECORD_STATUS_LEAVE = "leave";
    private static final String RECORD_STATUS_ABSENT = "absent";
    private static final String RECORD_STATUS_MAKEUP_PENDING = "makeup_pending";
    private static final String RECORD_STATUS_MAKEUP_APPROVED = "makeup_approved";
    private static final String RECORD_STATUS_MAKEUP_REJECTED = "makeup_rejected";
    private static final String LEAVE_STATUS_PENDING = "PENDING";
    private static final String LEAVE_STATUS_APPROVED = "APPROVED";
    private static final String LEAVE_STATUS_REJECTED = "REJECTED";
    private static final String SOURCE_STUDENT = "student";
    private static final String SOURCE_ADMIN = "admin";
    private static final String SOURCE_SYSTEM = "system";
    private static final String MEMBER_STATUS_ACTIVE = "active";
    private static final String MEMBER_ROLE_LAB_ADMIN = "lab_admin";
    private static final String SESSION_CODE_PLACEHOLDER = "";
    private static final Duration SIGN_IN_LOCK_TTL = Duration.ofSeconds(60);

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private AttendanceTaskMapper attendanceTaskMapper;

    @Autowired
    private AttendanceScheduleMapper attendanceScheduleMapper;

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private AttendanceLeaveMapper attendanceLeaveMapper;

    @Autowired
    private AttendanceChangeLogMapper attendanceChangeLogMapper;

    @Autowired
    private AttendancePhotoMapper attendancePhotoMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private StatisticsRefreshService statisticsRefreshService;

    @Autowired
    private IdempotencyLockService idempotencyLockService;

    @Override
    public Page<Map<String, Object>> getTaskPage(Integer pageNum, Integer pageSize, Long collegeId, String keyword, User currentUser) {
        Long scopedCollegeId = resolveTaskScopeCollegeId(collegeId, currentUser);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        String keywordLike = trimToNull(keyword);
        List<Object> args = new ArrayList<>();
        StringBuilder whereSql = new StringBuilder(" WHERE t.deleted = 0");
        if (scopedCollegeId != null) {
            whereSql.append(" AND t.college_id = ?");
            args.add(scopedCollegeId);
        }
        if (keywordLike != null) {
            whereSql.append(" AND (t.semester_name LIKE ? OR t.task_name LIKE ?)");
            args.add("%" + keywordLike + "%");
            args.add("%" + keywordLike + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance_task t" + whereSql,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT t.id, t.college_id AS collegeId, c.college_name AS collegeName, " +
                        "t.semester_name AS semesterName, t.task_name AS taskName, t.description, " +
                        "t.start_date AS startDate, t.end_date AS endDate, t.status, " +
                        "t.published_by AS publishedBy, t.published_time AS publishedTime, " +
                        "t.created_by AS createdBy, t.create_time AS createTime, " +
                        "(SELECT COUNT(*) FROM t_attendance_schedule s WHERE s.task_id = t.id AND s.deleted = 0) AS scheduleCount " +
                        "FROM t_attendance_task t " +
                        "LEFT JOIN t_college c ON c.id = t.college_id AND c.deleted = 0" +
                        whereSql +
                        " ORDER BY t.create_time DESC, t.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    @Transactional
    public Map<String, Object> saveTask(AttendanceTaskUpsertDTO taskDTO, User currentUser) {
        Long scopedCollegeId = resolveTaskScopeCollegeId(taskDTO.getCollegeId(), currentUser);
        if (!StringUtils.hasText(taskDTO.getSemesterName())) {
            throw new RuntimeException("Semester name is required");
        }
        if (!StringUtils.hasText(taskDTO.getTaskName())) {
            throw new RuntimeException("Task name is required");
        }
        if (taskDTO.getStartDate() == null || taskDTO.getEndDate() == null) {
            throw new RuntimeException("Task date range is required");
        }
        if (taskDTO.getStartDate().isAfter(taskDTO.getEndDate())) {
            throw new RuntimeException("Task start date cannot be later than the end date");
        }

        AttendanceTask task;
        if (taskDTO.getId() == null) {
            task = new AttendanceTask();
            task.setCollegeId(scopedCollegeId);
            task.setSemesterName(taskDTO.getSemesterName().trim());
            task.setTaskName(taskDTO.getTaskName().trim());
            task.setDescription(trimToNull(taskDTO.getDescription()));
            task.setStartDate(taskDTO.getStartDate());
            task.setEndDate(taskDTO.getEndDate());
            task.setStatus(TASK_STATUS_DRAFT);
            task.setCreatedBy(currentUser.getId());
            attendanceTaskMapper.insert(task);
        } else {
            task = attendanceTaskMapper.selectById(taskDTO.getId());
            if (task == null || !Objects.equals(task.getDeleted(), 0)) {
                throw new RuntimeException("Attendance task does not exist");
            }
            assertTaskScope(task, currentUser);
            task.setCollegeId(scopedCollegeId);
            task.setSemesterName(taskDTO.getSemesterName().trim());
            task.setTaskName(taskDTO.getTaskName().trim());
            task.setDescription(trimToNull(taskDTO.getDescription()));
            task.setStartDate(taskDTO.getStartDate());
            task.setEndDate(taskDTO.getEndDate());
            attendanceTaskMapper.updateById(task);
        }
        return buildTaskMap(task);
    }

    @Override
    @Transactional
    public boolean publishTask(Long taskId, User currentUser) {
        AttendanceTask task = attendanceTaskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getDeleted(), 0)) {
            throw new RuntimeException("Attendance task does not exist");
        }
        assertTaskScope(task, currentUser);
        Integer scheduleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance_schedule WHERE task_id = ? AND deleted = 0 AND status = 1",
                Integer.class,
                taskId
        );
        if (scheduleCount == null || scheduleCount <= 0) {
            throw new RuntimeException("At least one enabled schedule is required before publishing");
        }
        task.setStatus(TASK_STATUS_PUBLISHED);
        task.setPublishedBy(currentUser.getId());
        task.setPublishedTime(LocalDateTime.now());
        return attendanceTaskMapper.updateById(task) > 0;
    }

    @Override
    public List<AttendanceSchedule> getTaskSchedules(Long taskId, User currentUser) {
        AttendanceTask task = attendanceTaskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getDeleted(), 0)) {
            throw new RuntimeException("Attendance task does not exist");
        }
        assertTaskScope(task, currentUser);

        QueryWrapper<AttendanceSchedule> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId).eq("deleted", 0).orderByAsc("week_day");
        return attendanceScheduleMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public List<AttendanceSchedule> saveTaskSchedules(Long taskId, List<AttendanceScheduleDTO> schedules, User currentUser) {
        AttendanceTask task = attendanceTaskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getDeleted(), 0)) {
            throw new RuntimeException("Attendance task does not exist");
        }
        assertTaskScope(task, currentUser);
        if (schedules == null || schedules.isEmpty()) {
            throw new RuntimeException("At least one schedule is required");
        }

        List<Integer> seenWeekDays = new ArrayList<>();
        for (AttendanceScheduleDTO scheduleDTO : schedules) {
            if (scheduleDTO.getWeekDay() == null || scheduleDTO.getSignInStart() == null || scheduleDTO.getSignInEnd() == null) {
                throw new RuntimeException("Incomplete attendance schedule data");
            }
            if (scheduleDTO.getSignInStart().isAfter(scheduleDTO.getSignInEnd())) {
                throw new RuntimeException("Schedule start time cannot be later than the end time");
            }
            if (seenWeekDays.contains(scheduleDTO.getWeekDay())) {
                throw new RuntimeException("Duplicate week day schedules are not allowed");
            }
            seenWeekDays.add(scheduleDTO.getWeekDay());
        }

        jdbcTemplate.update("UPDATE t_attendance_schedule SET deleted = 1 WHERE task_id = ? AND deleted = 0", taskId);
        for (AttendanceScheduleDTO scheduleDTO : schedules) {
            AttendanceSchedule schedule = new AttendanceSchedule();
            schedule.setTaskId(taskId);
            schedule.setWeekDay(scheduleDTO.getWeekDay());
            schedule.setSignInStart(scheduleDTO.getSignInStart());
            schedule.setSignInEnd(scheduleDTO.getSignInEnd());
            schedule.setLateThresholdMinutes(defaultPositive(scheduleDTO.getLateThresholdMinutes(), 15));
            schedule.setSignCodeLength(limitRange(scheduleDTO.getSignCodeLength(), 4, 6, 4));
            schedule.setCodeTtlMinutes(defaultPositive(scheduleDTO.getCodeTtlMinutes(), 90));
            schedule.setStatus(1);
            schedule.setRemark(trimToNull(scheduleDTO.getRemark()));
            attendanceScheduleMapper.insert(schedule);
        }
        return getTaskSchedules(taskId, currentUser);
    }

    @Override
    public Map<String, Object> getAttendanceSummary(Long taskId, Long labId, User currentUser) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            fillSummary(summary,
                    "SELECT sign_status, COUNT(*) AS value FROM t_attendance_record WHERE deleted = 0 GROUP BY sign_status");
            summary.put("taskCount", count("SELECT COUNT(*) FROM t_attendance_task WHERE deleted = 0"));
            summary.put("todaySessionCount", count("SELECT COUNT(*) FROM t_attendance_session WHERE deleted = 0 AND session_date = CURDATE()"));
            summary.put("photoCount", count("SELECT COUNT(*) FROM t_attendance_photo WHERE deleted = 0"));
            return summary;
        }

        if (currentUserAccessor.isCollegeManager(currentUser)) {
            Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
            if (managedCollegeId == null) {
                throw new RuntimeException("Current account is not bound to a managed college");
            }
            fillSummary(summary,
                    "SELECT r.sign_status, COUNT(*) AS value " +
                            "FROM t_attendance_record r " +
                            "LEFT JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0 " +
                            "WHERE r.deleted = 0 AND l.college_id = ? GROUP BY r.sign_status",
                    managedCollegeId);
            summary.put("taskCount", count("SELECT COUNT(*) FROM t_attendance_task WHERE deleted = 0 AND college_id = ?", managedCollegeId));
            summary.put("todaySessionCount", count(
                    "SELECT COUNT(*) FROM t_attendance_session s " +
                            "LEFT JOIN t_lab l ON l.id = s.lab_id AND l.deleted = 0 " +
                            "WHERE s.deleted = 0 AND s.session_date = CURDATE() AND l.college_id = ?",
                    managedCollegeId));
            summary.put("photoCount", count(
                    "SELECT COUNT(*) FROM t_attendance_photo p " +
                            "LEFT JOIN t_lab l ON l.id = p.lab_id AND l.deleted = 0 " +
                            "WHERE p.deleted = 0 AND l.college_id = ?",
                    managedCollegeId));
            return summary;
        }

        if (currentUserAccessor.isLabManager(currentUser)) {
            Long scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
            fillSummary(summary,
                    "SELECT sign_status, COUNT(*) AS value FROM t_attendance_record WHERE deleted = 0 AND lab_id = ? GROUP BY sign_status",
                    scopedLabId);
            summary.put("taskCount", count(
                    "SELECT COUNT(*) FROM t_attendance_task t " +
                            "LEFT JOIN t_lab l ON l.college_id = t.college_id AND l.deleted = 0 " +
                            "WHERE t.deleted = 0 AND l.id = ?",
                    scopedLabId));
            summary.put("todaySessionCount", count(
                    "SELECT COUNT(*) FROM t_attendance_session WHERE deleted = 0 AND lab_id = ? AND session_date = CURDATE()",
                    scopedLabId));
            summary.put("photoCount", count(
                    "SELECT COUNT(*) FROM t_attendance_photo WHERE deleted = 0 AND lab_id = ?",
                    scopedLabId));
            return summary;
        }

        fillSummary(summary,
                "SELECT sign_status, COUNT(*) AS value FROM t_attendance_record WHERE deleted = 0 AND user_id = ? GROUP BY sign_status",
                currentUser.getId());
        Long studentLabId = resolveManagedLabId(currentUser);
        summary.put("todaySessionCount", count(
                "SELECT COUNT(*) FROM t_attendance_session WHERE deleted = 0 AND lab_id = ? AND session_date = CURDATE()",
                studentLabId));
        return summary;
    }

    @Override
    public Map<String, Object> getCurrentLabSession(User currentUser) {
        Long labId = resolveManagedLabId(currentUser);
        if (labId == null) {
            throw new RuntimeException("Current account is not bound to a managed lab");
        }
        AttendanceSession session = ensureSessionForToday(labId, currentUser, true);
        ensureActiveSessionCode(session, currentUser);
        return buildSessionMap(session, true, true);
    }

    @Override
    public List<Map<String, Object>> getCurrentLabSessionRecords(User currentUser) {
        Long labId = resolveManagedLabId(currentUser);
        if (labId == null) {
            throw new RuntimeException("Current account is not bound to a managed lab");
        }
        AttendanceSession session = ensureSessionForToday(labId, currentUser, true);
        return buildSessionRecordRows(session);
    }

    @Override
    @Transactional
    public boolean reviewLabAttendanceRecord(AttendanceRecordReviewDTO reviewDTO, User currentUser) {
        if (reviewDTO == null || reviewDTO.getSessionId() == null || reviewDTO.getUserId() == null) {
            throw new RuntimeException("Review data is required");
        }
        AttendanceSession session = attendanceSessionMapper.selectById(reviewDTO.getSessionId());
        if (session == null || !Objects.equals(session.getDeleted(), 0)) {
            throw new RuntimeException("Attendance session does not exist");
        }

        Long managedLabId = resolveManagedLabId(currentUser);
        if (!currentUserAccessor.isSuperAdmin(currentUser)
                && (managedLabId == null || !Objects.equals(managedLabId, session.getLabId()))) {
            throw new RuntimeException("You do not have access to this attendance session");
        }

        User member = userMapper.selectById(reviewDTO.getUserId());
        if (member == null || !Objects.equals(member.getDeleted(), 0)) {
            throw new RuntimeException("Student record does not exist");
        }
        assertActiveLabMember(session.getLabId(), member.getId());

        AttendanceRecord record = findAttendanceRecord(session.getId(), reviewDTO.getUserId());
        if (record == null) {
            record = new AttendanceRecord();
            record.setSessionId(session.getId());
            record.setTaskId(session.getTaskId());
            record.setLabId(session.getLabId());
            record.setUserId(reviewDTO.getUserId());
        }

        String beforeStatus = record.getSignStatus();
        String signStatus = normalizeSignStatus(reviewDTO.getSignStatus());
        record.setSignStatus(signStatus);
        record.setRemark(trimToNull(reviewDTO.getRemark()));
        record.setSource(SOURCE_ADMIN);
        record.setReviewedBy(currentUser == null ? null : currentUser.getId());
        record.setReviewTime(LocalDateTime.now());
        if (shouldWriteSignTime(signStatus) && record.getSignTime() == null) {
            record.setSignTime(LocalDateTime.now());
        } else if (!shouldWriteSignTime(signStatus)) {
            record.setSignTime(null);
        }

        boolean success = record.getId() == null
                ? attendanceRecordMapper.insert(record) > 0
                : attendanceRecordMapper.updateById(record) > 0;
          if (success) {
              writeChangeLog(record, beforeStatus, signStatus,
                      trimToNull(reviewDTO.getChangedReason()) == null
                              ? "attendance record reviewed"
                              : reviewDTO.getChangedReason(),
                    currentUser);
              auditLogService.record(currentUser == null ? null : currentUser.getId(),
                      "attendance_record_review",
                      "attendance_record",
                      record.getId(),
                      "sessionId=" + session.getId() + ",userId=" + reviewDTO.getUserId() + ",signStatus=" + record.getSignStatus());
              publishAttendanceStatisticsRefresh(session.getLabId(), currentUser == null ? null : currentUser.getId(), "attendance_record_review");
          }
          return success;
      }

    @Override
    @Transactional
    public Map<String, Object> uploadCurrentSessionPhoto(MultipartFile file, String remark, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Attendance photo is required");
        }
        Long labId = resolveManagedLabId(currentUser);
        if (labId == null) {
            throw new RuntimeException("Current account is not bound to a managed lab");
        }

        AttendanceSession session = ensureSessionForToday(labId, currentUser, true);
        LocalDate sessionDate = session.getSessionDate() == null ? LocalDate.now() : session.getSessionDate();
        Path targetDir = fileStorageService.resolveProtectedTargetDirectory(sessionDate);
        String fileName = buildAttendancePhotoFileName(file.getOriginalFilename());
        Path targetPath = targetDir.resolve(fileName);
        try {
            file.transferTo(targetPath);
        } catch (IOException | IllegalStateException ex) {
            throw new RuntimeException("Failed to save attendance photo", ex);
        }

        String photoUrl = fileStorageService.buildProtectedKey(sessionDate, fileName);
        AttendancePhoto photo = new AttendancePhoto();
        photo.setSessionId(session.getId());
        photo.setLabId(session.getLabId());
        photo.setUploaderId(currentUser == null ? null : currentUser.getId());
        photo.setPhotoUrl(photoUrl);
        photo.setRemark(trimToNull(remark));
        attendancePhotoMapper.insert(photo);
        auditLogService.record(currentUser == null ? null : currentUser.getId(),
                "attendance_photo_upload",
                "attendance_photo",
                photo.getId(),
                "sessionId=" + session.getId() + ",labId=" + session.getLabId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", photo.getId());
        result.put("sessionId", photo.getSessionId());
        result.put("labId", photo.getLabId());
        result.put("photoUrl", photo.getPhotoUrl());
        result.put("viewUrl", "/attendance-workflow/photos/" + photo.getId() + "/view");
        result.put("remark", photo.getRemark());
        result.put("createTime", photo.getCreateTime());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> setSessionDuty(Long sessionId, AttendanceDutyUpsertDTO dutyDTO, User currentUser) {
        if (sessionId == null || dutyDTO == null || dutyDTO.getDutyAdminUserId() == null) {
            throw new RuntimeException("Duty assignment data is required");
        }
        AttendanceSession session = attendanceSessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getDeleted(), 0)) {
            throw new RuntimeException("Attendance session does not exist");
        }
        AttendanceTask task = attendanceTaskMapper.selectById(session.getTaskId());
        if (task == null || !Objects.equals(task.getDeleted(), 0)) {
            throw new RuntimeException("Attendance task does not exist");
        }

        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            if (currentUserAccessor.isCollegeManager(currentUser)) {
                Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
                if (managedCollegeId == null || !managedCollegeId.equals(task.getCollegeId())) {
                    throw new RuntimeException("You do not have access to this attendance task");
                }
            } else {
                throw new RuntimeException("Only school directors or college managers can assign duty admins");
            }
        }

        User dutyAdmin = userMapper.selectById(dutyDTO.getDutyAdminUserId());
        if (dutyAdmin == null || !Objects.equals(dutyAdmin.getDeleted(), 0) || !Objects.equals(dutyAdmin.getStatus(), 1)) {
            throw new RuntimeException("Duty admin user does not exist");
        }
        UserAccessProfile dutyAdminProfile = userAccessService.buildProfile(dutyAdmin);
        if (!dutyAdminProfile.isSchoolDirector()
                && !(dutyAdminProfile.isCollegeManager() && Objects.equals(dutyAdminProfile.getManagedCollegeId(), task.getCollegeId()))) {
            throw new RuntimeException("Duty admin user must be a school director or the college manager of this task");
        }
        if (dutyDTO.getBackupAdminUserId() != null) {
            User backupAdmin = userMapper.selectById(dutyDTO.getBackupAdminUserId());
            if (backupAdmin == null || !Objects.equals(backupAdmin.getDeleted(), 0) || !Objects.equals(backupAdmin.getStatus(), 1)) {
                throw new RuntimeException("Backup admin user does not exist");
            }
            UserAccessProfile backupAdminProfile = userAccessService.buildProfile(backupAdmin);
            if (!backupAdminProfile.isSchoolDirector()
                    && !(backupAdminProfile.isCollegeManager() && Objects.equals(backupAdminProfile.getManagedCollegeId(), task.getCollegeId()))) {
                throw new RuntimeException("Backup admin user must be a school director or the college manager of this task");
            }
        }

        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance_duty WHERE deleted = 0 AND session_id = ?",
                Integer.class,
                sessionId
        );
        if (existingCount != null && existingCount > 0) {
            jdbcTemplate.update(
                    "UPDATE t_attendance_duty SET duty_admin_user_id = ?, backup_admin_user_id = ?, remark = ?, update_time = NOW() " +
                            "WHERE deleted = 0 AND session_id = ?",
                    dutyDTO.getDutyAdminUserId(),
                    dutyDTO.getBackupAdminUserId(),
                    trimToNull(dutyDTO.getRemark()),
                    sessionId
            );
        } else {
            jdbcTemplate.update(
                    "INSERT INTO t_attendance_duty (task_id, session_id, college_id, duty_admin_user_id, backup_admin_user_id, status, remark, create_time, update_time, deleted) " +
                            "VALUES (?, ?, ?, ?, ?, 'active', ?, NOW(), NOW(), 0)",
                    task.getId(),
                    sessionId,
                    task.getCollegeId(),
                    dutyDTO.getDutyAdminUserId(),
                    dutyDTO.getBackupAdminUserId(),
                    trimToNull(dutyDTO.getRemark())
            );
        }

        auditLogService.record(currentUser == null ? null : currentUser.getId(),
                "attendance_duty_set",
                "attendance_session",
                sessionId,
                "dutyAdminUserId=" + dutyDTO.getDutyAdminUserId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("taskId", task.getId());
        result.put("collegeId", task.getCollegeId());
        result.put("dutyAdminUserId", dutyDTO.getDutyAdminUserId());
        result.put("backupAdminUserId", dutyDTO.getBackupAdminUserId());
        result.put("remark", trimToNull(dutyDTO.getRemark()));
        return result;
    }

    @Override
    public Map<String, Object> getCurrentStudentSession(User currentUser) {
        Long labId = resolveManagedLabId(currentUser);
        if (currentUser == null || labId == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }

        AttendanceSession session = ensureSessionForToday(labId, currentUser, false);
        if (session == null) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("available", false);
            emptyResult.put("sessionDate", LocalDate.now());
            return emptyResult;
        }

        ensureActiveSessionCode(session, null);
        Map<String, Object> result = buildSessionMap(session, false, false);
        AttendanceRecord record = findAttendanceRecord(session.getId(), currentUser.getId());
        AttendanceLeave leave = findAttendanceLeave(session.getId(), currentUser.getId());
        result.put("available", true);
        result.put("myRecord", buildStudentRecordMap(record));
        result.put("myLeaveRequest", buildLeaveMap(leave));
        result.put("canSignIn", canStudentSignIn(session));
        result.put("canApplyLeave", !SESSION_STATUS_CLOSED.equals(session.getStatus()));
        return result;
    }

    @Override
    @Transactional
    public boolean studentSignIn(AttendanceSignInDTO signInDTO, User currentUser) {
        if (signInDTO == null || !StringUtils.hasText(signInDTO.getSignCode())) {
            throw new RuntimeException("Sign-in code is required");
        }
        Long labId = resolveManagedLabId(currentUser);
        if (currentUser == null || currentUser.getId() == null || labId == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }

        AttendanceSession session = ensureSessionForToday(labId, currentUser, false);
        if (session == null) {
            throw new RuntimeException("No attendance session is available today");
        }

        String lockKey = buildAttendanceSignLockKey(session.getId(), currentUser.getId());
        String lockToken = idempotencyLockService.tryLock(lockKey, SIGN_IN_LOCK_TTL);
        if (lockToken == null) {
            throw new RuntimeException("A sign-in request is already being processed");
        }

        try {
            refreshSessionStatus(session);
            ensureActiveSessionCode(session, null);
            LocalDateTime now = LocalDateTime.now();
            if (!SESSION_STATUS_ACTIVE.equals(session.getStatus())) {
                if (session.getSignStartTime() != null && now.isBefore(session.getSignStartTime())) {
                    throw new RuntimeException("Attendance sign-in has not started");
                }
                throw new RuntimeException("Attendance sign-in is already closed");
            }
            if (!StringUtils.hasText(session.getSessionCode())) {
                throw new RuntimeException("The lab admin has not published the dynamic sign-in code yet");
            }
            if (session.getCodeExpireTime() != null && now.isAfter(session.getCodeExpireTime())) {
                throw new RuntimeException("Sign-in code has expired");
            }

            String signCode = signInDTO.getSignCode().trim();
            if (!Objects.equals(session.getSessionCode(), signCode)) {
                throw new RuntimeException("Sign-in code is incorrect");
            }

            AttendanceLeave leave = findAttendanceLeave(session.getId(), currentUser.getId());
            if (leave != null && LEAVE_STATUS_APPROVED.equalsIgnoreCase(leave.getLeaveStatus())) {
                throw new RuntimeException("This session has already been approved as leave");
            }

            AttendanceRecord record = findAttendanceRecord(session.getId(), currentUser.getId());
            if (record == null) {
                record = new AttendanceRecord();
                record.setSessionId(session.getId());
                record.setTaskId(session.getTaskId());
                record.setLabId(session.getLabId());
                record.setUserId(currentUser.getId());
            } else if (record.getSignTime() != null && !RECORD_STATUS_ABSENT.equals(record.getSignStatus())) {
                throw new RuntimeException("You have already signed in for this session");
            }

            String signStatus = session.getLateTime() != null && now.isAfter(session.getLateTime())
                    ? RECORD_STATUS_LATE
                    : RECORD_STATUS_NORMAL;
            record.setSignStatus(signStatus);
            record.setSignCode(signCode);
            record.setSignTime(now);
            record.setRemark(trimToNull(signInDTO.getRemark()));
            record.setSource(SOURCE_STUDENT);
            record.setReviewedBy(null);
            record.setReviewTime(null);

            boolean success = record.getId() == null
                    ? attendanceRecordMapper.insert(record) > 0
                    : attendanceRecordMapper.updateById(record) > 0;
            if (success) {
                publishAttendanceStatisticsRefresh(session.getLabId(), currentUser.getId(), "attendance_sign_in");
            }
            return success;
        } finally {
            idempotencyLockService.unlock(lockKey, lockToken);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> studentApplyLeave(AttendanceLeaveApplyDTO leaveDTO, User currentUser) {
        if (leaveDTO == null || leaveDTO.getSessionId() == null || !StringUtils.hasText(leaveDTO.getLeaveReason())) {
            throw new RuntimeException("Leave request data is required");
        }
        Long labId = resolveManagedLabId(currentUser);
        if (currentUser == null || currentUser.getId() == null || labId == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }

        AttendanceSession session = attendanceSessionMapper.selectById(leaveDTO.getSessionId());
        if (session == null || !Objects.equals(session.getDeleted(), 0)) {
            throw new RuntimeException("Attendance session does not exist");
        }
        if (!Objects.equals(session.getLabId(), labId)) {
            throw new RuntimeException("You do not have access to this attendance session");
        }
        refreshSessionStatus(session);
        if (SESSION_STATUS_CLOSED.equals(session.getStatus())) {
            throw new RuntimeException("Leave requests are only allowed before the session closes");
        }

        AttendanceRecord record = findAttendanceRecord(session.getId(), currentUser.getId());
        if (record != null && record.getSignTime() != null && !RECORD_STATUS_ABSENT.equals(record.getSignStatus())) {
            throw new RuntimeException("You have already completed attendance for this session");
        }
        AttendanceLeave leave = findAttendanceLeave(session.getId(), currentUser.getId());
        if (leave == null) {
            leave = new AttendanceLeave();
            leave.setSessionId(session.getId());
            leave.setTaskId(session.getTaskId());
            leave.setLabId(session.getLabId());
            leave.setUserId(currentUser.getId());
        } else if (LEAVE_STATUS_APPROVED.equalsIgnoreCase(leave.getLeaveStatus())) {
            throw new RuntimeException("Leave has already been approved for this session");
        }

        leave.setLeaveReason(leaveDTO.getLeaveReason().trim());
        leave.setLeaveStatus(LEAVE_STATUS_PENDING);
        leave.setReviewComment(null);
        leave.setReviewedBy(null);
        leave.setReviewTime(null);

        boolean success = leave.getId() == null
                ? attendanceLeaveMapper.insert(leave) > 0
                : attendanceLeaveMapper.updateById(leave) > 0;
        if (!success) {
            throw new RuntimeException("Failed to save leave request");
        }

        auditLogService.record(currentUser.getId(),
                "attendance_leave_submit",
                "attendance_leave",
                leave.getId(),
                "sessionId=" + session.getId());
        publishAttendanceStatisticsRefresh(session.getLabId(), currentUser.getId(), "attendance_leave_submit");
        return buildLeaveMap(leave);
    }

    @Override
    @Transactional
    public boolean studentRequestMakeup(AttendanceMakeupRequestDTO requestDTO, User currentUser) {
        Long labId = resolveManagedLabId(currentUser);
        if (currentUser == null || currentUser.getId() == null || labId == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }
        AttendanceSession session = ensureSessionForToday(labId, currentUser, false);
        if (session == null) {
            throw new RuntimeException("No attendance session is available today");
        }
        refreshSessionStatus(session);
        if (!SESSION_STATUS_CLOSED.equals(session.getStatus())) {
            throw new RuntimeException("Makeup requests are only allowed after the session is closed");
        }

        AttendanceRecord record = findAttendanceRecord(session.getId(), currentUser.getId());
        if (record != null && record.getSignTime() != null && !RECORD_STATUS_ABSENT.equals(record.getSignStatus())) {
            throw new RuntimeException("You have already signed in for this session");
        }
        if (record == null) {
            record = new AttendanceRecord();
            record.setSessionId(session.getId());
            record.setTaskId(session.getTaskId());
            record.setLabId(session.getLabId());
            record.setUserId(currentUser.getId());
        }

        String currentStatus = trimToNull(record.getSignStatus());
        if (currentStatus != null) {
            currentStatus = normalizeSignStatus(currentStatus);
        }
        if (RECORD_STATUS_MAKEUP_PENDING.equals(currentStatus)
                || RECORD_STATUS_MAKEUP_APPROVED.equals(currentStatus)
                || RECORD_STATUS_MAKEUP_REJECTED.equals(currentStatus)) {
            throw new RuntimeException("Makeup request already exists for this session");
        }

        record.setSignStatus(RECORD_STATUS_MAKEUP_PENDING);
        record.setRemark(requestDTO == null ? null : trimToNull(requestDTO.getRemark()));
        record.setSource(SOURCE_STUDENT);
        record.setReviewedBy(null);
        record.setReviewTime(null);
        record.setSignCode(null);
        record.setSignTime(null);

        boolean success = record.getId() == null
                ? attendanceRecordMapper.insert(record) > 0
                : attendanceRecordMapper.updateById(record) > 0;
        if (success) {
            auditLogService.record(currentUser.getId(),
                    "attendance_makeup_request",
                    "attendance_record",
                    record.getId(),
                    "sessionId=" + session.getId());
            publishAttendanceStatisticsRefresh(session.getLabId(), currentUser.getId(), "attendance_makeup_request");
        }
        return success;
    }

    @Override
    public Page<Map<String, Object>> getStudentHistory(Integer pageNum, Integer pageSize, User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new RuntimeException("Current user does not exist");
        }

        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;
        long total = count("SELECT COUNT(*) FROM t_attendance_record WHERE deleted = 0 AND user_id = ?", currentUser.getId());

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT r.id, r.session_id AS sessionId, r.task_id AS taskId, r.lab_id AS labId, " +
                        "l.lab_name AS labName, s.session_date AS sessionDate, " +
                        "r.sign_status AS signStatus, r.sign_code AS signCode, r.sign_time AS signTime, " +
                        "r.remark, r.source, r.review_time AS reviewTime " +
                        "FROM t_attendance_record r " +
                        "LEFT JOIN t_attendance_session s ON s.id = r.session_id AND s.deleted = 0 " +
                        "LEFT JOIN t_lab l ON l.id = r.lab_id AND l.deleted = 0 " +
                        "WHERE r.deleted = 0 AND r.user_id = ? " +
                        "ORDER BY COALESCE(r.sign_time, s.session_date) DESC, r.id DESC LIMIT ?, ?",
                currentUser.getId(), offset, size
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    public Page<Map<String, Object>> getPendingLeavePage(Integer pageNum, Integer pageSize, Long labId,
                                                         String leaveStatus, String keyword, User currentUser) {
        Long scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;
        String scopedStatus = trimToNull(leaveStatus);
        String keywordLike = trimToNull(keyword);

        List<Object> args = new ArrayList<>();
        StringBuilder whereSql = new StringBuilder(" WHERE leave_req.deleted = 0 AND leave_req.lab_id = ?");
        args.add(scopedLabId);
        if (scopedStatus != null) {
            whereSql.append(" AND leave_req.leave_status = ?");
            args.add(scopedStatus.toUpperCase(Locale.ROOT));
        }
        if (keywordLike != null) {
            whereSql.append(" AND (u.real_name LIKE ? OR u.student_id LIKE ? OR leave_req.leave_reason LIKE ?)");
            args.add("%" + keywordLike + "%");
            args.add("%" + keywordLike + "%");
            args.add("%" + keywordLike + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance_leave leave_req " +
                        "LEFT JOIN t_user u ON u.id = leave_req.user_id AND u.deleted = 0" +
                        whereSql,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT leave_req.id, leave_req.session_id AS sessionId, leave_req.task_id AS taskId, " +
                        "leave_req.lab_id AS labId, leave_req.user_id AS userId, leave_req.leave_reason AS leaveReason, " +
                        "leave_req.leave_status AS leaveStatus, leave_req.review_comment AS reviewComment, " +
                        "leave_req.reviewed_by AS reviewedBy, leave_req.review_time AS reviewTime, " +
                        "leave_req.created_at AS createdAt, s.session_date AS sessionDate, " +
                        "u.real_name AS realName, u.student_id AS studentId, u.major, l.lab_name AS labName " +
                        "FROM t_attendance_leave leave_req " +
                        "LEFT JOIN t_attendance_session s ON s.id = leave_req.session_id AND s.deleted = 0 " +
                        "LEFT JOIN t_user u ON u.id = leave_req.user_id AND u.deleted = 0 " +
                        "LEFT JOIN t_lab l ON l.id = leave_req.lab_id AND l.deleted = 0" +
                        whereSql +
                        " ORDER BY CASE WHEN leave_req.leave_status = 'PENDING' THEN 0 ELSE 1 END, leave_req.created_at DESC, leave_req.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    @Transactional
    public Map<String, Object> approveLeave(Long leaveId, AttendanceLeaveReviewDTO reviewDTO, User currentUser) {
        return reviewLeave(leaveId, reviewDTO, currentUser, true);
    }

    @Override
    @Transactional
    public Map<String, Object> rejectLeave(Long leaveId, AttendanceLeaveReviewDTO reviewDTO, User currentUser) {
        return reviewLeave(leaveId, reviewDTO, currentUser, false);
    }

    private Map<String, Object> reviewLeave(Long leaveId, AttendanceLeaveReviewDTO reviewDTO, User currentUser, boolean approved) {
        if (leaveId == null || reviewDTO == null || !StringUtils.hasText(reviewDTO.getReviewComment())) {
            throw new RuntimeException("Leave review data is required");
        }

        AttendanceLeave leave = attendanceLeaveMapper.selectById(leaveId);
        if (leave == null || !Objects.equals(leave.getDeleted(), 0)) {
            throw new RuntimeException("Leave request does not exist");
        }

        Long scopedLabId = currentUserAccessor.resolveLabScope(currentUser, leave.getLabId());
        if (!Objects.equals(scopedLabId, leave.getLabId())) {
            throw new RuntimeException("You do not have access to this leave request");
        }

        AttendanceSession session = attendanceSessionMapper.selectById(leave.getSessionId());
        if (session == null || !Objects.equals(session.getDeleted(), 0)) {
            throw new RuntimeException("Attendance session does not exist");
        }
        assertActiveLabMember(session.getLabId(), leave.getUserId());

        leave.setLeaveStatus(approved ? LEAVE_STATUS_APPROVED : LEAVE_STATUS_REJECTED);
        leave.setReviewComment(reviewDTO.getReviewComment().trim());
        leave.setReviewedBy(currentUser == null ? null : currentUser.getId());
        leave.setReviewTime(LocalDateTime.now());
        attendanceLeaveMapper.updateById(leave);

        if (approved) {
            AttendanceRecord record = findAttendanceRecord(session.getId(), leave.getUserId());
            if (record != null && record.getSignTime() != null && !RECORD_STATUS_ABSENT.equals(record.getSignStatus())) {
                throw new RuntimeException("This student has already completed attendance for the session");
            }
            if (record == null) {
                record = new AttendanceRecord();
                record.setSessionId(session.getId());
                record.setTaskId(session.getTaskId());
                record.setLabId(session.getLabId());
                record.setUserId(leave.getUserId());
            }
            String beforeStatus = record.getSignStatus();
            record.setSignStatus(RECORD_STATUS_LEAVE);
            record.setSignCode(null);
            record.setSignTime(null);
            record.setRemark(leave.getLeaveReason());
            record.setSource(SOURCE_ADMIN);
            record.setReviewedBy(currentUser == null ? null : currentUser.getId());
            record.setReviewTime(LocalDateTime.now());

            if (record.getId() == null) {
                attendanceRecordMapper.insert(record);
            } else {
                attendanceRecordMapper.updateById(record);
            }

            writeChangeLog(record, beforeStatus, RECORD_STATUS_LEAVE, "leave approved", currentUser);
        }

        auditLogService.record(currentUser == null ? null : currentUser.getId(),
                approved ? "attendance_leave_approve" : "attendance_leave_reject",
                "attendance_leave",
                leave.getId(),
                "sessionId=" + leave.getSessionId() + ",userId=" + leave.getUserId());

        systemNotificationService.createNotificationAsync(
                leave.getUserId(),
                approved ? "Leave approved" : "Leave rejected",
                approved
                        ? "Your leave request has been approved."
                        : "Your leave request has been rejected. Check the review comment for details.",
                "ATTENDANCE_LEAVE",
                leave.getId(),
                "/student/attendance"
        );
        publishAttendanceStatisticsRefresh(session.getLabId(), currentUser == null ? null : currentUser.getId(),
                approved ? "attendance_leave_approve" : "attendance_leave_reject");

        return buildLeaveMap(leave);
    }

    private void publishAttendanceStatisticsRefresh(Long labId, Long operatorUserId, String reason) {
        statisticsRefreshService.refreshAsync(
                "attendance",
                currentUserAccessor.resolveCollegeIdByLabId(labId),
                labId,
                operatorUserId,
                reason
        );
    }

    private Long resolveTaskScopeCollegeId(Long collegeId, User currentUser) {
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return collegeId;
        }

        if (currentUserAccessor.isCollegeManager(currentUser)) {
            Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
            if (managedCollegeId == null) {
                throw new RuntimeException("Current account is not bound to a managed college");
            }
            if (collegeId != null && !Objects.equals(collegeId, managedCollegeId)) {
                throw new RuntimeException("You do not have access to another college");
            }
            return managedCollegeId;
        }

        if (resolveManagedLabId(currentUser) != null) {
            throw new RuntimeException("Only school directors or college managers can manage attendance tasks");
        }

        if (collegeId == null) {
            throw new RuntimeException("College id is required");
        }
        return collegeId;
    }

    private void assertActiveLabMember(Long labId, Long userId) {
        if (labId == null || userId == null) {
            throw new RuntimeException("Student record does not belong to this lab");
        }

        QueryWrapper<LabMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .and(wrapper -> wrapper.isNull("member_role").or().ne("member_role", MEMBER_ROLE_LAB_ADMIN))
                .last("LIMIT 1");
        if (labMemberMapper.selectOne(memberQuery) == null) {
            throw new RuntimeException("Student record does not belong to this lab");
        }
    }

    private void assertTaskScope(AttendanceTask task, User currentUser) {
        if (task == null || task.getId() == null) {
            throw new RuntimeException("Attendance task does not exist");
        }
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }
        Long scopedCollegeId = resolveTaskScopeCollegeId(task.getCollegeId(), currentUser);
        if (!Objects.equals(scopedCollegeId, task.getCollegeId())) {
            throw new RuntimeException("You do not have access to this attendance task");
        }
    }

    private Map<String, Object> buildTaskMap(AttendanceTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("collegeId", task.getCollegeId());
        result.put("semesterName", task.getSemesterName());
        result.put("taskName", task.getTaskName());
        result.put("description", task.getDescription());
        result.put("startDate", task.getStartDate());
        result.put("endDate", task.getEndDate());
        result.put("status", task.getStatus());
        result.put("publishedBy", task.getPublishedBy());
        result.put("publishedTime", task.getPublishedTime());
        result.put("createdBy", task.getCreatedBy());
        result.put("createTime", task.getCreateTime());
        result.put("updateTime", task.getUpdateTime());
        result.put("scheduleCount", count(
                "SELECT COUNT(*) FROM t_attendance_schedule WHERE deleted = 0 AND task_id = ?",
                task.getId()
        ));
        if (task.getCollegeId() != null) {
            List<Map<String, Object>> collegeRows = jdbcTemplate.queryForList(
                    "SELECT college_name AS collegeName FROM t_college WHERE id = ? AND deleted = 0",
                    task.getCollegeId()
            );
            if (!collegeRows.isEmpty()) {
                result.put("collegeName", collegeRows.get(0).get("collegeName"));
            }
        }
        return result;
    }

    private void fillSummary(Map<String, Object> summary, String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        Map<String, Long> counter = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String signStatus = row.get("sign_status") == null ? null : String.valueOf(row.get("sign_status"));
            Long value = row.get("value") instanceof Number ? ((Number) row.get("value")).longValue() : 0L;
            if (signStatus != null) {
                counter.put(signStatus, value);
            }
        }

        long presentCount = counter.getOrDefault(RECORD_STATUS_NORMAL, 0L);
        long lateCount = counter.getOrDefault(RECORD_STATUS_LATE, 0L);
        long leaveCount = counter.getOrDefault(RECORD_STATUS_LEAVE, 0L);
        long absentCount = counter.getOrDefault(RECORD_STATUS_ABSENT, 0L);
        long makeupPendingCount = counter.getOrDefault(RECORD_STATUS_MAKEUP_PENDING, 0L);
        long makeupApprovedCount = counter.getOrDefault(RECORD_STATUS_MAKEUP_APPROVED, 0L);
        long makeupRejectedCount = counter.getOrDefault(RECORD_STATUS_MAKEUP_REJECTED, 0L);

        long legacyMakeupCount = counter.getOrDefault("makeup", 0L);
        long legacyAnomalyCount = counter.getOrDefault("anomaly", 0L);

        long totalCount = presentCount + lateCount + leaveCount + absentCount
                + makeupPendingCount + makeupApprovedCount + makeupRejectedCount
                + legacyMakeupCount + legacyAnomalyCount;
        long denominator = presentCount + lateCount + absentCount
                + makeupPendingCount + makeupApprovedCount + makeupRejectedCount
                + legacyMakeupCount + legacyAnomalyCount;

        summary.put("presentCount", presentCount);
        summary.put("normalCount", presentCount);
        summary.put("lateCount", lateCount);
        summary.put("leaveCount", leaveCount);
        summary.put("absentCount", absentCount);
        summary.put("makeupPendingCount", makeupPendingCount);
        summary.put("makeupApprovedCount", makeupApprovedCount);
        summary.put("makeupRejectedCount", makeupRejectedCount);
        summary.put("makeupCount", makeupPendingCount + makeupApprovedCount + legacyMakeupCount);
        summary.put("anomalyCount", legacyAnomalyCount);
        summary.put("totalCount", totalCount);
        summary.put("attendanceRate", denominator <= 0
                ? 100D
                : round((presentCount + lateCount + makeupApprovedCount + legacyMakeupCount) * 100.0D / denominator));
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private Long resolveManagedLabId(User currentUser) {
        Long managedLabId = userAccessService.resolveManagedLabId(currentUser);
        if (managedLabId != null) {
            return managedLabId;
        }
        return null;
    }

    private AttendanceSession ensureSessionForToday(Long labId, User currentUser, boolean createIfMissing) {
        LocalDate today = LocalDate.now();
        QueryWrapper<AttendanceSession> sessionQuery = new QueryWrapper<>();
        sessionQuery.eq("lab_id", labId)
                .eq("session_date", today)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        AttendanceSession session = attendanceSessionMapper.selectOne(sessionQuery);
        if (session != null) {
            refreshSessionStatus(session);
            return session;
        }

        Lab lab = labMapper.selectById(labId);
        if (lab == null || !Objects.equals(lab.getDeleted(), 0)) {
            throw new RuntimeException("Lab does not exist");
        }

        AttendanceTask task = findCurrentTask(lab.getCollegeId(), today);
        if (task == null) {
            if (createIfMissing) {
                throw new RuntimeException("No published attendance task is available for today");
            }
            return null;
        }

        AttendanceSchedule schedule = findCurrentSchedule(task.getId(), today.getDayOfWeek());
        if (schedule == null) {
            if (createIfMissing) {
                throw new RuntimeException("No attendance schedule is configured for today");
            }
            return null;
        }

        session = new AttendanceSession();
        session.setTaskId(task.getId());
        session.setScheduleId(schedule.getId());
        session.setLabId(labId);
        session.setSessionDate(today);
        // The database schema still requires non-null session_code/code_expire_time.
        // Keep an inert placeholder here and replace it with a real dynamic code
        // only after the session becomes active.
        session.setSessionCode(SESSION_CODE_PLACEHOLDER);
        session.setSignStartTime(LocalDateTime.of(today, schedule.getSignInStart()));
        session.setSignEndTime(LocalDateTime.of(today, schedule.getSignInEnd()));
        session.setLateTime(LocalDateTime.of(today, schedule.getSignInStart())
                .plusMinutes(defaultPositive(schedule.getLateThresholdMinutes(), 15)));
        session.setCodeExpireTime(session.getSignStartTime());
        session.setStatus(resolveSessionStatus(session, LocalDateTime.now()));
        attendanceSessionMapper.insert(session);
        return session;
    }

    private void ensureActiveSessionCode(AttendanceSession session, User currentUser) {
        if (session == null || session.getId() == null) {
            return;
        }
        refreshSessionStatus(session);
        if (!SESSION_STATUS_ACTIVE.equals(session.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(session.getSessionCode())
                && (session.getCodeExpireTime() == null || !now.isAfter(session.getCodeExpireTime()))) {
            return;
        }

        AttendanceSchedule schedule = session.getScheduleId() == null
                ? null
                : attendanceScheduleMapper.selectById(session.getScheduleId());
        int signCodeLength = schedule == null ? 4 : limitRange(schedule.getSignCodeLength(), 4, 6, 4);
        int codeTtlMinutes = schedule == null ? 90 : defaultPositive(schedule.getCodeTtlMinutes(), 90);

        LocalDateTime expireTime = now.plusMinutes(codeTtlMinutes);
        if (session.getSignEndTime() != null && expireTime.isAfter(session.getSignEndTime())) {
            expireTime = session.getSignEndTime();
        }

        session.setSessionCode(generateSignCode(signCodeLength));
        session.setCodeExpireTime(expireTime);
        session.setGeneratedBy(currentUser == null ? null : currentUser.getId());
        session.setPublishTime(now);
        attendanceSessionMapper.updateById(session);
    }

    private AttendanceTask findCurrentTask(Long collegeId, LocalDate today) {
        if (collegeId == null) {
            return null;
        }
        QueryWrapper<AttendanceTask> taskQuery = new QueryWrapper<>();
        taskQuery.eq("college_id", collegeId)
                .eq("deleted", 0)
                .eq("status", TASK_STATUS_PUBLISHED)
                .le("start_date", today)
                .ge("end_date", today)
                .orderByDesc("published_time")
                .orderByDesc("id")
                .last("LIMIT 1");
        return attendanceTaskMapper.selectOne(taskQuery);
    }

    private AttendanceSchedule findCurrentSchedule(Long taskId, DayOfWeek dayOfWeek) {
        if (taskId == null || dayOfWeek == null) {
            return null;
        }
        QueryWrapper<AttendanceSchedule> scheduleQuery = new QueryWrapper<>();
        scheduleQuery.eq("task_id", taskId)
                .eq("week_day", dayOfWeek.getValue())
                .eq("status", 1)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return attendanceScheduleMapper.selectOne(scheduleQuery);
    }

    private void refreshSessionStatus(AttendanceSession session) {
        if (session == null || session.getId() == null) {
            return;
        }
        String targetStatus = resolveSessionStatus(session, LocalDateTime.now());
        if (!Objects.equals(session.getStatus(), targetStatus)) {
            session.setStatus(targetStatus);
            attendanceSessionMapper.updateById(session);
        }
    }

    private String resolveSessionStatus(AttendanceSession session, LocalDateTime now) {
        if (session == null) {
            return SESSION_STATUS_PENDING;
        }
        if (session.getSignStartTime() != null && now.isBefore(session.getSignStartTime())) {
            return SESSION_STATUS_PENDING;
        }
        if (session.getSignEndTime() != null && now.isAfter(session.getSignEndTime())) {
            return SESSION_STATUS_CLOSED;
        }
        return SESSION_STATUS_ACTIVE;
    }

    private Map<String, Object> buildSessionMap(AttendanceSession session, boolean includeCode, boolean includeRecords) {
        refreshSessionStatus(session);
        boolean codeReady = hasUsableSessionCode(session);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", session.getId());
        result.put("taskId", session.getTaskId());
        result.put("scheduleId", session.getScheduleId());
        result.put("labId", session.getLabId());
        result.put("sessionDate", session.getSessionDate());
        result.put("status", session.getStatus());
        result.put("signStartTime", session.getSignStartTime());
        result.put("signEndTime", session.getSignEndTime());
        result.put("lateTime", session.getLateTime());
        result.put("codeExpireTime", session.getCodeExpireTime());
        result.put("publishTime", session.getPublishTime());
        result.put("codeReady", codeReady);
        result.put("codeRemainingSeconds", resolveCodeRemainingSeconds(session));
        result.put("photoCount", count("SELECT COUNT(*) FROM t_attendance_photo WHERE deleted = 0 AND session_id = ?", session.getId()));
        List<Map<String, Object>> dutyRows = jdbcTemplate.queryForList(
                "SELECT duty_admin_user_id AS dutyAdminUserId, backup_admin_user_id AS backupAdminUserId, remark " +
                        "FROM t_attendance_duty WHERE deleted = 0 AND session_id = ? ORDER BY id DESC LIMIT 1",
                session.getId()
        );
        if (!dutyRows.isEmpty()) {
            Map<String, Object> dutyRow = dutyRows.get(0);
            result.put("dutyAdminUserId", dutyRow.get("dutyAdminUserId"));
            result.put("backupAdminUserId", dutyRow.get("backupAdminUserId"));
            result.put("dutyRemark", dutyRow.get("remark"));
        }
        fillSummary(result,
                "SELECT sign_status, COUNT(*) AS value FROM t_attendance_record WHERE deleted = 0 AND session_id = ? GROUP BY sign_status",
                session.getId());
        if (includeCode) {
            result.put("sessionCode", codeReady ? session.getSessionCode() : null);
        }
        if (includeRecords) {
            result.put("records", buildSessionRecordRows(session));
        }
        return result;
    }

    private List<Map<String, Object>> buildSessionRecordRows(AttendanceSession session) {
        List<Map<String, Object>> memberRows = jdbcTemplate.queryForList(
                "SELECT m.user_id AS userId, u.real_name AS realName, u.student_id AS studentId, " +
                        "u.major, m.member_role AS memberRole " +
                        "FROM t_lab_member m " +
                        "LEFT JOIN t_user u ON u.id = m.user_id AND u.deleted = 0 " +
                        "WHERE m.deleted = 0 AND m.status = 'active' AND m.lab_id = ? " +
                        "AND COALESCE(m.member_role, '') <> 'lab_admin' " +
                        "ORDER BY u.student_id ASC, u.id ASC",
                session.getLabId()
        );

        QueryWrapper<AttendanceRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("session_id", session.getId())
                .eq("deleted", 0);
        List<AttendanceRecord> recordList = attendanceRecordMapper.selectList(recordQuery);
        Map<Long, AttendanceRecord> recordMap = new LinkedHashMap<>();
        for (AttendanceRecord record : recordList) {
            recordMap.put(record.getUserId(), record);
        }

        QueryWrapper<AttendanceLeave> leaveQuery = new QueryWrapper<>();
        leaveQuery.eq("session_id", session.getId())
                .eq("deleted", 0);
        List<AttendanceLeave> leaveList = attendanceLeaveMapper.selectList(leaveQuery);
        Map<Long, AttendanceLeave> leaveMap = new LinkedHashMap<>();
        for (AttendanceLeave leave : leaveList) {
            leaveMap.put(leave.getUserId(), leave);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> memberRow : memberRows) {
            Long userId = memberRow.get("userId") instanceof Number
                    ? ((Number) memberRow.get("userId")).longValue()
                    : null;
            AttendanceRecord record = userId == null ? null : recordMap.get(userId);
            AttendanceLeave leave = userId == null ? null : leaveMap.get(userId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("recordId", record == null ? null : record.getId());
            item.put("userId", userId);
            item.put("realName", memberRow.get("realName"));
            item.put("studentId", memberRow.get("studentId"));
            item.put("major", memberRow.get("major"));
            item.put("memberRole", memberRow.get("memberRole"));
            item.put("signStatus", record == null ? defaultRecordStatus(session) : record.getSignStatus());
            item.put("signTime", record == null ? null : record.getSignTime());
            item.put("remark", record == null ? null : record.getRemark());
            item.put("source", record == null ? null : record.getSource());
            item.put("reviewTime", record == null ? null : record.getReviewTime());
            item.put("leaveRequest", buildLeaveMap(leave));
            item.put("changeCount", record == null ? 0L : count(
                    "SELECT COUNT(*) FROM t_attendance_change_log WHERE deleted = 0 AND record_id = ?",
                    record.getId()
            ));
            result.add(item);
        }
        return result;
    }

    private String defaultRecordStatus(AttendanceSession session) {
        return SESSION_STATUS_CLOSED.equals(session.getStatus()) ? RECORD_STATUS_ABSENT : "pending";
    }

    private AttendanceRecord findAttendanceRecord(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return null;
        }
        QueryWrapper<AttendanceRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return attendanceRecordMapper.selectOne(queryWrapper);
    }

    private AttendanceLeave findAttendanceLeave(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return null;
        }
        QueryWrapper<AttendanceLeave> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        return attendanceLeaveMapper.selectOne(queryWrapper);
    }

    private Map<String, Object> buildStudentRecordMap(AttendanceRecord record) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (record == null) {
            result.put("signStatus", null);
            result.put("signTime", null);
            result.put("remark", null);
            result.put("source", null);
            return result;
        }
        result.put("id", record.getId());
        result.put("signStatus", record.getSignStatus());
        result.put("signTime", record.getSignTime());
        result.put("remark", record.getRemark());
        result.put("source", record.getSource());
        result.put("reviewTime", record.getReviewTime());
        return result;
    }

    private Map<String, Object> buildLeaveMap(AttendanceLeave leave) {
        if (leave == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", leave.getId());
        result.put("sessionId", leave.getSessionId());
        result.put("taskId", leave.getTaskId());
        result.put("labId", leave.getLabId());
        result.put("userId", leave.getUserId());
        result.put("leaveReason", leave.getLeaveReason());
        result.put("leaveStatus", leave.getLeaveStatus());
        result.put("reviewComment", leave.getReviewComment());
        result.put("reviewedBy", leave.getReviewedBy());
        result.put("reviewTime", leave.getReviewTime());
        result.put("createdAt", leave.getCreatedAt());
        result.put("updatedAt", leave.getUpdatedAt());
        return result;
    }

    private boolean canStudentSignIn(AttendanceSession session) {
        if (session == null) {
            return false;
        }
        refreshSessionStatus(session);
        LocalDateTime now = LocalDateTime.now();
        return SESSION_STATUS_ACTIVE.equals(session.getStatus())
                && StringUtils.hasText(session.getSessionCode())
                && (session.getCodeExpireTime() == null || !now.isAfter(session.getCodeExpireTime()));
    }

    private boolean hasUsableSessionCode(AttendanceSession session) {
        if (session == null) {
            return false;
        }
        if (!StringUtils.hasText(session.getSessionCode())) {
            return false;
        }
        refreshSessionStatus(session);
        if (!SESSION_STATUS_ACTIVE.equals(session.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return session.getCodeExpireTime() == null || !now.isAfter(session.getCodeExpireTime());
    }

    private Long resolveCodeRemainingSeconds(AttendanceSession session) {
        if (!hasUsableSessionCode(session) || session == null || session.getCodeExpireTime() == null) {
            return 0L;
        }
        long seconds = Duration.between(LocalDateTime.now(), session.getCodeExpireTime()).getSeconds();
        return Math.max(seconds, 0L);
    }

    private String normalizeSignStatus(String signStatus) {
        String normalized = trimToNull(signStatus);
        if (normalized == null) {
            throw new RuntimeException("Sign status is required");
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if ("makeup".equals(normalized)) {
            return RECORD_STATUS_MAKEUP_PENDING;
        }
        if ("supplement".equals(normalized)) {
            return RECORD_STATUS_MAKEUP_APPROVED;
        }
        if ("anomaly".equals(normalized)) {
            return RECORD_STATUS_ABSENT;
        }
        if (!RECORD_STATUS_NORMAL.equals(normalized)
                && !RECORD_STATUS_LATE.equals(normalized)
                && !RECORD_STATUS_LEAVE.equals(normalized)
                && !RECORD_STATUS_ABSENT.equals(normalized)
                && !RECORD_STATUS_MAKEUP_PENDING.equals(normalized)
                && !RECORD_STATUS_MAKEUP_APPROVED.equals(normalized)
                && !RECORD_STATUS_MAKEUP_REJECTED.equals(normalized)) {
            throw new RuntimeException("Sign status is invalid");
        }
        return normalized;
    }

    private void writeChangeLog(AttendanceRecord record, String beforeStatus, String afterStatus,
                                String changedReason, User currentUser) {
        if (record == null || record.getId() == null) {
            return;
        }
        String normalizedBefore = trimToNull(beforeStatus);
        String normalizedAfter = trimToNull(afterStatus);
        if (Objects.equals(normalizedBefore, normalizedAfter)) {
            return;
        }

        AttendanceChangeLog changeLog = new AttendanceChangeLog();
        changeLog.setSessionId(record.getSessionId());
        changeLog.setRecordId(record.getId());
        changeLog.setLabId(record.getLabId());
        changeLog.setUserId(record.getUserId());
        changeLog.setBeforeStatus(normalizedBefore);
        changeLog.setAfterStatus(normalizedAfter);
        changeLog.setChangedBy(currentUser == null ? null : currentUser.getId());
        changeLog.setChangedReason(trimToNull(changedReason) == null ? "attendance status corrected" : changedReason.trim());
        attendanceChangeLogMapper.insert(changeLog);
    }

    private boolean shouldWriteSignTime(String signStatus) {
        if (signStatus == null) {
            return false;
        }
        return RECORD_STATUS_NORMAL.equals(signStatus)
                || RECORD_STATUS_LATE.equals(signStatus)
                || RECORD_STATUS_MAKEUP_APPROVED.equals(signStatus);
    }

    private String buildAttendancePhotoFileName(String originalFileName) {
        String extension = "";
        String safeOriginalName = trimToNull(originalFileName);
        if (safeOriginalName != null) {
            int dotIndex = safeOriginalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = safeOriginalName.substring(dotIndex).toLowerCase(Locale.ROOT);
            }
        }
        return "attendance-" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String generateSignCode(Integer length) {
        int realLength = limitRange(length, 4, 6, 4);
        StringBuilder builder = new StringBuilder(realLength);
        for (int index = 0; index < realLength; index++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    private String buildAttendanceSignLockKey(Long sessionId, Long userId) {
        return "attendance:sign:lock:" + sessionId + ":" + userId;
    }

    private int defaultPositive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private int limitRange(Integer value, int min, int max, int defaultValue) {
        int realValue = value == null ? defaultValue : value;
        if (realValue < min) {
            return min;
        }
        if (realValue > max) {
            return max;
        }
        return realValue;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApproveLeave(com.lab.recruitment.dto.AttendanceLeaveApprovalBatchDTO dto, User currentUser) {
        String action = dto.getAction();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new RuntimeException("审批动作只能为 APPROVE 或 REJECT");
        }

        String batchId = java.util.UUID.randomUUID().toString().replace("-", "");
        String targetLeaveStatus = "APPROVE".equals(action) ? "APPROVED" : "REJECTED";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        List<AttendanceLeave> leaves = attendanceLeaveMapper.selectBatchIds(dto.getLeaveIds());
        if (leaves.isEmpty()) {
            throw new RuntimeException("未找到对应的请假记录");
        }

        for (AttendanceLeave leave : leaves) {
            if (!"PENDING".equals(leave.getLeaveStatus())) {
                continue;
            }
            leave.setLeaveStatus(targetLeaveStatus);
            leave.setReviewComment(dto.getComment());
            leave.setReviewedBy(currentUser.getId());
            leave.setReviewTime(now);
            leave.setBatchId(batchId);
            attendanceLeaveMapper.updateById(leave);

            // 审批通过时，更新对应考勤记录状态为 LEAVE
            if ("APPROVED".equals(targetLeaveStatus) && leave.getSessionId() != null) {
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AttendanceRecord> wrapper =
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AttendanceRecord>()
                                .eq(AttendanceRecord::getSessionId, leave.getSessionId())
                                .eq(AttendanceRecord::getUserId, leave.getUserId());
                AttendanceRecord record = attendanceRecordMapper.selectOne(wrapper);
                if (record != null && !"LEAVE".equals(record.getSignStatus())) {
                    String beforeStatus = record.getSignStatus();
                    record.setSignStatus("LEAVE");
                    attendanceRecordMapper.updateById(record);

                    AttendanceChangeLog log = new AttendanceChangeLog();
                    log.setSessionId(record.getSessionId());
                    log.setRecordId(record.getId());
                    log.setLabId(record.getLabId());
                    log.setUserId(record.getUserId());
                    log.setBeforeStatus(beforeStatus);
                    log.setAfterStatus("LEAVE");
                    log.setChangedBy(currentUser.getId());
                    log.setChangedReason("批量审批请假通过");
                    attendanceChangeLogMapper.insert(log);
                }
            }

            // 发送通知给申请人
            if ("APPROVED".equals(targetLeaveStatus)) {
                systemNotificationService.createNotificationAsync(
                        leave.getUserId(),
                        "【请假审批】",
                        "您的请假申请已通过",
                        "ATTENDANCE",
                        leave.getId(),
                        "/student/attendance");
            } else {
                String reason = dto.getComment() != null ? dto.getComment() : "未说明原因";
                systemNotificationService.createNotificationAsync(
                        leave.getUserId(),
                        "【请假审批】",
                        "您的请假申请已被拒绝，原因：" + reason,
                        "ATTENDANCE",
                        leave.getId(),
                        "/student/attendance");
            }
        }
    }
}
