package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在 3 到 20 位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6 到 20 位之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 5, message = "真实姓名长度必须为2到5个字符")
    @Pattern(regexp = "^[\\p{IsHan}·]{2,5}$", message = "真实姓名需为2到5个中文字符，不能全是空格")
    private String realName;

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^3\\d{7,13}$", message = "学号必须为3开头的8到14位纯数字")
    private String studentId;

    @Size(max = 100, message = "学院名称长度不能超过 100 位")
    private String college;

    @NotBlank(message = "专业不能为空")
    @Size(max = 100, message = "专业名称长度不能超过 100 位")
    private String major;

    @NotBlank(message = "年级不能为空")
    @Size(max = 20, message = "年级长度不能超过 20 位")
    private String grade;

    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为 6 位数字")
    private String emailCode;

    private String role = "student";
}
