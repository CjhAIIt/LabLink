package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AttendanceManageTagDTO {

    @NotNull(message = "Attendance id is required")
    private Long attendanceId;

    @NotBlank(message = "Tag type is required")
    private String tagType;

    private String reason;
}
