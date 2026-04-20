package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AttendanceAnomalyHandleDTO;
import com.lab.recruitment.dto.AttendanceManageTagDTO;
import com.lab.recruitment.dto.AttendanceSessionCreateDTO;
import com.lab.recruitment.dto.AttendanceSessionExpireDTO;
import com.lab.recruitment.dto.AttendanceSessionSignDTO;
import com.lab.recruitment.entity.AttendanceRecord;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AttendanceAnomalyService;
import com.lab.recruitment.service.AttendanceDashboardService;
import com.lab.recruitment.service.AttendanceSessionService;
import com.lab.recruitment.service.AttendanceStatsService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.AttendanceDashboardVO;
import com.lab.recruitment.vo.AttendanceStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceSessionService attendanceSessionService;

    @Autowired
    private AttendanceDashboardService attendanceDashboardService;

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    @Autowired
    private AttendanceAnomalyService attendanceAnomalyService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping("/session/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> createSession(@Validated @RequestBody AttendanceSessionCreateDTO request) {
        try {
            return Result.success(attendanceSessionService.createSession(request.getLabId(), getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/session/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER', 'STUDENT')")
    public Result<Map<String, Object>> getActiveSession(@RequestParam(required = false) Long labId) {
        try {
            User currentUser = getCurrentUser();
            boolean includeSensitiveData = currentUserAccessor.isAdmin(currentUser)
                    || currentUserAccessor.isTeacherIdentity(currentUser);
            return Result.success(attendanceSessionService.getActiveSession(labId, currentUser, includeSensitiveData));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/session/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getSessionRecords(@RequestParam Long sessionId) {
        try {
            return Result.success(attendanceSessionService.listSessionRecords(sessionId, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/session/expire")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> expireSession(@Validated @RequestBody AttendanceSessionExpireDTO request) {
        try {
            return Result.success(attendanceSessionService.cancelSession(request.getSessionId(), getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/session/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> finalizeSession(@Validated @RequestBody AttendanceSessionExpireDTO request) {
        try {
            return Result.success(attendanceSessionService.finalizeSession(request.getSessionId(), getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/session/sign")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> sign(@Validated @RequestBody AttendanceSessionSignDTO request) {
        try {
            User currentUser = getCurrentUser();
            if (request.getSessionId() != null) {
                return Result.success(attendanceSessionService.signBySession(currentUser.getId(), request.getSessionId(), currentUser));
            }
            return Result.success(attendanceSessionService.signByCode(currentUser.getId(), request.getSignCode(), currentUser));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/manage/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getManageList(@RequestParam(required = false) Long labId,
                                                     @RequestParam(required = false) String date) {
        try {
            return Result.success(attendanceSessionService.getAttendanceList(labId, date, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/manage/tag")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> tagAttendance(@Validated @RequestBody AttendanceManageTagDTO request) {
        try {
            return Result.success(attendanceSessionService.tagAbsentMember(
                    request.getAttendanceId(),
                    request.getTagType(),
                    request.getReason(),
                    getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/manage/stat")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getManageStat(@RequestParam(required = false) Long labId,
                                                     @RequestParam(required = false) String date) {
        try {
            return Result.success(attendanceSessionService.getAttendanceStat(labId, date, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/manage/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long labId,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        byte[] bytes = attendanceSessionService.exportAttendanceExcel(labId, startDate, endDate, currentUser);
        String datePart = LocalDate.now().toString();
        String fileName = "attendance-" + datePart + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    private User getCurrentUser() {
        User user = currentUserAccessor.getCurrentUser();
        user.setPassword(null);
        return user;
    }

    // ==================== 今日出勤看板 ====================

    @GetMapping("/dashboard/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<AttendanceDashboardVO> getTodayDashboard(@RequestParam Long labId) {
        try {
            return Result.success(attendanceDashboardService.getTodayDashboard(labId, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    // ==================== 出勤统计报表 ====================

    @GetMapping("/stats/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<AttendanceStatsVO> getStatsSummary(@RequestParam Long labId,
                                                     @RequestParam(required = false) Long userId,
                                                     @RequestParam String startDate,
                                                     @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return Result.success(attendanceStatsService.getStats(labId, userId, start, end, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/stats/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<java.util.List<AttendanceStatsVO>> getStatsBatch(@RequestParam Long labId,
                                                                    @RequestParam String startDate,
                                                                    @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return Result.success(attendanceStatsService.getBatchStats(labId, start, end, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/stats/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportStats(@RequestParam Long labId,
                                              @RequestParam String startDate,
                                              @RequestParam String endDate) {
        User currentUser = getCurrentUser();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        byte[] bytes = attendanceStatsService.exportStats(labId, start, end, currentUser);
        String fileName = "attendance-stats-" + startDate + "-" + endDate + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // ==================== 异常出勤处理 ====================

    @GetMapping("/anomaly/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<AttendanceRecord>> listAnomalies(@RequestParam(required = false) Long labId,
                                                        @RequestParam(required = false) String date,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            LocalDate localDate = date != null ? LocalDate.parse(date) : null;
            return Result.success(attendanceAnomalyService.listAnomalies(labId, localDate, pageNum, pageSize, getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/anomaly/handle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> handleAnomalies(@Validated @RequestBody AttendanceAnomalyHandleDTO dto) {
        try {
            attendanceAnomalyService.handleAnomalies(dto, getCurrentUser());
            return Result.success("异常记录处理成功", null);
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }
}
