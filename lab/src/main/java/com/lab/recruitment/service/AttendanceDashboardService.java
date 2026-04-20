package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.vo.AttendanceDashboardVO;

public interface AttendanceDashboardService {

    AttendanceDashboardVO getTodayDashboard(Long labId, User currentUser);
}
