package com.lab.recruitment.messaging;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StatisticsRefreshMessage {

    private String domain;
    private Long collegeId;
    private Long labId;
    private Long operatorUserId;
    private String reason;
    private LocalDateTime occurredAt;
}
