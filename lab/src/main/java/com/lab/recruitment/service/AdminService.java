package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.vo.LoginVO;

public interface AdminService extends IService<User> {
    
    LoginVO login(String username, String password);
}