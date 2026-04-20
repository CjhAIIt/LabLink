package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.UserRegisterDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody User user) {
        try {
            LoginVO loginVO = userService.login(user.getUsername(), user.getPassword());
            return Result.success(loginVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Object> register(@RequestBody User user) {
        try {
            UserRegisterDTO registerDTO = new UserRegisterDTO();
            registerDTO.setUsername(user.getStudentId() != null ? user.getStudentId() : user.getUsername());
            registerDTO.setPassword(user.getPassword());
            registerDTO.setRealName(user.getRealName());
            registerDTO.setStudentId(user.getStudentId());
            registerDTO.setCollege(user.getCollege());
            registerDTO.setMajor(user.getMajor());
            registerDTO.setGrade(user.getGrade());
            registerDTO.setPhone(user.getPhone());
            registerDTO.setEmail(user.getEmail());

            boolean success = userService.register(registerDTO);
            return success ? Result.success("Register success", null) : Result.error("Register failed");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<User>> getUserList(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String realName,
                                          @RequestParam(required = false) String studentId,
                                          @RequestParam(required = false) String major,
                                          @RequestParam(required = false) Long collegeId,
                                          @RequestParam(required = false) Long labId,
                                          @RequestParam(required = false) String role) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            if (!StringUtils.hasText(role) || "student".equalsIgnoreCase(role)) {
                DataScope scope = currentUserAccessor.resolveManagementScope(currentUser, collegeId, labId);
                Page<User> userPage = userService.getStudentPageForAdmin(
                        pageNum, pageSize, keyword, realName, studentId, major, scope.getCollegeId(), scope.getLabId()
                );
                userPage.getRecords().forEach(user -> user.setPassword(null));
                return Result.success(userPage);
            }

            currentUserAccessor.assertSuperAdmin(currentUser);
            Page<User> userPage = userService.getUserPage(pageNum, pageSize, realName, studentId, major, role);
            userPage.getRecords().forEach(user -> user.setPassword(null));
            return Result.success(userPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        try {
            User user = currentUserAccessor.getCurrentUser();
            user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Object> logout() {
        return Result.success("Logout success", null);
    }

    @PutMapping("/avatar")
    public Result<Object> updateAvatar(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            User user = currentUserAccessor.getCurrentUser();
            String avatar = request.get("avatar");
            if (!StringUtils.hasText(avatar)) {
                return Result.error("Avatar is required");
            }
            user.setAvatar(avatar);
            boolean success = userService.updateById(user);
            return success ? Result.success("Avatar updated", null) : Result.error("Failed to update avatar");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/info")
    public Result<Object> updateUserInfo(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            User user = currentUserAccessor.getCurrentUser();
            if (request.containsKey("realName")) {
                user.setRealName((String) request.get("realName"));
            }
            if (request.containsKey("major")) {
                user.setMajor((String) request.get("major"));
            }
            if (request.containsKey("email")) {
                user.setEmail((String) request.get("email"));
            }
            if (request.containsKey("resume")) {
                user.setResume((String) request.get("resume"));
            }
            if (request.containsKey("avatar")) {
                user.setAvatar((String) request.get("avatar"));
            }

            boolean success = userService.updateById(user);
            return success ? Result.success("User info updated", null) : Result.error("Failed to update user info");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/password")
    public Result<Object> changePassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            User user = currentUserAccessor.getCurrentUser();
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return Result.error("Old password is incorrect");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            boolean success = userService.updateById(user);
            return success ? Result.success("Password updated", null) : Result.error("Failed to update password");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<User>> getAllAdmins() {
        try {
            List<User> adminList = userService.selectAllAdmins();
            adminList.forEach(admin -> admin.setPassword(null));
            return Result.success(adminList);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> addAdmin(@RequestBody User admin) {
        try {
            boolean success = userService.addAdmin(admin);
            return success ? Result.success("Admin added", null) : Result.error("Failed to add admin");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> updateAdmin(@PathVariable Long id, @RequestBody User admin) {
        try {
            admin.setId(id);
            boolean success = userService.updateAdmin(admin);
            return success ? Result.success("Admin updated", null) : Result.error("Failed to update admin");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> deleteAdmin(@PathVariable Long id) {
        try {
            boolean success = userService.deleteAdmin(id);
            return success ? Result.success("Admin deleted", null) : Result.error("Failed to delete admin");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<User> getAdminDetail(@PathVariable Long id) {
        try {
            User admin = userService.selectAdminById(id);
            if (admin == null) {
                return Result.error("Admin not found");
            }
            admin.setPassword(null);
            return Result.success(admin);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/student/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<User>> getStudentList() {
        try {
            List<User> studentList = userService.getUsersByRole("student");
            studentList.forEach(student -> student.setPassword(null));
            return Result.success(studentList);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
