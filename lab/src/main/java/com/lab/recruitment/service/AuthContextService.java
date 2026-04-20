package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.vo.AuthMenuVO;
import com.lab.recruitment.vo.UserProfileVO;

import java.util.List;

public interface AuthContextService {
    UserProfileVO buildContext(User user);

    List<AuthMenuVO> buildMenus(User user);

    List<String> buildPermissions(User user);
}
