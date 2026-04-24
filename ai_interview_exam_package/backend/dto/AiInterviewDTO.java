package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class AiInterviewDTO {

    @Data
    public static class StartSession {
        @NotBlank(message = "面试模式不能为空")
        private String mode; // mock / formal

        @NotNull(message = "模块 ID 不能为空")
        private Long moduleId;
    }

    @Data
    public static class ChatRequest {
        private Long sessionId;
        private String message;
        private Long moduleId;
        private String moduleName;
        private String mode;
        private List<Map<String, String>> chatHistory;
    }

    @Data
    public static class ReportRequest {
        private Long sessionId;
        private Long moduleId;
        private String moduleName;
        private String mode;
        private List<Map<String, String>> chatHistory;
    }

    @Data
    public static class FinishRequest {
        @NotNull(message = "会话 ID 不能为空")
        private Long sessionId;
        private Map<String, Object> report;
    }

    @Data
    public static class ModuleSave {
        private String moduleName;
        private String moduleCode;
        private String description;
        private String promptTemplate;
        private String scoreRule;
        private String icon;
        private String color;
        private Integer sortOrder;
        private Boolean status;
    }

    @Data
    public static class ToggleFormal {
        private Boolean open;
    }

    @Data
    public static class RecordQuery {
        private String studentName;
        private Long moduleId;
        private String scoreRange;
        private String startDate;
        private String endDate;
        private Integer page = 1;
        private Integer pageSize = 20;
    }
}
