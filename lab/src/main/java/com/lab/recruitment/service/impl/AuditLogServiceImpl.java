package com.lab.recruitment.service.impl;

import com.lab.recruitment.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void record(Long actorUserId, String action, String targetType, Long targetId, String detail) {
        if (!StringUtils.hasText(action)) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO t_audit_log (actor_user_id, action, target_type, target_id, detail, deleted) VALUES (?, ?, ?, ?, ?, 0)",
                    actorUserId,
                    action.trim(),
                    StringUtils.hasText(targetType) ? targetType.trim() : null,
                    targetId,
                    StringUtils.hasText(detail) ? detail.trim() : null
            );
        } catch (Exception ignored) {
        }
    }
}
