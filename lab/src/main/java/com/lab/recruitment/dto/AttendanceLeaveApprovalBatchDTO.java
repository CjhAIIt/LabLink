package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AttendanceLeaveApprovalBatchDTO {

    @NotEmpty(message = "请假ID列表不能为空")
    private List<Long> leaveIds;

    @NotBlank(message = "审批动作不能为空")
    private String action;

    private String comment;
}
