package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LabApplyAuditDTO {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String auditComment;
}
