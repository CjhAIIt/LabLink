package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.WrittenExamConfigDTO;
import com.lab.recruitment.dto.WrittenExamSubmitDTO;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.service.WrittenExamService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/written-exam")
public class WrittenExamController {

    @Autowired
    private WrittenExamService writtenExamService;

    @Autowired
    private UserService userService;

    @GetMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAdminConfig() {
        try {
            return Result.success(writtenExamService.getAdminConfig(getCurrentUser()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> saveAdminConfig(@RequestBody WrittenExamConfigDTO configDTO) {
        try {
            return Result.success(writtenExamService.saveAdminConfig(getCurrentUser(), configDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/admin/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<Map<String, Object>>> getAdminSubmissions(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(required = false) String realName) {
        try {
            return Result.success(writtenExamService.getAdminSubmissionPage(getCurrentUser(), pageNum, pageSize, status, realName));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/admin/review")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> reviewSubmission(@RequestBody Map<String, Object> request) {
        try {
            Long submissionId = Long.parseLong(String.valueOf(request.get("submissionId")));
            Integer status = Integer.parseInt(String.valueOf(request.get("status")));
            String adminRemark = request.get("adminRemark") == null ? null : String.valueOf(request.get("adminRemark"));
            return Result.success(writtenExamService.reviewSubmission(getCurrentUser(), submissionId, status, adminRemark));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/labs")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<Map<String, Object>>> getStudentLabs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "6") Integer pageSize,
                                                            @RequestParam(required = false) String labName,
                                                            @RequestParam(required = false) Integer status) {
        try {
            return Result.success(writtenExamService.getStudentLabPage(getCurrentUser(), pageNum, pageSize, labName, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/exam/{labId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getStudentExam(@PathVariable Long labId) {
        try {
            return Result.success(writtenExamService.getStudentExam(getCurrentUser(), labId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/submission/{labId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getStudentSubmission(@PathVariable Long labId) {
        try {
            return Result.success(writtenExamService.getStudentSubmission(getCurrentUser(), labId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> submitExam(@RequestBody WrittenExamSubmitDTO submitDTO) {
        try {
            return Result.success(writtenExamService.submitExam(getCurrentUser(), submitDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/gradpath-complete")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> completeGradPathExam(@RequestBody Map<String, Object> request) {
        try {
            Long labId = Long.parseLong(String.valueOf(request.get("labId")));
            return Result.success(writtenExamService.completeGradPathExam(getCurrentUser(), labId, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/notifications")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<SystemNotification>> getMyNotifications() {
        try {
            return Result.success(writtenExamService.getMyNotifications(getCurrentUser()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/notifications/read/{notificationId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> markNotificationRead(@PathVariable Long notificationId) {
        try {
            return Result.success(writtenExamService.markNotificationRead(getCurrentUser(), notificationId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }
}
