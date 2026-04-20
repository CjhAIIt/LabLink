package com.lab.recruitment.dto;

import lombok.Data;

@Data
public class PracticeQuestionSubmitDTO {

    private Long questionId;
    private String mode;
    private String answer;
    private String language;
    private String code;
    private String input;
}
