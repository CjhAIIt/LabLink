package com.lab.recruitment.service;

import com.lab.recruitment.entity.College;
import com.lab.recruitment.entity.User;

public interface SystemManagementAccountService {

    void ensureBuiltInManagementUsers();

    void ensureCollegeManagerAccount(College college);

    boolean isFixedManagementAccount(User user);
}
