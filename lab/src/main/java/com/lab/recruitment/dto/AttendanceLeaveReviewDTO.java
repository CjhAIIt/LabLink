package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AttendanceLeaveReviewDTO {

    @NotBlank(message = "Review comment is required")
    @Size(max = 255, message = "Review comment must be within 255 characters")
    private String reviewComment;
}
