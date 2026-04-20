package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.vo.AttendanceStatsVO;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceStatsService {

    AttendanceStatsVO getStats(Long labId, Long userId, LocalDate startDate, LocalDate endDate, User currentUser);

    List<AttendanceStatsVO> getBatchStats(Long labId, LocalDate startDate, LocalDate endDate, User currentUser);

    byte[] exportStats(Long labId, LocalDate startDate, LocalDate endDate, User currentUser);
}
