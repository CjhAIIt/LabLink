package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.StudentProfileSaveDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.StudentProfileService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/profiles")
public class StudentProfileController {

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getMyProfile() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.getMyProfile(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> saveMyProfile(@Validated @RequestBody StudentProfileSaveDTO saveDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.saveMyProfile(saveDTO, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/me/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> submitMyProfile() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.submitMyProfile(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Page<Map<String, Object>>> getProfilePage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) Long collegeId,
                                                            @RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.getProfilePage(
                    pageNum, pageSize, keyword, status, collegeId, labId, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{profileId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getProfileDetail(@PathVariable Long profileId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.getProfileDetail(profileId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{profileId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getProfileReviewHistory(@PathVariable Long profileId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.getProfileReviewHistory(profileId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{profileId}/archives")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getProfileArchiveHistory(@PathVariable Long profileId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(studentProfileService.getProfileArchiveHistory(profileId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
