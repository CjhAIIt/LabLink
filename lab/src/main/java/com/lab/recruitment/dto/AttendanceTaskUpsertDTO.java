package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class AttendanceTaskUpsertDTO {

    private Long id;

    private Long collegeId;

    @NotBlank(message = "Semester name is required")
    private String semesterName;

    @NotBlank(message = "Task name is required")
    private String taskName;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
