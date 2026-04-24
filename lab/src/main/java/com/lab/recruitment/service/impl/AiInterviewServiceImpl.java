package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AiInterviewDTO;
import com.lab.recruitment.entity.AiInterviewConfig;
import com.lab.recruitment.entity.AiInterviewModule;
import com.lab.recruitment.entity.AiInterviewRecord;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabApply;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AiInterviewConfigMapper;
import com.lab.recruitment.mapper.AiInterviewModuleMapper;
import com.lab.recruitment.mapper.AiInterviewRecordMapper;
import com.lab.recruitment.mapper.LabApplyMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AiChatService;
import com.lab.recruitment.service.AiInterviewService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class AiInterviewServiceImpl implements AiInterviewService {

    private static final int MAX_FORMAL_CHANCES = 2;

    @Autowired
    private AiInterviewModuleMapper moduleMapper;
    @Autowired
    private AiInterviewRecordMapper recordMapper;
    @Autowired
    private AiInterviewConfigMapper configMapper;
    @Autowired
    private AiChatService aiChatService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LabMapper labMapper;
    @Autowired
    private LabApplyMapper labApplyMapper;
    @Autowired
    private CurrentUserAccessor currentUserAccessor;
    @Autowired
    private UserAccessService userAccessService;

    // ========== 模块 ==========

    @Override
    public List<AiInterviewModule> listEnabledModules() {
        return moduleMapper.selectList(
            new LambdaQueryWrapper<AiInterviewModule>()
                .eq(AiInterviewModule::getStatus, 1)
                .orderByAsc(AiInterviewModule::getSortOrder));
    }

    // PLACEHOLDER_REMAINING_METHODS

    @Override
    public List<AiInterviewModule> listAllModules() {
        return moduleMapper.selectList(new LambdaQueryWrapper<AiInterviewModule>().orderByAsc(AiInterviewModule::getSortOrder));
    }

    @Override
    public AiInterviewModule createModule(AiInterviewDTO.ModuleSave dto) {
        AiInterviewModule m = new AiInterviewModule();
        copyModuleFields(m, dto);
        moduleMapper.insert(m);
        return m;
    }

    @Override
    public AiInterviewModule updateModule(Long id, AiInterviewDTO.ModuleSave dto) {
        AiInterviewModule m = moduleMapper.selectById(id);
        if (m == null) throw new RuntimeException("模块不存在");
        copyModuleFields(m, dto);
        moduleMapper.updateById(m);
        return m;
    }

    @Override
    public void deleteModule(Long id) {
        moduleMapper.deleteById(id);
    }

    private void copyModuleFields(AiInterviewModule m, AiInterviewDTO.ModuleSave dto) {
        m.setModuleName(dto.getModuleName());
        m.setModuleCode(dto.getModuleCode());
        m.setDescription(dto.getDescription());
        m.setPromptTemplate(dto.getPromptTemplate());
        m.setScoreRule(dto.getScoreRule());
        if (dto.getIcon() != null) m.setIcon(dto.getIcon());
        if (dto.getColor() != null) m.setColor(dto.getColor());
        if (dto.getSortOrder() != null) m.setSortOrder(dto.getSortOrder());
        m.setStatus(dto.getStatus() != null && dto.getStatus() ? 1 : 0);
    }

    // ========== 学生面试 ==========

    @Override
    public int getRemainingChances(Long studentId) {
        int used = recordMapper.countUsedChances(studentId);
        return Math.max(0, MAX_FORMAL_CHANCES - used);
    }

    @Override
    public AiInterviewRecord startFormalSession(Long studentId, String studentName, Long moduleId) {
        int remaining = getRemainingChances(studentId);
        if (remaining <= 0) throw new RuntimeException("正式面试次数已用完");

        AiInterviewModule module = moduleMapper.selectById(moduleId);
        if (module == null) throw new RuntimeException("面试模块不存在");

        int attemptNo = MAX_FORMAL_CHANCES - remaining + 1;
        AiInterviewRecord record = new AiInterviewRecord();
        record.setStudentId(studentId);
        record.setStudentName(studentName);
        record.setModuleId(moduleId);
        record.setModuleName(module.getModuleName());
        record.setAttemptNo(attemptNo);
        record.setStatus("进行中");
        record.setStartTime(LocalDateTime.now());
        recordMapper.insert(record);
        return record;
    }

    @Override
    public String chat(AiInterviewDTO.ChatRequest req) {
        AiInterviewModule module = null;
        if (req.getModuleId() != null) {
            module = moduleMapper.selectById(req.getModuleId());
        }
        String promptTemplate = module != null ? module.getPromptTemplate() : null;
        String moduleName = req.getModuleName() != null ? req.getModuleName() : "综合";
        return aiChatService.chat(moduleName, req.getMode(), req.getChatHistory(), promptTemplate);
    }

    @Override
    public Map<String, Object> generateReport(AiInterviewDTO.ReportRequest req) {
        String moduleName = req.getModuleName() != null ? req.getModuleName() : "综合";
        return aiChatService.generateReport(moduleName, req.getChatHistory());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void finishFormalInterview(AiInterviewDTO.FinishRequest req) {
        AiInterviewRecord record = recordMapper.selectById(req.getSessionId());
        if (record == null) throw new RuntimeException("面试记录不存在");

        Map<String, Object> report = req.getReport();
        if (report != null) {
            Object scoreObj = report.get("score");
            if (scoreObj instanceof Number) record.setScore(((Number) scoreObj).intValue());
            Object tagsObj = report.get("tags");
            if (tagsObj != null) record.setTagsJson(JSON.toJSONString(tagsObj));
            if (report.get("summary") != null) record.setSummary(report.get("summary").toString());
            if (report.get("strengths") != null) record.setStrengths(report.get("strengths").toString());
            if (report.get("weaknesses") != null) record.setWeaknesses(report.get("weaknesses").toString());
            if (report.get("suggestions") != null) record.setSuggestions(report.get("suggestions").toString());
        }
        record.setStatus("已完成");
        record.setEndTime(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Override
    public List<AiInterviewRecord> getStudentRecords(Long studentId) {
        return recordMapper.selectList(
            new LambdaQueryWrapper<AiInterviewRecord>()
                .eq(AiInterviewRecord::getStudentId, studentId)
                .orderByDesc(AiInterviewRecord::getCreateTime));
    }

    // ========== 管理端 ==========

    @Override
    public Page<AiInterviewRecord> queryRecords(AiInterviewDTO.RecordQuery query) {
        AiInterviewDTO.RecordQuery criteria = normalizeQuery(query);
        Page<AiInterviewRecord> page = new Page<>(criteria.getPage(), criteria.getPageSize());
        LambdaQueryWrapper<AiInterviewRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(criteria.getStudentName())) {
            wrapper.like(AiInterviewRecord::getStudentName, criteria.getStudentName());
        }
        if (criteria.getModuleId() != null) {
            wrapper.eq(AiInterviewRecord::getModuleId, criteria.getModuleId());
        }
        if (StringUtils.hasText(criteria.getScoreRange())) {
            String[] parts = criteria.getScoreRange().split("-");
            if (parts.length == 2) {
                wrapper.ge(AiInterviewRecord::getScore, Integer.parseInt(parts[0]));
                wrapper.le(AiInterviewRecord::getScore, Integer.parseInt(parts[1]));
            }
        }
        wrapper.orderByDesc(AiInterviewRecord::getCreateTime);
        return recordMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<Map<String, Object>> queryRecordViews(AiInterviewDTO.RecordQuery query) {
        AiInterviewDTO.RecordQuery criteria = normalizeQuery(query);
        DataScope scope = currentUserAccessor.getCurrentDataScope();
        Long labId = null;
        Long collegeId = null;
        Long studentUserId = null;
        if (scope != null && !scope.isSchoolLevel()) {
            if (scope.isLabLevel() || (scope.isSelfLevel() && scope.getLabId() != null)) {
                labId = scope.getLabId();
            } else if (scope.isCollegeLevel()) {
                collegeId = scope.getCollegeId();
            } else if (scope.isSelfLevel()) {
                studentUserId = scope.getUserId();
            } else {
                throw new RuntimeException("当前账号没有 AI 面试记录管理权限");
            }
        }

        Integer[] scoreRange = parseScoreRange(criteria.getScoreRange());
        Page<Map<String, Object>> page = new Page<>(criteria.getPage(), criteria.getPageSize());
        page.setOptimizeCountSql(false);
        return recordMapper.selectRecordViewPage(page,
                trimToNull(criteria.getStudentName()),
                criteria.getModuleId(),
                scoreRange[0],
                scoreRange[1],
                normalizeDateBoundary(criteria.getStartDate(), false),
                normalizeDateBoundary(criteria.getEndDate(), true),
                labId,
                collegeId,
                studentUserId);
    }

    @Override
    public AiInterviewRecord getRecordDetail(Long id) {
        return recordMapper.selectById(id);
    }

    @Override
    public Map<String, Object> getRecordDetailView(Long id) {
        AiInterviewRecord record = recordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        assertCanAccessRecord(record);
        return buildRecordView(record);
    }

    @Override
    public void invalidateRecord(Long id) {
        AiInterviewRecord record = recordMapper.selectById(id);
        if (record == null) throw new RuntimeException("记录不存在");
        assertCanAccessRecord(record);
        record.setStatus("作废");
        recordMapper.updateById(record);
    }

    @Override
    public void resetStudentChance(Long studentId) {
        assertCanAccessStudent(studentId);
        // 将该学生最近一条非作废记录标记为作废，等效于补偿 1 次
        List<AiInterviewRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<AiInterviewRecord>()
                .eq(AiInterviewRecord::getStudentId, studentId)
                .ne(AiInterviewRecord::getStatus, "作废")
                .orderByDesc(AiInterviewRecord::getCreateTime));
        if (!records.isEmpty()) {
            AiInterviewRecord latest = records.get(0);
            latest.setStatus("作废");
            recordMapper.updateById(latest);
        }
    }

    // ========== 配置 ==========

    @Override
    public boolean isFormalOpen() {
        AiInterviewConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<AiInterviewConfig>().eq(AiInterviewConfig::getConfigKey, "formal_interview_open"));
        return config != null && "true".equals(config.getConfigValue());
    }

    @Override
    public void setFormalOpen(boolean open) {
        AiInterviewConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<AiInterviewConfig>().eq(AiInterviewConfig::getConfigKey, "formal_interview_open"));
        if (config == null) {
            config = new AiInterviewConfig();
            config.setConfigKey("formal_interview_open");
            config.setConfigValue(String.valueOf(open));
            configMapper.insert(config);
        } else {
            config.setConfigValue(String.valueOf(open));
            configMapper.updateById(config);
        }
    }

    private Map<String, Object> buildRecordView(AiInterviewRecord record) {
        Map<String, Object> item = new HashMap<>();
        User student = record.getStudentId() == null ? null : userMapper.selectById(record.getStudentId());
        Lab lab = resolveRecordLab(student);
        item.put("id", record.getId());
        item.put("studentId", record.getStudentId());
        item.put("studentUserId", record.getStudentId());
        item.put("studentName", StringUtils.hasText(record.getStudentName()) ? record.getStudentName() : student == null ? "" : student.getRealName());
        item.put("studentNo", student == null ? "" : student.getStudentId());
        item.put("college", student == null ? "" : student.getCollege());
        item.put("major", student == null ? "" : student.getMajor());
        item.put("grade", student == null ? "" : student.getGrade());
        item.put("labId", lab == null ? null : lab.getId());
        item.put("labName", lab == null ? "" : lab.getLabName());
        item.put("moduleId", record.getModuleId());
        item.put("moduleName", record.getModuleName());
        item.put("attemptNo", record.getAttemptNo());
        item.put("score", record.getScore());
        item.put("tagsJson", record.getTagsJson());
        item.put("summary", record.getSummary());
        item.put("strengths", record.getStrengths());
        item.put("weaknesses", record.getWeaknesses());
        item.put("suggestions", record.getSuggestions());
        item.put("conversationJson", record.getConversationJson());
        item.put("status", record.getStatus());
        item.put("startTime", record.getStartTime());
        item.put("endTime", record.getEndTime());
        item.put("createTime", record.getCreateTime());
        item.put("recommendNext", record.getScore() != null && record.getScore() >= 80);
        return item;
    }

    private boolean canAccessRecord(AiInterviewRecord record) {
        if (record == null) {
            return false;
        }
        User student = record.getStudentId() == null ? null : userMapper.selectById(record.getStudentId());
        return canAccessStudent(student);
    }

    private void assertCanAccessRecord(AiInterviewRecord record) {
        if (!canAccessRecord(record)) {
            throw new RuntimeException("无权查看该学生的 AI 面试记录");
        }
    }

    private void assertCanAccessStudent(Long studentId) {
        User student = studentId == null ? null : userMapper.selectById(studentId);
        if (!canAccessStudent(student)) {
            throw new RuntimeException("无权操作该学生的 AI 面试记录");
        }
    }

    private boolean canAccessStudent(User student) {
        DataScope scope = currentUserAccessor.getCurrentDataScope();
        if (scope == null || scope.isSchoolLevel()) {
            return true;
        }
        Lab lab = resolveRecordLab(student);
        if (scope.isSelfLevel()) {
            if (scope.getLabId() != null && lab != null) {
                return Objects.equals(scope.getLabId(), lab.getId());
            }
            return student != null && Objects.equals(scope.getUserId(), student.getId());
        }
        if (lab == null) {
            return false;
        }
        if (scope.isLabLevel()) {
            return Objects.equals(scope.getLabId(), lab.getId());
        }
        if (scope.isCollegeLevel()) {
            return Objects.equals(scope.getCollegeId(), lab.getCollegeId());
        }
        return false;
    }

    private Lab resolveRecordLab(User student) {
        if (student == null) {
            return null;
        }
        Long activeLabId = userAccessService.resolveManagedLabId(student);
        if (activeLabId != null) {
            Lab lab = labMapper.selectById(activeLabId);
            if (lab != null && !Objects.equals(lab.getDeleted(), 1)) {
                return lab;
            }
        }
        LabApply latestApply = labApplyMapper.selectOne(new LambdaQueryWrapper<LabApply>()
                .eq(LabApply::getStudentUserId, student.getId())
                .eq(LabApply::getDeleted, 0)
                .orderByDesc(LabApply::getCreateTime)
                .last("LIMIT 1"));
        if (latestApply == null || latestApply.getLabId() == null) {
            return null;
        }
        Lab lab = labMapper.selectById(latestApply.getLabId());
        return lab != null && !Objects.equals(lab.getDeleted(), 1) ? lab : null;
    }

    private AiInterviewDTO.RecordQuery normalizeQuery(AiInterviewDTO.RecordQuery query) {
        AiInterviewDTO.RecordQuery criteria = query == null ? new AiInterviewDTO.RecordQuery() : query;
        if (criteria.getPage() == null || criteria.getPage() < 1) {
            criteria.setPage(1);
        }
        if (criteria.getPageSize() == null || criteria.getPageSize() < 1) {
            criteria.setPageSize(20);
        }
        return criteria;
    }

    private Integer[] parseScoreRange(String scoreRange) {
        Integer[] result = new Integer[] { null, null };
        if (!StringUtils.hasText(scoreRange)) {
            return result;
        }
        String[] parts = scoreRange.split("-");
        if (parts.length != 2) {
            return result;
        }
        try {
            result[0] = Integer.parseInt(parts[0].trim());
            result[1] = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException ignored) {
            result[0] = null;
            result[1] = null;
        }
        return result;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDateBoundary(String value, boolean inclusiveEndOfDay) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() == 10) {
            return normalized + (inclusiveEndOfDay ? " 23:59:59" : " 00:00:00");
        }
        if (normalized.length() == 19 && normalized.charAt(10) == 'T') {
            return normalized.replace('T', ' ');
        }
        return normalized;
    }
}
