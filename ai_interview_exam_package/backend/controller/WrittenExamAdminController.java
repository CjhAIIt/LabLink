package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.*;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.service.WrittenExamService;
import com.lab.recruitment.utils.Result;
import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/written-exam/admin")
public class WrittenExamAdminController {

    @Autowired
    private WrittenExamService writtenExamService;

    @Autowired
    private UserService userService;

    @GetMapping("/exams")
    public Result<?> listExams(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) Long labId,
                               @RequestParam(required = false) String status) {
        try {
            Page<WrittenExam> page = new Page<>(pageNum, pageSize);
            return Result.success(writtenExamService.getExamList(page, labId, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/exams/{examId}")
    public Result<?> getExamDetail(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getById(examId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exams")
    public Result<?> createExam(@RequestBody WrittenExam exam) {
        try {
            return Result.success(writtenExamService.createExam(exam));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    @PutMapping("/exams/{examId}")
    public Result<?> updateExam(@PathVariable Long examId, @RequestBody WrittenExam exam) {
        try {
            writtenExamService.updateExam(examId, exam);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/exams/{examId}")
    public Result<?> deleteExam(@PathVariable Long examId) {
        try {
            writtenExamService.deleteExam(examId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exams/{examId}/publish")
    public Result<?> publishExam(@PathVariable Long examId) {
        try {
            writtenExamService.publishExam(examId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/questions")
    public Result<?> listQuestions(@RequestParam(required = false) Long labId,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String difficulty,
                                   @RequestParam(required = false) String keyword) {
        try {
            return Result.success(writtenExamService.getQuestionList(labId, type, difficulty, keyword));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/questions")
    public Result<?> createQuestion(@RequestBody WrittenExamQuestion question) {
        try {
            return Result.success(writtenExamService.createQuestion(question));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/questions/{questionId}")
    public Result<?> updateQuestion(@PathVariable Long questionId, @RequestBody WrittenExamQuestion question) {
        try {
            writtenExamService.updateQuestion(questionId, question);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/questions/{questionId}")
    public Result<?> deleteQuestion(@PathVariable Long questionId) {
        try {
            writtenExamService.deleteQuestion(questionId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    @GetMapping("/exams/{examId}/paper")
    public Result<?> getPaperQuestions(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getPaperQuestions(examId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exams/{examId}/paper")
    @SuppressWarnings("unchecked")
    public Result<?> savePaperQuestions(@PathVariable Long examId, @RequestBody List<WrittenExamPaperQuestion> questions) {
        try {
            writtenExamService.savePaperQuestions(examId, questions);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/grading")
    public Result<?> getGradingList(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) Long examId) {
        try {
            Page<WrittenExamAttempt> page = new Page<>(pageNum, pageSize);
            return Result.success(writtenExamService.getGradingList(page, examId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/grading/{attemptId}")
    public Result<?> getStudentAnswerDetail(@PathVariable Long attemptId) {
        try {
            return Result.success(writtenExamService.getStudentAttemptDetail(attemptId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/grading/{attemptId}")
    @SuppressWarnings("unchecked")
    public Result<?> submitGrading(@PathVariable Long attemptId, @RequestBody Map<String, Object> body) {
        try {
            List<WrittenExamAnswer> scores = JSON.parseArray(
                    JSON.toJSONString(body.get("scores")), WrittenExamAnswer.class);
            String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
            writtenExamService.submitGrading(attemptId, scores, remark);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exams/{examId}/publish-scores")
    public Result<?> publishScores(@PathVariable Long examId) {
        try {
            writtenExamService.publishScores(examId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/interview-invitations")
    @SuppressWarnings("unchecked")
    public Result<?> sendInterviewInvitations(@RequestBody Map<String, Object> body) {
        try {
            Long examId = Long.parseLong(String.valueOf(body.get("examId")));
            List<Long> studentIds = JSON.parseArray(
                    JSON.toJSONString(body.get("studentIds")), Long.class);
            String title = body.get("title") == null ? null : String.valueOf(body.get("title"));
            String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
            writtenExamService.sendInterviewInvitations(examId, studentIds, title, description);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/exams/{examId}/statistics")
    public Result<?> getExamStatistics(@PathVariable Long examId) {
        try {
            return Result.success(writtenExamService.getExamStatistics(examId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/cheat-logs")
    public Result<?> getCheatLogs(@RequestParam(required = false) Long examId) {
        try {
            return Result.success(writtenExamService.getCheatLogs(examId));
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
