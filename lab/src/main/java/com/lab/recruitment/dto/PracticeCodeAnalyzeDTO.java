package com.lab.recruitment.dto;

import lombok.Data;

@Data
public class PracticeCodeAnalyzeDTO {

    private Long questionId;

    private String language;

    private String code;

    private String status;

    private String errorMsg;

    private String stdout;

    private Integer judgePassedCount;

    private Integer judgeTotalCount;
}
