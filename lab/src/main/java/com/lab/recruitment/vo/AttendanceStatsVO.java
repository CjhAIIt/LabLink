package com.lab.recruitment.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceStatsVO {

    private Long userId;
    private String realName;
    private String studentId;
    private int totalDays;
    private int signedDays;
    private int lateDays;
    private int absentDays;
    private int leaveDays;
    private double attendanceRate;
    private List<DailyRecord> dailyRecords;

    @Data
    public static class DailyRecord {
        private String date;
        private String status;
        private String signTime;
    }
}
