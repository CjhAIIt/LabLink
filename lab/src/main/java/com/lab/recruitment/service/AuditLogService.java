package com.lab.recruitment.service;

public interface AuditLogService {
    void record(Long actorUserId, String action, String targetType, Long targetId, String detail);
}
