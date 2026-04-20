package com.lab.recruitment.support;

import lombok.Data;

@Data
public class DataScope {

    private DataScopeLevel level;
    private Long userId;
    private Long collegeId;
    private Long labId;
    private String displayRole;

    public boolean isSchoolLevel() {
        return DataScopeLevel.SCHOOL == level;
    }

    public boolean isCollegeLevel() {
        return DataScopeLevel.COLLEGE == level;
    }

    public boolean isLabLevel() {
        return DataScopeLevel.LAB == level;
    }

    public boolean isSelfLevel() {
        return DataScopeLevel.SELF == level;
    }
}
