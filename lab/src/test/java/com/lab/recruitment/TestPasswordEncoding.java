package com.lab.recruitment;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPasswordEncoding {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String hashedPassword = "$2a$10$7JB720yubVSOfvVWbfXCOOxjTOQcQjmrJF1ZM4nAVccp/.rkMlDWy";
        
        System.out.println("Checking 123456: " + encoder.matches("123456", hashedPassword));
        System.out.println("Checking admin123: " + encoder.matches("admin123", hashedPassword));
        
        // 生成 123456 的新哈希
        System.out.println("Hash for 123456: " + encoder.encode("123456"));
        // 生成 admin123 的新哈希
        System.out.println("Hash for admin123: " + encoder.encode("admin123"));
    }
}