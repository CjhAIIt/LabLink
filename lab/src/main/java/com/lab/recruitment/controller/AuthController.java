package com.lab.recruitment.controller;

import com.lab.recruitment.dto.LoginDTO;
import com.lab.recruitment.dto.PasswordResetDTO;
import com.lab.recruitment.dto.PasswordResetSendCodeDTO;
import com.lab.recruitment.dto.RegisterEmailCodeDTO;
import com.lab.recruitment.dto.TeacherRegisterDTO;
import com.lab.recruitment.dto.TeacherRegisterEmailCodeDTO;
import com.lab.recruitment.dto.UserRegisterDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AuthContextService;
import com.lab.recruitment.service.TeacherRegisterApplyService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.AuthMenuVO;
import com.lab.recruitment.vo.LoginVO;
import com.lab.recruitment.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeacherRegisterApplyService teacherRegisterApplyService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private AuthContextService authContextService;

    @PostMapping("/register")
    public Result<Object> register(@Validated @RequestBody UserRegisterDTO registerDTO) {
        try {
            boolean success = userService.register(registerDTO);
            return success ? Result.success("注册成功") : Result.error("注册失败");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLIntegrityConstraintViolationException) {
                return Result.error(mapDuplicateMessage(cause.getMessage()));
            }
            return Result.error(mapDuplicateMessage(e.getMessage()));
        }
    }

    @PostMapping("/register/send-code")
    public Result<Object> sendRegisterCode(@Validated @RequestBody RegisterEmailCodeDTO registerEmailCodeDTO) {
        try {
            userService.sendRegisterEmailCode(registerEmailCodeDTO);
            return Result.success("验证码已发送，请查收邮箱", null);
        } catch (Exception e) {
            return Result.error(mapDuplicateMessage(e.getMessage()));
        }
    }

    @PostMapping("/teacher-register/send-code")
    public Result<Object> sendTeacherRegisterCode(@Validated @RequestBody TeacherRegisterEmailCodeDTO registerEmailCodeDTO) {
        try {
            teacherRegisterApplyService.sendRegisterEmailCode(registerEmailCodeDTO);
            return Result.success("验证码已发送，请查收邮箱", null);
        } catch (Exception e) {
            return Result.error(mapDuplicateMessage(e.getMessage()));
        }
    }

    @PostMapping("/teacher-register")
    public Result<Object> teacherRegister(@Validated @RequestBody TeacherRegisterDTO registerDTO) {
        try {
            boolean success = teacherRegisterApplyService.submitRegisterApply(registerDTO);
            return success
                    ? Result.success("教师注册申请已提交，请等待学院与学校审批", null)
                    : Result.error("教师注册申请提交失败");
        } catch (Exception e) {
            return Result.error(mapDuplicateMessage(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            LoginVO loginVO = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
            return Result.success(loginVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/me")
    public Result<UserProfileVO> getCurrentUserContext() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(authContextService.buildContext(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/menus")
    public Result<List<AuthMenuVO>> getCurrentMenus() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(authContextService.buildMenus(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/permissions")
    public Result<List<String>> getCurrentPermissions() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(authContextService.buildPermissions(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/password-reset/send-code")
    public Result<Object> sendPasswordResetCode(@Validated @RequestBody PasswordResetSendCodeDTO passwordResetSendCodeDTO) {
        try {
            userService.sendPasswordResetCode(passwordResetSendCodeDTO);
            return Result.success("验证码已发送，请查收邮箱", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/password-reset/confirm")
    public Result<Object> resetPassword(@Validated @RequestBody PasswordResetDTO passwordResetDTO) {
        try {
            userService.resetPassword(passwordResetDTO);
            return Result.success("密码已重置，请使用新密码登录", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private String mapDuplicateMessage(String message) {
        if (message == null) {
            return "注册失败";
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("student_id") || normalized.contains("uk_user_student_id")
                || normalized.contains("username")) {
            return "账号已被注册";
        }
        if (normalized.contains("email")) {
            return "邮箱已被注册";
        }
        if (normalized.contains("phone")) {
            return "手机号已被注册";
        }
        return message;
    }
}
