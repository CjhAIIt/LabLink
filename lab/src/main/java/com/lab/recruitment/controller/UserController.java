package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.UserRegisterDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.utils.JwtUtils;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.LoginVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody User user) {
        try {
            LoginVO loginVO = userService.login(user.getUsername(), user.getPassword());
            return Result.success(loginVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        try {
            // 将User转换为UserRegisterDTO
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
            if (success) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页获取用户列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String role) {
        try {
            Page<User> userPage = userService.getUserPage(pageNum, pageSize, realName, studentId, major, role);
            // 不返回密码
            userPage.getRecords().forEach(user -> user.setPassword(null));
            return Result.success(userPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            User user = userService.findByUsername(username);
            
            // 不返回密码
            user.setPassword(null);
            
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.success("登出成功");
    }

    /**
     * 更新用户头像
     */
    @PutMapping("/avatar")
    public Result updateAvatar(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            User user = userService.findByUsername(username);
            
            String avatar = request.get("avatar");
            // 简单的校验：确保avatar不为空
            if (avatar != null && !avatar.isEmpty()) {
                user.setAvatar(avatar);
                boolean success = userService.updateById(user);
                if (success) {
                    return Result.success("头像更新成功");
                } else {
                    return Result.error("头像更新失败");
                }
            } else {
                return Result.error("头像地址不能为空");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户个人信息
     */
    @PutMapping("/info")
    public Result updateUserInfo(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            User user = userService.findByUsername(username);
            
            // 更新字段
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
            if (success) {
                return Result.success("个人信息更新成功");
            } else {
                return Result.error("个人信息更新失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result changePassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            User user = userService.findByUsername(username);
            
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return Result.error("原密码不正确");
            }
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            boolean success = userService.updateById(user);
            
            if (success) {
                return Result.success("密码修改成功");
            } else {
                return Result.error("密码修改失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有管理员（仅总负责人可访问）
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<User>> getAllAdmins() {
        try {
            List<User> adminList = userService.selectAllAdmins();
            // 不返回密码
            adminList.forEach(admin -> admin.setPassword(null));
            return Result.success(adminList);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加管理员（仅总负责人可访问）
     */
    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result addAdmin(@RequestBody User admin) {
        try {
            boolean success = userService.addAdmin(admin);
            if (success) {
                return Result.success("添加管理员成功");
            } else {
                return Result.error("添加管理员失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新管理员信息（仅总负责人可访问）
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result updateAdmin(@PathVariable Long id, @RequestBody User admin) {
        try {
            admin.setId(id);
            boolean success = userService.updateAdmin(admin);
            if (success) {
                return Result.success("更新管理员成功");
            } else {
                return Result.error("更新管理员失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除管理员（仅总负责人可访问）
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result deleteAdmin(@PathVariable Long id) {
        try {
            boolean success = userService.deleteAdmin(id);
            if (success) {
                return Result.success("删除管理员成功");
            } else {
                return Result.error("删除管理员失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取管理员详情（仅总负责人可访问）
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<User> getAdminDetail(@PathVariable Long id) {
        try {
            User admin = userService.selectAdminById(id);
            if (admin != null) {
                // 不返回密码
                admin.setPassword(null);
                return Result.success(admin);
            } else {
                return Result.error("管理员不存在");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取学生列表（仅总负责人可访问）
     */
    @GetMapping("/student/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<User>> getStudentList() {
        try {
            List<User> studentList = userService.getUsersByRole("student");
            // 不返回密码
            studentList.forEach(student -> student.setPassword(null));
            return Result.success(studentList);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
