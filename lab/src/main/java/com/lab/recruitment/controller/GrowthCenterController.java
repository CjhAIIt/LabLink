package com.lab.recruitment.controller;

import com.lab.recruitment.dto.GrowthAssessmentSubmitDTO;
import com.lab.recruitment.dto.PracticeQuestionSubmitDTO;
import com.lab.recruitment.dto.WrittenExamQuestionDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.GrowthCenterService;
import com.lab.recruitment.service.GrowthPracticeQuestionService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.utils.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
@RequestMapping("/growth-center")
public class GrowthCenterController {

    @Autowired
    private GrowthCenterService growthCenterService;

    @Autowired
    private GrowthPracticeQuestionService growthPracticeQuestionService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getDashboard() {
        try {
            return Result.success(growthCenterService.getDashboard(getCurrentUser()));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/assessment/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getAssessmentQuestions() {
        try {
            return Result.success(growthCenterService.getAssessmentQuestions());
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/assessment/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> submitAssessment(@RequestBody GrowthAssessmentSubmitDTO submitDTO) {
        try {
            return Result.success(growthCenterService.submitAssessment(getCurrentUser(), submitDTO));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/tracks")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Map<String, Object>>> getTracks(@RequestParam(required = false) String category) {
        try {
            return Result.success(growthCenterService.getTracks(getCurrentUser(), category));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/tracks/{code}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getTrackDetail(@PathVariable String code) {
        try {
            return Result.success(growthCenterService.getTrackDetail(getCurrentUser(), code));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/practice/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getPracticeQuestions(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "9") Integer pageSize,
                                                            @RequestParam(required = false) String trackCode,
                                                            @RequestParam(required = false) String questionType,
                                                            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(growthPracticeQuestionService.getStudentQuestionPage(
                    getCurrentUser(), pageNum, pageSize, trackCode, questionType, keyword));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/practice/questions/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getPracticeQuestionDetail(@PathVariable Long questionId) {
        try {
            return Result.success(growthPracticeQuestionService.getStudentQuestionDetail(getCurrentUser(), questionId));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/practice/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> submitPracticeAnswer(@RequestBody PracticeQuestionSubmitDTO submitDTO) {
        try {
            return Result.success(growthPracticeQuestionService.submitPracticeAnswer(getCurrentUser(), submitDTO));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/admin/question-bank")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getAdminQuestionBank(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                                  @RequestParam(required = false) String trackCode,
                                                                  @RequestParam(required = false) String questionType,
                                                                  @RequestParam(required = false) String keyword) {
        try {
            return Result.success(growthPracticeQuestionService.getAdminQuestionPage(
                    getCurrentUser(), pageNum, pageSize, trackCode, questionType, keyword));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/admin/question-bank/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> getAdminQuestionDetail(@PathVariable Long questionId) {
        try {
            return Result.success(growthPracticeQuestionService.getAdminQuestionDetail(getCurrentUser(), questionId));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/admin/question-bank")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> saveAdminQuestion(@RequestBody WrittenExamQuestionDTO questionDTO) {
        try {
            return Result.success(growthPracticeQuestionService.saveAdminQuestion(getCurrentUser(), questionDTO));
        } catch (Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/admin/question-bank/delete/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> deleteAdminQuestion(@PathVariable Long questionId) {
        try {
            return Result.success(growthPracticeQuestionService.deleteAdminQuestion(getCurrentUser(), questionId));
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
