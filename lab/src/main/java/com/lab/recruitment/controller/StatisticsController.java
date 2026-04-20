package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.StatisticsService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
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

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(statisticsService.getDashboard(currentUser, startDate, endDate, collegeId, labId));
    }

    @GetMapping("/labs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> getLabs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(statisticsService.getLabDimension(currentUser, startDate, endDate, collegeId, labId));
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> getMembers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(statisticsService.getMemberDimension(currentUser, startDate, endDate, collegeId, labId));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(statisticsService.getAttendanceDimension(currentUser, startDate, endDate, collegeId, labId));
    }

    @GetMapping("/devices")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getDevices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(statisticsService.getDeviceDimension(currentUser, startDate, endDate, collegeId, labId));
    }

    @GetMapping("/profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getProfiles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(statisticsService.getProfileDimension(currentUser, startDate, endDate, collegeId, labId));
    }
}
