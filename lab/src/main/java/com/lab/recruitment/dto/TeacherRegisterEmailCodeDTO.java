package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class TeacherRegisterEmailCodeDTO {

    @NotBlank(message = "工号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$", message = "工号需为 3 到 32 位字母、数字、下划线或中划线")
    private String teacherNo;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
