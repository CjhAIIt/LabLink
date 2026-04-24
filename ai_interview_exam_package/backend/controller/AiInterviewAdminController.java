package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AiInterviewDTO;
import com.lab.recruitment.entity.AiInterviewModule;
import com.lab.recruitment.entity.AiInterviewRecord;
import com.lab.recruitment.service.AiInterviewService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/ai-interview")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AiInterviewAdminController {

    @Autowired
    private AiInterviewService aiInterviewService;

    /** 获取所有模块（含停用） */
    @GetMapping("/modules")
    public Result<?> listModules() {
        try {
            return Result.success(aiInterviewService.listAllModules());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 新增模块 */
    @PostMapping("/modules")
    public Result<?> createModule(@RequestBody AiInterviewDTO.ModuleSave dto) {
        try {
            AiInterviewModule m = aiInterviewService.createModule(dto);
            return Result.success("新增成功", m);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 修改模块 */
    @PutMapping("/modules/{id}")
    public Result<?> updateModule(@PathVariable Long id, @RequestBody AiInterviewDTO.ModuleSave dto) {
        try {
            AiInterviewModule m = aiInterviewService.updateModule(id, dto);
            return Result.success("修改成功", m);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 删除模块 */
    @DeleteMapping("/modules/{id}")
    public Result<?> deleteModule(@PathVariable Long id) {
        try {
            aiInterviewService.deleteModule(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 分页查询正式面试记录 */
    @GetMapping("/records")
    public Result<?> queryRecords(AiInterviewDTO.RecordQuery query) {
        try {
            Page<AiInterviewRecord> page = aiInterviewService.queryRecords(query);
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 查看单次面试详情 */
    @GetMapping("/records/{id}")
    public Result<?> getRecordDetail(@PathVariable Long id) {
        try {
            AiInterviewRecord record = aiInterviewService.getRecordDetail(id);
            if (record == null) return Result.error("记录不存在");
            return Result.success(record);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 作废记录 */
    @PostMapping("/records/{id}/invalidate")
    public Result<?> invalidateRecord(@PathVariable Long id) {
        try {
            aiInterviewService.invalidateRecord(id);
            return Result.success("已作废");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 补偿次数 */
    @PostMapping("/students/{studentId}/reset-chance")
    public Result<?> resetChance(@PathVariable Long studentId) {
        try {
            aiInterviewService.resetStudentChance(studentId);
            return Result.success("已补偿 1 次机会");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取正式面试开关状态 */
    @GetMapping("/status")
    public Result<?> getFormalStatus() {
        try {
            return Result.success(Map.of("open", aiInterviewService.isFormalOpen()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 切换正式面试开关 */
    @PostMapping("/toggle")
    public Result<?> toggleFormal(@RequestBody AiInterviewDTO.ToggleFormal dto) {
        try {
            aiInterviewService.setFormalOpen(dto.getOpen());
            return Result.success("已更新");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
