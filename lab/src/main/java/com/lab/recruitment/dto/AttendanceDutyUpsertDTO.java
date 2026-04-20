package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AttendanceDutyUpsertDTO {
    @NotNull(message = "Duty admin user id is required")
    private Long dutyAdminUserId;

    private Long backupAdminUserId;

    @Size(max = 255, message = "Remark length cannot exceed 255 characters")
    private String remark;
}
