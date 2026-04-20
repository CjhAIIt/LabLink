package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.StudentProfileSaveDTO;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface StudentProfileService {

    Map<String, Object> getMyProfile(User currentUser);

    Map<String, Object> saveMyProfile(StudentProfileSaveDTO saveDTO, User currentUser);

    Map<String, Object> submitMyProfile(User currentUser);

    Page<Map<String, Object>> getProfilePage(Integer pageNum, Integer pageSize, String keyword, String status,
                                             Long collegeId, Long labId, User currentUser);

    Map<String, Object> getProfileDetail(Long profileId, User currentUser);

    List<Map<String, Object>> getProfileReviewHistory(Long profileId, User currentUser);

    List<Map<String, Object>> getProfileArchiveHistory(Long profileId, User currentUser);

    Page<Map<String, Object>> getPendingReviewPage(Integer pageNum, Integer pageSize, String keyword,
                                                   Long collegeId, Long labId, User currentUser);

    Map<String, Object> approveReview(Long reviewId, String reviewComment, User currentUser);

    Map<String, Object> rejectReview(Long reviewId, String reviewComment, User currentUser);
}
