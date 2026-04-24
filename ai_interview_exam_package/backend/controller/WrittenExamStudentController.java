package com.lab.recruitment.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.service.WrittenExamService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/written-exam/student-exams")
public class WrittenExamStudentController {

    @Autowired
    private WrittenExamService writtenExamService;

    @Autowired
    private UserService userService;

    @GetMapping("/exams")
    public Result<?> getStudentExamList(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String status) {
        try {
            Long studentId = getCurrentUserId();
            Page<?> page = new Page<>(pageNum, pageSize);
            return Result.success(writtenExamService.getStudentExamList(page, studentId, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/detail/{examId}")
    public Result<?> getExamDetail(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getExamDetail(examId, getCurrentUserId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/eligibility/{examId}")
    public Result<?> checkEligibility(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.checkEligibility(examId, getCurrentUserId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    @PostMapping("/signature")
    public Result<?> submitSignature(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long examId = Long.parseLong(String.valueOf(body.get("examId")));
            String signature = String.valueOf(body.get("signature"));
            String ip = request.getRemoteAddr();
            String ua = request.getHeader("User-Agent");
            writtenExamService.submitSignature(examId, getCurrentUserId(), signature, ip, ua);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/start/{examId}")
    public Result<?> startExam(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.startExam(examId, getCurrentUserId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/save-answer")
    public Result<?> saveAnswer(@RequestBody Map<String, Object> body) {
        try {
            Long examId = Long.parseLong(String.valueOf(body.get("examId")));
            String answersJson = body.get("answers") == null ? null : JSON.toJSONString(body.get("answers"));
            writtenExamService.saveAnswer(examId, getCurrentUserId(), answersJson);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/submit-paper")
    public Result<?> submitPaper(@RequestBody Map<String, Object> body) {
        try {
            Long examId = Long.parseLong(String.valueOf(body.get("examId")));
            String answersJson = body.get("answers") == null ? null : JSON.toJSONString(body.get("answers"));
            writtenExamService.submitPaper(examId, getCurrentUserId(), answersJson);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/progress/{examId}")
    public Result<?> getProgress(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getProgress(examId, getCurrentUserId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cheat-report")
    public Result<?> reportCheatEvent(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long examId = Long.parseLong(String.valueOf(body.get("examId")));
            String eventType = String.valueOf(body.get("eventType"));
            String detail = body.get("detail") == null ? null : String.valueOf(body.get("detail"));
            String ip = request.getRemoteAddr();
            writtenExamService.reportCheatEvent(examId, getCurrentUserId(), eventType, detail, ip);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/result/{examId}")
    public Result<?> getSubmissionResult(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getSubmissionResult(examId, getCurrentUserId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/run-code")
    public Result<?> runCode(@RequestBody Map<String, Object> body) {
        try {
            Long questionId = Long.parseLong(String.valueOf(body.get("questionId")));
            String code = String.valueOf(body.get("code"));
            String language = String.valueOf(body.get("language"));
            String input = body.get("input") == null ? "" : String.valueOf(body.get("input"));
            return Result.success(writtenExamService.runCode(questionId, code, language, input));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/judge-code")
    public Result<?> judgeCode(@RequestBody Map<String, Object> body) {
        try {
            Long questionId = Long.parseLong(String.valueOf(body.get("questionId")));
            String code = String.valueOf(body.get("code"));
            String language = String.valueOf(body.get("language"));
            return Result.success(writtenExamService.judgeCode(questionId, code, language));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user.getId();
    }
}
