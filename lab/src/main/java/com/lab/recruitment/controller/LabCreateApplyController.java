package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabCreateApplyAuditDTO;
import com.lab.recruitment.dto.LabCreateApplyCreateDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.LabCreateApplyService;
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
@RequestMapping("/lab-create-applies")
public class LabCreateApplyController {

    @Autowired
    private LabCreateApplyService labCreateApplyService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Object> createApply(@Validated @RequestBody LabCreateApplyCreateDTO createDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return labCreateApplyService.createApply(createDTO, currentUser)
                    ? Result.success("Create application submitted successfully", null)
                    : Result.error("Failed to submit create application");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Map<String, Object>>> getApplyPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(labCreateApplyService.getApplyPage(pageNum, pageSize, status, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> auditApply(@PathVariable Long id, @Validated @RequestBody LabCreateApplyAuditDTO auditDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return labCreateApplyService.auditApply(id, auditDTO, currentUser)
                    ? Result.success("Create application audited successfully", null)
                    : Result.error("Failed to audit create application");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
