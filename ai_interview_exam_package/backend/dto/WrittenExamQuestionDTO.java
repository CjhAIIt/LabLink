package com.lab.recruitment.dto;

import lombok.Data;

import java.util.List;

@Data
public class WrittenExamQuestionDTO {

    private Long id;
    private Long bankQuestionId;
    private String questionType;
    private String trackCode;
    private String title;
    private String content;
    private String difficulty;
    private String inputFormat;
    private String outputFormat;
    private String sampleCase;
    private List<String> tags;
    private String analysisHint;
    private List<ExamOptionDTO> options;
    private String correctAnswer;
    private List<String> acceptableAnswers;
    private List<String> allowedLanguages;
    private List<JudgeCaseDTO> judgeCases;
    private Integer score;
    private Integer sortOrder;
}
