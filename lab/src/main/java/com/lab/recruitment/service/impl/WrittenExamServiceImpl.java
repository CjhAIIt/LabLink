package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.ExamOptionDTO;
import com.lab.recruitment.dto.JudgeCaseDTO;
import com.lab.recruitment.dto.WrittenExamAnswerDTO;
import com.lab.recruitment.dto.WrittenExamConfigDTO;
import com.lab.recruitment.dto.WrittenExamQuestionDTO;
import com.lab.recruitment.dto.WrittenExamSubmitDTO;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.entity.WrittenExam;
import com.lab.recruitment.entity.WrittenExamQuestion;
import com.lab.recruitment.entity.WrittenExamSubmission;
import com.lab.recruitment.mapper.WrittenExamMapper;
import com.lab.recruitment.mapper.WrittenExamQuestionMapper;
import com.lab.recruitment.mapper.WrittenExamSubmissionMapper;
import com.lab.recruitment.service.GrowthPracticeQuestionService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.service.WrittenExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WrittenExamServiceImpl implements WrittenExamService {

    private static final int EXAM_STATUS_CLOSED = 0;
    private static final int EXAM_STATUS_OPEN = 1;
    private static final int SUBMISSION_WAITING_REVIEW = 1;
    private static final int SUBMISSION_PASSED = 2;
    private static final int SUBMISSION_FAILED = 3;
    private static final String QUESTION_SINGLE_CHOICE = "single_choice";
    private static final String QUESTION_FILL_BLANK = "fill_blank";
    private static final String QUESTION_PROGRAMMING = "programming";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Autowired
    private WrittenExamMapper writtenExamMapper;

    @Autowired
    private WrittenExamQuestionMapper writtenExamQuestionMapper;

    @Autowired
    private WrittenExamSubmissionMapper writtenExamSubmissionMapper;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private GrowthPracticeQuestionService growthPracticeQuestionService;

    @Autowired
    private CodeJudgeService codeJudgeService;

    @Override
    public Map<String, Object> getAdminConfig(User admin) {
        ensureLabAdmin(admin);

        Lab lab = labService.getById(admin.getLabId());
        WrittenExam exam = findExamByLabId(admin.getLabId());

        Map<String, Object> data = new HashMap<>();
        data.put("lab", lab);
        data.put("recruitmentOpen", lab != null && Integer.valueOf(1).equals(lab.getStatus()));
        data.put("exam", exam);
        data.put("questions", exam == null ? Collections.emptyList() : getQuestionDTOList(exam.getId(), true));
        data.put("environmentStatus", codeJudgeService.getEnvironmentStatus());
        data.put("environmentDetails", codeJudgeService.getEnvironmentDetails());
        return data;
    }

    @Override
    @Transactional
    public boolean saveAdminConfig(User admin, WrittenExamConfigDTO configDTO) {
        ensureLabAdmin(admin);
        if (configDTO == null) {
            throw new RuntimeException("Exam config cannot be empty");
        }
        if (!StringUtils.hasText(configDTO.getTitle())) {
            throw new RuntimeException("Please enter an exam title");
        }
        if (configDTO.getQuestions() == null || configDTO.getQuestions().isEmpty()) {
            throw new RuntimeException("Please configure at least one exam question");
        }

        LocalDateTime startTime = parseDateTime(configDTO.getStartTime());
        LocalDateTime endTime = parseDateTime(configDTO.getEndTime());
        if (!endTime.isAfter(startTime)) {
            throw new RuntimeException("The exam end time must be later than the start time");
        }

        Lab lab = labService.getById(admin.getLabId());
        if (lab == null) {
            throw new RuntimeException("Lab not found");
        }

        WrittenExam exam = findExamByLabId(admin.getLabId());
        if (exam == null) {
            exam = new WrittenExam();
            exam.setLabId(admin.getLabId());
        }
        exam.setTitle(configDTO.getTitle().trim());
        exam.setDescription(StringUtils.hasText(configDTO.getDescription()) ? configDTO.getDescription().trim() : null);
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        exam.setPassScore(configDTO.getPassScore() == null ? 60 : configDTO.getPassScore());
        exam.setStatus(Boolean.TRUE.equals(configDTO.getRecruitmentOpen()) ? EXAM_STATUS_OPEN : EXAM_STATUS_CLOSED);

        if (exam.getId() == null) {
            writtenExamMapper.insert(exam);
        } else {
            writtenExamMapper.updateById(exam);
        }

        writtenExamQuestionMapper.delete(new QueryWrapper<WrittenExamQuestion>().eq("exam_id", exam.getId()));
        List<Long> bankQuestionIds = configDTO.getQuestions().stream()
                .map(WrittenExamQuestionDTO::getBankQuestionId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        Map<Long, WrittenExamQuestionDTO> bankQuestionMap = growthPracticeQuestionService
                .getQuestionSnapshots(bankQuestionIds, true)
                .stream()
                .collect(Collectors.toMap(WrittenExamQuestionDTO::getBankQuestionId, Function.identity(), (left, right) -> left));

        int sortIndex = 1;
        for (WrittenExamQuestionDTO questionDTO : configDTO.getQuestions()) {
            WrittenExamQuestionDTO resolvedQuestion = resolveQuestionSnapshot(questionDTO, bankQuestionMap);
            validateQuestion(resolvedQuestion);

            WrittenExamQuestion question = new WrittenExamQuestion();
            question.setExamId(exam.getId());
            question.setBankQuestionId(resolvedQuestion.getBankQuestionId());
            question.setQuestionType(resolvedQuestion.getQuestionType());
            question.setTrackCode(resolvedQuestion.getTrackCode());
            question.setTitle(resolvedQuestion.getTitle().trim());
            question.setContent(StringUtils.hasText(resolvedQuestion.getContent()) ? resolvedQuestion.getContent().trim() : null);
            question.setDifficulty(StringUtils.hasText(resolvedQuestion.getDifficulty()) ? resolvedQuestion.getDifficulty().trim() : null);
            question.setInputFormat(StringUtils.hasText(resolvedQuestion.getInputFormat()) ? resolvedQuestion.getInputFormat().trim() : null);
            question.setOutputFormat(StringUtils.hasText(resolvedQuestion.getOutputFormat()) ? resolvedQuestion.getOutputFormat().trim() : null);
            question.setSampleCaseJson(StringUtils.hasText(resolvedQuestion.getSampleCase()) ? resolvedQuestion.getSampleCase().trim() : null);
            question.setScore(questionDTO.getScore() == null ? 10 : questionDTO.getScore());
            question.setSortOrder(questionDTO.getSortOrder() == null ? sortIndex : questionDTO.getSortOrder());
            question.setOptionsJson(resolvedQuestion.getOptions() == null ? null : JSON.toJSONString(resolvedQuestion.getOptions()));
            question.setProgramLanguages(resolvedQuestion.getAllowedLanguages() == null ? null : JSON.toJSONString(resolvedQuestion.getAllowedLanguages()));
            question.setJudgeCaseJson(resolvedQuestion.getJudgeCases() == null ? null : JSON.toJSONString(resolvedQuestion.getJudgeCases()));
            question.setTagsJson(resolvedQuestion.getTags() == null ? null : JSON.toJSONString(resolvedQuestion.getTags()));
            question.setAnalysisHint(StringUtils.hasText(resolvedQuestion.getAnalysisHint()) ? resolvedQuestion.getAnalysisHint().trim() : null);

            if (QUESTION_SINGLE_CHOICE.equals(resolvedQuestion.getQuestionType())) {
                question.setAnswerConfig(resolvedQuestion.getCorrectAnswer());
            } else if (QUESTION_FILL_BLANK.equals(resolvedQuestion.getQuestionType())) {
                question.setAnswerConfig(JSON.toJSONString(resolvedQuestion.getAcceptableAnswers()));
            }

            writtenExamQuestionMapper.insert(question);
            sortIndex++;
        }

        lab.setStatus(Boolean.TRUE.equals(configDTO.getRecruitmentOpen()) ? 1 : 2);
        labService.updateById(lab);
        return true;
    }

    @Override
    public Page<Map<String, Object>> getAdminSubmissionPage(User admin, Integer pageNum, Integer pageSize, Integer status, String realName) {
        ensureLabAdmin(admin);

        Page<WrittenExamSubmission> page = new Page<>(pageNum, pageSize);
        QueryWrapper<WrittenExamSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", admin.getLabId())
                .orderByAsc("status")
                .orderByDesc("submit_time")
                .orderByDesc("id");
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (StringUtils.hasText(realName)) {
            List<Long> userIds = userService.list(new QueryWrapper<User>()
                            .eq("lab_id", admin.getLabId())
                            .eq("role", "student")
                            .like("real_name", realName.trim()))
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) {
                Page<Map<String, Object>> emptyPage = new Page<>(pageNum, pageSize);
                emptyPage.setRecords(Collections.emptyList());
                emptyPage.setTotal(0);
                return emptyPage;
            }
            queryWrapper.in("user_id", userIds);
        }

        Page<WrittenExamSubmission> submissionPage = writtenExamSubmissionMapper.selectPage(page, queryWrapper);
        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize);
        result.setRecords(buildSubmissionRecords(submissionPage.getRecords(), true));
        result.setTotal(submissionPage.getTotal());
        return result;
    }

    @Override
    @Transactional
    public boolean reviewSubmission(User admin, Long submissionId, Integer status, String adminRemark) {
        ensureLabAdmin(admin);
        if (status == null || (status != SUBMISSION_PASSED && status != SUBMISSION_FAILED)) {
            throw new RuntimeException("Review result is invalid");
        }

        WrittenExamSubmission submission = writtenExamSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("Exam submission not found");
        }
        if (!admin.getLabId().equals(submission.getLabId())) {
            throw new RuntimeException("You can only review your own lab submissions");
        }
        if (!Integer.valueOf(SUBMISSION_WAITING_REVIEW).equals(submission.getStatus())) {
            throw new RuntimeException("This submission has already been reviewed");
        }

        submission.setStatus(status);
        submission.setAdminRemark(StringUtils.hasText(adminRemark) ? adminRemark.trim() : null);
        submission.setReviewTime(LocalDateTime.now());
        writtenExamSubmissionMapper.updateById(submission);

        if (status == SUBMISSION_PASSED) {
            Lab lab = labService.getById(submission.getLabId());
            String labName = lab == null ? "实验室" : lab.getLabName();
            systemNotificationService.createNotification(
                    submission.getUserId(),
                    "笔试通过通知",
                    "你已通过 " + labName + " 的笔试，现在可以投递简历进入面试阶段。",
                    "written_exam_pass",
                    submission.getId()
            );
        }
        return true;
    }

    @Override
    public Page<Map<String, Object>> getStudentLabPage(User user, Integer pageNum, Integer pageSize, String labName, Integer status) {
        Page<Lab> labPage = labService.getLabPage(pageNum, pageSize, null, labName, status);
        List<Lab> labs = labPage.getRecords();
        if (labs == null || labs.isEmpty()) {
            Page<Map<String, Object>> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setRecords(Collections.emptyList());
            emptyPage.setTotal(labPage.getTotal());
            return emptyPage;
        }

        List<Long> labIds = labs.stream().map(Lab::getId).collect(Collectors.toList());
        Map<Long, WrittenExam> examMap = writtenExamMapper.selectList(new QueryWrapper<WrittenExam>().in("lab_id", labIds))
                .stream()
                .collect(Collectors.toMap(WrittenExam::getLabId, Function.identity(), (left, right) -> left));
        Map<Long, WrittenExamSubmission> submissionMap = writtenExamSubmissionMapper.selectList(
                        new QueryWrapper<WrittenExamSubmission>().eq("user_id", user.getId()).in("lab_id", labIds))
                .stream()
                .collect(Collectors.toMap(WrittenExamSubmission::getLabId, Function.identity(), (left, right) -> left));

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> records = new ArrayList<>();
        for (Lab lab : labs) {
            WrittenExam exam = examMap.get(lab.getId());
            WrittenExamSubmission submission = submissionMap.get(lab.getId());
            boolean hasExam = exam != null;
            boolean examOpen = hasExam && Integer.valueOf(EXAM_STATUS_OPEN).equals(exam.getStatus());
            boolean withinWindow = hasExam && !now.isBefore(exam.getStartTime()) && !now.isAfter(exam.getEndTime());

            Map<String, Object> item = new HashMap<>();
            item.put("id", lab.getId());
            item.put("labName", lab.getLabName());
            item.put("labDesc", lab.getLabDesc());
            item.put("requireSkill", lab.getRequireSkill());
            item.put("status", lab.getStatus());
            item.put("createTime", lab.getCreateTime());
            item.put("recruitNum", lab.getRecruitNum());
            item.put("currentNum", lab.getCurrentNum());
            item.put("hasWrittenExam", hasExam);
            item.put("writtenExamTitle", hasExam ? exam.getTitle() : null);
            item.put("writtenExamStartTime", hasExam ? exam.getStartTime() : null);
            item.put("writtenExamEndTime", hasExam ? exam.getEndTime() : null);
            item.put("writtenExamPassScore", hasExam ? exam.getPassScore() : null);
            item.put("writtenExamOpen", examOpen);
            item.put("writtenExamWithinWindow", withinWindow);
            item.put("myExamStatus", submission == null ? 0 : submission.getStatus());
            item.put("myExamScore", submission == null ? null : submission.getTotalScore());
            item.put("myExamSubmissionId", submission == null ? null : submission.getId());
            item.put("canTakeWrittenExam", hasExam && examOpen && withinWindow && submission == null);
            item.put("canDeliver", !hasExam || (submission != null && Integer.valueOf(SUBMISSION_PASSED).equals(submission.getStatus())));
            item.put("interviewLockedReason", getInterviewRequirementMessage(lab.getId(), user.getId()));
            records.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize);
        result.setRecords(records);
        result.setTotal(labPage.getTotal());
        return result;
    }

    @Override
    public Map<String, Object> getStudentExam(User user, Long labId) {
        WrittenExam exam = requireExam(labId);
        WrittenExamSubmission submission = findSubmissionByLabAndUser(labId, user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("lab", labService.getById(labId));
        data.put("exam", exam);
        data.put("environmentStatus", codeJudgeService.getEnvironmentStatus());
        data.put("environmentDetails", codeJudgeService.getEnvironmentDetails());
        data.put("alreadySubmitted", submission != null);
        data.put("submission", submission == null ? null : buildSubmissionRecords(Collections.singletonList(submission), true).get(0));
        data.put("questions", getQuestionDTOList(exam.getId(), false));

        if (submission == null) {
            assertExamCanBeTaken(exam);
        }
        return data;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(User user, WrittenExamSubmitDTO submitDTO) {
        if (submitDTO == null || submitDTO.getLabId() == null) {
            throw new RuntimeException("Lab id is required");
        }

        WrittenExam exam = requireExam(submitDTO.getLabId());
        assertExamCanBeTaken(exam);
        if (findSubmissionByLabAndUser(submitDTO.getLabId(), user.getId()) != null) {
            throw new RuntimeException("You have already submitted this written exam");
        }

        List<WrittenExamQuestion> questions = listQuestions(exam.getId());
        Map<Long, WrittenExamAnswerDTO> answerMap = (submitDTO.getAnswers() == null ? Collections.<WrittenExamAnswerDTO>emptyList() : submitDTO.getAnswers())
                .stream()
                .filter(answer -> answer.getQuestionId() != null)
                .collect(Collectors.toMap(WrittenExamAnswerDTO::getQuestionId, Function.identity(), (left, right) -> left));

        BigDecimal totalScore = BigDecimal.ZERO;
        List<Map<String, Object>> answerSheet = new ArrayList<>();
        List<String> remarks = new ArrayList<>();

        for (WrittenExamQuestion question : questions) {
            WrittenExamAnswerDTO answerDTO = answerMap.get(question.getId());
            Map<String, Object> answerResult = new HashMap<>();
            answerResult.put("questionId", question.getId());
            answerResult.put("questionType", question.getQuestionType());
            answerResult.put("title", question.getTitle());
            answerResult.put("fullScore", question.getScore());

            BigDecimal questionScore = BigDecimal.ZERO;
            String resultMessage;

            if (QUESTION_SINGLE_CHOICE.equals(question.getQuestionType())) {
                String userAnswer = answerDTO == null ? "" : safeTrim(answerDTO.getAnswer());
                boolean correct = normalizeAnswer(userAnswer).equals(normalizeAnswer(question.getAnswerConfig()));
                questionScore = correct ? BigDecimal.valueOf(question.getScore()) : BigDecimal.ZERO;
                answerResult.put("answer", userAnswer);
                resultMessage = correct ? "选择题答案正确" : "选择题答案错误";
            } else if (QUESTION_FILL_BLANK.equals(question.getQuestionType())) {
                String userAnswer = answerDTO == null ? "" : safeTrim(answerDTO.getAnswer());
                Set<String> acceptableAnswers = parseStringList(question.getAnswerConfig()).stream()
                        .map(this::normalizeAnswer)
                        .collect(Collectors.toSet());
                boolean correct = acceptableAnswers.contains(normalizeAnswer(userAnswer));
                questionScore = correct ? BigDecimal.valueOf(question.getScore()) : BigDecimal.ZERO;
                answerResult.put("answer", userAnswer);
                resultMessage = correct ? "填空题答案正确" : "填空题答案未匹配标准答案";
            } else if (QUESTION_PROGRAMMING.equals(question.getQuestionType())) {
                String language = answerDTO == null ? null : answerDTO.getLanguage();
                String code = answerDTO == null ? null : answerDTO.getCode();
                List<String> allowedLanguages = parseStringList(question.getProgramLanguages());
                if (!allowedLanguages.contains(language)) {
                    resultMessage = "编程题未选择受支持的语言";
                } else {
                    CodeJudgeService.JudgeResult judgeResult = codeJudgeService.judge(language, code, parseJudgeCases(question.getJudgeCaseJson()));
                    questionScore = codeJudgeService.calculateScore(question.getScore(), judgeResult);
                    answerResult.put("judgePassedCount", judgeResult.getPassedCount());
                    answerResult.put("judgeTotalCount", judgeResult.getTotalCount());
                    resultMessage = judgeResult.getMessage();
                }
                answerResult.put("language", language);
                answerResult.put("code", code);
            } else {
                resultMessage = "未知题型，未计分";
            }

            totalScore = totalScore.add(questionScore);
            answerResult.put("score", questionScore);
            answerResult.put("resultMessage", resultMessage);
            answerSheet.add(answerResult);
            remarks.add(question.getTitle() + ": " + resultMessage + "，得分 " + questionScore);
        }

        WrittenExamSubmission submission = new WrittenExamSubmission();
        submission.setExamId(exam.getId());
        submission.setLabId(submitDTO.getLabId());
        submission.setUserId(user.getId());
        submission.setAnswerSheetJson(JSON.toJSONString(answerSheet));
        submission.setTotalScore(totalScore);
        submission.setAiRemark(String.join(System.lineSeparator(), remarks));
        submission.setStatus(SUBMISSION_WAITING_REVIEW);
        submission.setSubmitTime(LocalDateTime.now());
        submission.setGradeTime(LocalDateTime.now());
        writtenExamSubmissionMapper.insert(submission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("score", submission.getTotalScore());
        result.put("status", submission.getStatus());
        result.put("aiRemark", submission.getAiRemark());
        result.put("answerSheet", answerSheet);
        result.put("passScore", exam.getPassScore());
        return result;
    }

    @Override
    public Map<String, Object> getStudentSubmission(User user, Long labId) {
        WrittenExamSubmission submission = findSubmissionByLabAndUser(labId, user.getId());
        if (submission == null) {
            return null;
        }
        return buildSubmissionRecords(Collections.singletonList(submission), true).get(0);
    }

    @Override
    @Transactional
    public Map<String, Object> completeGradPathExam(User user, Long labId, Map<String, Object> payload) {
        if (labId == null) {
            throw new RuntimeException("Lab id is required");
        }

        WrittenExam exam = requireExam(labId);
        WrittenExamSubmission submission = findSubmissionByLabAndUser(labId, user.getId());
        if (submission != null && Integer.valueOf(SUBMISSION_PASSED).equals(submission.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("completed", true);
            result.put("alreadyCompleted", true);
            result.put("submission", buildSubmissionRecords(Collections.singletonList(submission), true).get(0));
            return result;
        }

        assertExamCanBeTaken(exam);

        boolean correct = payload != null && Boolean.TRUE.equals(payload.get("correct"));
        String judgeStatus = extractPayloadString(payload, "judgeStatus");
        if (!correct && !"success".equalsIgnoreCase(judgeStatus) && !"accepted".equalsIgnoreCase(judgeStatus)) {
            throw new RuntimeException("Only accepted GradPath submissions can complete the written exam");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> answerSheetItem = new HashMap<>();
        answerSheetItem.put("platform", "GradPath");
        answerSheetItem.put("questionId", payload == null ? null : payload.get("questionId"));
        answerSheetItem.put("questionType", "gradpath_programming");
        answerSheetItem.put("title", extractPayloadString(payload, "questionTitle"));
        answerSheetItem.put("fullScore", exam.getPassScore() == null ? 100 : exam.getPassScore());
        answerSheetItem.put("score", exam.getPassScore() == null ? 100 : exam.getPassScore());
        answerSheetItem.put("language", extractPayloadString(payload, "language"));
        answerSheetItem.put("judgeStatus", judgeStatus);
        answerSheetItem.put("stdout", extractPayloadString(payload, "stdout"));
        answerSheetItem.put("resultMessage", "Passed through the GradPath coding platform");

        String remark = "GradPath accepted submission"
                + (answerSheetItem.get("title") == null || String.valueOf(answerSheetItem.get("title")).isEmpty()
                ? ""
                : " - " + answerSheetItem.get("title"))
                + (answerSheetItem.get("language") == null || String.valueOf(answerSheetItem.get("language")).isEmpty()
                ? ""
                : " (" + answerSheetItem.get("language") + ")");

        boolean newlyPassed = submission == null || !Integer.valueOf(SUBMISSION_PASSED).equals(submission.getStatus());
        if (submission == null) {
            submission = new WrittenExamSubmission();
            submission.setExamId(exam.getId());
            submission.setLabId(labId);
            submission.setUserId(user.getId());
        }

        submission.setAnswerSheetJson(JSON.toJSONString(Collections.singletonList(answerSheetItem)));
        submission.setTotalScore(BigDecimal.valueOf(exam.getPassScore() == null ? 100 : exam.getPassScore()));
        submission.setAiRemark(remark);
        submission.setAdminRemark("Automatically synced from GradPath");
        submission.setStatus(SUBMISSION_PASSED);
        submission.setSubmitTime(now);
        submission.setGradeTime(now);
        submission.setReviewTime(now);

        if (submission.getId() == null) {
            writtenExamSubmissionMapper.insert(submission);
        } else {
            writtenExamSubmissionMapper.updateById(submission);
        }

        if (newlyPassed) {
            Lab lab = labService.getById(labId);
            String labName = lab == null ? "the selected lab" : lab.getLabName();
            systemNotificationService.createNotification(
                    user.getId(),
                    "GradPath 笔试通过",
                    "你已通过 " + labName + " 的 GradPath 笔试，现在可以继续投递。",
                    "gradpath_exam_pass",
                    submission.getId()
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("completed", true);
        result.put("alreadyCompleted", false);
        result.put("submission", buildSubmissionRecords(Collections.singletonList(submission), true).get(0));
        return result;
    }

    @Override
    public List<SystemNotification> getMyNotifications(User user) {
        return systemNotificationService.list(new QueryWrapper<SystemNotification>()
                .eq("user_id", user.getId())
                .orderByAsc("is_read")
                .orderByDesc("create_time")
                .last("LIMIT 20"));
    }

    @Override
    @Transactional
    public boolean markNotificationRead(User user, Long notificationId) {
        SystemNotification notification = systemNotificationService.getById(notificationId);
        if (notification == null) {
            throw new RuntimeException("Notification not found");
        }
        if (!notification.getUserId().equals(user.getId())) {
            throw new RuntimeException("You can only operate on your own notifications");
        }
        notification.setIsRead(1);
        return systemNotificationService.updateById(notification);
    }

    @Override
    public boolean canEnterInterview(Long labId, Long userId) {
        return !StringUtils.hasText(getInterviewRequirementMessage(labId, userId));
    }

    @Override
    public String getInterviewRequirementMessage(Long labId, Long userId) {
        WrittenExam exam = findExamByLabId(labId);
        if (exam == null) {
            return null;
        }

        WrittenExamSubmission submission = findSubmissionByLabAndUser(labId, userId);
        if (submission != null) {
            if (Integer.valueOf(SUBMISSION_PASSED).equals(submission.getStatus())) {
                return null;
            }
            if (Integer.valueOf(SUBMISSION_WAITING_REVIEW).equals(submission.getStatus())) {
                return "笔试已提交，等待实验室管理员审核";
            }
            return "笔试未通过，暂时不能进入面试投递";
        }

        if (!Integer.valueOf(EXAM_STATUS_OPEN).equals(exam.getStatus())) {
            return "该实验室暂未开放笔试";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            return "笔试尚未开始";
        }
        if (now.isAfter(exam.getEndTime())) {
            return "笔试已结束，未参加笔试无法进入面试";
        }
        return "需要先完成笔试并通过审核后才能投递简历";
    }

    private void ensureLabAdmin(User admin) {
        if (admin == null || !"admin".equals(admin.getRole()) || admin.getLabId() == null) {
            throw new RuntimeException("Only lab admins can use this function");
        }
    }

    private WrittenExam requireExam(Long labId) {
        WrittenExam exam = findExamByLabId(labId);
        if (exam == null) {
            throw new RuntimeException("This lab has not configured a written exam");
        }
        return exam;
    }

    private WrittenExam findExamByLabId(Long labId) {
        return writtenExamMapper.selectOne(new QueryWrapper<WrittenExam>().eq("lab_id", labId));
    }

    private WrittenExamSubmission findSubmissionByLabAndUser(Long labId, Long userId) {
        return writtenExamSubmissionMapper.selectOne(new QueryWrapper<WrittenExamSubmission>()
                .eq("lab_id", labId)
                .eq("user_id", userId)
                .orderByDesc("submit_time")
                .last("LIMIT 1"));
    }

    private void assertExamCanBeTaken(WrittenExam exam) {
        if (!Integer.valueOf(EXAM_STATUS_OPEN).equals(exam.getStatus())) {
            throw new RuntimeException("This written exam is not open");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new RuntimeException("The written exam has not started yet");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new RuntimeException("The written exam has ended");
        }
    }

    private void validateQuestion(WrittenExamQuestionDTO questionDTO) {
        if (questionDTO == null || !StringUtils.hasText(questionDTO.getQuestionType()) || !StringUtils.hasText(questionDTO.getTitle())) {
            throw new RuntimeException("Each question must include a type and title");
        }
        if (questionDTO.getScore() == null || questionDTO.getScore() <= 0) {
            throw new RuntimeException("Question score must be greater than 0");
        }
        if (QUESTION_SINGLE_CHOICE.equals(questionDTO.getQuestionType())) {
            if (questionDTO.getOptions() == null || questionDTO.getOptions().size() < 2 || !StringUtils.hasText(questionDTO.getCorrectAnswer())) {
                throw new RuntimeException("Single choice questions must configure options and the correct answer");
            }
        } else if (QUESTION_FILL_BLANK.equals(questionDTO.getQuestionType())) {
            if (questionDTO.getAcceptableAnswers() == null || questionDTO.getAcceptableAnswers().isEmpty()) {
                throw new RuntimeException("Fill-in-the-blank questions must configure acceptable answers");
            }
        } else if (QUESTION_PROGRAMMING.equals(questionDTO.getQuestionType())) {
            if (questionDTO.getAllowedLanguages() == null || questionDTO.getAllowedLanguages().isEmpty()) {
                throw new RuntimeException("Programming questions must configure allowed languages");
            }
            if (questionDTO.getJudgeCases() == null || questionDTO.getJudgeCases().isEmpty()) {
                throw new RuntimeException("Programming questions must configure judge cases");
            }
        } else {
            throw new RuntimeException("Unsupported question type: " + questionDTO.getQuestionType());
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Exam time cannot be empty");
        }
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
    }

    private List<WrittenExamQuestion> listQuestions(Long examId) {
        return writtenExamQuestionMapper.selectList(new QueryWrapper<WrittenExamQuestion>()
                        .eq("exam_id", examId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .sorted(Comparator.comparing(WrittenExamQuestion::getSortOrder).thenComparing(WrittenExamQuestion::getId))
                .collect(Collectors.toList());
    }

    private List<WrittenExamQuestionDTO> getQuestionDTOList(Long examId, boolean includeAnswerConfig) {
        List<WrittenExamQuestionDTO> result = new ArrayList<>();
        for (WrittenExamQuestion question : listQuestions(examId)) {
            WrittenExamQuestionDTO questionDTO = new WrittenExamQuestionDTO();
            questionDTO.setId(question.getId());
            questionDTO.setBankQuestionId(question.getBankQuestionId());
            questionDTO.setQuestionType(question.getQuestionType());
            questionDTO.setTrackCode(question.getTrackCode());
            questionDTO.setTitle(question.getTitle());
            questionDTO.setContent(question.getContent());
            questionDTO.setDifficulty(question.getDifficulty());
            questionDTO.setInputFormat(question.getInputFormat());
            questionDTO.setOutputFormat(question.getOutputFormat());
            questionDTO.setSampleCase(question.getSampleCaseJson());
            questionDTO.setTags(parseStringList(question.getTagsJson()));
            questionDTO.setAnalysisHint(question.getAnalysisHint());
            questionDTO.setScore(question.getScore());
            questionDTO.setSortOrder(question.getSortOrder());
            questionDTO.setOptions(parseOptions(question.getOptionsJson()));
            questionDTO.setAllowedLanguages(parseStringList(question.getProgramLanguages()));
            if (includeAnswerConfig) {
                if (QUESTION_SINGLE_CHOICE.equals(question.getQuestionType())) {
                    questionDTO.setCorrectAnswer(question.getAnswerConfig());
                } else if (QUESTION_FILL_BLANK.equals(question.getQuestionType())) {
                    questionDTO.setAcceptableAnswers(parseStringList(question.getAnswerConfig()));
                } else if (QUESTION_PROGRAMMING.equals(question.getQuestionType())) {
                    questionDTO.setJudgeCases(parseJudgeCases(question.getJudgeCaseJson()));
                }
            }
            result.add(questionDTO);
        }
        return result;
    }

    private WrittenExamQuestionDTO resolveQuestionSnapshot(WrittenExamQuestionDTO questionDTO,
                                                           Map<Long, WrittenExamQuestionDTO> bankQuestionMap) {
        if (questionDTO == null) {
            throw new RuntimeException("Question cannot be empty");
        }
        if (questionDTO.getBankQuestionId() == null) {
            return questionDTO;
        }

        WrittenExamQuestionDTO bankQuestion = bankQuestionMap.get(questionDTO.getBankQuestionId());
        if (bankQuestion == null) {
            throw new RuntimeException("Selected question does not exist in the growth center bank");
        }
        return bankQuestion;
    }

    private List<Map<String, Object>> buildSubmissionRecords(List<WrittenExamSubmission> submissions, boolean includeAnswerSheet) {
        if (submissions == null || submissions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = userService.listByIds(submissions.stream().map(WrittenExamSubmission::getUserId).distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        Map<Long, WrittenExam> examMap = writtenExamMapper.selectBatchIds(submissions.stream().map(WrittenExamSubmission::getExamId).distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(WrittenExam::getId, Function.identity(), (left, right) -> left));

        List<Map<String, Object>> records = new ArrayList<>();
        for (WrittenExamSubmission submission : submissions) {
            User student = userMap.get(submission.getUserId());
            WrittenExam exam = examMap.get(submission.getExamId());

            Map<String, Object> item = new HashMap<>();
            item.put("id", submission.getId());
            item.put("examId", submission.getExamId());
            item.put("labId", submission.getLabId());
            item.put("userId", submission.getUserId());
            item.put("realName", student == null ? null : student.getRealName());
            item.put("studentId", student == null ? null : student.getStudentId());
            item.put("major", student == null ? null : student.getMajor());
            item.put("examTitle", exam == null ? null : exam.getTitle());
            item.put("totalScore", submission.getTotalScore());
            item.put("aiRemark", submission.getAiRemark());
            item.put("adminRemark", submission.getAdminRemark());
            item.put("status", submission.getStatus());
            item.put("submitTime", submission.getSubmitTime());
            item.put("gradeTime", submission.getGradeTime());
            item.put("reviewTime", submission.getReviewTime());
            if (includeAnswerSheet) {
                item.put("answerSheet", JSON.parseArray(submission.getAnswerSheetJson()));
            }
            records.add(item);
        }
        return records;
    }

    private List<ExamOptionDTO> parseOptions(String json) {
        return StringUtils.hasText(json) ? JSON.parseArray(json, ExamOptionDTO.class) : Collections.emptyList();
    }

    private List<JudgeCaseDTO> parseJudgeCases(String json) {
        return StringUtils.hasText(json) ? JSON.parseArray(json, JudgeCaseDTO.class) : Collections.emptyList();
    }

    private List<String> parseStringList(String json) {
        return StringUtils.hasText(json) ? JSON.parseArray(json, String.class) : Collections.emptyList();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String extractPayloadString(Map<String, Object> payload, String key) {
        if (payload == null || key == null || !payload.containsKey(key) || payload.get(key) == null) {
            return "";
        }
        return safeTrim(String.valueOf(payload.get(key)));
    }

    private String normalizeAnswer(String value) {
        return safeTrim(value).replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
