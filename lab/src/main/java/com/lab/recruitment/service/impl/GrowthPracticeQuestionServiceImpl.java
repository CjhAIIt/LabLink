package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.ExamOptionDTO;
import com.lab.recruitment.dto.JudgeCaseDTO;
import com.lab.recruitment.dto.PracticeQuestionSubmitDTO;
import com.lab.recruitment.dto.WrittenExamQuestionDTO;
import com.lab.recruitment.entity.GrowthPracticeQuestion;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.GrowthPracticeQuestionMapper;
import com.lab.recruitment.service.GrowthPracticeQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GrowthPracticeQuestionServiceImpl implements GrowthPracticeQuestionService {

    private static final int QUESTION_STATUS_ACTIVE = 1;
    private static final String QUESTION_SINGLE_CHOICE = "single_choice";
    private static final String QUESTION_FILL_BLANK = "fill_blank";
    private static final String QUESTION_PROGRAMMING = "programming";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 30;
    private static final int MAX_KEYWORD_LENGTH = 80;
    private static final int MAX_TITLE_LENGTH = 180;
    private static final int MAX_CONTENT_LENGTH = 6000;
    private static final int MAX_TEXTAREA_LENGTH = 2000;
    private static final int MAX_ANALYSIS_HINT_LENGTH = 1000;
    private static final int MAX_TAG_COUNT = 8;
    private static final int MAX_TAG_LENGTH = 24;
    private static final int MAX_OPTION_COUNT = 6;
    private static final int MAX_OPTION_TEXT_LENGTH = 200;
    private static final int MAX_ACCEPTABLE_ANSWER_COUNT = 8;
    private static final int MAX_ACCEPTABLE_ANSWER_LENGTH = 100;
    private static final int MAX_ALLOWED_LANGUAGE_COUNT = 4;
    private static final int MAX_JUDGE_CASE_COUNT = 12;
    private static final int MAX_SUBMIT_CODE_LENGTH = 20000;
    private static final int MAX_RUN_INPUT_LENGTH = 4000;
    private static final int MAX_SHORT_ANSWER_LENGTH = 500;
    private static final Set<String> SUPPORTED_QUESTION_TYPES = Set.of(
            QUESTION_SINGLE_CHOICE, QUESTION_FILL_BLANK, QUESTION_PROGRAMMING
    );
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("c", "cpp", "java", "python");
    private static final Set<String> SUPPORTED_DIFFICULTIES = Set.of("简单", "中等", "困难");

    @Autowired
    private GrowthPracticeQuestionMapper growthPracticeQuestionMapper;

    @Autowired
    private CodeJudgeService codeJudgeService;

    @Override
    public Map<String, Object> getStudentQuestionPage(User user,
                                                      Integer pageNum,
                                                      Integer pageSize,
                                                      String trackCode,
                                                      String questionType,
                                                      String keyword) {
        Page<GrowthPracticeQuestion> page = queryQuestionPage(
                pageNum, pageSize, trackCode, questionType, keyword, true, false
        );
        return buildPageResult(page, false);
    }

    @Override
    public Map<String, Object> getStudentQuestionDetail(User user, Long questionId) {
        GrowthPracticeQuestion question = requireQuestion(questionId, true);
        return buildQuestionDetail(question, false);
    }

    @Override
    public Map<String, Object> submitPracticeAnswer(User user, PracticeQuestionSubmitDTO submitDTO) {
        if (submitDTO == null || submitDTO.getQuestionId() == null) {
            throw new RuntimeException("Question id is required");
        }

        GrowthPracticeQuestion question = requireQuestion(submitDTO.getQuestionId(), true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questionId", question.getId());
        result.put("questionType", question.getQuestionType());

        if (QUESTION_SINGLE_CHOICE.equals(question.getQuestionType())) {
            String userAnswer = normalizeAnswer(trimToNull(submitDTO.getAnswer(), "Answer", MAX_SHORT_ANSWER_LENGTH));
            boolean correct = userAnswer.equals(normalizeAnswer(question.getAnswerConfig()));
            result.put("correct", correct);
            result.put("status", correct ? "success" : "wrong_answer");
            result.put("message", correct ? "Answer accepted" : "Wrong answer");
            return result;
        }

        if (QUESTION_FILL_BLANK.equals(question.getQuestionType())) {
            Set<String> acceptableAnswers = parseStringList(question.getAnswerConfig()).stream()
                    .map(this::normalizeAnswer)
                    .collect(Collectors.toSet());
            String userAnswer = normalizeAnswer(trimToNull(submitDTO.getAnswer(), "Answer", MAX_SHORT_ANSWER_LENGTH));
            boolean correct = acceptableAnswers.contains(userAnswer);
            result.put("correct", correct);
            result.put("status", correct ? "success" : "wrong_answer");
            result.put("message", correct ? "Answer accepted" : "Answer does not match");
            return result;
        }

        if (!QUESTION_PROGRAMMING.equals(question.getQuestionType())) {
            throw new RuntimeException("Unsupported question type");
        }

        String mode = normalizeSubmitMode(submitDTO.getMode());
        String language = normalizeLanguage(submitDTO.getLanguage());
        ensureAllowedLanguage(question, language);
        String code = normalizeSubmissionCode(submitDTO.getCode());
        String input = validateLength(submitDTO.getInput(), "Input", MAX_RUN_INPUT_LENGTH);

        if ("debug".equals(mode)) {
            CodeJudgeService.RunResult runResult = codeJudgeService.run(
                    language,
                    code,
                    input
            );
            result.put("status", runResult.getStatus());
            result.put("stdout", runResult.getStdout());
            result.put("stderr", runResult.getStderr());
            result.put("error", runResult.getError());
            result.put("correct", false);
            return result;
        }

        CodeJudgeService.JudgeResult judgeResult = codeJudgeService.judge(
                language,
                code,
                parseJudgeCases(question.getJudgeCaseJson())
        );
        boolean correct = judgeResult.isSuccess();
        String status;
        if (!judgeResult.isAvailable()) {
            status = "error";
        } else if (judgeResult.getTotalCount() <= 0) {
            status = "error";
        } else {
            status = correct ? "success" : "wrong_answer";
        }

        result.put("correct", correct);
        result.put("status", status);
        result.put("judgePassedCount", judgeResult.getPassedCount());
        result.put("judgeTotalCount", judgeResult.getTotalCount());
        result.put("message", judgeResult.getMessage());
        return result;
    }

    @Override
    public Page<Map<String, Object>> getAdminQuestionPage(User user,
                                                          Integer pageNum,
                                                          Integer pageSize,
                                                          String trackCode,
                                                          String questionType,
                                                          String keyword) {
        ensureQuestionAdmin(user);
        Page<GrowthPracticeQuestion> page = queryQuestionPage(
                pageNum, pageSize, trackCode, questionType, keyword, false, true
        );
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(this::buildAdminQuestionListItem)
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> getAdminQuestionDetail(User user, Long questionId) {
        ensureQuestionAdmin(user);
        GrowthPracticeQuestion question = requireQuestion(questionId, false);
        return buildQuestionDetail(question, true);
    }

    @Override
    public Map<String, Object> saveAdminQuestion(User user, WrittenExamQuestionDTO questionDTO) {
        ensureQuestionAdmin(user);
        normalizeQuestion(questionDTO);
        validateQuestion(questionDTO);

        GrowthPracticeQuestion question = questionDTO.getId() == null
                ? new GrowthPracticeQuestion()
                : growthPracticeQuestionMapper.selectById(questionDTO.getId());
        if (question == null) {
            throw new RuntimeException("Question not found");
        }

        if (question.getId() == null) {
            question.setCreatorId(user.getId());
        }

        question.setQuestionType(questionDTO.getQuestionType());
        question.setTrackCode(normalizeTrackCode(questionDTO.getTrackCode()));
        question.setTitle(questionDTO.getTitle().trim());
        question.setContent(trimToNull(questionDTO.getContent()));
        question.setDifficulty(trimToNull(questionDTO.getDifficulty()));
        question.setInputFormat(trimToNull(questionDTO.getInputFormat()));
        question.setOutputFormat(trimToNull(questionDTO.getOutputFormat()));
        question.setSampleCaseJson(trimToNull(questionDTO.getSampleCase()));
        question.setOptionsJson(questionDTO.getOptions() == null ? null : JSON.toJSONString(questionDTO.getOptions()));
        question.setProgramLanguages(questionDTO.getAllowedLanguages() == null ? null : JSON.toJSONString(questionDTO.getAllowedLanguages()));
        question.setJudgeCaseJson(questionDTO.getJudgeCases() == null ? null : JSON.toJSONString(questionDTO.getJudgeCases()));
        question.setTagsJson(questionDTO.getTags() == null ? null : JSON.toJSONString(questionDTO.getTags()));
        question.setAnalysisHint(trimToNull(questionDTO.getAnalysisHint()));
        question.setStatus(QUESTION_STATUS_ACTIVE);

        if (QUESTION_SINGLE_CHOICE.equals(questionDTO.getQuestionType())) {
            question.setAnswerConfig(questionDTO.getCorrectAnswer().trim());
        } else if (QUESTION_FILL_BLANK.equals(questionDTO.getQuestionType())) {
            question.setAnswerConfig(JSON.toJSONString(questionDTO.getAcceptableAnswers()));
        } else {
            question.setAnswerConfig(null);
        }

        if (question.getId() == null) {
            growthPracticeQuestionMapper.insert(question);
        } else {
            growthPracticeQuestionMapper.updateById(question);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", question.getId());
        result.put("question", buildQuestionDetail(question, true));
        return result;
    }

    @Override
    public boolean deleteAdminQuestion(User user, Long questionId) {
        ensureQuestionAdmin(user);
        GrowthPracticeQuestion question = growthPracticeQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("Question not found");
        }
        return growthPracticeQuestionMapper.deleteById(questionId) > 0;
    }

    @Override
    public List<WrittenExamQuestionDTO> getQuestionSnapshots(List<Long> questionIds, boolean includeAnswerConfig) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<GrowthPracticeQuestion> questions = growthPracticeQuestionMapper.selectBatchIds(questionIds);
        Map<Long, GrowthPracticeQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(GrowthPracticeQuestion::getId, Function.identity(), (left, right) -> left));

        List<WrittenExamQuestionDTO> result = new ArrayList<>();
        for (Long questionId : questionIds) {
            GrowthPracticeQuestion question = questionMap.get(questionId);
            if (question == null) {
                throw new RuntimeException("Question not found: " + questionId);
            }
            result.add(toQuestionDTO(question, includeAnswerConfig));
        }
        return result;
    }

    private Page<GrowthPracticeQuestion> queryQuestionPage(Integer pageNum,
                                                           Integer pageSize,
                                                           String trackCode,
                                                           String questionType,
                                                           String keyword,
                                                           boolean activeOnly,
                                                           boolean summaryOnly) {
        String normalizedQuestionType = normalizeQuestionTypeFilter(questionType);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedTrackCode = normalizeTrackCodeFilter(trackCode);
        Page<GrowthPracticeQuestion> page = new Page<>(sanitizePageNum(pageNum), sanitizePageSize(pageSize));
        QueryWrapper<GrowthPracticeQuestion> queryWrapper = new QueryWrapper<>();
        if (summaryOnly) {
            queryWrapper.select("id", "question_type", "track_code", "title", "difficulty", "tags_json", "status");
        }
        if (activeOnly) {
            queryWrapper.eq("status", QUESTION_STATUS_ACTIVE);
        }
        if (StringUtils.hasText(normalizedQuestionType)) {
            queryWrapper.eq("question_type", normalizedQuestionType);
        }
        if (StringUtils.hasText(normalizedKeyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", normalizedKeyword)
                    .or()
                    .like("content", normalizedKeyword));
        }
        if (StringUtils.hasText(normalizedTrackCode)) {
            queryWrapper.and(wrapper -> wrapper
                    .eq("track_code", normalizedTrackCode)
                    .or()
                    .eq("track_code", "common"));
        }
        if (summaryOnly) {
            queryWrapper.orderByDesc("id");
        } else {
            queryWrapper.orderByAsc("question_type")
                    .orderByAsc("track_code")
                    .orderByDesc("id");
        }
        return growthPracticeQuestionMapper.selectPage(page, queryWrapper);
    }

    private Map<String, Object> buildPageResult(Page<GrowthPracticeQuestion> page, boolean includeAnswerConfig) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", page.getRecords().stream()
                .map(item -> buildQuestionSummary(item, includeAnswerConfig))
                .collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        result.put("pages", page.getPages());
        result.put("environmentStatus", codeJudgeService.getEnvironmentStatus());
        result.put("environmentDetails", codeJudgeService.getEnvironmentDetails());
        return result;
    }

    private Map<String, Object> buildQuestionSummary(GrowthPracticeQuestion question, boolean includeAnswerConfig) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", question.getId());
        data.put("questionType", question.getQuestionType());
        data.put("trackCode", question.getTrackCode());
        data.put("title", question.getTitle());
        data.put("content", question.getContent());
        data.put("difficulty", question.getDifficulty());
        data.put("tags", parseStringList(question.getTagsJson()));
        data.put("analysisHint", question.getAnalysisHint());
        data.put("status", question.getStatus());
        if (includeAnswerConfig) {
            data.putAll(buildQuestionDetail(question, true));
        }
        return data;
    }

    private Map<String, Object> buildAdminQuestionListItem(GrowthPracticeQuestion question) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", question.getId());
        data.put("questionType", question.getQuestionType());
        data.put("trackCode", question.getTrackCode());
        data.put("title", question.getTitle());
        data.put("difficulty", question.getDifficulty());
        data.put("tags", parseStringList(question.getTagsJson()));
        data.put("status", question.getStatus());
        return data;
    }

    private Map<String, Object> buildQuestionDetail(GrowthPracticeQuestion question, boolean includeAnswerConfig) {
        WrittenExamQuestionDTO dto = toQuestionDTO(question, includeAnswerConfig);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", dto.getId());
        data.put("bankQuestionId", dto.getBankQuestionId());
        data.put("questionType", dto.getQuestionType());
        data.put("trackCode", dto.getTrackCode());
        data.put("title", dto.getTitle());
        data.put("content", dto.getContent());
        data.put("difficulty", dto.getDifficulty());
        data.put("inputFormat", dto.getInputFormat());
        data.put("outputFormat", dto.getOutputFormat());
        data.put("sampleCase", dto.getSampleCase());
        data.put("tags", dto.getTags());
        data.put("analysisHint", dto.getAnalysisHint());
        data.put("options", dto.getOptions());
        data.put("allowedLanguages", dto.getAllowedLanguages());
        data.put("score", dto.getScore());
        data.put("status", question.getStatus());
        data.put("environmentStatus", codeJudgeService.getEnvironmentStatus());
        data.put("environmentDetails", codeJudgeService.getEnvironmentDetails());
        if (includeAnswerConfig) {
            data.put("correctAnswer", dto.getCorrectAnswer());
            data.put("acceptableAnswers", dto.getAcceptableAnswers());
            data.put("judgeCases", dto.getJudgeCases());
        }
        return data;
    }

    private WrittenExamQuestionDTO toQuestionDTO(GrowthPracticeQuestion question, boolean includeAnswerConfig) {
        WrittenExamQuestionDTO dto = new WrittenExamQuestionDTO();
        dto.setId(question.getId());
        dto.setBankQuestionId(question.getId());
        dto.setQuestionType(question.getQuestionType());
        dto.setTrackCode(question.getTrackCode());
        dto.setTitle(question.getTitle());
        dto.setContent(question.getContent());
        dto.setDifficulty(question.getDifficulty());
        dto.setInputFormat(question.getInputFormat());
        dto.setOutputFormat(question.getOutputFormat());
        dto.setSampleCase(question.getSampleCaseJson());
        dto.setTags(parseStringList(question.getTagsJson()));
        dto.setAnalysisHint(question.getAnalysisHint());
        dto.setOptions(parseOptions(question.getOptionsJson()));
        dto.setAllowedLanguages(parseStringList(question.getProgramLanguages()));
        dto.setScore(10);
        if (includeAnswerConfig) {
            if (QUESTION_SINGLE_CHOICE.equals(question.getQuestionType())) {
                dto.setCorrectAnswer(question.getAnswerConfig());
            } else if (QUESTION_FILL_BLANK.equals(question.getQuestionType())) {
                dto.setAcceptableAnswers(parseStringList(question.getAnswerConfig()));
            } else if (QUESTION_PROGRAMMING.equals(question.getQuestionType())) {
                dto.setJudgeCases(parseJudgeCases(question.getJudgeCaseJson()));
            }
        }
        return dto;
    }

    private GrowthPracticeQuestion requireQuestion(Long questionId, boolean activeOnly) {
        GrowthPracticeQuestion question = growthPracticeQuestionMapper.selectById(questionId);
        if (question == null || (activeOnly && !Integer.valueOf(QUESTION_STATUS_ACTIVE).equals(question.getStatus()))) {
            throw new RuntimeException("Question not found");
        }
        return question;
    }

    private void validateQuestion(WrittenExamQuestionDTO questionDTO) {
        if (questionDTO == null || !StringUtils.hasText(questionDTO.getQuestionType()) || !StringUtils.hasText(questionDTO.getTitle())) {
            throw new RuntimeException("Question type and title are required");
        }

        if (QUESTION_SINGLE_CHOICE.equals(questionDTO.getQuestionType())) {
            if (questionDTO.getOptions() == null || questionDTO.getOptions().size() < 2 || !StringUtils.hasText(questionDTO.getCorrectAnswer())) {
                throw new RuntimeException("Single choice questions must configure options and the correct answer");
            }
            return;
        }

        if (QUESTION_FILL_BLANK.equals(questionDTO.getQuestionType())) {
            if (questionDTO.getAcceptableAnswers() == null || questionDTO.getAcceptableAnswers().isEmpty()) {
                throw new RuntimeException("Fill-in-the-blank questions must configure acceptable answers");
            }
            return;
        }

        if (QUESTION_PROGRAMMING.equals(questionDTO.getQuestionType())) {
            if (questionDTO.getAllowedLanguages() == null || questionDTO.getAllowedLanguages().isEmpty()) {
                throw new RuntimeException("Programming questions must configure allowed languages");
            }
            if (questionDTO.getJudgeCases() == null || questionDTO.getJudgeCases().isEmpty()) {
                throw new RuntimeException("Programming questions must configure judge cases");
            }
            return;
        }

        throw new RuntimeException("Unsupported question type");
    }

    private void normalizeQuestion(WrittenExamQuestionDTO questionDTO) {
        if (questionDTO == null) {
            throw new RuntimeException("Question data is required");
        }

        questionDTO.setQuestionType(normalizeQuestionType(questionDTO.getQuestionType()));
        questionDTO.setTrackCode(normalizeTrackCode(questionDTO.getTrackCode()));
        questionDTO.setTitle(requireText(questionDTO.getTitle(), "Title", MAX_TITLE_LENGTH));
        questionDTO.setContent(trimToNull(questionDTO.getContent(), "Content", MAX_CONTENT_LENGTH));
        questionDTO.setDifficulty(normalizeDifficulty(normalizeDifficultyAlias(questionDTO.getDifficulty())));
        questionDTO.setInputFormat(trimToNull(questionDTO.getInputFormat(), "Input format", MAX_TEXTAREA_LENGTH));
        questionDTO.setOutputFormat(trimToNull(questionDTO.getOutputFormat(), "Output format", MAX_TEXTAREA_LENGTH));
        questionDTO.setSampleCase(trimToNull(questionDTO.getSampleCase(), "Sample case", MAX_TEXTAREA_LENGTH));
        questionDTO.setTags(normalizeTags(questionDTO.getTags()));
        questionDTO.setAnalysisHint(trimToNull(questionDTO.getAnalysisHint(), "Analysis hint", MAX_ANALYSIS_HINT_LENGTH));

        if (QUESTION_SINGLE_CHOICE.equals(questionDTO.getQuestionType())) {
            List<ExamOptionDTO> options = normalizeOptions(questionDTO.getOptions());
            questionDTO.setOptions(options);
            questionDTO.setCorrectAnswer(normalizeCorrectAnswer(questionDTO.getCorrectAnswer(), options.size()));
            return;
        }

        if (QUESTION_FILL_BLANK.equals(questionDTO.getQuestionType())) {
            questionDTO.setAcceptableAnswers(normalizeAcceptableAnswers(questionDTO.getAcceptableAnswers()));
            return;
        }

        questionDTO.setAllowedLanguages(normalizeAllowedLanguages(questionDTO.getAllowedLanguages()));
        questionDTO.setJudgeCases(normalizeJudgeCases(questionDTO.getJudgeCases()));
    }

    private void ensureQuestionAdmin(User user) {
        if (user == null || user.getRole() == null) {
            throw new RuntimeException("Permission denied");
        }
        if (!"admin".equals(user.getRole()) && !"super_admin".equals(user.getRole())) {
            throw new RuntimeException("Only admins can manage the question bank");
        }
    }

    private List<ExamOptionDTO> parseOptions(String json) {
        return StringUtils.hasText(json) ? JSON.parseArray(json, ExamOptionDTO.class) : Collections.emptyList();
    }

    private List<String> parseStringList(String json) {
        return StringUtils.hasText(json) ? JSON.parseArray(json, String.class) : Collections.emptyList();
    }

    private List<JudgeCaseDTO> parseJudgeCases(String json) {
        return StringUtils.hasText(json) ? JSON.parseArray(json, JudgeCaseDTO.class) : Collections.emptyList();
    }

    private String normalizeAnswer(String value) {
        return value == null ? "" : value.trim().replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private int sanitizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int sanitizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        return trimToNull(keyword, "Keyword", MAX_KEYWORD_LENGTH);
    }

    private String normalizeQuestionTypeFilter(String questionType) {
        if (!StringUtils.hasText(questionType)) {
            return null;
        }
        return normalizeQuestionType(questionType);
    }

    private String normalizeTrackCodeFilter(String trackCode) {
        if (!StringUtils.hasText(trackCode) || "all".equalsIgnoreCase(trackCode.trim())) {
            return null;
        }
        return normalizeTrackCode(trackCode);
    }

    private String normalizeQuestionType(String questionType) {
        String normalized = requireText(questionType, "Question type", 30).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_QUESTION_TYPES.contains(normalized)) {
            throw new RuntimeException("Unsupported question type");
        }
        return normalized;
    }

    private String normalizeDifficultyAlias(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return difficulty;
        }
        switch (difficulty.trim().toLowerCase(Locale.ROOT)) {
            case "easy":
                return "\u7b80\u5355";
            case "medium":
                return "\u4e2d\u7b49";
            case "hard":
                return "\u56f0\u96be";
            default:
                return difficulty;
        }
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "中等";
        }
        String normalized = requireText(difficulty, "Difficulty", 32);
        if (!SUPPORTED_DIFFICULTIES.contains(normalized)) {
            throw new RuntimeException("Unsupported difficulty");
        }
        return normalized;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = trimToNull(tag, "Tag", MAX_TAG_LENGTH);
            if (value != null) {
                normalized.add(value);
            }
        }
        if (normalized.size() > MAX_TAG_COUNT) {
            throw new RuntimeException("Too many tags");
        }
        return new ArrayList<>(normalized);
    }

    private List<ExamOptionDTO> normalizeOptions(List<ExamOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        if (options.size() > MAX_OPTION_COUNT) {
            throw new RuntimeException("Too many options");
        }
        List<ExamOptionDTO> normalized = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            ExamOptionDTO option = options.get(index);
            ExamOptionDTO normalizedOption = new ExamOptionDTO();
            normalizedOption.setLabel(String.valueOf((char) ('A' + index)));
            normalizedOption.setText(requireText(option == null ? null : option.getText(), "Option text", MAX_OPTION_TEXT_LENGTH));
            normalized.add(normalizedOption);
        }
        return normalized;
    }

    private String normalizeCorrectAnswer(String correctAnswer, int optionCount) {
        String normalized = requireText(correctAnswer, "Correct answer", 2).toUpperCase(Locale.ROOT);
        if (normalized.length() != 1) {
            throw new RuntimeException("Correct answer is invalid");
        }
        char answer = normalized.charAt(0);
        if (answer < 'A' || answer >= 'A' + optionCount) {
            throw new RuntimeException("Correct answer is invalid");
        }
        return normalized;
    }

    private List<String> normalizeAcceptableAnswers(List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String answer : answers) {
            normalized.add(requireText(answer, "Acceptable answer", MAX_ACCEPTABLE_ANSWER_LENGTH));
        }
        if (normalized.size() > MAX_ACCEPTABLE_ANSWER_COUNT) {
            throw new RuntimeException("Too many acceptable answers");
        }
        return new ArrayList<>(normalized);
    }

    private List<String> normalizeAllowedLanguages(List<String> allowedLanguages) {
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String language : allowedLanguages) {
            normalized.add(normalizeLanguage(language));
        }
        if (normalized.size() > MAX_ALLOWED_LANGUAGE_COUNT) {
            throw new RuntimeException("Too many languages");
        }
        return new ArrayList<>(normalized);
    }

    private List<JudgeCaseDTO> normalizeJudgeCases(List<JudgeCaseDTO> judgeCases) {
        if (judgeCases == null || judgeCases.isEmpty()) {
            return Collections.emptyList();
        }
        if (judgeCases.size() > MAX_JUDGE_CASE_COUNT) {
            throw new RuntimeException("Too many judge cases");
        }
        List<JudgeCaseDTO> normalized = new ArrayList<>();
        for (JudgeCaseDTO judgeCase : judgeCases) {
            JudgeCaseDTO normalizedCase = new JudgeCaseDTO();
            normalizedCase.setInput(validateLength(judgeCase == null ? null : judgeCase.getInput(), "Judge case input", MAX_TEXTAREA_LENGTH));
            normalizedCase.setOutput(requireText(judgeCase == null ? null : judgeCase.getOutput(), "Judge case output", MAX_TEXTAREA_LENGTH));
            normalized.add(normalizedCase);
        }
        return normalized;
    }

    private String normalizeSubmitMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "submit";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!"submit".equals(normalized) && !"debug".equals(normalized)) {
            throw new RuntimeException("Unsupported submit mode");
        }
        return normalized;
    }

    private String normalizeLanguage(String language) {
        String normalized = requireText(language, "Language", 16).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_LANGUAGES.contains(normalized)) {
            throw new RuntimeException("Unsupported language");
        }
        return normalized;
    }

    private void ensureAllowedLanguage(GrowthPracticeQuestion question, String language) {
        Set<String> allowedLanguages = parseStringList(question.getProgramLanguages()).stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(SUPPORTED_LANGUAGES::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!allowedLanguages.isEmpty() && !allowedLanguages.contains(language)) {
            throw new RuntimeException("Selected language is not allowed for this question");
        }
    }

    private String normalizeSubmissionCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new RuntimeException("Code is required");
        }
        if (code.length() > MAX_SUBMIT_CODE_LENGTH) {
            throw new RuntimeException("Code is too large");
        }
        return code;
    }

    private String normalizeTrackCode(String trackCode) {
        String normalized = trimToNull(trackCode);
        if (normalized == null) {
            return "common";
        }
        if (normalized.length() > 50) {
            throw new RuntimeException("Track code is too long");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToNull(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new RuntimeException(fieldName + " is too long");
        }
        return normalized;
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value, fieldName, maxLength);
        if (normalized == null) {
            throw new RuntimeException(fieldName + " is required");
        }
        return normalized;
    }

    private String validateLength(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() > maxLength) {
            throw new RuntimeException(fieldName + " is too long");
        }
        return value;
    }
}
