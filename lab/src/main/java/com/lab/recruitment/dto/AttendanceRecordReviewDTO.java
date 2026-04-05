package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AttendanceRecordReviewDTO {

    @NotNull(message = "Session id is required")
    private Long sessionId;

    @NotNull(message = "User id is required")
    private Long userId;

    @NotBlank(message = "Sign status is required")
    private String signStatus;

    private String remark;
}
