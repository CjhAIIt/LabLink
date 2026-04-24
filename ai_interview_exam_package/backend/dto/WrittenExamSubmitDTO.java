package com.lab.recruitment.dto;

import lombok.Data;

import java.util.List;

@Data
public class WrittenExamSubmitDTO {

    private Long labId;
    private List<WrittenExamAnswerDTO> answers;
}
