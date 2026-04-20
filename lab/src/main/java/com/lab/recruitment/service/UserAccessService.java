package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.support.UserAccessProfile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public interface UserAccessService {

    UserAccessProfile buildProfile(User user);

    List<SimpleGrantedAuthority> buildAuthorities(User user);

    Long resolveManagedCollegeId(User user);

    Long resolveManagedLabId(User user);

    boolean isStudentIdentity(User user);

    boolean isTeacherIdentity(User user);

    boolean isLabManager(User user, Long labId);

    void ensureStudentLabManager(User user, Long labId);

    void refreshCompatibilityAccess(Long userId);
}
