package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LabApplyCreateDTO {

    @NotNull(message = "实验室不能为空")
    private Long labId;

    private Long recruitPlanId;

    @NotBlank(message = "申请理由不能为空")
    private String applyReason;

    private String researchInterest;

    private String skillSummary;
}
