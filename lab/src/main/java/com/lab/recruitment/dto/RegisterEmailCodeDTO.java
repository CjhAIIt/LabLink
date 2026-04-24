package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RegisterEmailCodeDTO {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^3\\d{7,13}$", message = "学号必须为3开头的8到14位纯数字")
    private String studentId;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
