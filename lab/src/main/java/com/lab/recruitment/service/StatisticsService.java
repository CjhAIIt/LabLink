package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;

import java.util.Map;

public interface StatisticsService {

    Map<String, Object> getOverview(User currentUser);

    Map<String, Object> getLabStatistics(Long labId, User currentUser);
}
