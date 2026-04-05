package com.lab.recruitment.dto;

import lombok.Data;

@Data
public class WrittenExamAnswerDTO {

    private Long questionId;
    private String answer;
    private String language;
    private String code;
}
