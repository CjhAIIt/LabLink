package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface AttendanceSessionService {

    Map<String, Object> createSession(Long labId, User currentUser);

    Map<String, Object> getActiveSession(Long labId, User currentUser, boolean includeSensitiveData);

    Map<String, Object> cancelSession(Long sessionId, User currentUser);

    Map<String, Object> finalizeSession(Long sessionId, User currentUser);

    Map<String, Object> refreshSessionStatus(Long sessionId, User currentUser);

    Map<String, Object> expireAndFinalizeSession(Long sessionId, Long operatorId);

    Map<String, Object> signByCode(Long userId, String signCode, User currentUser);

    Map<String, Object> signBySession(Long userId, Long sessionId, User currentUser);

    Map<String, Object> listSessionRecords(Long sessionId, User currentUser);

    Map<String, Object> finalizeAttendanceResult(Long sessionId, Long operatorId);

    Map<String, Object> tagAbsentMember(Long attendanceId, String tagType, String reason, User currentUser);

    Map<String, Object> getAttendanceStat(Long labId, String date, User currentUser);

    Map<String, Object> getAttendanceList(Long labId, String date, User currentUser);

    byte[] exportAttendanceExcel(Long labId, String startDate, String endDate, User currentUser);
}
