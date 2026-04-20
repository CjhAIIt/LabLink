package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AttendanceSignInDTO {

    @NotBlank(message = "Sign code is required")
    private String signCode;

    private String remark;
}
