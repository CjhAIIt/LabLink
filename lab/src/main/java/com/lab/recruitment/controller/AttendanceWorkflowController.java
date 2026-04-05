package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AttendanceRecordReviewDTO;
import com.lab.recruitment.dto.AttendanceScheduleDTO;
import com.lab.recruitment.dto.AttendanceSignInDTO;
import com.lab.recruitment.dto.AttendanceTaskUpsertDTO;
import com.lab.recruitment.entity.AttendanceSchedule;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AttendanceWorkflowService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance-workflow")
public class AttendanceWorkflowController {

    @Autowired
    private AttendanceWorkflowService attendanceWorkflowService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getTaskPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) Long collegeId,
                                                         @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getTaskPage(pageNum, pageSize, collegeId, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> saveTask(@Validated @RequestBody AttendanceTaskUpsertDTO taskDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.saveTask(taskDTO, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> publishTask(@PathVariable Long taskId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.publishTask(taskId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/tasks/{taskId}/schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<AttendanceSchedule>> getTaskSchedules(@PathVariable Long taskId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getTaskSchedules(taskId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<AttendanceSchedule>> saveTaskSchedules(@PathVariable Long taskId,
                                                              @RequestBody List<AttendanceScheduleDTO> schedules) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.saveTaskSchedules(taskId, schedules, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> getSummary(@RequestParam(required = false) Long taskId,
                                                  @RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getAttendanceSummary(taskId, labId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/lab/session/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getCurrentLabSession() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getCurrentLabSession(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/lab/session/current/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> getCurrentLabSessionRecords() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getCurrentLabSessionRecords(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/lab/records/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> reviewLabAttendanceRecord(@Validated @RequestBody AttendanceRecordReviewDTO reviewDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.reviewLabAttendanceRecord(reviewDTO, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/lab/session/current/photo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> uploadCurrentSessionPhoto(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(value = "remark", required = false) String remark) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.uploadCurrentSessionPhoto(file, remark, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/session/current")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getCurrentStudentSession() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getCurrentStudentSession(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/session/sign-in")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> studentSignIn(@Validated @RequestBody AttendanceSignInDTO signInDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.studentSignIn(signInDTO, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/history")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<Map<String, Object>>> getStudentHistory(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(attendanceWorkflowService.getStudentHistory(pageNum, pageSize, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
