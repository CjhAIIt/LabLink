package com.lab.recruitment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class DatabaseSchemaUpdate implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.modules.growth-center.enabled:false}")
    private boolean growthCenterEnabled;

    @Value("${app.modules.written-exam.enabled:false}")
    private boolean writtenExamEnabled;

    @Value("${app.seed.normalize-demo-data:true}")
    private boolean normalizeDemoData;

    @Override
    public void run(String... args) {
        createCollegeTable();
        updateLabTable();
        updateUserTable();
        createEmailAuthCodeTable();
        updateDeliveryTable();
        createRecruitPlanTable();
        createLabApplyTable();
        createLabCreateApplyTable();
        createTeacherRegisterApplyTable();
        createLabMemberTable();
        createLabSpaceFolderTable();
        createLabSpaceFileTable();
        createNoticeTable();
        createEquipmentTable();
        createEquipmentBorrowTable();
        createOutstandingGraduateTable();
        createLabAttendanceTable();
        createLabExitApplicationTable();
        if (growthCenterEnabled) {
            createGrowthPracticeQuestionTable();
        }
        if (writtenExamEnabled) {
            createWrittenExamTable();
            createWrittenExamQuestionTable();
            createWrittenExamSubmissionTable();
        }
        createSystemNotificationTable();
        seedCompetitionCoreData();
        if (normalizeDemoData) {
            normalizeSchoolEditionDemoData();
        }
    }

    private void createCollegeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_college (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "college_code VARCHAR(64) NOT NULL COMMENT '学院编码'," +
                "college_name VARCHAR(128) NOT NULL COMMENT '学院名称'," +
                "admin_user_id BIGINT NULL COMMENT '学院管理员用户ID'," +
                "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用'," +
                "remark VARCHAR(255) NULL COMMENT '备注'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_college_code (college_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表'");
    }

    private void updateLabTable() {
        addColumnIfMissing("t_lab", "lab_code",
                "ALTER TABLE t_lab ADD COLUMN lab_code VARCHAR(64) NULL COMMENT '实验室编码' AFTER lab_name");
        addColumnIfMissing("t_lab", "college_id",
                "ALTER TABLE t_lab ADD COLUMN college_id BIGINT NULL COMMENT '所属学院ID' AFTER lab_code");
        addColumnIfMissing("t_lab", "teacher_name",
                "ALTER TABLE t_lab ADD COLUMN teacher_name VARCHAR(64) NULL COMMENT '指导教师' AFTER lab_desc");
        addColumnIfMissing("t_lab", "location",
                "ALTER TABLE t_lab ADD COLUMN location VARCHAR(128) NULL COMMENT '实验室地点' AFTER teacher_name");
        addColumnIfMissing("t_lab", "contact_email",
                "ALTER TABLE t_lab ADD COLUMN contact_email VARCHAR(128) NULL COMMENT '联系邮箱' AFTER location");
        addColumnIfMissing("t_lab", "founding_date",
                "ALTER TABLE t_lab ADD COLUMN founding_date VARCHAR(50) COMMENT '成立时间'");
        addColumnIfMissing("t_lab", "awards",
                "ALTER TABLE t_lab ADD COLUMN awards TEXT COMMENT '获奖信息'");
        addColumnIfMissing("t_lab", "basic_info",
                "ALTER TABLE t_lab ADD COLUMN basic_info TEXT COMMENT '基础信息'");
        addColumnIfMissing("t_lab", "advisors",
                "ALTER TABLE t_lab ADD COLUMN advisors VARCHAR(255) COMMENT '指导老师'");
        addColumnIfMissing("t_lab", "current_admins",
                "ALTER TABLE t_lab ADD COLUMN current_admins VARCHAR(255) COMMENT '当前管理员'");
    }

    private void updateUserTable() {
        expandVarcharColumnIfNeeded("t_user", "resume", 1024,
                "ALTER TABLE t_user MODIFY COLUMN resume VARCHAR(1024) COMMENT 'resume url'");
        addColumnIfMissing("t_user", "system_account_code",
                "ALTER TABLE t_user ADD COLUMN system_account_code VARCHAR(64) NULL COMMENT 'system reserved account code' AFTER can_edit");

        ensureUserUniqueIndex("uk_user_student_id", "student_id");
        ensureUserUniqueIndex("uk_user_email", "email");
        ensureUserUniqueIndex("uk_user_system_account_code", "system_account_code");
        normalizeSystemAccountCodes();
    }

    private void createEmailAuthCodeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_email_auth_code (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "account VARCHAR(64) NOT NULL COMMENT '账号或学号'," +
                "email VARCHAR(100) NOT NULL COMMENT '邮箱'," +
                "purpose VARCHAR(32) NOT NULL COMMENT '用途: register/reset_password'," +
                "code VARCHAR(6) NOT NULL COMMENT '验证码'," +
                "expire_time DATETIME NOT NULL COMMENT '过期时间'," +
                "is_used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表'");

        addIndexIfMissing("t_email_auth_code", "idx_email_auth_code_lookup",
                "ALTER TABLE t_email_auth_code ADD INDEX idx_email_auth_code_lookup (purpose, email, account, is_used, expire_time)");
    }

    private void updateDeliveryTable() {
        expandVarcharColumnIfNeeded("t_delivery", "attachment_url", 1024,
                "ALTER TABLE t_delivery MODIFY COLUMN attachment_url VARCHAR(1024) COMMENT 'attachment urls'");

        addColumnIfMissing("t_delivery", "delivery_attempt_count",
                "ALTER TABLE t_delivery ADD COLUMN delivery_attempt_count INT NOT NULL DEFAULT 1 COMMENT '投递次数(最多2次)'");
        addColumnIfMissing("t_delivery", "withdraw_count",
                "ALTER TABLE t_delivery ADD COLUMN withdraw_count INT NOT NULL DEFAULT 0 COMMENT '撤销次数(最多1次)'");
        addColumnIfMissing("t_delivery", "update_time",
                "ALTER TABLE t_delivery ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
    }

    private void createRecruitPlanTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_recruit_plan (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "title VARCHAR(128) NOT NULL COMMENT '计划标题'," +
                "start_time DATETIME NULL COMMENT '开始时间'," +
                "end_time DATETIME NULL COMMENT '结束时间'," +
                "quota INT NOT NULL DEFAULT 0 COMMENT '计划名额'," +
                "requirement TEXT NULL COMMENT '申请要求'," +
                "status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '状态'," +
                "created_by BIGINT NULL COMMENT '创建人'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招新计划表'");
    }

    private void createLabApplyTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_apply (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "student_user_id BIGINT NOT NULL COMMENT '申请学生用户ID'," +
                "recruit_plan_id BIGINT NULL COMMENT '招新计划ID'," +
                "apply_reason TEXT NOT NULL COMMENT '申请理由'," +
                "research_interest TEXT NULL COMMENT '研究兴趣'," +
                "skill_summary TEXT NULL COMMENT '技能说明'," +
                "status VARCHAR(32) NOT NULL DEFAULT 'submitted' COMMENT '状态'," +
                "audit_by BIGINT NULL COMMENT '审核人'," +
                "audit_time DATETIME NULL COMMENT '审核时间'," +
                "audit_comment VARCHAR(255) NULL COMMENT '审核意见'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (student_user_id) REFERENCES t_user(id)," +
                "FOREIGN KEY (recruit_plan_id) REFERENCES t_recruit_plan(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室申请表'");
    }

    private void createLabCreateApplyTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_create_apply (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "applicant_user_id BIGINT NOT NULL COMMENT '申请人ID'," +
                "college_id BIGINT NOT NULL COMMENT '所属学院ID'," +
                "lab_name VARCHAR(120) NOT NULL COMMENT '实验室名称'," +
                "teacher_name VARCHAR(64) NOT NULL COMMENT '指导老师'," +
                "location VARCHAR(128) NULL COMMENT '地点'," +
                "contact_email VARCHAR(128) NULL COMMENT '联系邮箱'," +
                "research_direction TEXT NULL COMMENT '研究方向'," +
                "apply_reason TEXT NOT NULL COMMENT '申请说明'," +
                "status VARCHAR(32) NOT NULL DEFAULT 'submitted' COMMENT '状态'," +
                "college_audit_by BIGINT NULL COMMENT '学院审核人'," +
                "college_audit_time DATETIME NULL COMMENT '学院审核时间'," +
                "college_audit_comment VARCHAR(255) NULL COMMENT '学院审核意见'," +
                "school_audit_by BIGINT NULL COMMENT '学校审核人'," +
                "school_audit_time DATETIME NULL COMMENT '学校审核时间'," +
                "school_audit_comment VARCHAR(255) NULL COMMENT '学校审核意见'," +
                "generated_lab_id BIGINT NULL COMMENT '生成的实验室ID'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (applicant_user_id) REFERENCES t_user(id)," +
                "FOREIGN KEY (college_id) REFERENCES t_college(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室创建申请表'");
    }

    private void createTeacherRegisterApplyTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_teacher_register_apply (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "teacher_no VARCHAR(32) NOT NULL COMMENT '教师工号'," +
                "password_hash VARCHAR(100) NOT NULL COMMENT '注册密码哈希'," +
                "real_name VARCHAR(50) NOT NULL COMMENT '真实姓名'," +
                "college_id BIGINT NOT NULL COMMENT '所属学院ID'," +
                "title VARCHAR(50) NULL COMMENT '职称'," +
                "phone VARCHAR(20) NULL COMMENT '手机号'," +
                "email VARCHAR(100) NOT NULL COMMENT '邮箱'," +
                "apply_reason VARCHAR(500) NOT NULL COMMENT '申请说明'," +
                "status VARCHAR(32) NOT NULL DEFAULT 'submitted' COMMENT '状态'," +
                "college_audit_by BIGINT NULL COMMENT '学院审核人'," +
                "college_audit_time DATETIME NULL COMMENT '学院审核时间'," +
                "college_audit_comment VARCHAR(255) NULL COMMENT '学院审核备注'," +
                "school_audit_by BIGINT NULL COMMENT '学校审核人'," +
                "school_audit_time DATETIME NULL COMMENT '学校审核时间'," +
                "school_audit_comment VARCHAR(255) NULL COMMENT '学校审核备注'," +
                "generated_user_id BIGINT NULL COMMENT '生成的教师用户ID'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "INDEX idx_teacher_register_apply_lookup (teacher_no, email, status)," +
                "INDEX idx_teacher_register_apply_college (college_id, status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师注册申请表'");
    }

    private void createLabMemberTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_member (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "user_id BIGINT NOT NULL COMMENT '成员用户ID'," +
                "member_role VARCHAR(32) NOT NULL DEFAULT 'member' COMMENT '成员角色'," +
                "join_date DATE NULL COMMENT '加入日期'," +
                "quit_date DATE NULL COMMENT '退出日期'," +
                "status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态'," +
                "appointed_by BIGINT NULL COMMENT '任命人'," +
                "remark VARCHAR(255) NULL COMMENT '备注'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室成员表'");

        addIndexIfMissing("t_lab_member", "idx_lab_member_lab_user_status",
                "ALTER TABLE t_lab_member ADD INDEX idx_lab_member_lab_user_status (lab_id, user_id, status)");
    }

    private void createLabSpaceFolderTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_space_folder (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "parent_id BIGINT NULL DEFAULT 0 COMMENT '父目录ID'," +
                "folder_name VARCHAR(120) NOT NULL COMMENT '目录名称'," +
                "category VARCHAR(50) NULL COMMENT '目录分类'," +
                "sort_order INT NOT NULL DEFAULT 10 COMMENT '排序'," +
                "access_scope VARCHAR(32) NOT NULL DEFAULT 'lab' COMMENT '权限范围'," +
                "archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否归档'," +
                "created_by BIGINT NULL COMMENT '创建人ID'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室空间目录表'");
    }

    private void createLabSpaceFileTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_space_file (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "folder_id BIGINT NOT NULL COMMENT '目录ID'," +
                "file_name VARCHAR(255) NOT NULL COMMENT '文件名称'," +
                "file_url VARCHAR(1024) NOT NULL COMMENT '文件地址'," +
                "file_size BIGINT NULL COMMENT '文件大小'," +
                "file_type VARCHAR(100) NULL COMMENT '文件类型'," +
                "archive_flag TINYINT NOT NULL DEFAULT 0 COMMENT '归档标记'," +
                "access_scope VARCHAR(32) NOT NULL DEFAULT 'lab' COMMENT '权限范围'," +
                "version_no INT NOT NULL DEFAULT 1 COMMENT '版本号'," +
                "upload_user_id BIGINT NULL COMMENT '上传人ID'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (folder_id) REFERENCES t_lab_space_folder(id)," +
                "FOREIGN KEY (upload_user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室空间文件表'");

        addIndexIfMissing("t_lab_space_file", "idx_lab_space_file_scope",
                "ALTER TABLE t_lab_space_file ADD INDEX idx_lab_space_file_scope (lab_id, folder_id, archive_flag)");
    }

    private void createNoticeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_notice (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "title VARCHAR(128) NOT NULL COMMENT '公告标题'," +
                "content LONGTEXT NOT NULL COMMENT '公告内容'," +
                "publish_scope VARCHAR(32) NOT NULL DEFAULT 'school' COMMENT '发布范围'," +
                "college_id BIGINT NULL COMMENT '学院ID'," +
                "lab_id BIGINT NULL COMMENT '实验室ID'," +
                "publisher_id BIGINT NOT NULL COMMENT '发布人ID'," +
                "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态'," +
                "publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (publisher_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表'");
    }

    private void createEquipmentTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_equipment (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '所属实验室ID'," +
                "name VARCHAR(100) NOT NULL COMMENT '设备名称'," +
                "type VARCHAR(50) NOT NULL COMMENT '设备类型'," +
                "serial_number VARCHAR(100) COMMENT '设备编号'," +
                "image_url VARCHAR(255) COMMENT '设备图片'," +
                "description TEXT COMMENT '设备描述'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-空闲 1-借用中 2-维修中'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表'");
    }

    private void createEquipmentBorrowTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_equipment_borrow (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "equipment_id BIGINT NOT NULL COMMENT '设备ID'," +
                "user_id BIGINT NOT NULL COMMENT '借用人ID'," +
                "borrow_time DATETIME COMMENT '借用时间'," +
                "return_time DATETIME COMMENT '归还时间'," +
                "reason VARCHAR(255) COMMENT '借用理由'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-申请中 1-已借出 2-已拒绝 3-已归还'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (equipment_id) REFERENCES t_equipment(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备借用记录表'");
    }

    private void createOutstandingGraduateTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_outstanding_graduate (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '所属实验室ID'," +
                "name VARCHAR(50) NOT NULL COMMENT '姓名'," +
                "major VARCHAR(100) COMMENT '专业'," +
                "graduation_year VARCHAR(20) COMMENT '毕业年份'," +
                "description TEXT COMMENT '介绍'," +
                "avatar_url VARCHAR(255) COMMENT '头像'," +
                "company VARCHAR(100) COMMENT '就业单位'," +
                "position VARCHAR(100) COMMENT '职位'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优秀毕业生表'");
    }

    private void createLabAttendanceTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_attendance (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "user_id BIGINT NOT NULL COMMENT '成员ID'," +
                "attendance_date VARCHAR(20) NOT NULL COMMENT '打卡日期'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未确认 1-已到 2-未到'," +
                "reason VARCHAR(255) COMMENT '原因或备注'," +
                "confirmed_by BIGINT COMMENT '确认管理员ID'," +
                "confirm_time DATETIME COMMENT '确认时间'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_lab_attendance (lab_id, user_id, attendance_date)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室每日打卡表'");
    }

    private void createLabExitApplicationTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_exit_application (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "user_id BIGINT NOT NULL COMMENT '申请人ID'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "reason TEXT NOT NULL COMMENT '退出原因'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待审核 1-已通过 2-已拒绝'," +
                "audit_remark VARCHAR(255) COMMENT '审核备注'," +
                "audit_by BIGINT COMMENT '审核人ID'," +
                "audit_time DATETIME COMMENT '审核时间'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室退出申请表'");
    }

    private void createGrowthPracticeQuestionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_growth_practice_question (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "creator_id BIGINT NOT NULL COMMENT '创建人ID'," +
                "question_type VARCHAR(30) NOT NULL COMMENT '题型: single_choice/fill_blank/programming'," +
                "track_code VARCHAR(50) NOT NULL DEFAULT 'common' COMMENT '成长路径编码'," +
                "title VARCHAR(255) NOT NULL COMMENT '题目标题'," +
                "content LONGTEXT COMMENT '题目内容'," +
                "difficulty VARCHAR(32) COMMENT '难度'," +
                "input_format TEXT COMMENT '输入格式'," +
                "output_format TEXT COMMENT '输出格式'," +
                "sample_case_json TEXT COMMENT '样例 JSON'," +
                "options_json LONGTEXT COMMENT '选项 JSON'," +
                "answer_config LONGTEXT COMMENT '答案配置 JSON'," +
                "program_languages VARCHAR(255) COMMENT '允许语言 JSON'," +
                "judge_case_json LONGTEXT COMMENT '判题用例 JSON'," +
                "tags_json TEXT COMMENT '标签 JSON'," +
                "analysis_hint TEXT COMMENT '解题提示'," +
                "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-停用'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (creator_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长中心共享题库'");

        addIndexIfMissing("t_growth_practice_question", "idx_growth_practice_question_deleted_id",
                "ALTER TABLE t_growth_practice_question ADD INDEX idx_growth_practice_question_deleted_id (deleted, id)");
        addIndexIfMissing("t_growth_practice_question", "idx_growth_practice_question_deleted_status_track_type_id",
                "ALTER TABLE t_growth_practice_question ADD INDEX idx_growth_practice_question_deleted_status_track_type_id (deleted, status, track_code, question_type, id)");
    }

    private void createWrittenExamTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "title VARCHAR(120) NOT NULL COMMENT '笔试标题'," +
                "description TEXT COMMENT '笔试说明'," +
                "start_time DATETIME NOT NULL COMMENT '开始时间'," +
                "end_time DATETIME NOT NULL COMMENT '结束时间'," +
                "pass_score INT NOT NULL DEFAULT 60 COMMENT '通过分数'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-关闭 1-开启'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_written_exam_lab (lab_id)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室笔试配置表'");
    }

    private void createWrittenExamQuestionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam_question (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "exam_id BIGINT NOT NULL COMMENT '笔试ID'," +
                "bank_question_id BIGINT COMMENT '共享题库题目ID'," +
                "question_type VARCHAR(30) NOT NULL COMMENT '题型: single_choice/fill_blank/programming'," +
                "track_code VARCHAR(50) COMMENT '成长路径编码'," +
                "title VARCHAR(255) NOT NULL COMMENT '题目标题'," +
                "content LONGTEXT COMMENT '题目内容'," +
                "difficulty VARCHAR(32) COMMENT '难度'," +
                "input_format TEXT COMMENT '输入格式'," +
                "output_format TEXT COMMENT '输出格式'," +
                "sample_case_json TEXT COMMENT '样例 JSON'," +
                "options_json LONGTEXT COMMENT '选项JSON'," +
                "answer_config LONGTEXT COMMENT '标准答案配置'," +
                "program_languages VARCHAR(255) COMMENT '允许语言JSON'," +
                "judge_case_json LONGTEXT COMMENT '判题用例JSON'," +
                "tags_json TEXT COMMENT '标签 JSON'," +
                "analysis_hint TEXT COMMENT '解题提示'," +
                "score INT NOT NULL DEFAULT 10 COMMENT '分值'," +
                "sort_order INT NOT NULL DEFAULT 0 COMMENT '排序'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (exam_id) REFERENCES t_written_exam(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室笔试题目表'");

        addColumnIfMissing("t_written_exam_question", "bank_question_id",
                "ALTER TABLE t_written_exam_question ADD COLUMN bank_question_id BIGINT COMMENT '共享题库题目ID' AFTER exam_id");
        addColumnIfMissing("t_written_exam_question", "track_code",
                "ALTER TABLE t_written_exam_question ADD COLUMN track_code VARCHAR(50) COMMENT '成长路径编码' AFTER question_type");
        addColumnIfMissing("t_written_exam_question", "difficulty",
                "ALTER TABLE t_written_exam_question ADD COLUMN difficulty VARCHAR(32) COMMENT '难度' AFTER content");
        addColumnIfMissing("t_written_exam_question", "input_format",
                "ALTER TABLE t_written_exam_question ADD COLUMN input_format TEXT COMMENT '输入格式' AFTER difficulty");
        addColumnIfMissing("t_written_exam_question", "output_format",
                "ALTER TABLE t_written_exam_question ADD COLUMN output_format TEXT COMMENT '输出格式' AFTER input_format");
        addColumnIfMissing("t_written_exam_question", "sample_case_json",
                "ALTER TABLE t_written_exam_question ADD COLUMN sample_case_json TEXT COMMENT '样例 JSON' AFTER output_format");
        addColumnIfMissing("t_written_exam_question", "tags_json",
                "ALTER TABLE t_written_exam_question ADD COLUMN tags_json TEXT COMMENT '标签 JSON' AFTER judge_case_json");
        addColumnIfMissing("t_written_exam_question", "analysis_hint",
                "ALTER TABLE t_written_exam_question ADD COLUMN analysis_hint TEXT COMMENT '解题提示' AFTER tags_json");
    }

    private void createWrittenExamSubmissionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam_submission (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "exam_id BIGINT NOT NULL COMMENT '笔试ID'," +
                "lab_id BIGINT NOT NULL COMMENT '实验室ID'," +
                "user_id BIGINT NOT NULL COMMENT '学生ID'," +
                "answer_sheet_json LONGTEXT COMMENT '答卷JSON'," +
                "total_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '总分'," +
                "ai_remark TEXT COMMENT '评分说明'," +
                "admin_remark VARCHAR(255) COMMENT '管理员备注'," +
                "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-待审核 2-已通过 3-未通过'," +
                "submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间'," +
                "grade_time DATETIME COMMENT '批改时间'," +
                "review_time DATETIME COMMENT '审核时间'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_written_exam_submission (exam_id, user_id)," +
                "FOREIGN KEY (exam_id) REFERENCES t_written_exam(id)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室笔试提交表'");
    }

    private void createSystemNotificationTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_system_notification (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "user_id BIGINT NOT NULL COMMENT '接收人ID'," +
                "title VARCHAR(120) NOT NULL COMMENT '通知标题'," +
                "content TEXT NOT NULL COMMENT '通知内容'," +
                "notification_type VARCHAR(40) NOT NULL COMMENT '通知类型'," +
                "related_id BIGINT COMMENT '关联业务ID'," +
                "is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表'");
    }

    private void seedCompetitionCoreData() {
        try {
            upsertDefaultCollege("CS", "计算机与软件工程学院", "安徽信息工程学院学院主数据");
            upsertDefaultCollege("AI", "大数据与人工智能学院", "安徽信息工程学院学院主数据");
            upsertDefaultCollege("EEE", "电气与电子工程学院", "安徽信息工程学院学院主数据");
            upsertDefaultCollege("ME", "机械工程学院", "安徽信息工程学院学院主数据");
            upsertDefaultCollege("MGT", "管理工程学院", "安徽信息工程学院学院主数据");
            upsertDefaultCollege("ART", "艺术设计学院", "安徽信息工程学院学院主数据");
            upsertDefaultCollege("FLA", "通识教育与外国语学院", "安徽信息工程学院学院主数据");

            Long collegeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_college WHERE deleted = 0", Long.class);
            if (collegeCount != null && collegeCount == 0) {
                jdbcTemplate.update("INSERT INTO t_college (college_code, college_name, status, remark) VALUES " +
                        "('CS', '计算机与软件工程学院', 1, '竞赛版默认学院')," +
                        "('AI', '人工智能学院', 1, '竞赛版默认学院')," +
                        "('EE', '电子信息工程学院', 1, '竞赛版默认学院')");
            }

            jdbcTemplate.update("UPDATE t_lab SET college_id = COALESCE(college_id, 1) WHERE deleted = 0");
            jdbcTemplate.update("UPDATE t_lab SET lab_code = COALESCE(lab_code, CONCAT('LAB-', LPAD(id, 3, '0'))) WHERE deleted = 0");
            jdbcTemplate.update("UPDATE t_lab SET teacher_name = COALESCE(NULLIF(teacher_name, ''), '待维护') WHERE deleted = 0");
            jdbcTemplate.update("UPDATE t_lab SET location = COALESCE(NULLIF(location, ''), '待维护') WHERE deleted = 0");

            Long recruitPlanCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recruit_plan WHERE deleted = 0", Long.class);
            if (recruitPlanCount != null && recruitPlanCount == 0) {
                jdbcTemplate.update("INSERT INTO t_recruit_plan (lab_id, title, start_time, end_time, quota, requirement, status, created_by) " +
                        "SELECT id, CONCAT(lab_name, ' 2026 春季招新'), NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), " +
                        "GREATEST(COALESCE(recruit_num, 10), 5), COALESCE(require_skill, '责任心、实践能力、团队协作'), 'open', 1 " +
                        "FROM t_lab WHERE deleted = 0");
            }

            Long schoolNoticeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_notice WHERE deleted = 0 AND title = '系统启用通知' AND publish_scope = 'school'",
                    Long.class
            );
            if (schoolNoticeCount == null || schoolNoticeCount == 0) {
                jdbcTemplate.update("INSERT INTO t_notice (title, content, publish_scope, publisher_id, status, publish_time) VALUES " +
                        "('系统启用通知', '安徽信息工程学院实验室管理平台已启用，请各学院及时完善学院主数据、实验室档案和审批流程配置。', 'school', 1, 1, NOW())");
            } else {
                jdbcTemplate.update(
                        "UPDATE t_notice SET content = ? WHERE deleted = 0 AND title = '系统启用通知' AND publish_scope = 'school'",
                        "安徽信息工程学院实验室管理平台已启用，请各学院及时完善学院主数据、实验室档案和审批流程配置。"
                );
            }

            Long noticeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_notice WHERE deleted = 0", Long.class);
            if (noticeCount != null && noticeCount == 0) {
                jdbcTemplate.update("INSERT INTO t_notice (title, content, publish_scope, publisher_id, status, publish_time) VALUES " +
                        "('系统启用通知', '高校实验室综合管理信息系统已启用，请各学院、实验室及时完善基础资料。', 'school', 1, 1, NOW())");
            }
            Long aiitLabCreateApplyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0", Long.class);
            if (aiitLabCreateApplyCount == null || aiitLabCreateApplyCount == 0) {
                jdbcTemplate.update(
                        "INSERT INTO t_lab_create_apply (applicant_user_id, college_id, lab_name, teacher_name, location, contact_email, research_direction, apply_reason, status) VALUES " +
                                "(2, 1, '智能软件与应用创新实验室', '陈志远', '科创楼 A-301', 'software_lab@aiit.edu.cn', '软件工程、Web 系统与校园数字化应用', '拟面向全校软件类项目实践与实验室协同建设，申请创建校级实验室。', 'submitted')," +
                                "(3, 2, '数据智能决策实验室', '刘敏', '科创楼 B-402', 'ai_lab@aiit.edu.cn', '数据治理、可视化分析与人工智能应用', '已完成学院初审，提交学校管理员终审。', 'college_approved')"
                );
            }

            Long teacherRegisterApplyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_teacher_register_apply WHERE deleted = 0", Long.class);
            if (teacherRegisterApplyCount == null || teacherRegisterApplyCount == 0) {
                jdbcTemplate.update(
                        "INSERT INTO t_teacher_register_apply (teacher_no, password_hash, real_name, college_id, title, phone, email, apply_reason, status, college_audit_by, college_audit_time, college_audit_comment) VALUES " +
                                "('T2026001', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '陈志远', 1, '讲师', '13800000121', 'chen.zhiyuan@aiit.edu.cn', '申请开通教师账号，用于发起实验室创建申请与管理实验室业务。', 'submitted', NULL, NULL, NULL)," +
                                "('T2026002', '$2a$10$TkpWpInMFmWbL4HWZXC7yuMC3szk.vR/ghGvrCEClWQxWBWgFQ6LK', '刘敏', 2, '副教授', '13800000122', 'liu.min@aiit.edu.cn', '学院已完成材料初审，申请进入学校终审。', 'college_approved', 3, NOW(), '学院初审通过')"
                );
            }

            Long labCreateApplyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_lab_create_apply WHERE deleted = 0", Long.class);
            if (labCreateApplyCount != null && labCreateApplyCount == 0) {
                jdbcTemplate.update(
                        "INSERT INTO t_lab_create_apply (applicant_user_id, college_id, lab_name, teacher_name, location, contact_email, research_direction, apply_reason, status) VALUES " +
                                "(2, 1, '智能制造协同实验室', '张维', '实训楼 B-201', 'maker_lab@example.com', '智能制造、工业互联网与数字孪生', '拟面向学院创新实践与竞赛训练组建新实验室。', 'submitted')," +
                                "(3, 2, '数据智能决策实验室', '刘敏', '科技楼 A-402', 'decision_lab@example.com', '数据治理、决策分析与可视化', '已完成学院论证，等待学校级审批。', 'college_approved')"
                );
            }

            seedLabSpaceFolders();
            seedAttendanceRecords();
        } catch (Exception ignored) {
        }
    }

    private void upsertDefaultCollege(String collegeCode, String collegeName, String remark) {
        Long existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_college WHERE college_code = ? AND deleted = 0",
                Long.class,
                collegeCode
        );
        if (existingCount != null && existingCount > 0) {
            jdbcTemplate.update(
                    "UPDATE t_college SET college_name = ?, remark = ?, status = 1 WHERE college_code = ? AND deleted = 0",
                    collegeName, remark, collegeCode
            );
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO t_college (college_code, college_name, status, remark) VALUES (?, ?, 1, ?)",
                collegeCode, collegeName, remark
        );
    }

    private void normalizeSchoolEditionDemoData() {
        try {
            normalizeSeedUser("superadmin", "学校管理员", "super_admin", null, null, null, "13800000001", "superadmin@aiit.edu.cn", null);
            normalizeSeedUser("cs_admin", "计算机与软件工程学院管理员", "admin", null, null, null, "13800000002", "cs_admin@aiit.edu.cn", 1L);
            normalizeSeedUser("ai_admin", "大数据与人工智能学院管理员", "admin", null, null, null, "13800000003", "ai_admin@aiit.edu.cn", 2L);
            normalizeSeedUser("ee_admin", "电气与电子工程学院管理员", "admin", null, null, null, "13800000004", "ee_admin@aiit.edu.cn", 3L);
            normalizeSeedUser("20231001", "张晨", "student", "计算机与软件工程学院", "软件工程", "2023级", "13800000011", "20231001@aiit.edu.cn", 1L);
            normalizeSeedUser("20231002", "李沐", "student", "计算机与软件工程学院", "计算机科学与技术", "2023级", "13800000012", "20231002@aiit.edu.cn", 1L);
            normalizeSeedUser("20231003", "王宁", "student", "大数据与人工智能学院", "人工智能", "2023级", "13800000013", "20231003@aiit.edu.cn", null);
            normalizeSeedUser("20231004", "赵楠", "student", "电气与电子工程学院", "电子信息工程", "2023级", "13800000014", "20231004@aiit.edu.cn", 3L);

            normalizeSeedLab(1L, "智能软件与应用创新实验室", "LAB-CS-001", 1L,
                    "面向 Web 系统、软件工程实践、校园数字化应用与创新创业项目。", "待维护", "科创楼 A-301",
                    "software_lab@aiit.edu.cn", "Java、Spring Boot、Vue、数据库基础",
                    20, 2, "2021-09", "承担多项校级创新创业与竞赛项目",
                    "服务软件工程与计算机方向学生的项目化培养。", "待维护", "cs_admin");
            normalizeSeedLab(2L, "数据智能决策实验室", "LAB-AI-002", 2L,
                    "聚焦数据治理、机器学习、可视化分析和人工智能应用。", "待维护", "科创楼 B-402",
                    "ai_lab@aiit.edu.cn", "Python、机器学习、数据分析基础",
                    18, 1, "2022-03", "支持学院数据智能方向科研与实践训练",
                    "服务大数据与人工智能学院的实验与竞赛协同。", "待维护", "ai_admin");
            normalizeSeedLab(3L, "工业智能与嵌入式系统实验室", "LAB-EEE-003", 3L,
                    "聚焦嵌入式系统、物联网应用、电子信息与工程实践。", "待维护", "工程楼 C-214",
                    "embedded_lab@aiit.edu.cn", "C 语言、单片机、嵌入式基础",
                    16, 1, "2020-06", "面向电子信息和嵌入式方向提供实践平台",
                    "服务电气与电子工程学院的实验教学与竞赛训练。", "待维护", "ee_admin");

            normalizeSeedRecruitPlan(1L, 1L, "智能软件与应用创新实验室 2026 春季招募",
                    "2026-03-01 08:00:00", "2026-04-20 23:59:59", 8,
                    "具备 Java、前端基础或对校园数字化项目有兴趣。", "open", 2L);
            normalizeSeedRecruitPlan(2L, 2L, "数据智能决策实验室 2026 春季招募",
                    "2026-03-05 08:00:00", "2026-04-25 23:59:59", 6,
                    "具备 Python 与数据分析基础，愿意参与 AI 相关项目。", "open", 3L);
            normalizeSeedRecruitPlan(3L, 3L, "工业智能与嵌入式系统实验室 2026 春季招募",
                    "2026-03-10 08:00:00", "2026-04-15 23:59:59", 5,
                    "具备 C 语言、单片机或嵌入式基础优先。", "open", 4L);

            normalizeSeedLabMember(1L, 1L, 5L, "lab_leader", "2026-03-01", 2L, "实验室项目负责人");
            normalizeSeedLabMember(2L, 1L, 6L, "member", "2026-03-05", 2L, "实验室正式成员");
            normalizeSeedLabMember(3L, 3L, 8L, "member", "2026-03-18", 4L, "审核通过自动入组");

            normalizeSeedLabCreateApply(1L, 2L, 1L, "校园软件协同创新实验室", "陈志远",
                    "科创楼 A-315", "campus_software@aiit.edu.cn",
                    "校园数字化、业务协同、软件工程实践",
                    "拟面向校内数字化建设和软件工程训练，创建校级实验室。",
                    "submitted", null, null, null, null, null, null, null);
            normalizeSeedLabCreateApply(2L, 3L, 2L, "智能数据治理实验室", "刘敏",
                    "科创楼 B-420", "data_governance@aiit.edu.cn",
                    "数据治理、智能分析、AI 应用",
                    "学院已完成初审，现提交学校管理员终审。",
                    "college_approved", 1L, "2026-03-18 09:30:00", "学院初审通过", null, null, null, null);

            normalizeSeedNotice(1L, "系统启用通知",
                    "安徽信息工程学院实验室管理平台已启用，请各学院及时完善学院主数据、实验室档案和审批流程配置。",
                    "school", null, null, 1L, "2026-03-20 09:00:00");
            normalizeSeedNotice(2L, "学院实验室建档提醒",
                    "请相关学院在本周内完成实验室负责人、地点、研究方向和招募要求的维护。",
                    "school", null, null, 1L, "2026-03-22 14:00:00");
            normalizeSeedNotice(3L, "智能软件与应用创新实验室宣讲",
                    "本周五晚 19:00 在科创楼 A-301 举行实验室宣讲，欢迎感兴趣同学到场了解。",
                    "lab", 1L, 1L, 2L, "2026-03-24 18:00:00");
            normalizeSeedNotice(4L, "系统启用通知",
                    "安徽信息工程学院实验室管理平台已启用，请各学院及时完善学院主数据、实验室档案和审批流程配置。",
                    "school", null, null, 1L, "2026-03-20 09:00:00");
        } catch (Exception ignored) {
        }
    }

    private void normalizeSeedUser(String username, String realName, String role, String college, String major,
                                   String grade, String phone, String email, Long labId) {
        jdbcTemplate.update(
                "UPDATE t_user SET real_name = ?, role = ?, college = ?, major = ?, grade = ?, phone = ?, email = ?, lab_id = ?, status = 1, deleted = 0 WHERE username = ?",
                realName, role, college, major, grade, phone, email, labId, username
        );
    }

    private void normalizeSeedLab(Long id, String labName, String labCode, Long collegeId, String labDesc,
                                  String teacherName, String location, String contactEmail, String requireSkill,
                                  Integer recruitNum, Integer currentNum, String foundingDate, String awards,
                                  String basicInfo, String advisors, String currentAdmins) {
        jdbcTemplate.update(
                "UPDATE t_lab SET lab_name = ?, lab_code = ?, college_id = ?, lab_desc = ?, teacher_name = ?, location = ?, contact_email = ?, require_skill = ?, recruit_num = ?, current_num = ?, status = 1, founding_date = ?, awards = ?, basic_info = ?, advisors = ?, current_admins = ?, deleted = 0 WHERE id = ?",
                labName, labCode, collegeId, labDesc, teacherName, location, contactEmail, requireSkill,
                recruitNum, currentNum, foundingDate, awards, basicInfo, advisors, currentAdmins, id
        );
    }

    private void normalizeSeedRecruitPlan(Long id, Long labId, String title, String startTime, String endTime,
                                          Integer quota, String requirement, String status, Long createdBy) {
        jdbcTemplate.update(
                "UPDATE t_recruit_plan SET lab_id = ?, title = ?, start_time = ?, end_time = ?, quota = ?, requirement = ?, status = ?, created_by = ?, deleted = 0 WHERE id = ?",
                labId, title, startTime, endTime, quota, requirement, status, createdBy, id
        );
    }

    private void normalizeSeedLabCreateApply(Long id, Long applicantUserId, Long collegeId, String labName,
                                             String teacherName, String location, String contactEmail,
                                             String researchDirection, String applyReason, String status,
                                             Long collegeAuditBy, String collegeAuditTime, String collegeAuditComment,
                                             Long schoolAuditBy, String schoolAuditTime, String schoolAuditComment,
                                             Long generatedLabId) {
        jdbcTemplate.update(
                "UPDATE t_lab_create_apply SET applicant_user_id = ?, college_id = ?, lab_name = ?, teacher_name = ?, location = ?, contact_email = ?, research_direction = ?, apply_reason = ?, status = ?, college_audit_by = ?, college_audit_time = ?, college_audit_comment = ?, school_audit_by = ?, school_audit_time = ?, school_audit_comment = ?, generated_lab_id = ?, deleted = 0 WHERE id = ?",
                applicantUserId, collegeId, labName, teacherName, location, contactEmail, researchDirection,
                applyReason, status, collegeAuditBy, collegeAuditTime, collegeAuditComment,
                schoolAuditBy, schoolAuditTime, schoolAuditComment, generatedLabId, id
        );
    }

    private void normalizeSeedLabMember(Long id, Long labId, Long userId, String memberRole, String joinDate,
                                        Long appointedBy, String remark) {
        jdbcTemplate.update(
                "UPDATE t_lab_member SET lab_id = ?, user_id = ?, member_role = ?, join_date = ?, quit_date = NULL, status = 'active', appointed_by = ?, remark = ?, deleted = 0 WHERE id = ?",
                labId, userId, memberRole, joinDate, appointedBy, remark, id
        );
    }

    private void normalizeSeedNotice(Long id, String title, String content, String publishScope,
                                     Long collegeId, Long labId, Long publisherId, String publishTime) {
        jdbcTemplate.update(
                "UPDATE t_notice SET title = ?, content = ?, publish_scope = ?, college_id = ?, lab_id = ?, publisher_id = ?, status = 1, publish_time = ?, deleted = 0 WHERE id = ?",
                title, content, publishScope, collegeId, labId, publisherId, publishTime, id
        );
    }

    private void normalizeSystemAccountCodes() {
        jdbcTemplate.update(
                "UPDATE t_user SET system_account_code = ? WHERE username = ? AND deleted = 0 AND (system_account_code IS NULL OR system_account_code <> ?)",
                "SYS_SCHOOL_DIRECTOR", "superadmin", "SYS_SCHOOL_DIRECTOR");
        jdbcTemplate.update(
                "UPDATE t_user SET system_account_code = ? WHERE username = ? AND deleted = 0 AND (system_account_code IS NULL OR system_account_code <> ?)",
                "SYS_COLLEGE_MANAGER#1", "cs_admin", "SYS_COLLEGE_MANAGER#1");
        jdbcTemplate.update(
                "UPDATE t_user SET system_account_code = ? WHERE username = ? AND deleted = 0 AND (system_account_code IS NULL OR system_account_code <> ?)",
                "SYS_COLLEGE_MANAGER#2", "ai_admin", "SYS_COLLEGE_MANAGER#2");
        jdbcTemplate.update(
                "UPDATE t_user SET system_account_code = ? WHERE username = ? AND deleted = 0 AND (system_account_code IS NULL OR system_account_code <> ?)",
                "SYS_COLLEGE_MANAGER#3", "ee_admin", "SYS_COLLEGE_MANAGER#3");
    }

    private void seedLabSpaceFolders() {
        Long folderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_lab_space_folder WHERE deleted = 0", Long.class);
        if (folderCount != null && folderCount > 0) {
            return;
        }

        seedLabSpaceFolder("基础档案", "profile", 10);
        seedLabSpaceFolder("招新归档", "recruit", 20);
        seedLabSpaceFolder("成员档案", "member", 30);
        seedLabSpaceFolder("考勤资料", "attendance", 40);
        seedLabSpaceFolder("项目文档", "project", 50);
        seedLabSpaceFolder("会议与活动", "meeting", 60);
        seedLabSpaceFolder("成果档案", "achievement", 70);
        seedLabSpaceFolder("模板与规范", "template", 80);
    }

    private void seedLabSpaceFolder(String folderName, String category, int sortOrder) {
        jdbcTemplate.update(
                "INSERT INTO t_lab_space_folder (lab_id, parent_id, folder_name, category, sort_order, access_scope, archived, created_by) " +
                        "SELECT id, 0, ?, ?, ?, 'lab', 0, NULL FROM t_lab WHERE deleted = 0",
                folderName, category, sortOrder
        );
    }

    private void seedAttendanceRecords() {
        Long attendanceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_lab_attendance WHERE deleted = 0", Long.class);
        if (attendanceCount != null && attendanceCount > 0) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO t_lab_attendance (lab_id, user_id, attendance_date, status, reason, confirmed_by, confirm_time) VALUES " +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '%Y-%m-%d'), 2, '迟到 10 分钟', 2, NOW())," +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y-%m-%d'), 5, '补签通过', 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y-%m-%d'), 4, '请假未批', 2, NOW())," +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '%Y-%m-%d'), 3, '课程冲突', 2, NOW())," +
                        "(3, 8, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '%Y-%m-%d'), 1, NULL, 4, NOW())," +
                        "(3, 8, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d'), 2, '晚到', 4, NOW())"
        );
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    tableName, columnName);
            return !columns.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void expandVarcharColumnIfNeeded(String tableName, String columnName, int expectedLength, String ddl) {
        Integer currentLength = getCharacterMaximumLength(tableName, columnName);
        if (currentLength != null && currentLength < expectedLength) {
            jdbcTemplate.execute(ddl);
        }
    }

    private Integer getCharacterMaximumLength(String tableName, String columnName) {
        try {
            List<Integer> result = jdbcTemplate.query(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    (rs, rowNum) -> rs.getInt(1),
                    tableName, columnName);
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private void addUniqueIndexIfMissing(String tableName, String indexName, String ddl) {
        if (!indexExists(tableName, indexName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String ddl) {
        if (!indexExists(tableName, indexName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureUserUniqueIndex(String indexName, String columnName) {
        if (indexMatchesColumns("t_user", indexName, columnName, "deleted")) {
            return;
        }

        if (indexExists("t_user", indexName)) {
            jdbcTemplate.execute("ALTER TABLE t_user DROP INDEX " + indexName);
        }

        if (!hasUserFieldDuplicateByDeletedState(columnName)) {
            jdbcTemplate.execute("ALTER TABLE t_user ADD UNIQUE KEY " + indexName + " (" + columnName + ", deleted)");
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        try {
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                    "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                    tableName, indexName);
            return !indexes.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean indexMatchesColumns(String tableName, String indexName, String... expectedColumns) {
        try {
            List<String> actualColumns = jdbcTemplate.query(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? " +
                            "ORDER BY SEQ_IN_INDEX",
                    (rs, rowNum) -> rs.getString("COLUMN_NAME"),
                    tableName, indexName);

            if (actualColumns.size() != expectedColumns.length) {
                return false;
            }

            for (int i = 0; i < expectedColumns.length; i++) {
                if (!expectedColumns[i].equalsIgnoreCase(actualColumns.get(i))) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasUserFieldDuplicateByDeletedState(String columnName) {
        try {
            Integer duplicateCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM (" +
                            "SELECT " + columnName + ", deleted FROM t_user " +
                            "WHERE " + columnName + " IS NOT NULL AND " + columnName + " <> '' " +
                            "GROUP BY " + columnName + ", deleted HAVING COUNT(*) > 1" +
                            ") duplicate_values",
                    Integer.class);
            return duplicateCount != null && duplicateCount > 0;
        } catch (Exception e) {
            return true;
        }
    }
}
