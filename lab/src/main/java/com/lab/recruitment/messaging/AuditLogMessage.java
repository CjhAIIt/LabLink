package com.lab.recruitment.messaging;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogMessage {

    private Long actorUserId;
    private String operatorRole;
    private Long collegeId;
    private Long labId;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String requestPath;
    private String requestMethod;
    private String requestIp;
    private String result;
    private String detailJson;
    private LocalDateTime createdAt;
}
