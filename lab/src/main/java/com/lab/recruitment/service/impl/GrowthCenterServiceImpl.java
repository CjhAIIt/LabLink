package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.dto.GrowthAssessmentAnswerDTO;
import com.lab.recruitment.dto.GrowthAssessmentSubmitDTO;
import com.lab.recruitment.entity.GrowthAssessmentOption;
import com.lab.recruitment.entity.GrowthAssessmentQuestion;
import com.lab.recruitment.entity.GrowthAssessmentResult;
import com.lab.recruitment.entity.GrowthTrack;
import com.lab.recruitment.entity.GrowthTrackStage;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.GrowthAssessmentOptionMapper;
import com.lab.recruitment.mapper.GrowthAssessmentQuestionMapper;
import com.lab.recruitment.mapper.GrowthAssessmentResultMapper;
import com.lab.recruitment.mapper.GrowthTrackMapper;
import com.lab.recruitment.mapper.GrowthTrackStageMapper;
import com.lab.recruitment.service.GrowthCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GrowthCenterServiceImpl implements GrowthCenterService {

    private static final int ACTIVE_STATUS = 1;
    private static final int CURRENT_ASSESSMENT_VERSION = 1;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private GrowthTrackMapper growthTrackMapper;

    @Autowired
    private GrowthTrackStageMapper growthTrackStageMapper;

    @Autowired
    private GrowthAssessmentQuestionMapper growthAssessmentQuestionMapper;

    @Autowired
    private GrowthAssessmentOptionMapper growthAssessmentOptionMapper;

    @Autowired
    private GrowthAssessmentResultMapper growthAssessmentResultMapper;

    @Override
    public Map<String, Object> getDashboard(User user) {
        List<GrowthTrack> tracks = listActiveTracks(null);
        GrowthAssessmentResult latestResult = findLatestResult(user.getId());

        Map<String, Integer> scoreMap = latestResult == null
                ? Collections.emptyMap()
                : parseIntegerMap(latestResult.getScoreJson());
        List<Map<String, Object>> trackList = buildTrackList(tracks, scoreMap);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assessmentVersion", CURRENT_ASSESSMENT_VERSION);
        data.put("tracks", trackList);
        data.put("hasResult", latestResult != null);
        data.put("latestResult", latestResult == null ? null : buildResultView(latestResult, tracks));
        return data;
    }

    @Override
    public Map<String, Object> getAssessmentQuestions() {
        List<GrowthAssessmentQuestion> questions = listQuestionsByVersion(CURRENT_ASSESSMENT_VERSION);
        List<GrowthAssessmentOption> options = listOptionsByQuestionIds(
                questions.stream().map(GrowthAssessmentQuestion::getId).collect(Collectors.toList())
        );
        Map<Long, List<GrowthAssessmentOption>> optionMap = options.stream()
                .collect(Collectors.groupingBy(GrowthAssessmentOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> questionList = new ArrayList<>();
        for (GrowthAssessmentQuestion question : questions) {
            Map<String, Object> questionData = new LinkedHashMap<>();
            questionData.put("id", question.getId());
            questionData.put("versionNo", question.getVersionNo());
            questionData.put("questionNo", question.getQuestionNo());
            questionData.put("dimension", question.getDimension());
            questionData.put("title", question.getTitle());
            questionData.put("description", question.getDescription());
            questionData.put("questionType", question.getQuestionType());
            questionData.put("options", optionMap.getOrDefault(question.getId(), Collections.emptyList()).stream()
                    .map(this::buildOptionView)
                    .collect(Collectors.toList()));
            questionList.add(questionData);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("versionNo", CURRENT_ASSESSMENT_VERSION);
        data.put("questions", questionList);
        return data;
    }

    @Override
    @Transactional
    public Map<String, Object> submitAssessment(User user, GrowthAssessmentSubmitDTO submitDTO) {
        if (submitDTO == null || submitDTO.getAnswers() == null || submitDTO.getAnswers().isEmpty()) {
            throw new RuntimeException("请先完成成长测评题目");
        }

        List<GrowthTrack> tracks = listActiveTracks(null);
        if (tracks.isEmpty()) {
            throw new RuntimeException("成长路径尚未初始化");
        }

        List<GrowthAssessmentQuestion> questions = listQuestionsByVersion(CURRENT_ASSESSMENT_VERSION);
        if (questions.isEmpty()) {
            throw new RuntimeException("成长测评题库尚未初始化");
        }

        Map<Long, GrowthAssessmentQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(GrowthAssessmentQuestion::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<GrowthAssessmentOption> options = listOptionsByQuestionIds(
                questions.stream().map(GrowthAssessmentQuestion::getId).collect(Collectors.toList())
        );

        Map<String, GrowthAssessmentOption> optionLookup = new HashMap<>();
        Map<Long, List<GrowthAssessmentOption>> optionByQuestion = new LinkedHashMap<>();
        for (GrowthAssessmentOption option : options) {
            optionLookup.put(buildOptionLookupKey(option.getQuestionId(), option.getOptionKey()), option);
            optionByQuestion.computeIfAbsent(option.getQuestionId(), key -> new ArrayList<>()).add(option);
        }

        Map<Long, GrowthAssessmentAnswerDTO> answerByQuestion = new LinkedHashMap<>();
        for (GrowthAssessmentAnswerDTO answer : submitDTO.getAnswers()) {
            if (answer == null || answer.getQuestionId() == null || !StringUtils.hasText(answer.getOptionKey())) {
                continue;
            }
            answerByQuestion.put(answer.getQuestionId(), answer);
        }

        if (answerByQuestion.size() != questions.size()) {
            throw new RuntimeException("请完成全部 20 道成长测评题目");
        }

        Map<String, Integer> rawScoreMap = new LinkedHashMap<>();
        Map<String, Integer> maxScoreMap = buildMaxPossibleScoreMap(tracks, optionByQuestion);
        for (GrowthTrack track : tracks) {
            rawScoreMap.put(track.getCode(), 0);
        }

        List<Map<String, Object>> normalizedAnswers = new ArrayList<>();
        for (GrowthAssessmentQuestion question : questions) {
            GrowthAssessmentAnswerDTO answer = answerByQuestion.get(question.getId());
            if (answer == null) {
                throw new RuntimeException("题目 " + question.getQuestionNo() + " 尚未作答");
            }

            GrowthAssessmentOption selectedOption = optionLookup.get(buildOptionLookupKey(question.getId(), answer.getOptionKey()));
            if (selectedOption == null) {
                throw new RuntimeException("题目 " + question.getQuestionNo() + " 的选项无效");
            }

            Map<String, Integer> scoreContribution = parseIntegerMap(selectedOption.getScoreJson());
            for (Map.Entry<String, Integer> entry : scoreContribution.entrySet()) {
                rawScoreMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }

            Map<String, Object> answerData = new LinkedHashMap<>();
            answerData.put("questionId", question.getId());
            answerData.put("questionNo", question.getQuestionNo());
            answerData.put("title", question.getTitle());
            answerData.put("optionKey", selectedOption.getOptionKey());
            answerData.put("optionTitle", selectedOption.getOptionTitle());
            normalizedAnswers.add(answerData);
        }

        Map<String, Integer> matchScoreMap = new LinkedHashMap<>();
        for (GrowthTrack track : tracks) {
            int rawScore = rawScoreMap.getOrDefault(track.getCode(), 0);
            int maxScore = maxScoreMap.getOrDefault(track.getCode(), 0);
            int matchScore = maxScore <= 0 ? 0 : (int) Math.round(rawScore * 100.0 / maxScore);
            matchScoreMap.put(track.getCode(), Math.min(100, Math.max(0, matchScore)));
        }

        List<Map<String, Object>> ranking = buildRanking(tracks, rawScoreMap, maxScoreMap, matchScoreMap);
        List<String> topTrackCodes = ranking.stream()
                .limit(3)
                .map(item -> String.valueOf(item.get("code")))
                .collect(Collectors.toList());

        GrowthAssessmentResult result = new GrowthAssessmentResult();
        result.setUserId(user.getId());
        result.setAssessmentVersion(CURRENT_ASSESSMENT_VERSION);
        result.setAnswerJson(JSON.toJSONString(normalizedAnswers));
        result.setScoreJson(JSON.toJSONString(matchScoreMap));
        result.setTopTrackCodesJson(JSON.toJSONString(topTrackCodes));
        result.setSummary(buildSummary(ranking, tracks));
        growthAssessmentResultMapper.insert(result);

        return buildResultView(result, tracks);
    }

    @Override
    public List<Map<String, Object>> getTracks(User user, String category) {
        List<GrowthTrack> tracks = listActiveTracks(category);
        GrowthAssessmentResult latestResult = findLatestResult(user.getId());
        Map<String, Integer> scoreMap = latestResult == null
                ? Collections.emptyMap()
                : parseIntegerMap(latestResult.getScoreJson());
        return buildTrackList(tracks, scoreMap);
    }

    @Override
    public Map<String, Object> getTrackDetail(User user, String code) {
        if (!StringUtils.hasText(code)) {
            throw new RuntimeException("路径编码不能为空");
        }

        GrowthTrack track = growthTrackMapper.selectOne(
                new QueryWrapper<GrowthTrack>()
                        .eq("code", code)
                        .eq("status", ACTIVE_STATUS)
                        .orderByAsc("id")
                        .last("LIMIT 1")
        );
        if (track == null) {
            throw new RuntimeException("成长路径不存在");
        }

        GrowthAssessmentResult latestResult = findLatestResult(user.getId());
        Map<String, Integer> scoreMap = latestResult == null
                ? Collections.emptyMap()
                : parseIntegerMap(latestResult.getScoreJson());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("track", buildTrackView(track, scoreMap.get(track.getCode())));
        data.put("stages", listStagesByTrack(track.getId()).stream()
                .map(this::buildStageView)
                .collect(Collectors.toList()));
        data.put("latestResult", latestResult == null ? null : buildResultView(latestResult, listActiveTracks(null)));
        return data;
    }

    private List<GrowthTrack> listActiveTracks(String category) {
        QueryWrapper<GrowthTrack> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", ACTIVE_STATUS);
        if (StringUtils.hasText(category)) {
            queryWrapper.eq("category", category);
        }
        queryWrapper.orderByAsc("sort_order").orderByAsc("id");
        return growthTrackMapper.selectList(queryWrapper);
    }

    private List<GrowthTrackStage> listStagesByTrack(Long trackId) {
        QueryWrapper<GrowthTrackStage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("track_id", trackId)
                .eq("status", ACTIVE_STATUS)
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return growthTrackStageMapper.selectList(queryWrapper);
    }

    private List<GrowthAssessmentQuestion> listQuestionsByVersion(Integer versionNo) {
        QueryWrapper<GrowthAssessmentQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version_no", versionNo)
                .eq("status", ACTIVE_STATUS)
                .orderByAsc("question_no")
                .orderByAsc("id");
        return growthAssessmentQuestionMapper.selectList(queryWrapper);
    }

    private List<GrowthAssessmentOption> listOptionsByQuestionIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<GrowthAssessmentOption> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("question_id", questionIds)
                .orderByAsc("question_id")
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return growthAssessmentOptionMapper.selectList(queryWrapper);
    }

    private GrowthAssessmentResult findLatestResult(Long userId) {
        if (userId == null) {
            return null;
        }
        return growthAssessmentResultMapper.selectOne(
                new QueryWrapper<GrowthAssessmentResult>()
                        .eq("user_id", userId)
                        .orderByDesc("id")
                        .last("LIMIT 1")
        );
    }

    private List<Map<String, Object>> buildTrackList(List<GrowthTrack> tracks, Map<String, Integer> scoreMap) {
        return tracks.stream()
                .map(track -> buildTrackView(track, scoreMap.get(track.getCode())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildTrackView(GrowthTrack track, Integer matchScore) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", track.getId());
        data.put("code", track.getCode());
        data.put("name", track.getName());
        data.put("shortName", track.getShortName());
        data.put("category", track.getCategory());
        data.put("subtitle", track.getSubtitle());
        data.put("description", track.getDescription());
        data.put("fitScene", track.getFitScene());
        data.put("salaryRange", track.getSalaryRange());
        data.put("recommendedKeyword", track.getRecommendedKeyword());
        data.put("interviewPosition", track.getInterviewPosition());
        data.put("iconKey", track.getIconKey());
        data.put("difficultyLabel", track.getDifficultyLabel());
        data.put("tags", parseStringList(track.getTagsJson()));
        data.put("courses", parseStringList(track.getCoursesJson()));
        data.put("books", parseStringList(track.getBooksJson()));
        data.put("competitions", parseStringList(track.getCompetitionsJson()));
        data.put("certificates", parseStringList(track.getCertificatesJson()));
        data.put("competencies", parseObjectList(track.getCompetencyJson()));
        data.put("matchScore", matchScore);
        return data;
    }

    private Map<String, Object> buildStageView(GrowthTrackStage stage) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", stage.getId());
        data.put("stageNo", stage.getStageNo());
        data.put("phaseCode", stage.getPhaseCode());
        data.put("title", stage.getTitle());
        data.put("duration", stage.getDuration());
        data.put("goal", stage.getGoal());
        data.put("resourceName", stage.getResourceName());
        data.put("resourceUrl", stage.getResourceUrl());
        data.put("practiceKeyword", stage.getPracticeKeyword());
        data.put("actionHint", stage.getActionHint());
        data.put("focusSkills", parseStringList(stage.getFocusSkillsJson()));
        return data;
    }

    private Map<String, Object> buildOptionView(GrowthAssessmentOption option) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", option.getId());
        data.put("questionId", option.getQuestionId());
        data.put("optionKey", option.getOptionKey());
        data.put("optionTitle", option.getOptionTitle());
        data.put("optionDesc", option.getOptionDesc());
        return data;
    }

    private Map<String, Object> buildResultView(GrowthAssessmentResult result, List<GrowthTrack> tracks) {
        Map<String, Integer> scoreMap = parseIntegerMap(result.getScoreJson());
        Map<String, GrowthTrack> trackMap = tracks.stream()
                .collect(Collectors.toMap(GrowthTrack::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<Map<String, Object>> ranking = scoreMap.entrySet().stream()
                .map(entry -> {
                    GrowthTrack track = trackMap.get(entry.getKey());
                    if (track == null) {
                        return null;
                    }
                    Map<String, Object> data = buildTrackView(track, entry.getValue());
                    data.put("score", entry.getValue());
                    return data;
                })
                .filter(Objects::nonNull)
                .sorted((left, right) -> Integer.compare(
                        Integer.parseInt(String.valueOf(right.get("matchScore"))),
                        Integer.parseInt(String.valueOf(left.get("matchScore")))
                ))
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", result.getId());
        data.put("summary", result.getSummary());
        data.put("scores", scoreMap);
        data.put("topTrackCodes", parseStringList(result.getTopTrackCodesJson()));
        data.put("ranking", ranking);
        data.put("topTracks", ranking.stream().limit(3).collect(Collectors.toList()));
        data.put("createTime", result.getCreateTime() == null ? null : result.getCreateTime().format(DATE_TIME_FORMATTER));
        data.put("answerCount", parseAnswerCount(result.getAnswerJson()));
        return data;
    }

    private List<Map<String, Object>> buildRanking(List<GrowthTrack> tracks,
                                                   Map<String, Integer> rawScoreMap,
                                                   Map<String, Integer> maxScoreMap,
                                                   Map<String, Integer> matchScoreMap) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (GrowthTrack track : tracks) {
            Map<String, Object> data = buildTrackView(track, matchScoreMap.get(track.getCode()));
            data.put("rawScore", rawScoreMap.getOrDefault(track.getCode(), 0));
            data.put("maxScore", maxScoreMap.getOrDefault(track.getCode(), 0));
            ranking.add(data);
        }

        ranking.sort(Comparator
                .comparing((Map<String, Object> item) -> Integer.parseInt(String.valueOf(item.get("matchScore"))))
                .thenComparing(item -> Integer.parseInt(String.valueOf(item.get("rawScore"))))
                .reversed());
        return ranking;
    }

    private Map<String, Integer> buildMaxPossibleScoreMap(List<GrowthTrack> tracks,
                                                          Map<Long, List<GrowthAssessmentOption>> optionByQuestion) {
        Map<String, Integer> maxScoreMap = tracks.stream()
                .collect(Collectors.toMap(GrowthTrack::getCode, item -> 0, (left, right) -> left, LinkedHashMap::new));

        for (List<GrowthAssessmentOption> optionList : optionByQuestion.values()) {
            Map<String, Integer> questionMax = new HashMap<>();
            for (GrowthAssessmentOption option : optionList) {
                Map<String, Integer> scoreMap = parseIntegerMap(option.getScoreJson());
                for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
                    questionMax.merge(entry.getKey(), entry.getValue(), Math::max);
                }
            }
            for (Map.Entry<String, Integer> entry : questionMax.entrySet()) {
                maxScoreMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return maxScoreMap;
    }

    private String buildSummary(List<Map<String, Object>> ranking, List<GrowthTrack> tracks) {
        if (ranking.isEmpty()) {
            return "当前暂无可用的成长路径推荐。";
        }

        Map<String, GrowthTrack> trackMap = tracks.stream()
                .collect(Collectors.toMap(GrowthTrack::getCode, item -> item, (left, right) -> left));

        Map<String, Object> first = ranking.get(0);
        Map<String, Object> second = ranking.size() > 1 ? ranking.get(1) : null;
        GrowthTrack topTrack = trackMap.get(String.valueOf(first.get("code")));

        int firstScore = Integer.parseInt(String.valueOf(first.get("matchScore")));
        int secondScore = second == null ? 0 : Integer.parseInt(String.valueOf(second.get("matchScore")));
        int gap = firstScore - secondScore;

        if (topTrack == null) {
            return "你已经完成成长测评，可以先查看推荐路径并进入练习中心。";
        }

        if (gap >= 15) {
            return String.format(Locale.ROOT,
                    "你的倾向比较明确，当前最适合优先走「%s」路径。建议先按该路径的学习阶段推进，再用“%s”相关题目把手感补起来。",
                    topTrack.getName(),
                    topTrack.getRecommendedKeyword());
        }

        if (second != null) {
            GrowthTrack secondTrack = trackMap.get(String.valueOf(second.get("code")));
            return String.format(Locale.ROOT,
                    "你的测评结果呈现交叉倾向，目前「%s」和「%s」两条路径都比较适合。建议先根据最近一学期最愿意投入的任务类型，选定一条作为主路径推进。",
                    topTrack.getName(),
                    secondTrack == null ? "第二推荐路径" : secondTrack.getName());
        }

        return String.format(Locale.ROOT,
                "当前推荐路径为「%s」。建议先完成路径首页的学习阶段，再进入练习中心和 AI 面试继续验证方向。",
                topTrack.getName());
    }

    private int parseAnswerCount(String answerJson) {
        if (!StringUtils.hasText(answerJson)) {
            return 0;
        }
        try {
            JSONArray array = JSON.parseArray(answerJson);
            return array == null ? 0 : array.size();
        } catch (Exception exception) {
            return 0;
        }
    }

    private Map<String, Integer> parseIntegerMap(String json) {
        Map<String, Integer> data = new LinkedHashMap<>();
        if (!StringUtils.hasText(json)) {
            return data;
        }
        try {
            JSONObject object = JSON.parseObject(json);
            if (object == null) {
                return data;
            }
            Set<String> keys = object.keySet();
            for (String key : keys) {
                data.put(key, object.getIntValue(key));
            }
            return data;
        } catch (Exception exception) {
            return data;
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(json);
            if (array == null) {
                return Collections.emptyList();
            }
            return array.stream().map(String::valueOf).collect(Collectors.toList());
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> parseObjectList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(json);
            if (array == null) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> list = new ArrayList<>();
            for (Object item : array) {
                if (item instanceof JSONObject) {
                    list.add(new LinkedHashMap<>((JSONObject) item));
                } else {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("value", item);
                    list.add(data);
                }
            }
            return list;
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String buildOptionLookupKey(Long questionId, String optionKey) {
        return questionId + "#" + optionKey;
    }
}
