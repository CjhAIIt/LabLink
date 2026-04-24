package com.lab.recruitment.controller;

import com.lab.recruitment.dto.AiInterviewDTO;
import com.lab.recruitment.entity.AiInterviewRecord;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AiInterviewService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai-interview")
public class AiInterviewController {

    @Autowired
    private AiInterviewService aiInterviewService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    /** 获取启用的面试模块列表 */
    @GetMapping("/modules")
    public Result<?> getModules() {
        try {
            return Result.success(aiInterviewService.listEnabledModules());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取正式面试剩余次数 */
    @GetMapping("/formal/chances")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> getFormalChances() {
        try {
            User user = currentUserAccessor.getCurrentUser();
            int remaining = aiInterviewService.getRemainingChances(user.getId());
            Map<String, Object> data = new HashMap<>();
            data.put("remaining", remaining);
            data.put("total", 2);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 开始面试会话 */
    @PostMapping("/session/start")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> startSession(@RequestBody AiInterviewDTO.StartSession dto) {
        try {
            if ("formal".equals(dto.getMode())) {
                User user = currentUserAccessor.getCurrentUser();
                AiInterviewRecord record = aiInterviewService.startFormalSession(
                        user.getId(), user.getRealName(), dto.getModuleId());
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", record.getId());
                return Result.success(data);
            }
            // 模拟面试不创建记录
            return Result.success(Map.of("sessionId", (Object) null));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** AI 对话 */
    @PostMapping("/chat")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> chat(@RequestBody AiInterviewDTO.ChatRequest req) {
        try {
            String reply = aiInterviewService.chat(req);
            return Result.success(Map.of("reply", reply));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 生成面试报告 */
    @PostMapping("/report")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> generateReport(@RequestBody AiInterviewDTO.ReportRequest req) {
        try {
            Map<String, Object> report = aiInterviewService.generateReport(req);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 结束正式面试并保存结果 */
    @PostMapping("/session/finish")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> finishSession(@RequestBody AiInterviewDTO.FinishRequest req) {
        try {
            aiInterviewService.finishFormalInterview(req);
            return Result.success("面试结果已保存");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取学生自己的正式面试记录 */
    @GetMapping("/my-records")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> getMyRecords() {
        try {
            User user = currentUserAccessor.getCurrentUser();
            List<AiInterviewRecord> records = aiInterviewService.getStudentRecords(user.getId());
            return Result.success(records);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}