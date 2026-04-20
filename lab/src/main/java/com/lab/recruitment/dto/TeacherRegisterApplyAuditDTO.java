package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TeacherRegisterApplyAuditDTO {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    @Size(max = 255, message = "审核备注长度不能超过 255 位")
    private String auditComment;
}
