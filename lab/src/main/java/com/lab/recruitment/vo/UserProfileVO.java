package com.lab.recruitment.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String studentId;
    private String college;
    private String major;
    private String grade;
    private String phone;
    private String email;
    private String avatar;
    private String resume;
    private Long labId;
    private Long managedLabId;
    private String labName;
    private Integer canEdit;
    private Integer status;
    private String primaryIdentity;
    private String labMemberRole;
    private Long managedCollegeId;
    private Boolean schoolDirector;
    private Boolean collegeManager;
    private Boolean labManager;
    private List<String> platformPostCodes = new ArrayList<>();
    private String portalRole;
    private List<String> authorities = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
}
