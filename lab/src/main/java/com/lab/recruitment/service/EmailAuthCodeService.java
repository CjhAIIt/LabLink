package com.lab.recruitment.service;

public interface EmailAuthCodeService {

    void sendRegisterCode(String studentId, String email);

    void sendPasswordResetCode(String username, String email);

    void verifyRegisterCode(String studentId, String email, String code);

    void verifyPasswordResetCode(String username, String email, String code);
}
