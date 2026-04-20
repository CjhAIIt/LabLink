package com.lab.recruitment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.modules.forum.enabled", havingValue = "true")
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Create t_forum_post table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_forum_post (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "title VARCHAR(255) NOT NULL," +
                "content TEXT," +
                "is_pinned TINYINT(1) DEFAULT 0," +
                "is_essence TINYINT(1) DEFAULT 0," +
                "like_count INT DEFAULT 0," +
                "comment_count INT DEFAULT 0," +
                "view_count INT DEFAULT 0," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT(1) DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        // Create t_forum_comment table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_forum_comment (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "post_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "content TEXT NOT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "deleted TINYINT(1) DEFAULT 0," +
                "INDEX idx_post_id (post_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        // Create t_forum_like table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_forum_like (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "post_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_post_user (post_id, user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
                
        System.out.println("Forum tables initialized successfully.");
    }
}
