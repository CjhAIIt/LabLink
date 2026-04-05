package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.UserAccessProfile;
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
    private UserAccessService userAccessService;

    @GetMapping("/profile")
    public Result<UserProfileVO> getCurrentProfile() {
        try {
            User user = currentUserAccessor.getCurrentUser();
            return Result.success(buildProfile(user));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private UserProfileVO buildProfile(User user) {
        UserAccessProfile profile = userAccessService.buildProfile(user);
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setId(user.getId());
        profileVO.setUsername(user.getUsername());
        profileVO.setRealName(user.getRealName());
        profileVO.setRole(profile.getDisplayRole());
        profileVO.setStudentId(user.getStudentId());
        profileVO.setCollege(user.getCollege());
        profileVO.setMajor(user.getMajor());
        profileVO.setGrade(user.getGrade());
        profileVO.setPhone(user.getPhone());
        profileVO.setEmail(user.getEmail());
        profileVO.setAvatar(user.getAvatar());
        profileVO.setResume(user.getResume());
        profileVO.setLabId(profile.getManagedLabId() != null ? profile.getManagedLabId() : user.getLabId());
        profileVO.setCanEdit(user.getCanEdit());
        profileVO.setStatus(user.getStatus());
        profileVO.setPrimaryIdentity(profile.getPrimaryIdentity());
        profileVO.setLabMemberRole(profile.getLabMemberRole());
        profileVO.setManagedCollegeId(profile.getManagedCollegeId());
        profileVO.setSchoolDirector(profile.isSchoolDirector());
        profileVO.setCollegeManager(profile.isCollegeManager());
        profileVO.setLabManager(profile.isLabManager());
        profileVO.setPlatformPostCodes(profile.getPlatformPostCodes());
        return profileVO;
    }
}
