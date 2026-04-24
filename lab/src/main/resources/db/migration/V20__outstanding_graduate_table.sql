CREATE TABLE IF NOT EXISTS `t_outstanding_graduate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `lab_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `major` VARCHAR(100) DEFAULT NULL,
    `graduation_year` VARCHAR(20) DEFAULT NULL,
    `description` TEXT DEFAULT NULL,
    `avatar_url` VARCHAR(255) DEFAULT NULL,
    `company` VARCHAR(100) DEFAULT NULL,
    `position` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_outstanding_graduate_lab` (`lab_id`),
    CONSTRAINT `fk_outstanding_graduate_lab`
        FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
