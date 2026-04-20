package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface LabInfoChangeReviewService {

    Map<String, Object> submitChange(Lab requestedLab, User currentUser);

    Page<Map<String, Object>> getPendingReviewPage(Integer pageNum, Integer pageSize, String keyword,
                                                   Long collegeId, Long labId, User currentUser);

    List<Map<String, Object>> getReviewHistory(Long labId, User currentUser);

    Map<String, Object> approveReview(Long reviewId, String reviewComment, User currentUser);

    Map<String, Object> rejectReview(Long reviewId, String reviewComment, User currentUser);
}
