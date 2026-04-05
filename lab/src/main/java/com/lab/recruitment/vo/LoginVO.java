package com.lab.recruitment.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LoginVO {
    
    private Long id;
    private String token;
    private String username;
    private String realName;
    private String role;
    private Long labId;
    private String avatar;
    private String primaryIdentity;
    private String labMemberRole;
    private Long managedCollegeId;
    private Boolean schoolDirector;
    private Boolean collegeManager;
    private Boolean labManager;
    private List<String> platformPostCodes = new ArrayList<>();
}
