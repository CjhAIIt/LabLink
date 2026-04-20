package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.LabMemberService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lab-members")
public class LabMemberController {

    @Autowired
    private LabMemberService labMemberService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Map<String, Object>>> getMemberPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) Long labId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String memberRole,
                                                           @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(labMemberService.getMemberPage(pageNum, pageSize, labId, status, memberRole, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getActiveMembers(@RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(labMemberService.getActiveMembers(labId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/appoint-leader")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> appointLeader(@PathVariable Long id) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return labMemberService.appointLeader(id, currentUser) ? Result.success("负责人任命成功", null) : Result.error("负责人任命失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/remove")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> removeMember(@PathVariable Long id, @RequestParam(required = false) String remark) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return labMemberService.removeMember(id, remark, currentUser) ? Result.success("成员移出成功", null) : Result.error("成员移出失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
