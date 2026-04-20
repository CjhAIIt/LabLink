package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabInfoChangeReviewActionDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.LabInfoChangeReviewService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lab-info-reviews")
public class LabInfoChangeReviewController {

    @Autowired
    private LabInfoChangeReviewService labInfoChangeReviewService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getPendingReviewPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                                  @RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) Long collegeId,
                                                                  @RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(labInfoChangeReviewService.getPendingReviewPage(
                    pageNum, pageSize, keyword, collegeId, labId, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<Map<String, Object>>> getReviewHistory(@RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(labInfoChangeReviewService.getReviewHistory(labId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> approveReview(@PathVariable Long reviewId,
                                                     @Validated @RequestBody LabInfoChangeReviewActionDTO actionDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(labInfoChangeReviewService.approveReview(
                    reviewId, actionDTO.getReviewComment(), currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> rejectReview(@PathVariable Long reviewId,
                                                    @Validated @RequestBody LabInfoChangeReviewActionDTO actionDTO) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(labInfoChangeReviewService.rejectReview(
                    reviewId, actionDTO.getReviewComment(), currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
