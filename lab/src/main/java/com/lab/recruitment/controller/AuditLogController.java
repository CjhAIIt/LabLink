package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/audit")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getAuditLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String action,
                                                          @RequestParam(required = false) String targetType,
                                                          @RequestParam(required = false) Long collegeId,
                                                          @RequestParam(required = false) Long labId,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                                  LocalDateTime startTime,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                                  LocalDateTime endTime) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.apiSuccess(auditLogService.getAuditLogPage(
                    pageNum, pageSize, keyword, action, targetType, collegeId, labId, startTime, endTime, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
