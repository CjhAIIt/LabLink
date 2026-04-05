package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.TeacherRegisterApplyAuditDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.TeacherRegisterApplyService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/teacher-register-applies")
public class TeacherRegisterApplyController {

    @Autowired
    private TeacherRegisterApplyService teacherRegisterApplyService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getApplyPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(teacherRegisterApplyService.getApplyPage(pageNum, pageSize, status, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> auditApply(@PathVariable Long id, @Validated @RequestBody TeacherRegisterApplyAuditDTO auditDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return teacherRegisterApplyService.auditApply(id, auditDTO, currentUser)
                    ? Result.success("教师注册申请处理完成", null)
                    : Result.error("教师注册申请处理失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
