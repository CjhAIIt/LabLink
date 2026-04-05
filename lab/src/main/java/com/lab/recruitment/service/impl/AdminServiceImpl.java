package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AdminMapper;
import com.lab.recruitment.service.AdminService;
import com.lab.recruitment.utils.JwtUtils;
import com.lab.recruitment.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, User> implements AdminService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public LoginVO login(String username, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("deleted", 0);
        queryWrapper.in("role", "admin", "super_admin");
        User admin = this.getOne(queryWrapper);

        if (admin == null) {
            throw new RuntimeException("Admin account not found or disabled");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }

        String token = jwtUtils.generateToken(username, admin.getRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setId(admin.getId());
        loginVO.setToken(token);
        loginVO.setUsername(admin.getUsername());
        loginVO.setRealName(admin.getRealName());
        loginVO.setRole(admin.getRole());
        loginVO.setLabId(admin.getLabId());
        loginVO.setAvatar(admin.getAvatar());
        return loginVO;
    }
}
