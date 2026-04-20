package com.lab.recruitment.dto;

import lombok.Data;

@Data
public class StudentProfileSaveDTO {

    private String studentNo;
    private String realName;
    private String gender;
    private Long collegeId;
    private String major;
    private String className;
    private String phone;
    private String email;
    private String direction;
    private String introduction;
}
