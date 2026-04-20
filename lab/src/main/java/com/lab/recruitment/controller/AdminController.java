package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AdminService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public Result<Page<User>> getUserList(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String realName,
                                          @RequestParam(required = false) String studentId,
                                          @RequestParam(required = false) String major,
                                          @RequestParam(required = false) Long collegeId,
                                          @RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            DataScope scope = currentUserAccessor.resolveManagementScope(currentUser, collegeId, labId);
            Page<User> userPage = userService.getStudentPageForAdmin(
                    pageNum, pageSize, keyword, realName, studentId, major, scope.getCollegeId(), scope.getLabId()
            );
            return Result.success(userPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Page<User>> getAdminList(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String realName,
                                           @RequestParam(required = false) String username) {
        try {
            Page<User> adminPage = userService.getUserPage(pageNum, pageSize, realName, null, null, "ADMIN");
            return Result.success(adminPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> deleteUser(@PathVariable Long id) {
        try {
            boolean success = userService.removeById(id);
            return success ? Result.success("User deleted", null) : Result.error("Failed to delete user");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
