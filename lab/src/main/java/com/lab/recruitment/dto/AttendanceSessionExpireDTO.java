package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AttendanceSessionExpireDTO {

    @NotNull(message = "Session id is required")
    private Long sessionId;
}
