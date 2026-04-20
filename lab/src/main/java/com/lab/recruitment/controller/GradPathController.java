package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.GradPathBridgeService;
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

import java.util.Map;

@RestController
@RequestMapping("/gradpath")
public class GradPathController {

    @Autowired
    private GradPathBridgeService gradPathBridgeService;

    @Autowired
    private WrittenExamService writtenExamService;

    @Autowired
    private UserService userService;

    @GetMapping("/config")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, String>> getConfig() {
        try {
            return Result.success(gradPathBridgeService.getConfig());
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> getQuestions(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "9") Integer pageSize,
                                       @RequestParam(required = false) String trackCode,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String orderBy,
                                       @RequestParam(required = false) String orderDirection) {
        try {
            return Result.success(gradPathBridgeService.getQuestionList(
                    pageNum,
                    pageSize,
                    keyword,
                    orderBy,
                    orderDirection,
                    trackCode
            ));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> getQuestionDetail(@PathVariable Long questionId) {
        try {
            return Result.success(gradPathBridgeService.getQuestionDetail(questionId));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/questions/generate")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> generateQuestion(@RequestParam String keyword,
                                           @RequestParam(required = false) String trackCode) {
        try {
            return Result.success(gradPathBridgeService.generateQuestion(keyword, trackCode));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/judge/debug")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> debugCode(@RequestBody Map<String, Object> payload) {
        try {
            return Result.success(gradPathBridgeService.debugCode(payload));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/judge/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> submitCode(@RequestBody Map<String, Object> payload) {
        try {
            return Result.success(gradPathBridgeService.submitCode(payload));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/judge/analyze")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> analyzeCode(@RequestBody Map<String, Object> payload) {
        try {
            return Result.success(gradPathBridgeService.analyzeCode(payload));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/exam/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> completeExam(@RequestBody Map<String, Object> payload) {
        try {
            Long labId = Long.parseLong(String.valueOf(payload.get("labId")));
            return Result.success(writtenExamService.completeGradPathExam(getCurrentUser(), labId, payload));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
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
