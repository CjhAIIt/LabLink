package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.messaging.AuditLogMessage;
import com.lab.recruitment.messaging.PlatformEventPublisher;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private PlatformEventPublisher platformEventPublisher;

    @Override
    public void record(Long actorUserId, String action, String targetType, Long targetId, String detail) {
        if (!StringUtils.hasText(action)) {
            return;
        }

        try {
            User currentUser = null;
            DataScope dataScope = null;
            try {
                currentUser = currentUserAccessor.getCurrentUser();
                dataScope = currentUserAccessor.getCurrentDataScope();
            } catch (Exception ignored) {
            }

            HttpServletRequest request = currentRequest();
            Long operatorId = actorUserId != null ? actorUserId : (currentUser == null ? null : currentUser.getId());
            String operatorRole = dataScope != null && StringUtils.hasText(dataScope.getDisplayRole())
                    ? dataScope.getDisplayRole()
                    : (currentUser == null ? null : currentUser.getRole());
            Long collegeId = dataScope == null ? null : dataScope.getCollegeId();
            Long labId = dataScope == null ? null : dataScope.getLabId();
            String requestPath = request == null ? null : request.getRequestURI();
            String requestMethod = request == null ? null : request.getMethod();
            String requestIp = request == null ? null : extractClientIp(request);

            AuditLogMessage message = new AuditLogMessage();
            message.setActorUserId(operatorId);
            message.setOperatorRole(trimToNull(operatorRole));
            message.setCollegeId(collegeId);
            message.setLabId(labId);
            message.setAction(action.trim());
            message.setTargetType(trimToNull(targetType));
            message.setTargetId(targetId);
            message.setDetail(trimToNull(detail));
            message.setRequestPath(trimToNull(requestPath));
            message.setRequestMethod(trimToNull(requestMethod));
            message.setRequestIp(trimToNull(requestIp));
            message.setResult("SUCCESS");
            message.setDetailJson(trimToNull(detail));
            message.setCreatedAt(LocalDateTime.now());
            platformEventPublisher.publishAuditLog(message);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Page<Map<String, Object>> getAuditLogPage(Integer pageNum, Integer pageSize, String keyword, String action,
                                                     String targetType, Long collegeId, Long labId,
                                                     LocalDateTime startTime, LocalDateTime endTime, User currentUser) {
        DataScope scope = currentUserAccessor.resolveManagementScope(currentUser, collegeId, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE a.deleted = 0");
        if (scope.getCollegeId() != null) {
            where.append(" AND a.college_id = ?");
            args.add(scope.getCollegeId());
        }
        if (scope.getLabId() != null) {
            where.append(" AND a.lab_id = ?");
            args.add(scope.getLabId());
        }
        if (StringUtils.hasText(action)) {
            where.append(" AND a.action = ?");
            args.add(action.trim());
        }
        if (StringUtils.hasText(targetType)) {
            where.append(" AND a.target_type = ?");
            args.add(targetType.trim());
        }
        if (startTime != null) {
            where.append(" AND a.create_time >= ?");
            args.add(startTime);
        }
        if (endTime != null) {
            where.append(" AND a.create_time <= ?");
            args.add(endTime);
        }
        if (StringUtils.hasText(keyword)) {
            String likeValue = "%" + keyword.trim() + "%";
            where.append(" AND (u.username LIKE ? OR u.real_name LIKE ? OR a.action LIKE ? OR a.target_type LIKE ? OR a.detail LIKE ? OR a.request_path LIKE ?)");
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_audit_log a " +
                        "LEFT JOIN t_user u ON u.id = a.actor_user_id AND u.deleted = 0" +
                        where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT a.id, a.actor_user_id AS actorUserId, u.username AS actorUsername, u.real_name AS actorRealName, " +
                        "a.operator_role AS operatorRole, a.college_id AS collegeId, a.lab_id AS labId, " +
                        "a.action, a.target_type AS targetType, a.target_id AS targetId, a.detail, " +
                        "a.request_path AS requestPath, a.request_method AS requestMethod, a.request_ip AS requestIp, " +
                        "a.result, a.create_time AS createTime " +
                        "FROM t_audit_log a " +
                        "LEFT JOIN t_user u ON u.id = a.actor_user_id AND u.deleted = 0" +
                        where +
                        " ORDER BY a.create_time DESC, a.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
