package com.lab.recruitment.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabAttendanceMemberVO {

    private Long userId;
    private String realName;
    private String studentId;
    private String major;
    private String attendanceDate;
    private Integer status;
    private String reason;
    private LocalDateTime confirmTime;
}
