package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AiInterviewDTO;
import com.lab.recruitment.entity.AiInterviewConfig;
import com.lab.recruitment.entity.AiInterviewModule;
import com.lab.recruitment.entity.AiInterviewRecord;
import com.lab.recruitment.mapper.AiInterviewConfigMapper;
import com.lab.recruitment.mapper.AiInterviewModuleMapper;
import com.lab.recruitment.mapper.AiInterviewRecordMapper;
import com.lab.recruitment.service.AiChatService;
import com.lab.recruitment.service.AiInterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        Page<AiInterviewRecord> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<AiInterviewRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStudentName())) {
            wrapper.like(AiInterviewRecord::getStudentName, query.getStudentName());
        }
        if (query.getModuleId() != null) {
            wrapper.eq(AiInterviewRecord::getModuleId, query.getModuleId());
        }
        if (StringUtils.hasText(query.getScoreRange())) {
            String[] parts = query.getScoreRange().split("-");
            if (parts.length == 2) {
                wrapper.ge(AiInterviewRecord::getScore, Integer.parseInt(parts[0]));
                wrapper.le(AiInterviewRecord::getScore, Integer.parseInt(parts[1]));
            }
        }
        wrapper.orderByDesc(AiInterviewRecord::getCreateTime);
        return recordMapper.selectPage(page, wrapper);
    }

    @Override
    public AiInterviewRecord getRecordDetail(Long id) {
        return recordMapper.selectById(id);
    }

    @Override
    public void invalidateRecord(Long id) {
        AiInterviewRecord record = recordMapper.selectById(id);
        if (record == null) throw new RuntimeException("记录不存在");
        record.setStatus("作废");
        recordMapper.updateById(record);
    }

    @Override
    public void resetStudentChance(Long studentId) {
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
}
