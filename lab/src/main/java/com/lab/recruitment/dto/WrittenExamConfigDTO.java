package com.lab.recruitment.dto;

import lombok.Data;

import java.util.List;

@Data
public class WrittenExamConfigDTO {

    private Boolean recruitmentOpen;
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private Integer passScore;
    private List<WrittenExamQuestionDTO> questions;
}
