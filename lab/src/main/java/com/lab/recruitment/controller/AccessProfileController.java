package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AuthContextService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/access")
public class AccessProfileController {

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private AuthContextService authContextService;

    @GetMapping("/profile")
    public Result<UserProfileVO> getCurrentProfile() {
        try {
            User user = currentUserAccessor.getCurrentUser();
            return Result.success(authContextService.buildContext(user));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
