package com.lab.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
public class PasswordEncodingTest {

    @Test
    public void testPasswordEncoding() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        
        // Generate a new hash
        String newHash = passwordEncoder.encode(rawPassword);
        System.out.println("New hash for '123456': " + newHash);
        
        // Test with the existing hash from database
        String existingHash = "$2a$10$7JB720yubVSOfvVWbfXCOOxjTOQcQjmrJF1ZM4nAVccp/.rkMlDWy";
        
        // Check if the raw password matches the existing hash
        boolean matches = passwordEncoder.matches(rawPassword, existingHash);
        System.out.println("Does '123456' match existing hash? " + matches);
        
        // Check if the raw password matches the new hash
        boolean matchesNew = passwordEncoder.matches(rawPassword, newHash);
        System.out.println("Does '123456' match new hash? " + matchesNew);
        
        // Test with wrong password
        boolean wrongPassword = passwordEncoder.matches("wrong", existingHash);
        System.out.println("Does 'wrong' match existing hash? " + wrongPassword);
    }
}