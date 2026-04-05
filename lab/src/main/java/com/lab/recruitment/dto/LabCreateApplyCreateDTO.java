package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LabCreateApplyCreateDTO {

    @NotNull(message = "College is required")
    private Long collegeId;

    @NotBlank(message = "Lab name is required")
    private String labName;

    @NotBlank(message = "Teacher name is required")
    private String teacherName;

    private String location;

    private String contactEmail;

    @NotBlank(message = "Research direction is required")
    private String researchDirection;

    @NotBlank(message = "Apply reason is required")
    private String applyReason;
}
