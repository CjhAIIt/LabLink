package com.lab.recruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.dto.WrittenExamConfigDTO;
import com.lab.recruitment.dto.WrittenExamSubmitDTO;
import com.lab.recruitment.entity.*;

import java.util.List;
import java.util.Map;

public interface WrittenExamService extends IService<WrittenExam> {

    // ==================== Admin (new) ====================

    IPage<WrittenExam> getExamList(Page<WrittenExam> page, Long labId, String status);

    WrittenExam createExam(WrittenExam exam);

    void updateExam(Long examId, WrittenExam exam);

    void deleteExam(Long examId);

    void publishExam(Long examId);

    List<WrittenExamQuestion> getQuestionList(Long labId, String questionType, String difficulty, String keyword);

    WrittenExamQuestion createQuestion(WrittenExamQuestion question);

    void updateQuestion(Long questionId, WrittenExamQuestion question);

    void deleteQuestion(Long questionId);

    List<WrittenExamPaperQuestion> getPaperQuestions(Long examId);

    void savePaperQuestions(Long examId, List<WrittenExamPaperQuestion> questions);

    IPage<WrittenExamAttempt> getGradingList(Page<WrittenExamAttempt> page, Long examId);

    WrittenExamAttempt getStudentAttemptDetail(Long attemptId);

    void submitGrading(Long attemptId, List<WrittenExamAnswer> scores, String remark);

    void publishScores(Long examId);

    Map<String, Object> getExamStatistics(Long examId);

    // ==================== Student (new) ====================

    IPage<Map<String, Object>> getStudentExamList(Page<?> page, Long studentId, String status);

    Map<String, Object> getExamDetail(Long examId, Long studentId);

    Map<String, Object> checkEligibility(Long examId, Long studentId);

    void submitSignature(Long examId, Long studentId, String signatureName, String ip, String ua);

    Map<String, Object> startExam(Long examId, Long studentId);

    void saveAnswer(Long examId, Long studentId, String answersJson);

    void submitPaper(Long examId, Long studentId, String answersJson);

    Map<String, Object> getProgress(Long examId, Long studentId);

    void reportCheatEvent(Long examId, Long studentId, String eventType, String detail, String ip);

    Map<String, Object> getSubmissionResult(Long examId, Long studentId);

    // ==================== Judge ====================

    Map<String, Object> judgeCode(Long questionId, String code, String language);

    void sendInterviewInvitations(Long examId, List<Long> studentIds, String title, String description);

    List<WrittenExamCheatLog> getCheatLogs(Long examId);

    boolean canEnterInterview(Long labId, Long studentId);

    String getInterviewRequirementMessage(Long labId, Long studentId);

    Map<String, Object> completeGradPathExam(Object user, Long labId, Map<String, Object> payload);

    // ==================== Legacy (WrittenExamController) ====================

    Map<String, Object> getAdminConfig(User user);

    boolean saveAdminConfig(User user, WrittenExamConfigDTO configDTO);

    Page<Map<String, Object>> getAdminSubmissionPage(User user, Integer pageNum, Integer pageSize, Integer status, String realName);

    boolean reviewSubmission(User user, Long submissionId, Integer status, String adminRemark);

    Page<Map<String, Object>> getStudentLabPage(User user, Integer pageNum, Integer pageSize, String labName, String status);

    Map<String, Object> getStudentExam(User user, Long labId);

    Map<String, Object> getStudentSubmission(User user, Long labId);

    Map<String, Object> submitExam(User user, WrittenExamSubmitDTO submitDTO);

    List<SystemNotification> getMyNotifications(User user);

    boolean markNotificationRead(User user, Long notificationId);
}
