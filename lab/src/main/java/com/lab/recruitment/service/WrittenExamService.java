package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.WrittenExamConfigDTO;
import com.lab.recruitment.dto.WrittenExamSubmitDTO;
import com.lab.recruitment.entity.SystemNotification;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface WrittenExamService {

    Map<String, Object> getAdminConfig(User admin);

    boolean saveAdminConfig(User admin, WrittenExamConfigDTO configDTO);

    Page<Map<String, Object>> getAdminSubmissionPage(User admin, Integer pageNum, Integer pageSize, Integer status, String realName);

    boolean reviewSubmission(User admin, Long submissionId, Integer status, String adminRemark);

    Page<Map<String, Object>> getStudentLabPage(User user, Integer pageNum, Integer pageSize, String labName, Integer status);

    Map<String, Object> getStudentExam(User user, Long labId);

    Map<String, Object> submitExam(User user, WrittenExamSubmitDTO submitDTO);

    Map<String, Object> getStudentSubmission(User user, Long labId);

    Map<String, Object> completeGradPathExam(User user, Long labId, Map<String, Object> payload);

    List<SystemNotification> getMyNotifications(User user);

    boolean markNotificationRead(User user, Long notificationId);

    boolean canEnterInterview(Long labId, Long userId);

    String getInterviewRequirementMessage(Long labId, Long userId);
}
