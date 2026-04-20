package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AttendanceSessionCreateDTO {

    @NotNull(message = "Lab id is required")
    private Long labId;
}
