package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AiInterviewDTO;
import com.lab.recruitment.entity.AiInterviewModule;
import com.lab.recruitment.entity.AiInterviewRecord;

import java.util.List;
import java.util.Map;

public interface AiInterviewService {

    // ========== 模块 ==========
    List<AiInterviewModule> listEnabledModules();
    List<AiInterviewModule> listAllModules();
    AiInterviewModule createModule(AiInterviewDTO.ModuleSave dto);
    AiInterviewModule updateModule(Long id, AiInterviewDTO.ModuleSave dto);
    void deleteModule(Long id);

    // ========== 学生面试 ==========
    int getRemainingChances(Long studentId);
    AiInterviewRecord startFormalSession(Long studentId, String studentName, Long moduleId);
    String chat(AiInterviewDTO.ChatRequest req);
    Map<String, Object> generateReport(AiInterviewDTO.ReportRequest req);
    void finishFormalInterview(AiInterviewDTO.FinishRequest req);
    List<AiInterviewRecord> getStudentRecords(Long studentId);

    // ========== 管理端 ==========
    Page<AiInterviewRecord> queryRecords(AiInterviewDTO.RecordQuery query);
    AiInterviewRecord getRecordDetail(Long id);
    void invalidateRecord(Long id);
    void resetStudentChance(Long studentId);

    // ========== 配置 ==========
    boolean isFormalOpen();
    void setFormalOpen(boolean open);
}
