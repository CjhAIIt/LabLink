package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.StatisticsService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> getOverview() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(statisticsService.getOverview(currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/lab/{labId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> getLabStatistics(@PathVariable Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(statisticsService.getLabStatistics(labId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
