package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AttendanceMakeupRequestDTO {
    @Size(max = 120, message = "Remark length cannot exceed 120 characters")
    private String remark;
}
