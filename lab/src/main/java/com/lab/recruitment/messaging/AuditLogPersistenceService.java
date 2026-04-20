package com.lab.recruitment.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogPersistenceService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void persist(AuditLogMessage message) {
        if (message == null || !StringUtils.hasText(message.getAction())) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO t_audit_log (" +
                        "actor_user_id, operator_role, college_id, lab_id, action, target_type, target_id, detail, " +
                        "request_path, request_method, request_ip, result, detail_json, deleted" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                message.getActorUserId(),
                trimToNull(message.getOperatorRole()),
                message.getCollegeId(),
                message.getLabId(),
                trimToNull(message.getAction()),
                trimToNull(message.getTargetType()),
                message.getTargetId(),
                trimToNull(message.getDetail()),
                trimToNull(message.getRequestPath()),
                trimToNull(message.getRequestMethod()),
                trimToNull(message.getRequestIp()),
                defaultValue(message.getResult(), "SUCCESS"),
                trimToNull(message.getDetailJson())
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }
}
