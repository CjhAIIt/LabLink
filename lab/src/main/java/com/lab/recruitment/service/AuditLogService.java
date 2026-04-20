package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.User;

import java.time.LocalDateTime;
import java.util.Map;

public interface AuditLogService {

    void record(Long actorUserId, String action, String targetType, Long targetId, String detail);

    Page<Map<String, Object>> getAuditLogPage(Integer pageNum, Integer pageSize, String keyword, String action,
                                              String targetType, Long collegeId, Long labId,
                                              LocalDateTime startTime, LocalDateTime endTime, User currentUser);
}
