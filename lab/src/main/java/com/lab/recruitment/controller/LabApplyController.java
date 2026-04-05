package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabApplyAuditDTO;
import com.lab.recruitment.dto.LabApplyCreateDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.LabApplyService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lab-applies")
public class LabApplyController {

    @Autowired
    private LabApplyService labApplyService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> createApply(@Validated @RequestBody LabApplyCreateDTO createDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return labApplyService.createApply(createDTO, currentUser) ? Result.success("申请提交成功", null) : Result.error("申请提交失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getApplyPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) Long labId,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(labApplyService.getApplyPage(pageNum, pageSize, labId, status, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<Map<String, Object>>> getMyApplyPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String status) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(labApplyService.getMyApplyPage(pageNum, pageSize, status, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> auditApply(@PathVariable Long id, @Validated @RequestBody LabApplyAuditDTO auditDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return labApplyService.auditApply(id, auditDTO, currentUser) ? Result.success("申请审核完成", null) : Result.error("申请审核失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<Map<String, Object>>> getLatestApplies(@RequestParam(required = false) Long labId,
                                                              @RequestParam(defaultValue = "5") Integer limit) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            Long scopedLabId = currentUserAccessor.isSuperAdmin(currentUser)
                    ? labId
                    : currentUserAccessor.resolveLabScope(currentUser, labId);
            return Result.success(labApplyService.getLatestApplies(scopedLabId, limit));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
