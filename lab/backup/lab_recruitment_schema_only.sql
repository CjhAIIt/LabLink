/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `t_college`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_college` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `college_code` varchar(64) NOT NULL COMMENT '学院编码',
  `college_name` varchar(128) NOT NULL COMMENT '学院名称',
  `admin_user_id` bigint DEFAULT NULL COMMENT '学院管理员用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-启用 0-停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_college_code` (`college_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学院表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键（投递记录唯一标识）',
  `user_id` bigint NOT NULL COMMENT '关联学生ID',
  `lab_id` bigint NOT NULL COMMENT '关联实验室ID',
  `skill_tags` varchar(255) NOT NULL COMMENT '学生特长标签（如"Java、机器学习"）',
  `study_progress` text NOT NULL COMMENT '近期学习进度（如"已学完Java基础，正在学Spring Boot"）',
  `attachment_url` varchar(1024) DEFAULT NULL COMMENT 'attachment urls',
  `delivery_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态（0-待审核，1-通过，2-拒绝）',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注（管理员填写）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `is_admitted` tinyint NOT NULL DEFAULT '0' COMMENT '是否录取（0-未录取，1-已录取）',
  `admit_time` datetime DEFAULT NULL COMMENT '录取时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0-未删除，1-已删除）',
  `delivery_attempt_count` int NOT NULL DEFAULT '1' COMMENT '投递次数(最多2次)',
  `withdraw_count` int NOT NULL DEFAULT '0' COMMENT '撤销次数(最多1次)',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_lab` (`user_id`,`lab_id`),
  KEY `idx_delivery_lab_deleted_time` (`lab_id`,`deleted`,`delivery_time`,`id`),
  KEY `idx_delivery_lab_audit_deleted_time` (`lab_id`,`audit_status`,`deleted`,`delivery_time`,`id`),
  KEY `idx_delivery_lab_admit_deleted` (`lab_id`,`is_admitted`,`deleted`,`admit_time`,`id`),
  KEY `idx_delivery_user_deleted_time` (`user_id`,`deleted`,`delivery_time`,`id`),
  KEY `idx_delivery_user_admit_deleted` (`user_id`,`is_admitted`,`deleted`,`admit_time`,`id`),
  CONSTRAINT `t_delivery_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `t_delivery_ibfk_2` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投递记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_email_auth_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_email_auth_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(64) NOT NULL COMMENT '账号或学号',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `purpose` varchar(32) NOT NULL COMMENT '用途: register/reset_password',
  `code` varchar(6) NOT NULL COMMENT '验证码',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `is_used` tinyint NOT NULL DEFAULT '0' COMMENT '是否已使用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email_auth_code_lookup` (`purpose`,`email`,`account`,`is_used`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱验证码表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '所属实验室ID',
  `name` varchar(100) NOT NULL COMMENT '设备名称',
  `type` varchar(50) NOT NULL COMMENT '设备类型',
  `serial_number` varchar(100) DEFAULT NULL COMMENT '设备编号',
  `image_url` varchar(255) DEFAULT NULL COMMENT '设备图片',
  `description` text COMMENT '设备描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0-空闲, 1-借用中, 2-维修中)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_lab_deleted_status_create` (`lab_id`,`deleted`,`status`,`create_time`,`id`),
  KEY `idx_equipment_lab_deleted_name` (`lab_id`,`deleted`,`name`),
  CONSTRAINT `t_equipment_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_equipment_borrow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_equipment_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_id` bigint NOT NULL COMMENT '设备ID',
  `user_id` bigint NOT NULL COMMENT '借用人ID',
  `borrow_time` datetime DEFAULT NULL COMMENT '借用时间',
  `return_time` datetime DEFAULT NULL COMMENT '归还时间',
  `reason` varchar(255) DEFAULT NULL COMMENT '借用理由',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0-申请中, 1-已借出, 2-已拒绝, 3-已归还)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_borrow_equipment_status_deleted_time` (`equipment_id`,`status`,`deleted`,`create_time`,`id`),
  KEY `idx_equipment_borrow_user_deleted_time` (`user_id`,`deleted`,`create_time`,`id`),
  CONSTRAINT `t_equipment_borrow_ibfk_1` FOREIGN KEY (`equipment_id`) REFERENCES `t_equipment` (`id`),
  CONSTRAINT `t_equipment_borrow_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备借用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_forum_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_forum_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_forum_comment_post_deleted_create` (`post_id`,`deleted`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_forum_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_forum_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  UNIQUE KEY `uk_forum_like_post_user` (`post_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_forum_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_forum_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text,
  `is_pinned` tinyint(1) DEFAULT '0',
  `is_essence` tinyint(1) DEFAULT '0',
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `view_count` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_forum_post_deleted_pinned_create` (`deleted`,`is_pinned`,`create_time`,`id`),
  KEY `idx_forum_post_user_deleted_create` (`user_id`,`deleted`,`create_time`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_gradpath_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_gradpath_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `creator_id` bigint NOT NULL,
  `question_type` varchar(30) NOT NULL DEFAULT 'programming',
  `track_code` varchar(50) NOT NULL DEFAULT 'common',
  `title` varchar(255) NOT NULL,
  `content` longtext,
  `difficulty` varchar(32) DEFAULT NULL,
  `input_format` text,
  `output_format` text,
  `sample_case_json` text,
  `program_languages` varchar(255) DEFAULT NULL,
  `judge_case_json` longtext,
  `tags_json` text,
  `analysis_hint` text,
  `status` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_gradpath_question_creator` (`creator_id`),
  KEY `idx_gradpath_question_deleted_id` (`deleted`,`id`),
  KEY `idx_gradpath_question_deleted_status_track_id` (`deleted`,`status`,`track_code`,`id`),
  CONSTRAINT `fk_gradpath_question_creator` FOREIGN KEY (`creator_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_growth_assessment_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_growth_assessment_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `option_key` varchar(16) NOT NULL COMMENT '选项标识',
  `option_title` varchar(255) NOT NULL COMMENT '选项标题',
  `option_desc` varchar(500) DEFAULT NULL COMMENT '选项描述',
  `score_json` text NOT NULL COMMENT '各路径分值',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_assessment_option` (`question_id`,`option_key`),
  CONSTRAINT `fk_growth_option_question` FOREIGN KEY (`question_id`) REFERENCES `t_growth_assessment_question` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1081 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长测评选项';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_growth_assessment_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_growth_assessment_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version_no` int NOT NULL COMMENT '版本号',
  `question_no` int NOT NULL COMMENT '题号',
  `dimension` varchar(64) NOT NULL COMMENT '维度',
  `title` varchar(255) NOT NULL COMMENT '题目',
  `description` text COMMENT '题目说明',
  `question_type` varchar(32) NOT NULL DEFAULT 'single_choice' COMMENT '题型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_assessment_question` (`version_no`,`question_no`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长测评题目';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_growth_assessment_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_growth_assessment_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `assessment_version` int NOT NULL COMMENT '测评版本',
  `answer_json` longtext NOT NULL COMMENT '答题记录',
  `score_json` longtext NOT NULL COMMENT '路径得分',
  `top_track_codes_json` text NOT NULL COMMENT '推荐路径编码',
  `summary` text NOT NULL COMMENT '结果摘要',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_growth_assessment_user` (`user_id`),
  CONSTRAINT `fk_growth_result_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长测评结果';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_growth_practice_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_growth_practice_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `question_type` varchar(30) NOT NULL COMMENT '题型: single_choice/fill_blank/programming',
  `track_code` varchar(50) NOT NULL DEFAULT 'common' COMMENT '成长路径编码',
  `title` varchar(255) NOT NULL COMMENT '题目标题',
  `content` longtext COMMENT '题目内容',
  `difficulty` varchar(32) DEFAULT NULL COMMENT '难度',
  `input_format` text COMMENT '输入格式',
  `output_format` text COMMENT '输出格式',
  `sample_case_json` text COMMENT '样例 JSON',
  `options_json` longtext COMMENT '选项 JSON',
  `answer_config` longtext COMMENT '答案配置 JSON',
  `program_languages` varchar(255) DEFAULT NULL COMMENT '允许语言 JSON',
  `judge_case_json` longtext COMMENT '判题用例 JSON',
  `tags_json` text COMMENT '标签 JSON',
  `analysis_hint` text COMMENT '解题提示',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1-启用 0-停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `creator_id` (`creator_id`),
  KEY `idx_growth_practice_question_deleted_id` (`deleted`,`id`),
  KEY `idx_growth_practice_question_deleted_status_track_type_id` (`deleted`,`status`,`track_code`,`question_type`,`id`),
  CONSTRAINT `t_growth_practice_question_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长中心共享题库';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_growth_track`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_growth_track` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT '路径编码',
  `name` varchar(100) NOT NULL COMMENT '路径名称',
  `short_name` varchar(64) NOT NULL COMMENT '短名称',
  `category` varchar(64) NOT NULL COMMENT '分类',
  `subtitle` varchar(255) NOT NULL COMMENT '副标题',
  `description` text NOT NULL COMMENT '路径描述',
  `fit_scene` text NOT NULL COMMENT '适配场景',
  `salary_range` varchar(64) DEFAULT NULL COMMENT '薪资区间',
  `recommended_keyword` varchar(128) DEFAULT NULL COMMENT '推荐练题关键词',
  `interview_position` varchar(128) DEFAULT NULL COMMENT '推荐面试岗位',
  `icon_key` varchar(64) DEFAULT NULL COMMENT '图标标识',
  `difficulty_label` varchar(64) DEFAULT NULL COMMENT '难度标签',
  `tags_json` text COMMENT '标签列表',
  `courses_json` text COMMENT '课程列表',
  `books_json` text COMMENT '书籍列表',
  `competitions_json` text COMMENT '竞赛列表',
  `certificates_json` text COMMENT '证书列表',
  `competency_json` text COMMENT '能力模型',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_track_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长路径';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_growth_track_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_growth_track_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `track_id` bigint NOT NULL COMMENT '路径ID',
  `stage_no` int NOT NULL COMMENT '阶段编号',
  `phase_code` varchar(64) NOT NULL COMMENT '阶段编码',
  `title` varchar(255) NOT NULL COMMENT '阶段标题',
  `duration` varchar(64) NOT NULL COMMENT '建议时长',
  `goal` text NOT NULL COMMENT '阶段目标',
  `resource_name` varchar(255) DEFAULT NULL COMMENT '资源名称',
  `resource_url` varchar(500) DEFAULT NULL COMMENT '资源链接',
  `practice_keyword` varchar(128) DEFAULT NULL COMMENT '练题关键词',
  `action_hint` varchar(255) DEFAULT NULL COMMENT '阶段动作提示',
  `focus_skills_json` text COMMENT '聚焦技能',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_growth_track_stage` (`track_id`,`sort_order`),
  CONSTRAINT `fk_growth_stage_track` FOREIGN KEY (`track_id`) REFERENCES `t_growth_track` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长路径阶段';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键（实验室唯一标识）',
  `lab_name` varchar(100) NOT NULL COMMENT '实验室名称（如"AI竞赛实验室"）',
  `lab_code` varchar(64) DEFAULT NULL COMMENT '实验室编码',
  `college_id` bigint DEFAULT NULL COMMENT '所属学院ID',
  `lab_desc` text NOT NULL COMMENT '实验室介绍（研究方向、竞赛成果等）',
  `teacher_name` varchar(64) DEFAULT NULL COMMENT '指导教师',
  `location` varchar(128) DEFAULT NULL COMMENT '实验室地点',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `require_skill` varchar(255) NOT NULL COMMENT '所需技能（如"Python、算法基础"）',
  `recruit_num` int NOT NULL DEFAULT '0' COMMENT '招生人数',
  `current_num` int NOT NULL DEFAULT '0' COMMENT '已投递人数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '招新状态（0-未开始，1-进行中，2-已结束）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（预设）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0-未删除，1-已删除）',
  `founding_date` varchar(50) DEFAULT NULL COMMENT '成立时间',
  `awards` text COMMENT '获得奖项',
  `outstanding_seniors` text COMMENT '优秀学长（JSON格式：[{"name":"张三","major":"软件工程","achievement":"入职腾讯"}]）',
  `basic_info` text COMMENT '基础信息',
  `advisors` varchar(255) DEFAULT NULL COMMENT '指导老师',
  `current_admins` varchar(255) DEFAULT NULL COMMENT '现任管理员',
  `interview_open` tinyint NOT NULL DEFAULT '1' COMMENT '是否开放面试',
  PRIMARY KEY (`id`),
  UNIQUE KEY `lab_name` (`lab_name`),
  KEY `idx_lab_deleted_status_create` (`deleted`,`status`,`create_time`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `student_user_id` bigint NOT NULL COMMENT '申请学生用户ID',
  `recruit_plan_id` bigint DEFAULT NULL COMMENT '招新计划ID',
  `apply_reason` text NOT NULL COMMENT '申请理由',
  `research_interest` text COMMENT '研究兴趣',
  `skill_summary` text COMMENT '技能说明',
  `status` varchar(32) NOT NULL DEFAULT 'submitted' COMMENT '状态',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_comment` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `lab_id` (`lab_id`),
  KEY `student_user_id` (`student_user_id`),
  KEY `recruit_plan_id` (`recruit_plan_id`),
  CONSTRAINT `t_lab_apply_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`),
  CONSTRAINT `t_lab_apply_ibfk_2` FOREIGN KEY (`student_user_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `t_lab_apply_ibfk_3` FOREIGN KEY (`recruit_plan_id`) REFERENCES `t_recruit_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室申请表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `user_id` bigint NOT NULL COMMENT '成员ID',
  `attendance_date` varchar(20) NOT NULL COMMENT '打卡日期',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-未确认, 1-已到, 2-未到',
  `reason` varchar(255) DEFAULT NULL COMMENT '未到原因或备注',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认管理员ID',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_attendance` (`lab_id`,`user_id`,`attendance_date`),
  KEY `idx_lab_attendance_lab_date_deleted_status` (`lab_id`,`attendance_date`,`deleted`,`status`),
  KEY `idx_lab_attendance_user_deleted_date` (`user_id`,`deleted`,`attendance_date`,`confirm_time`,`id`),
  CONSTRAINT `t_lab_attendance_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`),
  CONSTRAINT `t_lab_attendance_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室每日打卡表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_create_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_create_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `applicant_user_id` bigint NOT NULL COMMENT '申请人ID',
  `college_id` bigint NOT NULL COMMENT '所属学院ID',
  `lab_name` varchar(120) NOT NULL COMMENT '实验室名称',
  `teacher_name` varchar(64) NOT NULL COMMENT '指导老师',
  `location` varchar(128) DEFAULT NULL COMMENT '地点',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `research_direction` text COMMENT '研究方向',
  `apply_reason` text NOT NULL COMMENT '申请说明',
  `status` varchar(32) NOT NULL DEFAULT 'submitted' COMMENT '状态',
  `college_audit_by` bigint DEFAULT NULL COMMENT '学院审核人',
  `college_audit_time` datetime DEFAULT NULL COMMENT '学院审核时间',
  `college_audit_comment` varchar(255) DEFAULT NULL COMMENT '学院审核意见',
  `school_audit_by` bigint DEFAULT NULL COMMENT '学校审核人',
  `school_audit_time` datetime DEFAULT NULL COMMENT '学校审核时间',
  `school_audit_comment` varchar(255) DEFAULT NULL COMMENT '学校审核意见',
  `generated_lab_id` bigint DEFAULT NULL COMMENT '生成的实验室ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `applicant_user_id` (`applicant_user_id`),
  KEY `college_id` (`college_id`),
  CONSTRAINT `t_lab_create_apply_ibfk_1` FOREIGN KEY (`applicant_user_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `t_lab_create_apply_ibfk_2` FOREIGN KEY (`college_id`) REFERENCES `t_college` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室创建申请表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_exit_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_exit_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `reason` text NOT NULL COMMENT '退出原因',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_exit_application_user_status_deleted_create` (`user_id`,`status`,`deleted`,`create_time`,`id`),
  KEY `idx_exit_application_lab_status_deleted_create` (`lab_id`,`status`,`deleted`,`create_time`,`id`),
  CONSTRAINT `t_lab_exit_application_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `t_lab_exit_application_ibfk_2` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室退出申请表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `user_id` bigint NOT NULL COMMENT '成员用户ID',
  `member_role` varchar(32) NOT NULL DEFAULT 'member' COMMENT '成员角色',
  `join_date` date DEFAULT NULL COMMENT '加入日期',
  `quit_date` date DEFAULT NULL COMMENT '退出日期',
  `status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  `appointed_by` bigint DEFAULT NULL COMMENT '任命人',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_lab_member_lab_user_status` (`lab_id`,`user_id`,`status`),
  CONSTRAINT `t_lab_member_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`),
  CONSTRAINT `t_lab_member_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室成员表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_space_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_space_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `folder_id` bigint NOT NULL COMMENT '目录ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_url` varchar(1024) NOT NULL COMMENT '文件地址',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_type` varchar(100) DEFAULT NULL COMMENT '文件类型',
  `archive_flag` tinyint NOT NULL DEFAULT '0' COMMENT '归档标记',
  `access_scope` varchar(32) NOT NULL DEFAULT 'lab' COMMENT '权限范围',
  `version_no` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `folder_id` (`folder_id`),
  KEY `upload_user_id` (`upload_user_id`),
  KEY `idx_lab_space_file_scope` (`lab_id`,`folder_id`,`archive_flag`),
  CONSTRAINT `t_lab_space_file_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`),
  CONSTRAINT `t_lab_space_file_ibfk_2` FOREIGN KEY (`folder_id`) REFERENCES `t_lab_space_folder` (`id`),
  CONSTRAINT `t_lab_space_file_ibfk_3` FOREIGN KEY (`upload_user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室空间文件表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_lab_space_folder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_lab_space_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父目录ID',
  `folder_name` varchar(120) NOT NULL COMMENT '目录名称',
  `category` varchar(50) DEFAULT NULL COMMENT '目录分类',
  `sort_order` int NOT NULL DEFAULT '10' COMMENT '排序',
  `access_scope` varchar(32) NOT NULL DEFAULT 'lab' COMMENT '权限范围',
  `archived` tinyint NOT NULL DEFAULT '0' COMMENT '是否归档',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `lab_id` (`lab_id`),
  CONSTRAINT `t_lab_space_folder_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室空间目录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL COMMENT '公告标题',
  `content` longtext NOT NULL COMMENT '公告内容',
  `publish_scope` varchar(32) NOT NULL DEFAULT 'school' COMMENT '发布范围',
  `college_id` bigint DEFAULT NULL COMMENT '学院ID',
  `lab_id` bigint DEFAULT NULL COMMENT '实验室ID',
  `publisher_id` bigint NOT NULL COMMENT '发布人ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `publisher_id` (`publisher_id`),
  CONSTRAINT `t_notice_ibfk_1` FOREIGN KEY (`publisher_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_outstanding_graduate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_outstanding_graduate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '所属实验室ID',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `major` varchar(100) DEFAULT NULL COMMENT '专业',
  `graduation_year` varchar(20) DEFAULT NULL COMMENT '毕业年份',
  `description` text COMMENT '介绍',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像',
  `company` varchar(100) DEFAULT NULL COMMENT '就业单位',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `lab_id` (`lab_id`),
  CONSTRAINT `t_outstanding_graduate_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优秀毕业生表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_recruit_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_recruit_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `title` varchar(128) NOT NULL COMMENT '计划标题',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `quota` int NOT NULL DEFAULT '0' COMMENT '计划名额',
  `requirement` text COMMENT '申请要求',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `lab_id` (`lab_id`),
  CONSTRAINT `t_recruit_plan_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招新计划表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_system_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_system_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '接收人ID',
  `title` varchar(120) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `notification_type` varchar(40) NOT NULL COMMENT '通知类型',
  `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `t_system_notification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键（用户唯一标识）',
  `username` varchar(50) NOT NULL COMMENT '用户名（学号或管理员账号）',
  `password` varchar(100) NOT NULL COMMENT '密码（加密存储）',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `role` varchar(20) NOT NULL DEFAULT 'student' COMMENT '角色（student-学生，admin-管理员，super_admin-总负责人）',
  `student_id` varchar(20) DEFAULT NULL COMMENT '学号（学生角色必填）',
  `college` varchar(100) DEFAULT NULL COMMENT '学院（学生角色必填）',
  `major` varchar(100) DEFAULT NULL COMMENT '专业（学生角色必填）',
  `grade` varchar(20) DEFAULT NULL COMMENT '年级（学生角色必填）',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `resume` varchar(1024) DEFAULT NULL COMMENT 'resume url',
  `lab_id` bigint DEFAULT NULL COMMENT '关联实验室ID（管理员角色必填）',
  `can_edit` tinyint NOT NULL DEFAULT '1' COMMENT '是否可编辑账号信息（0-不可编辑，1-可编辑）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态（0-禁用，1-正常）',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `uk_user_student_id` (`student_id`,`deleted`),
  UNIQUE KEY `uk_user_email` (`email`,`deleted`),
  KEY `idx_user_role_deleted_create` (`role`,`deleted`,`create_time`,`id`),
  KEY `idx_user_lab_role_deleted_student` (`lab_id`,`role`,`deleted`,`student_id`,`id`),
  KEY `idx_user_phone_deleted` (`phone`,`deleted`),
  CONSTRAINT `t_user_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_written_exam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_written_exam` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `title` varchar(120) NOT NULL COMMENT '笔试标题',
  `description` text COMMENT '笔试说明',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `pass_score` int NOT NULL DEFAULT '60' COMMENT '通过分数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-关闭, 1-开放',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_written_exam_lab` (`lab_id`),
  KEY `idx_written_exam_lab_deleted_status` (`lab_id`,`deleted`,`status`,`id`),
  CONSTRAINT `t_written_exam_ibfk_1` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室笔试配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_written_exam_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_written_exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exam_id` bigint NOT NULL COMMENT '笔试ID',
  `bank_question_id` bigint DEFAULT NULL COMMENT '共享题库题目ID',
  `question_type` varchar(30) NOT NULL COMMENT '题型: single_choice/fill_blank/programming',
  `track_code` varchar(50) DEFAULT NULL COMMENT '成长路径编码',
  `title` varchar(255) NOT NULL COMMENT '题目标题',
  `content` text COMMENT '题目内容',
  `difficulty` varchar(32) DEFAULT NULL COMMENT '难度',
  `input_format` text COMMENT '输入格式',
  `output_format` text COMMENT '输出格式',
  `sample_case_json` text COMMENT '样例 JSON',
  `options_json` text COMMENT '选项JSON',
  `answer_config` text COMMENT '标准答案配置',
  `program_languages` varchar(255) DEFAULT NULL COMMENT '允许语言JSON',
  `judge_case_json` longtext COMMENT '判题用例JSON',
  `tags_json` text COMMENT '标签 JSON',
  `analysis_hint` text COMMENT '解题提示',
  `score` int NOT NULL DEFAULT '10' COMMENT '分值',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `exam_id` (`exam_id`),
  CONSTRAINT `t_written_exam_question_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `t_written_exam` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室笔试题目表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_written_exam_submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_written_exam_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exam_id` bigint NOT NULL COMMENT '笔试ID',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `answer_sheet_json` longtext COMMENT '答卷JSON',
  `total_score` decimal(6,2) NOT NULL DEFAULT '0.00' COMMENT '总分',
  `ai_remark` text COMMENT '智能评分说明',
  `admin_remark` varchar(255) DEFAULT NULL COMMENT '管理员备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-待审核, 2-已通过, 3-未通过',
  `submit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `grade_time` datetime DEFAULT NULL COMMENT '批改时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_written_exam_submission` (`exam_id`,`user_id`),
  KEY `idx_exam_submission_lab_status_deleted_submit` (`lab_id`,`status`,`deleted`,`submit_time`,`id`),
  KEY `idx_exam_submission_user_lab_deleted_submit` (`user_id`,`lab_id`,`deleted`,`submit_time`,`id`),
  KEY `idx_exam_submission_exam_deleted` (`exam_id`,`deleted`,`id`),
  CONSTRAINT `t_written_exam_submission_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `t_written_exam` (`id`),
  CONSTRAINT `t_written_exam_submission_ibfk_2` FOREIGN KEY (`lab_id`) REFERENCES `t_lab` (`id`),
  CONSTRAINT `t_written_exam_submission_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验室笔试提交表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


