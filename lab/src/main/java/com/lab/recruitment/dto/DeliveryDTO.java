package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DeliveryDTO {
    
    @NotNull(message = "实验室ID不能为空")
    private Long labId;
    
    @NotBlank(message = "特长标签不能为空")
    private String skillTags;
    
    @NotBlank(message = "学习进度不能为空")
    private String studyProgress;
    
    private String attachmentUrl;
}