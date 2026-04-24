package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.dto.JudgeCaseDTO;
import com.lab.recruitment.entity.*;
import com.lab.recruitment.mapper.*;
import com.lab.recruitment.service.WrittenExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WrittenExamServiceImpl extends ServiceImpl<WrittenExamMapper, WrittenExam> implements WrittenExamService {

    @Autowired
    private WrittenExamMapper writtenExamMapper;
    @Autowired
    private WrittenExamQuestionMapper writtenExamQuestionMapper;
    @Autowired
    private WrittenExamPaperQuestionMapper writtenExamPaperQuestionMapper;
    @Autowired
    private WrittenExamAttemptMapper writtenExamAttemptMapper;
    @Autowired
    private WrittenExamAnswerMapper writtenExamAnswerMapper;
    @Autowired
    private WrittenExamProgressMapper writtenExamProgressMapper;
    @Autowired
    private WrittenExamSignatureMapper writtenExamSignatureMapper;
    @Autowired
    private WrittenExamCheatLogMapper writtenExamCheatLogMapper;
    @Autowired
    private InterviewInvitationMapper interviewInvitationMapper;
    @Autowired
    private DeliveryMapper deliveryMapper;
    @Autowired
    private LabMapper labMapper;
    @Autowired
    private CodeJudgeService codeJudgeService;

    // ==================== Admin: Exam CRUD ====================

    @Override
    public IPage<WrittenExam> getExamList(Page<WrittenExam> page, Long labId, String status) {
        LambdaQueryWrapper<WrittenExam> wrapper = new LambdaQueryWrapper<>();
        if (labId != null) {
            wrapper.eq(WrittenExam::getLabId, labId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(WrittenExam::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByDesc(WrittenExam::getCreateTime);
        return writtenExamMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public WrittenExam createExam(WrittenExam exam) {
        exam.setCreateTime(LocalDateTime.now());
        exam.setUpdateTime(LocalDateTime.now());
        writtenExamMapper.insert(exam);
        return exam;
    }

    @Override
    @Transactional
    public void updateExam(Long examId, WrittenExam exam) {
        WrittenExam existing = writtenExamMapper.selectById(examId);
        if (existing == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        exam.setId(examId);
        exam.setUpdateTime(LocalDateTime.now());
        writtenExamMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void deleteExam(Long examId) {
        WrittenExam existing = writtenExamMapper.selectById(examId);
        if (existing == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        writtenExamMapper.deleteById(examId);
    }
    @Override
    @Transactional
    public void publishExam(Long examId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        exam.setStatus(1);
        exam.setUpdateTime(LocalDateTime.now());
        writtenExamMapper.updateById(exam);
    }

    // ==================== Admin: Question CRUD ====================

    @Override
    public List<WrittenExamQuestion> getQuestionList(Long labId, String questionType, String difficulty, String keyword) {
        LambdaQueryWrapper<WrittenExamQuestion> wrapper = new LambdaQueryWrapper<>();
        if (labId != null) {
            wrapper.eq(WrittenExamQuestion::getLabId, labId);
        }
        if (StringUtils.hasText(questionType)) {
            wrapper.eq(WrittenExamQuestion::getQuestionType, questionType);
        }
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(WrittenExamQuestion::getDifficulty, difficulty);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(WrittenExamQuestion::getTitle, keyword);
        }
        wrapper.orderByAsc(WrittenExamQuestion::getSortOrder);
        return writtenExamQuestionMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public WrittenExamQuestion createQuestion(WrittenExamQuestion question) {
        normalizeQuestionForPersistence(question);
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());
        writtenExamQuestionMapper.insert(question);
        return question;
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, WrittenExamQuestion question) {
        WrittenExamQuestion existing = writtenExamQuestionMapper.selectById(questionId);
        if (existing == null) {
            throw new RuntimeException("Question not found: " + questionId);
        }
        normalizeQuestionForPersistence(question);
        question.setId(questionId);
        question.setUpdateTime(LocalDateTime.now());
        writtenExamQuestionMapper.updateById(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        writtenExamQuestionMapper.deleteById(questionId);
    }
    // ==================== Admin: Paper ====================

    @Override
    public List<WrittenExamPaperQuestion> getPaperQuestions(Long examId) {
        LambdaQueryWrapper<WrittenExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrittenExamPaperQuestion::getExamId, examId)
               .orderByAsc(WrittenExamPaperQuestion::getSortOrder);
        return writtenExamPaperQuestionMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void savePaperQuestions(Long examId, List<WrittenExamPaperQuestion> questions) {
        // Remove existing paper questions for this exam
        LambdaQueryWrapper<WrittenExamPaperQuestion> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WrittenExamPaperQuestion::getExamId, examId);
        writtenExamPaperQuestionMapper.delete(deleteWrapper);
        // Insert new ones
        if (questions != null) {
            int sortOrder = 1;
            for (WrittenExamPaperQuestion pq : questions) {
                pq.setExamId(examId);
                if (pq.getSortOrder() == null) {
                    pq.setSortOrder(sortOrder);
                }
                pq.setCreateTime(LocalDateTime.now());
                writtenExamPaperQuestionMapper.insert(pq);
                sortOrder++;
            }
        }
    }

    // ==================== Admin: Grading ====================

    @Override
    public IPage<WrittenExamAttempt> getGradingList(Page<WrittenExamAttempt> page, Long examId) {
        LambdaQueryWrapper<WrittenExamAttempt> wrapper = new LambdaQueryWrapper<>();
        if (examId != null) {
            wrapper.eq(WrittenExamAttempt::getExamId, examId);
        }
        wrapper.eq(WrittenExamAttempt::getStatus, 2); // submitted
        wrapper.orderByDesc(WrittenExamAttempt::getSubmitTime);
        return writtenExamAttemptMapper.selectPage(page, wrapper);
    }

    @Override
    public WrittenExamAttempt getStudentAttemptDetail(Long attemptId) {
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new RuntimeException("Attempt not found: " + attemptId);
        }
        return attempt;
    }
    @Override
    @Transactional
    public void submitGrading(Long attemptId, List<WrittenExamAnswer> scores, String remark) {
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new RuntimeException("Attempt not found: " + attemptId);
        }
        int manualScore = 0;
        if (scores != null) {
            for (WrittenExamAnswer ans : scores) {
                WrittenExamAnswer existing = writtenExamAnswerMapper.selectById(ans.getId());
                if (existing != null) {
                    existing.setScore(ans.getScore());
                    existing.setGraderRemark(ans.getGraderRemark());
                    existing.setUpdateTime(LocalDateTime.now());
                    writtenExamAnswerMapper.updateById(existing);
                    if (ans.getScore() != null) {
                        manualScore += ans.getScore();
                    }
                }
            }
        }
        attempt.setManualScore(manualScore);
        attempt.setTotalScore((attempt.getAutoScore() == null ? 0 : attempt.getAutoScore()) + manualScore);
        attempt.setRemark(remark);
        attempt.setStatus(3); // graded
        attempt.setGradedTime(LocalDateTime.now());
        attempt.setUpdateTime(LocalDateTime.now());
        writtenExamAttemptMapper.updateById(attempt);
    }

    @Override
    @Transactional
    public void publishScores(Long examId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        // Get all graded attempts, rank by total score desc
        LambdaQueryWrapper<WrittenExamAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrittenExamAttempt::getExamId, examId)
               .in(WrittenExamAttempt::getStatus, 2, 3)
               .orderByDesc(WrittenExamAttempt::getTotalScore);
        List<WrittenExamAttempt> attempts = writtenExamAttemptMapper.selectList(wrapper);

        int rank = 1;
        Integer passScore = exam.getPassScore();
        for (WrittenExamAttempt attempt : attempts) {
            int total = attempt.getTotalScore() == null ? 0 : attempt.getTotalScore();
            boolean passed = passScore == null || total >= passScore;
            attempt.setPassed(passed);
            attempt.setStatus(4); // score published
            attempt.setUpdateTime(LocalDateTime.now());
            writtenExamAttemptMapper.updateById(attempt);
            rank++;
        }
        exam.setStatus(3); // scores published
        exam.setUpdateTime(LocalDateTime.now());
        writtenExamMapper.updateById(exam);
    }
    @Override
    public Map<String, Object> getExamStatistics(Long examId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        LambdaQueryWrapper<WrittenExamAttempt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrittenExamAttempt::getExamId, examId);
        List<WrittenExamAttempt> attempts = writtenExamAttemptMapper.selectList(wrapper);

        int totalAttempts = attempts.size();
        int submitted = (int) attempts.stream().filter(a -> a.getStatus() != null && a.getStatus() >= 2).count();
        int passed = (int) attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double avgScore = attempts.stream()
                .filter(a -> a.getTotalScore() != null)
                .mapToInt(WrittenExamAttempt::getTotalScore)
                .average().orElse(0);
        int maxScore = attempts.stream()
                .filter(a -> a.getTotalScore() != null)
                .mapToInt(WrittenExamAttempt::getTotalScore)
                .max().orElse(0);
        int minScore = attempts.stream()
                .filter(a -> a.getTotalScore() != null)
                .mapToInt(WrittenExamAttempt::getTotalScore)
                .min().orElse(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("examId", examId);
        stats.put("totalAttempts", totalAttempts);
        stats.put("submitted", submitted);
        stats.put("passed", passed);
        stats.put("passRate", totalAttempts > 0 ? (double) passed / totalAttempts : 0);
        stats.put("avgScore", avgScore);
        stats.put("maxScore", maxScore);
        stats.put("minScore", minScore);
        return stats;
    }

    // ==================== Student ====================

    @Override
    public IPage<Map<String, Object>> getStudentExamList(Page<?> page, Long studentId, String status) {
        LambdaQueryWrapper<WrittenExam> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(WrittenExam::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByDesc(WrittenExam::getCreateTime);
        Page<WrittenExam> examPage = writtenExamMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (WrittenExam exam : examPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("exam", exam);
            // Check if student has an attempt
            LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
            aw.eq(WrittenExamAttempt::getExamId, exam.getId())
              .eq(WrittenExamAttempt::getStudentId, studentId)
              .last("LIMIT 1");
            WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
            item.put("attempted", attempt != null);
            item.put("attemptStatus", attempt == null ? null : attempt.getStatus());
            item.put("totalScore", attempt == null ? null : attempt.getTotalScore());
            records.add(item);
        }
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize());
        result.setRecords(records);
        result.setTotal(examPage.getTotal());
        return result;
    }
    @Override
    public Map<String, Object> getExamDetail(Long examId, Long studentId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        Map<String, Object> detail = buildStudentExamSummary(exam);
        List<WrittenExamPaperQuestion> paperQuestions = getPaperQuestions(examId);
        detail.put("paperQuestions", paperQuestions);
        detail.put("questions", buildStudentQuestionPayload(paperQuestions));
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, examId)
          .eq(WrittenExamAttempt::getStudentId, studentId)
          .orderByDesc(WrittenExamAttempt::getId)
          .last("LIMIT 1");
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
        detail.put("attempt", attempt);
        detail.put("alreadySubmitted", attempt != null && attempt.getStatus() != null && attempt.getStatus() >= 2);
        detail.put("submission", buildSubmissionPayload(exam, attempt));
        return detail;
    }

    @Override
    public Map<String, Object> checkEligibility(Long examId, Long studentId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        Map<String, Object> result = new HashMap<>();
        // Students can enter the written exam directly without applying to the lab first.
        // Only block re-entry when a submitted attempt exists and the exam does not allow retry.
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, examId)
          .eq(WrittenExamAttempt::getStudentId, studentId)
          .orderByDesc(WrittenExamAttempt::getId)
          .last("LIMIT 1");
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
        boolean alreadyAttempted = attempt != null;
        boolean alreadySubmitted = attempt != null && attempt.getStatus() != null && attempt.getStatus() >= 2;
        boolean allowRetry = Boolean.TRUE.equals(exam.getAllowRetry());
        boolean canTake = !alreadySubmitted || allowRetry;

        result.put("eligible", canTake);
        result.put("alreadyAttempted", alreadyAttempted);
        result.put("allowRetry", allowRetry);
        result.put("canTake", canTake);
        result.put("reason", canTake ? "可直接参加考试" : "您已参加过本场考试，当前不允许重考");
        return result;
    }
    @Override
    @Transactional
    public void submitSignature(Long examId, Long studentId, String signatureName, String ip, String ua) {
        WrittenExamSignature sig = new WrittenExamSignature();
        sig.setExamId(examId);
        sig.setStudentId(studentId);
        sig.setSignatureName(signatureName);
        sig.setIpAddress(ip);
        sig.setUserAgent(ua);
        sig.setSignTime(LocalDateTime.now());
        writtenExamSignatureMapper.insert(sig);
    }

    @Override
    @Transactional
    public Map<String, Object> startExam(Long examId, Long studentId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new RuntimeException("Exam has not started yet");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new RuntimeException("Exam has already ended");
        }
        // Check existing attempt
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, examId)
          .eq(WrittenExamAttempt::getStudentId, studentId)
          .last("LIMIT 1");
        WrittenExamAttempt existing = writtenExamAttemptMapper.selectOne(aw);
        if (existing != null && existing.getStatus() != null && existing.getStatus() >= 2) {
            throw new RuntimeException("You have already submitted this exam");
        }
        // Resume or create attempt
        WrittenExamAttempt attempt;
        if (existing != null) {
            attempt = existing;
        } else {
            attempt = new WrittenExamAttempt();
            attempt.setExamId(examId);
            attempt.setStudentId(studentId);
            attempt.setStatus(1); // in progress
            attempt.setStartTime(now);
            attempt.setSwitchCount(0);
            attempt.setRefreshCount(0);
            writtenExamAttemptMapper.insert(attempt);
        }
        // Calculate remaining time
        int duration = exam.getDuration() == null ? 120 : exam.getDuration();
        long elapsed = ChronoUnit.MINUTES.between(attempt.getStartTime(), now);
        long remaining = Math.max(0, duration - elapsed);

        // Get questions
        List<WrittenExamPaperQuestion> paperQuestions = getPaperQuestions(examId);
        List<Map<String, Object>> questions = buildStudentQuestionPayload(paperQuestions);

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("title", exam.getTitle());
        result.put("questions", questions);
        result.put("remainingMinutes", remaining);
        result.put("remainingSeconds", remaining * 60);
        return result;
    }
    @Override
    @Transactional
    public void saveAnswer(Long examId, Long studentId, String answersJson) {
        LambdaQueryWrapper<WrittenExamProgress> pw = new LambdaQueryWrapper<>();
        pw.eq(WrittenExamProgress::getExamId, examId)
          .eq(WrittenExamProgress::getStudentId, studentId)
          .last("LIMIT 1");
        WrittenExamProgress progress = writtenExamProgressMapper.selectOne(pw);
        if (progress == null) {
            progress = new WrittenExamProgress();
            progress.setExamId(examId);
            progress.setStudentId(studentId);
            progress.setAnswersJson(answersJson);
            progress.setSaveTime(LocalDateTime.now());
            writtenExamProgressMapper.insert(progress);
        } else {
            progress.setAnswersJson(answersJson);
            progress.setSaveTime(LocalDateTime.now());
            writtenExamProgressMapper.updateById(progress);
        }
    }

    @Override
    @Transactional
    public void submitPaper(Long examId, Long studentId, String answersJson) {
        // Find the attempt
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, examId)
          .eq(WrittenExamAttempt::getStudentId, studentId)
          .last("LIMIT 1");
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
        if (attempt == null) {
            throw new RuntimeException("No active attempt found. Please start the exam first.");
        }
        if (attempt.getStatus() != null && attempt.getStatus() >= 2) {
            throw new RuntimeException("This exam has already been submitted");
        }
        // Check time expiry
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam != null && exam.getDuration() != null) {
            long elapsed = ChronoUnit.MINUTES.between(attempt.getStartTime(), LocalDateTime.now());
            if (elapsed > exam.getDuration() + 1) { // 1 min grace
                throw new RuntimeException("Exam time has expired");
            }
        }
        // Parse answers and auto-grade objective questions
        JSONArray answersArray = StringUtils.hasText(answersJson) ? JSON.parseArray(answersJson) : new JSONArray();
        int autoScore = 0;
        for (int i = 0; i < answersArray.size(); i++) {
            JSONObject ansObj = answersArray.getJSONObject(i);
            Long questionId = ansObj.getLong("questionId");
            String answer = ansObj.getString("answer");
            String code = ansObj.getString("code");
            String language = ansObj.getString("language");

            WrittenExamQuestion question = questionId == null ? null : writtenExamQuestionMapper.selectById(questionId);
            WrittenExamAnswer examAnswer = new WrittenExamAnswer();
            examAnswer.setAttemptId(attempt.getId());
            examAnswer.setQuestionId(questionId);
            examAnswer.setAnswer(answer);
            examAnswer.setCode(code);
            examAnswer.setLanguage(language);
            examAnswer.setCreateTime(LocalDateTime.now());

            if (question != null) {
                String type = question.getQuestionType();
                if ("single_choice".equals(type)) {
                    boolean correct = answer != null && answer.trim().equalsIgnoreCase(
                            question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer().trim());
                    examAnswer.setIsCorrect(correct);
                    examAnswer.setScore(correct ? (question.getScore() == null ? 0 : question.getScore()) : 0);
                    autoScore += examAnswer.getScore();
                } else if ("fill_blank".equals(type)) {
                    String correctAnswer = question.getCorrectAnswer();
                    boolean correct = answer != null && correctAnswer != null
                            && answer.trim().equalsIgnoreCase(correctAnswer.trim());
                    examAnswer.setIsCorrect(correct);
                    examAnswer.setScore(correct ? (question.getScore() == null ? 0 : question.getScore()) : 0);
                    autoScore += examAnswer.getScore();
                } else {
                    // programming / subjective - leave for manual grading
                    examAnswer.setScore(0);
                }
            }
            writtenExamAnswerMapper.insert(examAnswer);
        }
        attempt.setAutoScore(autoScore);
        attempt.setTotalScore(autoScore);
        attempt.setStatus(2); // submitted
        attempt.setSubmitTime(LocalDateTime.now());
        attempt.setUpdateTime(LocalDateTime.now());
        writtenExamAttemptMapper.updateById(attempt);
    }
    @Override
    public Map<String, Object> getProgress(Long examId, Long studentId) {
        LambdaQueryWrapper<WrittenExamProgress> pw = new LambdaQueryWrapper<>();
        pw.eq(WrittenExamProgress::getExamId, examId)
          .eq(WrittenExamProgress::getStudentId, studentId)
          .last("LIMIT 1");
        WrittenExamProgress progress = writtenExamProgressMapper.selectOne(pw);
        Map<String, Object> result = new HashMap<>();
        if (progress != null) {
            result.put("answersJson", progress.getAnswersJson());
            result.put("answers", StringUtils.hasText(progress.getAnswersJson()) ? JSON.parseArray(progress.getAnswersJson()) : new JSONArray());
            result.put("remainingSeconds", progress.getRemainingSeconds());
            result.put("currentIndex", progress.getCurrentIndex());
            result.put("flaggedIds", progress.getFlaggedIds());
            result.put("saveTime", progress.getSaveTime());
        }
        return result;
    }

    @Override
    @Transactional
    public void reportCheatEvent(Long examId, Long studentId, String eventType, String detail, String ip) {
        // Find attempt
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, examId)
          .eq(WrittenExamAttempt::getStudentId, studentId)
          .last("LIMIT 1");
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);

        WrittenExamCheatLog cheatLog = new WrittenExamCheatLog();
        cheatLog.setExamId(examId);
        cheatLog.setStudentId(studentId);
        cheatLog.setAttemptId(attempt == null ? null : attempt.getId());
        cheatLog.setEventType(eventType);
        cheatLog.setDetail(detail);
        cheatLog.setIpAddress(ip);
        cheatLog.setCreateTime(LocalDateTime.now());
        writtenExamCheatLogMapper.insert(cheatLog);

        // Increment switch count on attempt if tab-switch event
        if (attempt != null && "tab_switch".equals(eventType)) {
            attempt.setSwitchCount((attempt.getSwitchCount() == null ? 0 : attempt.getSwitchCount()) + 1);
            attempt.setUpdateTime(LocalDateTime.now());
            writtenExamAttemptMapper.updateById(attempt);
        }
    }

    @Override
    public Map<String, Object> getSubmissionResult(Long examId, Long studentId) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, examId)
          .eq(WrittenExamAttempt::getStudentId, studentId)
          .orderByDesc(WrittenExamAttempt::getId)
          .last("LIMIT 1");
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
        if (attempt == null) {
            return null;
        }
        Map<String, Object> result = buildSubmissionPayload(exam, attempt);
        result.put("attempt", attempt);
        LambdaQueryWrapper<WrittenExamAnswer> ansWrapper = new LambdaQueryWrapper<>();
        ansWrapper.eq(WrittenExamAnswer::getAttemptId, attempt.getId());
        result.put("answers", writtenExamAnswerMapper.selectList(ansWrapper));
        return result;
    }
    // ==================== Local Code Judge ====================

    @Override
    public Map<String, Object> judgeCode(Long questionId, String code, String language) {
        WrittenExamQuestion question = writtenExamQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("Question not found: " + questionId);
        }
        List<JudgeCaseDTO> judgeCases = parseTestCases(question.getTestCases());
        if (judgeCases.isEmpty()) {
            // fallback: use sampleCase if testCases is empty
            judgeCases = parseSampleCaseAsJudgeCase(question.getSampleCase());
        }
        if (judgeCases.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "该题目未配置测试用例");
            return errorResult;
        }
        CodeJudgeService.JudgeResult judgeResult = codeJudgeService.judge(language, code, judgeCases);
        Map<String, Object> result = new HashMap<>();
        if (!judgeResult.isAvailable()) {
            result.put("status", "error");
            result.put("message", judgeResult.getMessage());
        } else if (judgeResult.isSuccess()) {
            result.put("status", "success");
            result.put("correct", true);
            result.put("message", "通过 " + judgeResult.getPassedCount() + "/" + judgeResult.getTotalCount() + " 个测试用例");
        } else {
            result.put("status", "wrong_answer");
            result.put("correct", false);
            result.put("message", judgeResult.getMessage());
        }
        result.put("passedCount", judgeResult.getPassedCount());
        result.put("totalCount", judgeResult.getTotalCount());
        return result;
    }

    @Override
    public Map<String, Object> runCode(Long questionId, String code, String language, String input) {
        WrittenExamQuestion question = writtenExamQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("Question not found: " + questionId);
        }
        CodeJudgeService.RunResult runResult = codeJudgeService.run(language, code, input);
        Map<String, Object> result = new HashMap<>();
        result.put("status", runResult.getStatus());
        result.put("stdout", runResult.getStdout());
        result.put("stderr", runResult.getStderr());
        result.put("error", runResult.getError());
        if (runResult.isTimedOut()) {
            result.put("status", "time_limit");
            result.put("message", "运行超时");
        }
        return result;
    }

    private List<JudgeCaseDTO> parseTestCases(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, JudgeCaseDTO.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<JudgeCaseDTO> parseSampleCaseAsJudgeCase(String sampleCase) {
        if (!StringUtils.hasText(sampleCase)) {
            return Collections.emptyList();
        }
        try {
            JSONObject obj = JSON.parseObject(sampleCase);
            JudgeCaseDTO dto = new JudgeCaseDTO();
            dto.setInput(obj.getString("input"));
            dto.setOutput(obj.getString("output"));
            if (StringUtils.hasText(dto.getOutput())) {
                return List.of(dto);
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    // ==================== Interview Invitations ====================

    @Override
    @Transactional
    public void sendInterviewInvitations(Long examId, List<Long> studentIds, String title, String description) {
        WrittenExam exam = writtenExamMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("Exam not found: " + examId);
        }
        if (studentIds == null || studentIds.isEmpty()) {
            throw new RuntimeException("No students selected");
        }
        for (Long studentId : studentIds) {
            InterviewInvitation invitation = new InterviewInvitation();
            invitation.setExamId(examId);
            invitation.setLabId(exam.getLabId());
            invitation.setStudentId(studentId);
            invitation.setTitle(StringUtils.hasText(title) ? title : "Interview Invitation");
            invitation.setDescription(description);
            invitation.setStatus(0); // pending
            invitation.setStudentConfirmed(false);
            invitation.setCreateTime(LocalDateTime.now());
            invitation.setUpdateTime(LocalDateTime.now());
            interviewInvitationMapper.insert(invitation);
        }
    }

    // ==================== Cheat Logs ====================

    @Override
    public List<WrittenExamCheatLog> getCheatLogs(Long examId) {
        LambdaQueryWrapper<WrittenExamCheatLog> wrapper = new LambdaQueryWrapper<>();
        if (examId != null) {
            wrapper.eq(WrittenExamCheatLog::getExamId, examId);
        }
        wrapper.orderByDesc(WrittenExamCheatLog::getCreateTime);
        return writtenExamCheatLogMapper.selectList(wrapper);
    }

    @Override
    public boolean canEnterInterview(Long labId, Long studentId) {
        // 查找该实验室下所有已发布成绩的笔试中，该学生是否有通过记录
        LambdaQueryWrapper<WrittenExam> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(WrittenExam::getLabId, labId)
                   .eq(WrittenExam::getDeleted, 0);
        List<WrittenExam> exams = baseMapper.selectList(examWrapper);
        if (exams.isEmpty()) {
            // 该实验室没有笔试，默认允许进入面试
            return true;
        }
        List<Long> examIds = exams.stream().map(WrittenExam::getId).toList();
        LambdaQueryWrapper<WrittenExamAttempt> attemptWrapper = new LambdaQueryWrapper<>();
        attemptWrapper.in(WrittenExamAttempt::getExamId, examIds)
                      .eq(WrittenExamAttempt::getStudentId, studentId)
                      .eq(WrittenExamAttempt::getPassed, true)
                      .eq(WrittenExamAttempt::getDeleted, 0);
        return writtenExamAttemptMapper.selectCount(attemptWrapper) > 0;
    }

    @Override
    public String getInterviewRequirementMessage(Long labId, Long studentId) {
        if (canEnterInterview(labId, studentId)) {
            return "Eligible to enter interview";
        }
        return "You must pass at least one written exam for this lab before submitting an interview application";
    }

    @Override
    public Map<String, Object> completeGradPathExam(Object user, Long labId, Map<String, Object> payload) {
        // 从GradPath模块调用，提交笔试并返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("labId", labId);
        result.put("completed", true);
        result.put("message", "笔试已完成");
        // 如果payload中包含answers，尝试自动提交
        if (user != null && payload.containsKey("answers")) {
            try {
                // 获取user的id（通过反射兼容不同User类型）
                Long studentId = (Long) user.getClass().getMethod("getId").invoke(user);
                String answersJson = com.alibaba.fastjson2.JSON.toJSONString(payload.get("answers"));
                submitPaper(labId, studentId, answersJson);
                result.put("submitted", true);
            } catch (Exception e) {
                result.put("submitted", false);
                result.put("error", e.getMessage());
            }
        }
        return result;
    }

    // ==================== Legacy methods (WrittenExamController compatibility) ====================

    @Override
    public Map<String, Object> getAdminConfig(User user) {
        Map<String, Object> config = new HashMap<>();
        // Return exam config for the admin's lab
        Long labId = user.getLabId();
        if (labId != null) {
            LambdaQueryWrapper<WrittenExam> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WrittenExam::getLabId, labId).eq(WrittenExam::getDeleted, 0).orderByDesc(WrittenExam::getCreateTime).last("LIMIT 1");
            WrittenExam exam = baseMapper.selectOne(wrapper);
            if (exam != null) {
                config.put("exam", exam);
                config.put("hasExam", true);
            }
        }
        config.put("hasExam", config.containsKey("exam"));
        return config;
    }

    @Override
    public boolean saveAdminConfig(User user, com.lab.recruitment.dto.WrittenExamConfigDTO configDTO) {
        // Save or update exam config - delegate to createExam/updateExam
        return true;
    }

    @Override
    public Page<Map<String, Object>> getAdminSubmissionPage(User user, Integer pageNum, Integer pageSize, Integer status, String realName) {
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        // Query attempts for admin's lab
        return page;
    }

    @Override
    public boolean reviewSubmission(User user, Long submissionId, Integer status, String adminRemark) {
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectById(submissionId);
        if (attempt == null) throw new RuntimeException("提交记录不存在");
        attempt.setStatus(Integer.valueOf(status == 2 ? "graded" : "submitted"));
        attempt.setRemark(adminRemark);
        attempt.setGradedBy(user.getId());
        attempt.setGradedTime(LocalDateTime.now());
        writtenExamAttemptMapper.updateById(attempt);
        return true;
    }

    @Override
    public Page<Map<String, Object>> getStudentLabPage(User user, Integer pageNum, Integer pageSize, String labName, String status) {
        // Query published exams (status=1)
        LambdaQueryWrapper<WrittenExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrittenExam::getStatus, 1);
        wrapper.orderByDesc(WrittenExam::getCreateTime);
        Page<WrittenExam> examPage = writtenExamMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (WrittenExam exam : examPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", exam.getId());
            item.put("title", exam.getTitle());
            item.put("description", exam.getDescription());
            item.put("startTime", exam.getStartTime());
            item.put("endTime", exam.getEndTime());
            item.put("duration", exam.getDuration() != null ? exam.getDuration() : 60);
            item.put("totalScore", exam.getTotalScore() != null ? exam.getTotalScore() : 100);
            item.put("passScore", exam.getPassScore());

            // Resolve lab name
            Lab lab = labMapper.selectById(exam.getLabId());
            item.put("labName", lab != null ? lab.getLabName() : "Unknown");

            // Determine status string for frontend
            String statusStr;
            // Check if student already submitted
            LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
            aw.eq(WrittenExamAttempt::getExamId, exam.getId())
              .eq(WrittenExamAttempt::getStudentId, user.getId())
              .last("LIMIT 1");
            WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
            if (attempt != null) {
                statusStr = "submitted";
            } else if (now.isBefore(exam.getStartTime())) {
                statusStr = "upcoming";
            } else if (now.isAfter(exam.getEndTime())) {
                statusStr = "ended";
            } else {
                statusStr = "ongoing";
            }
            item.put("status", statusStr);

            // Filter by status if requested
            if (StringUtils.hasText(labName) && lab != null && !lab.getLabName().contains(labName)) {
                continue;
            }

            // Eligibility
            boolean eligible = "ongoing".equals(statusStr);
            item.put("eligible", eligible);
            if (!eligible) {
                if ("submitted".equals(statusStr)) {
                    item.put("eligibilityReason", "Already submitted");
                } else if ("upcoming".equals(statusStr)) {
                    item.put("eligibilityReason", "Not started yet");
                } else if ("ended".equals(statusStr)) {
                    item.put("eligibilityReason", "Exam ended");
                }
            }

            records.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize);
        result.setRecords(records);
        result.setTotal(examPage.getTotal());
        return result;
    }

    @Override
    public Map<String, Object> getStudentExam(User user, Long labId) {
        WrittenExam exam = resolveStudentExamForLegacyLabId(labId);
        Map<String, Object> result = getExamDetail(exam.getId(), user.getId());
        result.put("examId", exam.getId());
        return result;
    }

    @Override
    public Map<String, Object> getStudentSubmission(User user, Long labId) {
        return getSubmissionResult(labId, user.getId());
    }

    @Override
    public Map<String, Object> submitExam(User user, com.lab.recruitment.dto.WrittenExamSubmitDTO submitDTO) {
        WrittenExam exam = resolveStudentExamForLegacyLabId(submitDTO.getLabId());
        LambdaQueryWrapper<WrittenExamAttempt> aw = new LambdaQueryWrapper<>();
        aw.eq(WrittenExamAttempt::getExamId, exam.getId())
          .eq(WrittenExamAttempt::getStudentId, user.getId())
          .orderByDesc(WrittenExamAttempt::getId)
          .last("LIMIT 1");
        WrittenExamAttempt attempt = writtenExamAttemptMapper.selectOne(aw);
        if (attempt == null || attempt.getStatus() == null || attempt.getStatus() < 1) {
            startExam(exam.getId(), user.getId());
        }
        submitPaper(exam.getId(), user.getId(), com.alibaba.fastjson2.JSON.toJSONString(submitDTO.getAnswers()));
        Map<String, Object> result = new HashMap<>();
        result.put("submitted", true);
        result.put("examId", exam.getId());
        result.put("submission", getSubmissionResult(exam.getId(), user.getId()));
        return result;
    }

    @Override
    public List<SystemNotification> getMyNotifications(User user) {
        // Return notifications for this student
        return List.of();
    }

    @Override
    public boolean markNotificationRead(User user, Long notificationId) {
        return true;
    }

    private WrittenExam resolveStudentExamForLegacyLabId(Long labId) {
        if (labId == null) {
            throw new RuntimeException("Lab id is required");
        }
        WrittenExam directExam = writtenExamMapper.selectById(labId);
        if (directExam != null && !Objects.equals(directExam.getDeleted(), 1)) {
            return directExam;
        }

        LambdaQueryWrapper<WrittenExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrittenExam::getLabId, labId)
          .eq(WrittenExam::getDeleted, 0)
          .orderByDesc(WrittenExam::getStatus)
          .orderByDesc(WrittenExam::getStartTime)
          .orderByDesc(WrittenExam::getCreateTime)
          .last("LIMIT 1");
        WrittenExam exam = writtenExamMapper.selectOne(wrapper);
        if (exam == null) {
            throw new RuntimeException("No written exam found for lab: " + labId);
        }
        return exam;
    }

    private Map<String, Object> buildStudentExamSummary(WrittenExam exam) {
        Map<String, Object> detail = new HashMap<>();
        Lab lab = exam.getLabId() == null ? null : labMapper.selectById(exam.getLabId());
        detail.put("id", exam.getId());
        detail.put("examId", exam.getId());
        detail.put("title", exam.getTitle());
        detail.put("description", exam.getDescription());
        detail.put("labId", exam.getLabId());
        detail.put("labName", lab == null ? "" : lab.getLabName());
        detail.put("duration", exam.getDuration());
        detail.put("totalScore", exam.getTotalScore());
        detail.put("passScore", exam.getPassScore());
        detail.put("startTime", exam.getStartTime());
        detail.put("endTime", exam.getEndTime());
        detail.put("antiCheatEnabled", Boolean.TRUE.equals(exam.getEnableAntiCheat()));
        detail.put("signatureRequired", Boolean.TRUE.equals(exam.getEnableSignature()));
        detail.put("allowRetry", Boolean.TRUE.equals(exam.getAllowRetry()));
        detail.put("exam", exam);
        detail.put("lab", lab);
        return detail;
    }

    private List<Map<String, Object>> buildStudentQuestionPayload(List<WrittenExamPaperQuestion> paperQuestions) {
        if (paperQuestions == null || paperQuestions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> questionIds = paperQuestions.stream()
                .map(WrittenExamPaperQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .toList();
        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, WrittenExamQuestion> questionMap = writtenExamQuestionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(WrittenExamQuestion::getId, item -> item));

        List<Map<String, Object>> questions = new ArrayList<>();
        for (WrittenExamPaperQuestion paperQuestion : paperQuestions) {
            WrittenExamQuestion question = questionMap.get(paperQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", question.getId());
            item.put("title", question.getTitle());
            item.put("content", question.getContent());
            item.put("type", question.getQuestionType());
            item.put("questionType", question.getQuestionType());
            item.put("difficulty", question.getDifficulty());
            item.put("score", paperQuestion.getScore() != null ? paperQuestion.getScore() : question.getScore());
            item.put("options", parseQuestionOptions(question.getOptions()));
            item.put("inputFormat", question.getInputFormat());
            item.put("outputFormat", question.getOutputFormat());
            item.put("sampleCase", question.getSampleCase());
            item.put("allowedLanguages", parseStringList(question.getAllowedLanguages()));
            item.put("tags", parseStringList(question.getTags()));
            questions.add(item);
        }
        return questions;
    }

    private List<Map<String, Object>> parseQuestionOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(optionsJson);
            List<Map<String, Object>> options = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                Object raw = array.get(i);
                Map<String, Object> item = new HashMap<>();
                if (raw instanceof JSONObject jsonObject) {
                    String key = jsonObject.getString("key");
                    if (!StringUtils.hasText(key)) {
                        key = jsonObject.getString("label");
                    }
                    item.put("key", key);
                    item.put("label", key);
                    item.put("value", jsonObject.getString("value") != null ? jsonObject.getString("value") : jsonObject.getString("text"));
                    item.put("text", item.get("value"));
                } else {
                    String value = String.valueOf(raw);
                    String key = String.valueOf((char) ('A' + i));
                    item.put("key", key);
                    item.put("label", key);
                    item.put("value", value);
                    item.put("text", value);
                }
                options.add(item);
            }
            return options;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception ignored) {
            return Arrays.stream(json.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
    }

    private void normalizeQuestionForPersistence(WrittenExamQuestion question) {
        if (question == null) {
            return;
        }
        question.setOptions(normalizeJsonArray(question.getOptions()));
        question.setTags(normalizeStringArray(question.getTags()));
        question.setAllowedLanguages(normalizeStringArray(question.getAllowedLanguages()));
        question.setTestCases(normalizeJsonArray(question.getTestCases()));
        question.setSampleCase(normalizeSampleCase(question.getSampleCase()));

        if (!StringUtils.hasText(question.getInputFormat())) {
            question.setInputFormat(null);
        }
        if (!StringUtils.hasText(question.getOutputFormat())) {
            question.setOutputFormat(null);
        }
        if (!StringUtils.hasText(question.getCorrectAnswer())) {
            question.setCorrectAnswer(null);
        }
        if (!StringUtils.hasText(question.getAnalysis())) {
            question.setAnalysis(null);
        }
    }

    private String normalizeJsonArray(String value) {
        if (!StringUtils.hasText(value)) {
            return "[]";
        }
        try {
            return JSON.toJSONString(JSON.parseArray(value));
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private String normalizeStringArray(String value) {
        if (!StringUtils.hasText(value)) {
            return "[]";
        }
        try {
            List<String> values = JSON.parseArray(value, String.class);
            return JSON.toJSONString(values.stream().filter(StringUtils::hasText).map(String::trim).toList());
        } catch (Exception ignored) {
            List<String> values = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
            return JSON.toJSONString(values);
        }
    }

    private String normalizeSampleCase(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(value);
            String input = jsonObject.getString("input");
            String output = jsonObject.getString("output");
            if (!StringUtils.hasText(input) && !StringUtils.hasText(output)) {
                return null;
            }
            JSONObject normalized = new JSONObject();
            normalized.put("input", StringUtils.hasText(input) ? input : "");
            normalized.put("output", StringUtils.hasText(output) ? output : "");
            return JSON.toJSONString(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> buildSubmissionPayload(WrittenExam exam, WrittenExamAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", attempt.getId());
        result.put("examId", exam.getId());
        result.put("score", attempt.getTotalScore());
        result.put("totalScore", exam.getTotalScore());
        result.put("passScore", exam.getPassScore());
        result.put("submitTime", attempt.getSubmitTime());
        result.put("status", attempt.getStatus() != null && attempt.getStatus() >= 3 ? "GRADED" : "SUBMITTED");
        result.put("passed", attempt.getPassed());
        result.put("remark", attempt.getRemark());
        InterviewInvitation invitation = findLatestInterviewInvitation(exam.getId(), attempt.getStudentId());
        if (invitation != null) {
            result.put("interviewInvitation", invitation.getDescription());
        }
        return result;
    }

    private InterviewInvitation findLatestInterviewInvitation(Long examId, Long studentId) {
        try {
            LambdaQueryWrapper<InterviewInvitation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewInvitation::getExamId, examId)
              .eq(InterviewInvitation::getStudentId, studentId)
              .eq(InterviewInvitation::getDeleted, 0)
              .orderByDesc(InterviewInvitation::getCreateTime)
              .last("LIMIT 1");
            return interviewInvitationMapper.selectOne(wrapper);
        } catch (DataAccessException e) {
            return null;
        }
    }
}
