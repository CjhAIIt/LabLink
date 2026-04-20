-- AI 面试模块表
CREATE TABLE IF NOT EXISTS `t_ai_interview_module` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `module_name` VARCHAR(64) NOT NULL COMMENT '模块名称',
    `module_code` VARCHAR(32) NOT NULL COMMENT '模块编码',
    `description` VARCHAR(512) DEFAULT '' COMMENT '模块说明',
    `prompt_template` TEXT COMMENT '该模块的提示词模板',
    `score_rule` TEXT COMMENT '评分规则配置',
    `icon` VARCHAR(16) DEFAULT '' COMMENT '图标 emoji',
    `color` VARCHAR(128) DEFAULT '' COMMENT '渐变色 CSS',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '是否启用 1=启用 0=停用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_module_code` (`module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 面试模块';

-- AI 面试记录表（仅正式面试）
CREATE TABLE IF NOT EXISTS `t_ai_interview_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_id` BIGINT NOT NULL COMMENT '学生 ID',
    `student_name` VARCHAR(64) DEFAULT '' COMMENT '学生姓名（冗余）',
    `module_id` BIGINT NOT NULL COMMENT '面试模块 ID',
    `module_name` VARCHAR(64) DEFAULT '' COMMENT '模块名称（冗余）',
    `attempt_no` INT NOT NULL DEFAULT 1 COMMENT '第几次正式面试',
    `score` INT DEFAULT NULL COMMENT 'AI 评分 0-100',
    `tags_json` TEXT COMMENT '标签列表 JSON',
    `summary` TEXT COMMENT 'AI 评价摘要',
    `strengths` TEXT COMMENT '优势',
    `weaknesses` TEXT COMMENT '待改进',
    `suggestions` TEXT COMMENT '学习建议',
    `conversation_json` MEDIUMTEXT COMMENT '对话记录 JSON',
    `status` VARCHAR(16) DEFAULT '进行中' COMMENT '进行中/已完成/异常中断/作废',
    `start_time` DATETIME DEFAULT NULL,
    `end_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_module_id` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 正式面试记录';

-- AI 面试全局配置表
CREATE TABLE IF NOT EXISTS `t_ai_interview_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(64) NOT NULL,
    `config_value` VARCHAR(256) DEFAULT '',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 面试全局配置';

-- 初始化配置：正式面试开关
INSERT INTO `t_ai_interview_config` (`config_key`, `config_value`) VALUES ('formal_interview_open', 'false');

-- 初始化默认模块
INSERT INTO `t_ai_interview_module` (`module_name`, `module_code`, `description`, `icon`, `color`, `sort_order`) VALUES
('Java 基础', 'java', '面向对象、集合、多线程、JVM 等核心知识', '☕', 'linear-gradient(135deg,#f97316,#ea580c)', 1),
('Spring Boot', 'spring', 'IoC、AOP、自动配置、Web 开发等', '🌱', 'linear-gradient(135deg,#22c55e,#16a34a)', 2),
('前端基础', 'frontend', 'HTML/CSS/JS、DOM、事件、ES6+ 等', '🎨', 'linear-gradient(135deg,#3b82f6,#2563eb)', 3),
('Vue / React', 'vue-react', '组件化、响应式、路由、状态管理等', '⚡', 'linear-gradient(135deg,#8b5cf6,#7c3aed)', 4),
('MySQL', 'mysql', '索引、事务、SQL 优化、锁机制等', '🗄️', 'linear-gradient(135deg,#06b6d4,#0891b2)', 5),
('数据结构与算法', 'algorithm', '链表、树、排序、动态规划等', '🧮', 'linear-gradient(135deg,#ec4899,#db2777)', 6),
('计算机网络', 'network', 'TCP/IP、HTTP、DNS、网络安全等', '🌐', 'linear-gradient(135deg,#14b8a6,#0d9488)', 7),
('操作系统', 'os', '进程、内存管理、文件系统、调度等', '💻', 'linear-gradient(135deg,#64748b,#475569)', 8),
('项目经历表达', 'project', '项目介绍、技术选型、难点与解决方案', '📋', 'linear-gradient(135deg,#f59e0b,#d97706)', 9);
