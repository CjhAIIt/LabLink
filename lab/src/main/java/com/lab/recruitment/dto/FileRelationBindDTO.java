package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class FileRelationBindDTO {

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotNull(message = "Business id is required")
    private Long businessId;

    @NotEmpty(message = "File ids are required")
    private List<Long> fileIds;
}
