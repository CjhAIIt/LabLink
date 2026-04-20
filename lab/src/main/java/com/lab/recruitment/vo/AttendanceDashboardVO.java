package com.lab.recruitment.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceDashboardVO {

    private String date;
    private int total;
    private int signed;
    private int late;
    private int absent;
    private int leave;
    private List<MemberStatusVO> members;

    @Data
    public static class MemberStatusVO {
        private Long userId;
        private String realName;
        private String studentId;
        private String status;
        private String signTime;
    }
}
