package com.lab.recruitment.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class TeacherRegisterDTO {

    @NotBlank(message = "工号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$", message = "工号需为 3 到 32 位字母、数字、下划线或中划线")
    private String teacherNo;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6 到 20 位之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过 50 位")
    @Pattern(regexp = "^[\\p{IsHan}·]{2,50}$", message = "真实姓名必须为中文")
    private String realName;

    @NotNull(message = "所属学院不能为空")
    private Long collegeId;

    @Size(max = 50, message = "职称长度不能超过 50 位")
    private String title;

    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "邮箱验证码必须为 6 位数字")
    private String emailCode;

    @NotBlank(message = "申请说明不能为空")
    @Size(max = 500, message = "申请说明长度不能超过 500 位")
    private String applyReason;
}
