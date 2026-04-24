package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatisticsService {

    Map<String, Object> getOverview(User currentUser);

    Map<String, Object> getLabStatistics(Long labId, User currentUser);

    Map<String, Object> getDashboard(User currentUser, LocalDate startDate, LocalDate endDate,
                                     Long collegeId, Long labId);

    byte[] exportDashboardExcel(User currentUser, LocalDate startDate, LocalDate endDate,
                                Long collegeId, Long labId);

    List<Map<String, Object>> getLabDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                              Long collegeId, Long labId);

    List<Map<String, Object>> getMemberDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                                 Long collegeId, Long labId);

    Map<String, Object> getAttendanceDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                               Long collegeId, Long labId);

    Map<String, Object> getDeviceDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                           Long collegeId, Long labId);

    Map<String, Object> getProfileDimension(User currentUser, LocalDate startDate, LocalDate endDate,
                                            Long collegeId, Long labId);
}
