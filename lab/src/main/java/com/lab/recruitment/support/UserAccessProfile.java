package com.lab.recruitment.support;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAccessProfile {

    private Long userId;
    private String displayRole;
    private String primaryIdentity;
    private Long managedCollegeId;
    private Long managedLabId;
    private String labMemberRole;
    private boolean schoolDirector;
    private boolean collegeManager;
    private boolean labManager;
    private List<String> platformPostCodes = new ArrayList<>();
    private List<String> authorities = new ArrayList<>();
}
