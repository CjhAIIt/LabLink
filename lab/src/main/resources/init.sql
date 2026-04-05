SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_college (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    college_code VARCHAR(64) NOT NULL,
    college_name VARCHAR(128) NOT NULL,
    admin_user_id BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_college_code (college_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表';

CREATE TABLE IF NOT EXISTS t_lab (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_name VARCHAR(100) NOT NULL,
    lab_code VARCHAR(64) NULL,
    college_id BIGINT NULL,
    lab_desc TEXT NOT NULL,
    teacher_name VARCHAR(64) NULL,
    location VARCHAR(128) NULL,
    contact_email VARCHAR(128) NULL,
    require_skill VARCHAR(255) NOT NULL,
    recruit_num INT NOT NULL DEFAULT 0,
    current_num INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    founding_date VARCHAR(50) NULL,
    awards TEXT NULL,
    basic_info TEXT NULL,
    advisors VARCHAR(255) NULL,
    current_admins VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_lab_name (lab_name),
    UNIQUE KEY uk_lab_code (lab_code),
    CONSTRAINT fk_lab_college FOREIGN KEY (college_id) REFERENCES t_college(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室表';

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'student',
    student_id VARCHAR(20) NULL,
    college VARCHAR(100) NULL,
    major VARCHAR(100) NULL,
    grade VARCHAR(20) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    avatar VARCHAR(255) NULL,
    resume VARCHAR(1024) NULL,
    lab_id BIGINT NULL,
    can_edit TINYINT NOT NULL DEFAULT 1,
    system_account_code VARCHAR(64) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_student_id (student_id, deleted),
    UNIQUE KEY uk_user_email (email, deleted),
    UNIQUE KEY uk_user_system_account_code (system_account_code, deleted),
    CONSTRAINT fk_user_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS t_email_auth_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expire_time DATETIME NOT NULL,
    is_used TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email_auth_code_lookup (purpose, email, account, is_used, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

CREATE TABLE IF NOT EXISTS t_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    skill_tags VARCHAR(255) NOT NULL,
    study_progress TEXT NOT NULL,
    attachment_url VARCHAR(1024) NULL,
    delivery_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audit_status TINYINT NOT NULL DEFAULT 0,
    audit_remark VARCHAR(255) NULL,
    audit_time DATETIME NULL,
    is_admitted TINYINT NOT NULL DEFAULT 0,
    admit_time DATETIME NULL,
    delivery_attempt_count INT NOT NULL DEFAULT 1,
    withdraw_count INT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_lab (user_id, lab_id),
    CONSTRAINT fk_delivery_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_delivery_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室投递表';

CREATE TABLE IF NOT EXISTS t_recruit_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    quota INT NOT NULL DEFAULT 0,
    requirement TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_recruit_plan_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招新计划表';

CREATE TABLE IF NOT EXISTS t_lab_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    recruit_plan_id BIGINT NULL,
    apply_reason TEXT NOT NULL,
    research_interest TEXT NULL,
    skill_summary TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    audit_by BIGINT NULL,
    audit_time DATETIME NULL,
    audit_comment VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lab_apply_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id),
    CONSTRAINT fk_lab_apply_user FOREIGN KEY (student_user_id) REFERENCES t_user(id),
    CONSTRAINT fk_lab_apply_plan FOREIGN KEY (recruit_plan_id) REFERENCES t_recruit_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室入组申请表';

CREATE TABLE IF NOT EXISTS t_lab_create_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_user_id BIGINT NOT NULL,
    college_id BIGINT NOT NULL,
    lab_name VARCHAR(120) NOT NULL,
    teacher_name VARCHAR(64) NOT NULL,
    location VARCHAR(128) NULL,
    contact_email VARCHAR(128) NULL,
    research_direction TEXT NULL,
    apply_reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    college_audit_by BIGINT NULL,
    college_audit_time DATETIME NULL,
    college_audit_comment VARCHAR(255) NULL,
    school_audit_by BIGINT NULL,
    school_audit_time DATETIME NULL,
    school_audit_comment VARCHAR(255) NULL,
    generated_lab_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lab_create_apply_user FOREIGN KEY (applicant_user_id) REFERENCES t_user(id),
    CONSTRAINT fk_lab_create_apply_college FOREIGN KEY (college_id) REFERENCES t_college(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室创建申请表';

CREATE TABLE IF NOT EXISTS t_lab_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(32) NOT NULL DEFAULT 'member',
    join_date DATE NULL,
    quit_date DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    appointed_by BIGINT NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_lab_member_lab_user_status (lab_id, user_id, status),
    CONSTRAINT fk_lab_member_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id),
    CONSTRAINT fk_lab_member_user FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室成员表';

CREATE TABLE IF NOT EXISTS t_lab_space_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    parent_id BIGINT NULL DEFAULT 0,
    folder_name VARCHAR(120) NOT NULL,
    category VARCHAR(50) NULL,
    sort_order INT NOT NULL DEFAULT 10,
    access_scope VARCHAR(32) NOT NULL DEFAULT 'lab',
    archived TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lab_space_folder_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室空间目录表';

CREATE TABLE IF NOT EXISTS t_lab_space_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_size BIGINT NULL,
    file_type VARCHAR(100) NULL,
    archive_flag TINYINT NOT NULL DEFAULT 0,
    access_scope VARCHAR(32) NOT NULL DEFAULT 'lab',
    version_no INT NOT NULL DEFAULT 1,
    upload_user_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_lab_space_file_scope (lab_id, folder_id, archive_flag),
    CONSTRAINT fk_lab_space_file_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id),
    CONSTRAINT fk_lab_space_file_folder FOREIGN KEY (folder_id) REFERENCES t_lab_space_folder(id),
    CONSTRAINT fk_lab_space_file_user FOREIGN KEY (upload_user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室空间文件表';

CREATE TABLE IF NOT EXISTS t_lab_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    attendance_date VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    reason VARCHAR(255) NULL,
    confirmed_by BIGINT NULL,
    confirm_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_lab_attendance (lab_id, user_id, attendance_date),
    CONSTRAINT fk_lab_attendance_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id),
    CONSTRAINT fk_lab_attendance_user FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室考勤表';

CREATE TABLE IF NOT EXISTS t_lab_exit_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    audit_remark VARCHAR(255) NULL,
    audit_by BIGINT NULL,
    audit_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lab_exit_application_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_lab_exit_application_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室退出申请表';

CREATE TABLE IF NOT EXISTS t_teacher_register_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_no VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    college_id BIGINT NOT NULL,
    title VARCHAR(50) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NOT NULL,
    apply_reason VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    college_audit_by BIGINT NULL,
    college_audit_time DATETIME NULL,
    college_audit_comment VARCHAR(255) NULL,
    school_audit_by BIGINT NULL,
    school_audit_time DATETIME NULL,
    school_audit_comment VARCHAR(255) NULL,
    generated_user_id BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_teacher_register_apply_lookup (teacher_no, email, status, deleted),
    INDEX idx_teacher_register_apply_college (college_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师注册申请表';

CREATE TABLE IF NOT EXISTS t_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    content LONGTEXT NOT NULL,
    publish_scope VARCHAR(32) NOT NULL DEFAULT 'school',
    college_id BIGINT NULL,
    lab_id BIGINT NULL,
    publisher_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

INSERT INTO t_college (id, college_code, college_name, admin_user_id, status, remark, deleted) VALUES
    (1, 'CS', '计算机与软件工程学院', NULL, 1, '安徽信息工程学院学院主数据', 0),
    (2, 'AI', '大数据与人工智能学院', NULL, 1, '安徽信息工程学院学院主数据', 0),
    (3, 'EEE', '电气与电子工程学院', NULL, 1, '安徽信息工程学院学院主数据', 0),
    (4, 'ME', '机械工程学院', NULL, 1, '安徽信息工程学院学院主数据', 0),
    (5, 'MGT', '管理工程学院', NULL, 1, '安徽信息工程学院学院主数据', 0),
    (6, 'ART', '艺术设计学院', NULL, 1, '安徽信息工程学院学院主数据', 0),
    (7, 'FLA', '通识教育与外国语学院', NULL, 1, '安徽信息工程学院学院主数据', 0);

INSERT INTO t_lab (id, lab_name, lab_code, college_id, lab_desc, teacher_name, location, contact_email, require_skill, recruit_num, current_num, status, founding_date, awards, basic_info, advisors, current_admins, deleted) VALUES
    (1, '智能软件与应用创新实验室', 'LAB-CS-001', 1, '面向 Web 系统、软件工程实践、校园数字化应用与创新创业项目。', '待维护', '科创楼 A-301', 'software_lab@aiit.edu.cn', 'Java、Spring Boot、Vue、数据库基础', 20, 2, 1, '2021-09', '承担多项校级创新创业与竞赛项目', '服务软件工程与计算机方向学生的项目化培养。', '待维护', 'cs_admin', 0),
    (2, '数据智能决策实验室', 'LAB-AI-002', 2, '聚焦数据治理、机器学习、可视化分析和人工智能应用。', '待维护', '科创楼 B-402', 'ai_lab@aiit.edu.cn', 'Python、机器学习、数据分析基础', 18, 1, 1, '2022-03', '支持学院数据智能方向科研与实践训练', '服务大数据与人工智能学院的实验与竞赛协同。', '待维护', 'ai_admin', 0),
    (3, '工业智能与嵌入式系统实验室', 'LAB-EEE-003', 3, '聚焦嵌入式系统、物联网应用、电子信息与工程实践。', '待维护', '工程楼 C-214', 'embedded_lab@aiit.edu.cn', 'C 语言、单片机、嵌入式基础', 16, 1, 1, '2020-06', '面向电子信息和嵌入式方向提供实践平台', '服务电气与电子工程学院的实验教学与竞赛训练。', '待维护', 'ee_admin', 0);

INSERT INTO t_user (id, username, password, real_name, role, student_id, college, major, grade, phone, email, lab_id, can_edit, status, deleted) VALUES
    (1, 'superadmin', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '学校管理员', 'super_admin', NULL, NULL, NULL, NULL, '13800000001', 'superadmin@aiit.edu.cn', NULL, 0, 1, 0),
    (2, 'cs_admin', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '计算机与软件工程学院管理员', 'admin', NULL, NULL, NULL, NULL, '13800000002', 'cs_admin@aiit.edu.cn', 1, 1, 1, 0),
    (3, 'ai_admin', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '大数据与人工智能学院管理员', 'admin', NULL, NULL, NULL, NULL, '13800000003', 'ai_admin@aiit.edu.cn', 2, 1, 1, 0),
    (4, 'ee_admin', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '电气与电子工程学院管理员', 'admin', NULL, NULL, NULL, NULL, '13800000004', 'ee_admin@aiit.edu.cn', 3, 1, 1, 0),
    (5, '20231001', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '张晨', 'student', '20231001', '计算机与软件工程学院', '软件工程', '2023级', '13800000011', '20231001@aiit.edu.cn', 1, 1, 1, 0),
    (6, '20231002', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '李沐', 'student', '20231002', '计算机与软件工程学院', '计算机科学与技术', '2023级', '13800000012', '20231002@aiit.edu.cn', 1, 1, 1, 0),
    (7, '20231003', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '王宁', 'student', '20231003', '大数据与人工智能学院', '人工智能', '2023级', '13800000013', '20231003@aiit.edu.cn', NULL, 1, 1, 0),
    (8, '20231004', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '赵楠', 'student', '20231004', '电气与电子工程学院', '电子信息工程', '2023级', '13800000014', '20231004@aiit.edu.cn', 3, 1, 1, 0);

UPDATE t_college SET admin_user_id = 2 WHERE id = 1;
UPDATE t_college SET admin_user_id = 3 WHERE id = 2;
UPDATE t_college SET admin_user_id = 4 WHERE id = 3;

INSERT INTO t_lab_create_apply (id, applicant_user_id, college_id, lab_name, teacher_name, location, contact_email, research_direction, apply_reason, status, college_audit_by, college_audit_time, college_audit_comment, school_audit_by, school_audit_time, school_audit_comment, generated_lab_id, deleted) VALUES
    (1, 2, 1, '校园软件协同创新实验室', '陈志远', '科创楼 A-315', 'campus_software@aiit.edu.cn', '校园数字化、业务协同、软件工程实践', '拟面向校内数字化建设和软件工程训练，创建校级实验室。', 'submitted', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0),
    (2, 3, 2, '智能数据治理实验室', '刘敏', '科创楼 B-420', 'data_governance@aiit.edu.cn', '数据治理、智能分析、AI 应用', '学院已完成初审，现提交学校管理员终审。', 'college_approved', 1, '2026-03-18 09:30:00', '学院初审通过', NULL, NULL, NULL, NULL, 0);

INSERT INTO t_teacher_register_apply (id, teacher_no, password_hash, real_name, college_id, title, phone, email, apply_reason, status, college_audit_by, college_audit_time, college_audit_comment, school_audit_by, school_audit_time, school_audit_comment, generated_user_id, deleted) VALUES
    (1, 'T2026001', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '陈志远', 1, '讲师', '13800000101', 'teacher01@aiit.edu.cn', '拟申请教师账号，用于提交全校版实验室创建申请和后续管理协同。', 'submitted', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0),
    (2, 'T2026002', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '刘敏', 2, '副教授', '13800000102', 'teacher02@aiit.edu.cn', '已完成学院侧材料准备，申请开通教师账号用于实验室建设与招生协同。', 'college_approved', 3, '2026-03-26 10:00:00', '学院初审通过', NULL, NULL, NULL, NULL, 0);

INSERT INTO t_recruit_plan (id, lab_id, title, start_time, end_time, quota, requirement, status, created_by, deleted) VALUES
    (1, 1, '智能软件与应用创新实验室 2026 春季招募', '2026-03-01 08:00:00', '2026-04-20 23:59:59', 8, '具备 Java、前端基础或对校园数字化项目有兴趣。', 'open', 2, 0),
    (2, 2, '数据智能决策实验室 2026 春季招募', '2026-03-05 08:00:00', '2026-04-25 23:59:59', 6, '具备 Python 与数据分析基础，愿意参与 AI 相关项目。', 'open', 3, 0),
    (3, 3, '工业智能与嵌入式系统实验室 2026 春季招募', '2026-03-10 08:00:00', '2026-04-15 23:59:59', 5, '具备 C 语言、单片机或嵌入式基础优先。', 'open', 4, 0);

INSERT INTO t_lab_member (id, lab_id, user_id, member_role, join_date, status, appointed_by, remark, deleted) VALUES
    (1, 1, 5, 'lab_leader', '2026-03-01', 'active', 2, '实验室项目负责人', 0),
    (2, 1, 6, 'member', '2026-03-05', 'active', 2, '实验室正式成员', 0),
    (3, 3, 8, 'member', '2026-03-18', 'active', 4, '审核通过自动入组', 0);

INSERT INTO t_lab_space_folder (id, lab_id, parent_id, folder_name, category, sort_order, access_scope, archived, created_by, deleted) VALUES
    (1, 1, 0, '基础档案', 'profile', 10, 'lab', 0, 2, 0),
    (2, 1, 0, '招募归档', 'recruit', 20, 'lab', 0, 2, 0),
    (3, 1, 0, '成员资料', 'member', 30, 'lab', 0, 2, 0),
    (4, 1, 0, '项目文档', 'project', 40, 'lab', 0, 2, 0),
    (5, 2, 0, '基础档案', 'profile', 10, 'lab', 0, 3, 0),
    (6, 2, 0, '招募归档', 'recruit', 20, 'lab', 0, 3, 0),
    (7, 2, 0, '数据样本', 'project', 30, 'lab', 0, 3, 0),
    (8, 3, 0, '基础档案', 'profile', 10, 'lab', 0, 4, 0),
    (9, 3, 0, '项目文档', 'project', 20, 'lab', 0, 4, 0),
    (10, 3, 0, '成果归档', 'achievement', 30, 'lab', 0, 4, 0);

INSERT INTO t_lab_apply (id, lab_id, student_user_id, recruit_plan_id, apply_reason, research_interest, skill_summary, status, audit_by, audit_time, audit_comment, deleted) VALUES
    (1, 2, 7, 2, '希望参与数据分析和机器学习项目训练，提升科研与竞赛能力。', '机器学习、数据可视化', '掌握 Python、Pandas 和基础建模。', 'submitted', NULL, NULL, NULL, 0),
    (2, 3, 8, 3, '希望加入嵌入式方向实验室，参与电子设计竞赛训练。', '嵌入式开发、传感器应用', '熟悉 C 语言与 STM32 基础。', 'approved', 4, '2026-03-18 10:30:00', '具备良好基础，同意录用。', 0);

INSERT INTO t_lab_attendance (id, lab_id, user_id, attendance_date, status, reason, confirmed_by, confirm_time, deleted) VALUES
    (1, 1, 5, '2026-03-25', 1, NULL, 2, '2026-03-25 21:10:00', 0),
    (2, 1, 6, '2026-03-25', 2, '迟到 10 分钟', 2, '2026-03-25 21:15:00', 0),
    (3, 1, 5, '2026-03-26', 1, NULL, 2, '2026-03-26 21:08:00', 0),
    (4, 1, 6, '2026-03-26', 4, '缺勤', 2, '2026-03-26 22:00:00', 0),
    (5, 1, 5, '2026-03-27', 5, '补签通过', 2, '2026-03-27 22:05:00', 0),
    (6, 1, 6, '2026-03-27', 3, '课程冲突', 2, '2026-03-27 18:30:00', 0),
    (7, 3, 8, '2026-03-29', 1, NULL, 4, '2026-03-29 21:01:00', 0),
    (8, 3, 8, '2026-03-30', 2, '晚到', 4, '2026-03-30 21:20:00', 0);

INSERT INTO t_notice (id, title, content, publish_scope, college_id, lab_id, publisher_id, status, publish_time, deleted) VALUES
    (1, '系统启用通知', '安徽信息工程学院实验室管理平台已启用，请各学院及时完善学院主数据、实验室档案和审批流程配置。', 'school', NULL, NULL, 1, 1, '2026-03-20 09:00:00', 0),
    (2, '学院实验室建档提醒', '请相关学院在本周内完成实验室负责人、地点、研究方向和招募要求的维护。', 'school', NULL, NULL, 1, 1, '2026-03-22 14:00:00', 0),
    (3, '智能软件与应用创新实验室宣讲', '本周五晚 19:00 在科创楼 A-301 举行实验室宣讲，欢迎感兴趣同学到场了解。', 'lab', 1, 1, 2, 1, '2026-03-24 18:00:00', 0);

SET FOREIGN_KEY_CHECKS = 1;
