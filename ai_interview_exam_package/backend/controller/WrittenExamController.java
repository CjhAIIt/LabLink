package com.lab.recruitment.controller;

import com.alibaba.fastjson2.JSON;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/written-exam")
public class WrittenExamController {

    @Autowired
    private WrittenExamService writtenExamService;

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

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
                                                            @RequestParam(required = false) String status) {
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

    @GetMapping("/student/result/{labId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getStudentSubmissionResult(@PathVariable Long labId) {
        try {
            return Result.success(writtenExamService.getStudentSubmission(getCurrentUser(), labId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/eligibility/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> checkExamEligibility(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.checkEligibility(examId, getCurrentUser().getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/detail/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getExamDetail(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getExamDetail(examId, getCurrentUser().getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/signature")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> submitExamSignature(@RequestBody Map<String, Object> request) {
        try {
            Long examId = Long.parseLong(String.valueOf(request.get("examId")));
            String signatureName = request.get("signatureName") == null ? null : String.valueOf(request.get("signatureName"));
            writtenExamService.submitSignature(
                    examId,
                    getCurrentUser().getId(),
                    signatureName,
                    resolveClientIp(),
                    httpServletRequest.getHeader("User-Agent")
            );
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/start/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> startStudentExam(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.startExam(examId, getCurrentUser().getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/save-answer")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> saveStudentAnswer(@RequestBody Map<String, Object> request) {
        try {
            Long examId = Long.parseLong(String.valueOf(request.get("examId")));
            Object answers = request.get("answers");
            writtenExamService.saveAnswer(examId, getCurrentUser().getId(), JSON.toJSONString(answers));
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/submit-paper")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> submitExamPaper(@RequestBody Map<String, Object> request) {
        try {
            Long examId = Long.parseLong(String.valueOf(request.get("examId")));
            Object answers = request.get("answers");
            writtenExamService.submitPaper(examId, getCurrentUser().getId(), JSON.toJSONString(answers));
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/progress/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getStudentExamProgress(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getProgress(examId, getCurrentUser().getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/cheat-report")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> reportCheatEvent(@RequestBody Map<String, Object> request) {
        try {
            Long examId = Long.parseLong(String.valueOf(request.get("examId")));
            String eventType = request.get("eventType") == null ? null : String.valueOf(request.get("eventType"));
            String detail = request.get("detail") == null ? null : String.valueOf(request.get("detail"));
            writtenExamService.reportCheatEvent(examId, getCurrentUser().getId(), eventType, detail, resolveClientIp());
            return Result.success(true);
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

    @PostMapping("/student/run-code")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> runCode(@RequestBody Map<String, Object> request) {
        try {
            Long questionId = Long.parseLong(String.valueOf(request.get("questionId")));
            String code = String.valueOf(request.get("code"));
            String language = String.valueOf(request.get("language"));
            String input = request.get("input") == null ? "" : String.valueOf(request.get("input"));
            return Result.success(writtenExamService.runCode(questionId, code, language, input));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/student/judge-code")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> judgeCode(@RequestBody Map<String, Object> request) {
        try {
            Long questionId = Long.parseLong(String.valueOf(request.get("questionId")));
            String code = String.valueOf(request.get("code"));
            String language = String.valueOf(request.get("language"));
            return Result.success(writtenExamService.judgeCode(questionId, code, language));
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

    private String resolveClientIp() {
        String forwarded = httpServletRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = httpServletRequest.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return httpServletRequest.getRemoteAddr();
    }
}
