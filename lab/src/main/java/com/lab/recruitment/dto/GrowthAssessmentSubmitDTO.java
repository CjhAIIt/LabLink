package com.lab.recruitment.dto;

import lombok.Data;

import java.util.List;

@Data
public class GrowthAssessmentSubmitDTO {

    private Integer versionNo;

    private List<GrowthAssessmentAnswerDTO> answers;
}
