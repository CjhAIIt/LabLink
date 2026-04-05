package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LabCreateApplyAuditDTO {

    @NotBlank(message = "Action is required")
    private String action;

    private String auditComment;
}
