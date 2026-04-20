package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AttendanceAnomalyHandleDTO {

    @NotEmpty(message = "记录ID列表不能为空")
    private List<Long> recordIds;

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    private String remark;
}
