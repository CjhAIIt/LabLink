package com.lab.recruitment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验初始化脚本中内置账号的密码哈希与文档记录的默认口令一致。
 *
 * <p>如果有人误改了 init.sql 里的哈希而没有同步文档，这里会直接失败。</p>
 */
class PasswordEncodingTest {

    /**
     * init.sql 中 superadmin 与全部示例学生账号共用的哈希，对应默认口令 Lab123456。
     */
    private static final String SEEDED_HASH =
            "$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK";

    private static final String DOCUMENTED_DEFAULT_PASSWORD = "Lab123456";

    @Test
    @DisplayName("内置账号的哈希匹配文档记录的默认口令")
    void seededHashMatchesDocumentedPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        assertTrue(encoder.matches(DOCUMENTED_DEFAULT_PASSWORD, SEEDED_HASH),
                "init.sql 中的哈希应与文档记录的默认口令一致");
    }

    @Test
    @DisplayName("其他口令不应匹配内置账号的哈希")
    void seededHashRejectsOtherPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        assertFalse(encoder.matches("123456", SEEDED_HASH));
        assertFalse(encoder.matches("password", SEEDED_HASH));
        assertFalse(encoder.matches("admin123", SEEDED_HASH));
    }

    @Test
    @DisplayName("相同口令每次加密得到不同哈希且都能校验通过")
    void encoderProducesSaltedHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String first = encoder.encode(DOCUMENTED_DEFAULT_PASSWORD);
        String second = encoder.encode(DOCUMENTED_DEFAULT_PASSWORD);

        assertFalse(first.equals(second), "bcrypt 应为每次加密加入随机盐");
        assertTrue(encoder.matches(DOCUMENTED_DEFAULT_PASSWORD, first));
        assertTrue(encoder.matches(DOCUMENTED_DEFAULT_PASSWORD, second));
    }
}
