package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalTime;

@Data
public class AttendanceScheduleDTO {

    private Long id;

    @NotNull(message = "Week day is required")
    @Min(value = 1, message = "Week day must be between 1 and 7")
    @Max(value = 7, message = "Week day must be between 1 and 7")
    private Integer weekDay;

    @NotNull(message = "Sign-in start time is required")
    private LocalTime signInStart;

    @NotNull(message = "Sign-in end time is required")
    private LocalTime signInEnd;

    private Integer lateThresholdMinutes;

    private Integer signCodeLength;

    private Integer codeTtlMinutes;

    private String remark;
}
