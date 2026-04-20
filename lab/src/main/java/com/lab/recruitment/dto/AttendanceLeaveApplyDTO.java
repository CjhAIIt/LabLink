package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AttendanceLeaveApplyDTO {

    @NotNull(message = "Session id is required")
    private Long sessionId;

    @NotBlank(message = "Leave reason is required")
    @Size(max = 255, message = "Leave reason must be within 255 characters")
    private String leaveReason;
}
