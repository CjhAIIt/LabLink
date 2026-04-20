package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AdminManagementService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-management")
public class AdminManagementController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> assignAdminToLab(@RequestBody Map<String, Long> request) {
        Long labId = request.get("labId");
        Long userId = request.get("userId");

        if (labId == null || userId == null) {
            return Result.error("Lab id and user id are required");
        }

        User currentUser = currentUserAccessor.getCurrentUser();
        currentUserAccessor.assertLabScope(currentUser, labId);
        return adminManagementService.assignAdminToLab(labId, userId, currentUser);
    }

    @DeleteMapping("/remove/{labId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> removeAdminFromLab(@PathVariable Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        if (!currentUserAccessor.isSuperAdmin(currentUser) && !currentUserAccessor.isCollegeManager(currentUser)) {
            return Result.error("仅学校管理员或学院管理员可以撤销实验室管理员");
        }
        currentUserAccessor.assertLabScope(currentUser, labId);
        return adminManagementService.removeAdminFromLab(labId, currentUser);
    }

    @GetMapping("/lab/{labId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<User> getLabAdmin(@PathVariable Long labId) {
        return adminManagementService.getLabAdmin(labId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<User>> getAllAdminsWithLabs() {
        return adminManagementService.getAllAdminsWithLabs();
    }

    @GetMapping("/can-be-admin/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Boolean> canUserBeAdmin(@PathVariable Long userId) {
        return adminManagementService.canUserBeAdmin(userId);
    }
}
