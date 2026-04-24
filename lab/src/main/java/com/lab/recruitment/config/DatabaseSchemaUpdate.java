package com.lab.recruitment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
@ConditionalOnProperty(value = "app.schema.runtime-update-enabled", havingValue = "true")
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
        createUnifiedFileTables();
        createNoticeTable();
        createEquipmentTable();
        createEquipmentBorrowTable();
        createOutstandingGraduateTable();
        createLabAttendanceTable();
        createLabExitApplicationTable();
        createAuditLogTable();
        createLabInfoChangeReviewTable();
        updateStudentProfileTable();
        if (growthCenterEnabled) {
            createGrowthPracticeQuestionTable();
        }
        if (writtenExamEnabled) {
            createWrittenExamTable();
            createWrittenExamQuestionTable();
            createWrittenExamSubmissionTable();
            createWrittenExamProgressTable();
        }
        createSystemNotificationTable();
        seedCompetitionCoreData();
        if (normalizeDemoData) {
            normalizeSchoolEditionDemoData();
        }
    }

    private void createCollegeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_college (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "college_code VARCHAR(64) NOT NULL" +
                "college_name VARCHAR(128) NOT NULL" +
                "admin_user_id BIGINT NULL" +
                "status TINYINT NOT NULL DEFAULT 1" +
                "remark VARCHAR(255) NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_college_code (college_code)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void updateLabTable() {
        addColumnIfMissing("t_lab", "lab_code",
                "ALTER TABLE t_lab ADD COLUMN lab_code VARCHAR(64) NULL AFTER lab_name");
        addColumnIfMissing("t_lab", "college_id",
                "ALTER TABLE t_lab ADD COLUMN college_id BIGINT NULL AFTER lab_code");
        addColumnIfMissing("t_lab", "teacher_name",
                "ALTER TABLE t_lab ADD COLUMN teacher_name VARCHAR(64) NULL AFTER lab_desc");
        addColumnIfMissing("t_lab", "location",
                "ALTER TABLE t_lab ADD COLUMN location VARCHAR(128) NULL AFTER teacher_name");
        addColumnIfMissing("t_lab", "contact_email",
                "ALTER TABLE t_lab ADD COLUMN contact_email VARCHAR(128) NULL AFTER location");
        addColumnIfMissing("t_lab", "founding_date",
                "ALTER TABLE t_lab ADD COLUMN founding_date VARCHAR(50)");
        addColumnIfMissing("t_lab", "awards",
                "ALTER TABLE t_lab ADD COLUMN awards TEXT");
        addColumnIfMissing("t_lab", "outstanding_seniors",
                "ALTER TABLE t_lab ADD COLUMN outstanding_seniors TEXT");
        addColumnIfMissing("t_lab", "basic_info",
                "ALTER TABLE t_lab ADD COLUMN basic_info TEXT");
        addColumnIfMissing("t_lab", "advisors",
                "ALTER TABLE t_lab ADD COLUMN advisors VARCHAR(255)");
        addColumnIfMissing("t_lab", "current_admins",
                "ALTER TABLE t_lab ADD COLUMN current_admins VARCHAR(255)");
        addColumnIfMissing("t_lab", "logo_url",
                "ALTER TABLE t_lab ADD COLUMN logo_url VARCHAR(255) NULL AFTER current_admins");
        addColumnIfMissing("t_lab", "cover_image_url",
                "ALTER TABLE t_lab ADD COLUMN cover_image_url VARCHAR(255) NULL AFTER logo_url");
    }

    private void updateUserTable() {
        expandVarcharColumnIfNeeded("t_user", "resume", 1024,
                "ALTER TABLE t_user MODIFY COLUMN resume VARCHAR(1024)");
        addColumnIfMissing("t_user", "system_account_code",
                "ALTER TABLE t_user ADD COLUMN system_account_code VARCHAR(64) NULL AFTER can_edit");

        ensureUserUniqueIndex("uk_user_student_id", "student_id");
        ensureUserUniqueIndex("uk_user_email", "email");
        ensureUserUniqueIndex("uk_user_system_account_code", "system_account_code");
        normalizeSystemAccountCodes();
    }

    private void createEmailAuthCodeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_email_auth_code (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "account VARCHAR(64) NOT NULL" +
                "email VARCHAR(100) NOT NULL" +
                "purpose VARCHAR(32) NOT NULL" +
                "code VARCHAR(6) NOT NULL" +
                "expire_time DATETIME NOT NULL" +
                "is_used TINYINT NOT NULL DEFAULT 0" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addIndexIfMissing("t_email_auth_code", "idx_email_auth_code_lookup",
                "ALTER TABLE t_email_auth_code ADD INDEX idx_email_auth_code_lookup (purpose, email, account, is_used, expire_time)");
    }

    private void updateDeliveryTable() {
        expandVarcharColumnIfNeeded("t_delivery", "attachment_url", 1024,
                "ALTER TABLE t_delivery MODIFY COLUMN attachment_url VARCHAR(1024)");

        addColumnIfMissing("t_delivery", "delivery_attempt_count",
                "ALTER TABLE t_delivery ADD COLUMN delivery_attempt_count INT NOT NULL DEFAULT 1");
        addColumnIfMissing("t_delivery", "withdraw_count",
                "ALTER TABLE t_delivery ADD COLUMN withdraw_count INT NOT NULL DEFAULT 0");
        addColumnIfMissing("t_delivery", "update_time",
                "ALTER TABLE t_delivery ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    }

    private void createRecruitPlanTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_recruit_plan (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "title VARCHAR(128) NOT NULL" +
                "start_time DATETIME NULL" +
                "end_time DATETIME NULL" +
                "quota INT NOT NULL DEFAULT 0" +
                "requirement TEXT NULL" +
                "status VARCHAR(32) NOT NULL DEFAULT 'draft'" +
                "created_by BIGINT NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createLabApplyTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_apply (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "student_user_id BIGINT NOT NULL" +
                "recruit_plan_id BIGINT NULL" +
                "apply_reason TEXT NOT NULL" +
                "research_interest TEXT NULL" +
                "skill_summary TEXT NULL" +
                "status VARCHAR(32) NOT NULL DEFAULT 'submitted'" +
                "audit_by BIGINT NULL" +
                "audit_time DATETIME NULL" +
                "audit_comment VARCHAR(255) NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (student_user_id) REFERENCES t_user(id)," +
                "FOREIGN KEY (recruit_plan_id) REFERENCES t_recruit_plan(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createLabCreateApplyTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_create_apply (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "applicant_user_id BIGINT NOT NULL" +
                "college_id BIGINT NOT NULL" +
                "lab_name VARCHAR(120) NOT NULL" +
                "teacher_name VARCHAR(64) NOT NULL" +
                "location VARCHAR(128) NULL" +
                "contact_email VARCHAR(128) NULL" +
                "research_direction TEXT NULL" +
                "apply_reason TEXT NOT NULL" +
                "status VARCHAR(32) NOT NULL DEFAULT 'submitted'" +
                "college_audit_by BIGINT NULL" +
                "college_audit_time DATETIME NULL" +
                "college_audit_comment VARCHAR(255) NULL" +
                "school_audit_by BIGINT NULL" +
                "school_audit_time DATETIME NULL" +
                "school_audit_comment VARCHAR(255) NULL" +
                "generated_lab_id BIGINT NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (applicant_user_id) REFERENCES t_user(id)," +
                "FOREIGN KEY (college_id) REFERENCES t_college(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createTeacherRegisterApplyTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_teacher_register_apply (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "teacher_no VARCHAR(32) NOT NULL" +
                "password_hash VARCHAR(100) NOT NULL" +
                "real_name VARCHAR(50) NOT NULL" +
                "college_id BIGINT NOT NULL" +
                "title VARCHAR(50) NULL" +
                "phone VARCHAR(20) NULL" +
                "email VARCHAR(100) NOT NULL" +
                "apply_reason VARCHAR(500) NOT NULL" +
                "status VARCHAR(32) NOT NULL DEFAULT 'submitted'" +
                "college_audit_by BIGINT NULL" +
                "college_audit_time DATETIME NULL" +
                "college_audit_comment VARCHAR(255) NULL" +
                "school_audit_by BIGINT NULL" +
                "school_audit_time DATETIME NULL" +
                "school_audit_comment VARCHAR(255) NULL" +
                "generated_user_id BIGINT NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "INDEX idx_teacher_register_apply_lookup (teacher_no, email, status)," +
                "INDEX idx_teacher_register_apply_college (college_id, status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createLabMemberTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_member (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "user_id BIGINT NOT NULL" +
                "member_role VARCHAR(32) NOT NULL DEFAULT 'member'" +
                "join_date DATE NULL" +
                "quit_date DATE NULL" +
                "status VARCHAR(32) NOT NULL DEFAULT 'active'" +
                "appointed_by BIGINT NULL" +
                "remark VARCHAR(255) NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addIndexIfMissing("t_lab_member", "idx_lab_member_lab_user_status",
                "ALTER TABLE t_lab_member ADD INDEX idx_lab_member_lab_user_status (lab_id, user_id, status)");
    }

    private void createLabSpaceFolderTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_space_folder (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "parent_id BIGINT NULL DEFAULT 0" +
                "folder_name VARCHAR(120) NOT NULL" +
                "category VARCHAR(50) NULL" +
                "sort_order INT NOT NULL DEFAULT 10" +
                "access_scope VARCHAR(32) NOT NULL DEFAULT 'lab'" +
                "archived TINYINT NOT NULL DEFAULT 0" +
                "created_by BIGINT NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createLabSpaceFileTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_space_file (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "folder_id BIGINT NOT NULL" +
                "file_name VARCHAR(255) NOT NULL" +
                "file_url VARCHAR(1024) NOT NULL" +
                "file_size BIGINT NULL" +
                "file_type VARCHAR(100) NULL" +
                "archive_flag TINYINT NOT NULL DEFAULT 0" +
                "access_scope VARCHAR(32) NOT NULL DEFAULT 'lab'" +
                "version_no INT NOT NULL DEFAULT 1" +
                "upload_user_id BIGINT NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (folder_id) REFERENCES t_lab_space_folder(id)," +
                "FOREIGN KEY (upload_user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addIndexIfMissing("t_lab_space_file", "idx_lab_space_file_scope",
                "ALTER TABLE t_lab_space_file ADD INDEX idx_lab_space_file_scope (lab_id, folder_id, archive_flag)");
    }

    private void createUnifiedFileTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_file_object (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "file_name VARCHAR(255) NOT NULL," +
                "original_name VARCHAR(255) NOT NULL," +
                "content_type VARCHAR(128) NULL," +
                "file_size BIGINT NOT NULL," +
                "storage_path VARCHAR(1024) NOT NULL," +
                "md5 VARCHAR(32) NULL," +
                "uploaded_by BIGINT NULL," +
                "uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addIndexIfMissing("t_file_object", "idx_file_object_uploaded_time",
                "ALTER TABLE t_file_object ADD INDEX idx_file_object_uploaded_time (uploaded_at, deleted)");
        addIndexIfMissing("t_file_object", "idx_file_object_uploader_time",
                "ALTER TABLE t_file_object ADD INDEX idx_file_object_uploader_time (uploaded_by, uploaded_at, deleted)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_business_file_relation (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "business_type VARCHAR(64) NOT NULL," +
                "business_id BIGINT NOT NULL," +
                "file_id BIGINT NOT NULL," +
                "created_by BIGINT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (file_id) REFERENCES t_file_object(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addIndexIfMissing("t_business_file_relation", "uk_business_file_relation",
                "ALTER TABLE t_business_file_relation ADD UNIQUE INDEX uk_business_file_relation (business_type, business_id, file_id, deleted)");
        addIndexIfMissing("t_business_file_relation", "idx_business_file_relation_scope",
                "ALTER TABLE t_business_file_relation ADD INDEX idx_business_file_relation_scope (business_type, business_id, deleted)");
        addIndexIfMissing("t_business_file_relation", "idx_business_file_relation_file",
                "ALTER TABLE t_business_file_relation ADD INDEX idx_business_file_relation_file (file_id, deleted)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_search_keyword_hot (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "keyword VARCHAR(128) NOT NULL," +
                "scope_level VARCHAR(32) NOT NULL," +
                "college_id BIGINT NULL," +
                "lab_id BIGINT NULL," +
                "search_count BIGINT NOT NULL DEFAULT 1," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addIndexIfMissing("t_search_keyword_hot", "uk_search_keyword_scope",
                "ALTER TABLE t_search_keyword_hot ADD UNIQUE INDEX uk_search_keyword_scope (keyword, scope_level, college_id, lab_id, deleted)");
        addIndexIfMissing("t_search_keyword_hot", "idx_search_keyword_hot_count",
                "ALTER TABLE t_search_keyword_hot ADD INDEX idx_search_keyword_hot_count (search_count, updated_at)");
        addIndexIfMissing("t_search_keyword_hot", "idx_search_keyword_hot_scope",
                "ALTER TABLE t_search_keyword_hot ADD INDEX idx_search_keyword_hot_scope (scope_level, college_id, lab_id, updated_at)");
    }

    private void createNoticeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_notice (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "title VARCHAR(128) NOT NULL" +
                "content LONGTEXT NOT NULL" +
                "publish_scope VARCHAR(32) NOT NULL DEFAULT 'school'" +
                "college_id BIGINT NULL" +
                "lab_id BIGINT NULL" +
                "publisher_id BIGINT NOT NULL" +
                "status TINYINT NOT NULL DEFAULT 1" +
                "publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (publisher_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createEquipmentTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_equipment (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "name VARCHAR(100) NOT NULL" +
                "type VARCHAR(50) NOT NULL" +
                "serial_number VARCHAR(100)" +
                "image_url VARCHAR(255)" +
                "description TEXT" +
                "status TINYINT NOT NULL DEFAULT 0" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createEquipmentBorrowTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_equipment_borrow (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "equipment_id BIGINT NOT NULL" +
                "user_id BIGINT NOT NULL" +
                "borrow_time DATETIME" +
                "return_time DATETIME" +
                "expected_return_time DATETIME NULL" +
                "pickup_time DATETIME NULL" +
                "pickup_confirmed_by BIGINT NULL" +
                "return_apply_time DATETIME NULL" +
                "return_confirmed_by BIGINT NULL" +
                "return_confirm_time DATETIME NULL" +
                "acceptance_checklist TEXT NULL" +
                "reason VARCHAR(255)" +
                "status TINYINT NOT NULL DEFAULT 0" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (equipment_id) REFERENCES t_equipment(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addColumnIfMissing("t_equipment_borrow", "expected_return_time",
                "ALTER TABLE t_equipment_borrow ADD COLUMN expected_return_time DATETIME NULL AFTER return_time");
        addColumnIfMissing("t_equipment_borrow", "pickup_time",
                "ALTER TABLE t_equipment_borrow ADD COLUMN pickup_time DATETIME NULL AFTER expected_return_time");
        addColumnIfMissing("t_equipment_borrow", "pickup_confirmed_by",
                "ALTER TABLE t_equipment_borrow ADD COLUMN pickup_confirmed_by BIGINT NULL AFTER pickup_time");
        addColumnIfMissing("t_equipment_borrow", "return_apply_time",
                "ALTER TABLE t_equipment_borrow ADD COLUMN return_apply_time DATETIME NULL AFTER pickup_confirmed_by");
        addColumnIfMissing("t_equipment_borrow", "return_confirmed_by",
                "ALTER TABLE t_equipment_borrow ADD COLUMN return_confirmed_by BIGINT NULL AFTER return_apply_time");
        addColumnIfMissing("t_equipment_borrow", "return_confirm_time",
                "ALTER TABLE t_equipment_borrow ADD COLUMN return_confirm_time DATETIME NULL AFTER return_confirmed_by");
        addColumnIfMissing("t_equipment_borrow", "acceptance_checklist",
                "ALTER TABLE t_equipment_borrow ADD COLUMN acceptance_checklist TEXT NULL AFTER return_confirm_time");
    }

    private void createOutstandingGraduateTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_outstanding_graduate (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "lab_id BIGINT NOT NULL," +
                "name VARCHAR(50) NOT NULL," +
                "major VARCHAR(100)," +
                "graduation_year VARCHAR(20)," +
                "description TEXT," +
                "avatar_url VARCHAR(255)," +
                "cover_image_url VARCHAR(255)," +
                "company VARCHAR(100)," +
                "position VARCHAR(100)," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("t_outstanding_graduate", "cover_image_url",
                "ALTER TABLE t_outstanding_graduate ADD COLUMN cover_image_url VARCHAR(255) NULL AFTER avatar_url");
    }

    private void createLabAttendanceTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_attendance (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "user_id BIGINT NOT NULL" +
                "attendance_date VARCHAR(20) NOT NULL" +
                "status TINYINT NOT NULL DEFAULT 0" +
                "reason VARCHAR(255)" +
                "confirmed_by BIGINT" +
                "confirm_time DATETIME" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_lab_attendance (lab_id, user_id, attendance_date)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createLabExitApplicationTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_exit_application (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "user_id BIGINT NOT NULL" +
                "lab_id BIGINT NOT NULL" +
                "reason TEXT NOT NULL" +
                "status TINYINT NOT NULL DEFAULT 0" +
                "audit_remark VARCHAR(255)" +
                "audit_by BIGINT" +
                "audit_time DATETIME" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createAuditLogTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_audit_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "actor_user_id BIGINT NULL" +
                "action VARCHAR(64) NOT NULL" +
                "target_type VARCHAR(64) NULL" +
                "target_id BIGINT NULL" +
                "detail TEXT NULL" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "INDEX idx_audit_log_action_time (action, create_time)," +
                "INDEX idx_audit_log_target (target_type, target_id, create_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
    private void createLabInfoChangeReviewTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_info_change_review (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "lab_id BIGINT NOT NULL," +
                "version_no INT NOT NULL," +
                "applicant_user_id BIGINT NOT NULL," +
                "reviewer_id BIGINT NULL," +
                "review_status VARCHAR(32) NOT NULL," +
                "review_comment VARCHAR(500) NULL," +
                "review_snapshot LONGTEXT NOT NULL," +
                "old_snapshot LONGTEXT NULL," +
                "review_time DATETIME NULL," +
                "created_by BIGINT NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_by BIGINT NULL," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "version INT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_lab_info_change_review_version (lab_id, version_no)," +
                "KEY idx_lab_info_change_review_status (review_status, create_time)," +
                "KEY idx_lab_info_change_review_lab (lab_id, create_time)," +
                "KEY idx_lab_info_change_review_applicant (applicant_user_id, create_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfMissing("t_lab_info_change_review", "old_snapshot",
                "ALTER TABLE t_lab_info_change_review ADD COLUMN old_snapshot LONGTEXT NULL AFTER review_snapshot");
    }
    private void updateStudentProfileTable() {
        addColumnIfMissing("t_student_profile", "attachment_url",
                "ALTER TABLE t_student_profile ADD COLUMN attachment_url VARCHAR(1024) NULL AFTER introduction");
    }
    private void createGrowthPracticeQuestionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_growth_practice_question (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "creator_id BIGINT NOT NULL" +
                "question_type VARCHAR(30) NOT NULL" +
                "track_code VARCHAR(50) NOT NULL DEFAULT 'common'" +
                "title VARCHAR(255) NOT NULL" +
                "content LONGTEXT" +
                "difficulty VARCHAR(32)" +
                "input_format TEXT" +
                "output_format TEXT" +
                "sample_case_json TEXT" +
                "options_json LONGTEXT" +
                "answer_config LONGTEXT" +
                "program_languages VARCHAR(255)" +
                "judge_case_json LONGTEXT" +
                "tags_json TEXT" +
                "analysis_hint TEXT" +
                "status TINYINT NOT NULL DEFAULT 1" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (creator_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addIndexIfMissing("t_growth_practice_question", "idx_growth_practice_question_deleted_id",
                "ALTER TABLE t_growth_practice_question ADD INDEX idx_growth_practice_question_deleted_id (deleted, id)");
        addIndexIfMissing("t_growth_practice_question", "idx_growth_practice_question_deleted_status_track_type_id",
                "ALTER TABLE t_growth_practice_question ADD INDEX idx_growth_practice_question_deleted_status_track_type_id (deleted, status, track_code, question_type, id)");
    }

    private void createWrittenExamTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "lab_id BIGINT NOT NULL" +
                "title VARCHAR(120) NOT NULL" +
                "description TEXT" +
                "start_time DATETIME NOT NULL" +
                "end_time DATETIME NOT NULL" +
                "pass_score INT NOT NULL DEFAULT 60" +
                "status TINYINT NOT NULL DEFAULT 0" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_written_exam_lab (lab_id)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createWrittenExamQuestionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam_question (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "exam_id BIGINT NOT NULL" +
                "bank_question_id BIGINT" +
                "question_type VARCHAR(30) NOT NULL" +
                "track_code VARCHAR(50)" +
                "title VARCHAR(255) NOT NULL" +
                "content LONGTEXT" +
                "difficulty VARCHAR(32)" +
                "input_format TEXT" +
                "output_format TEXT" +
                "sample_case_json TEXT" +
                "options_json LONGTEXT" +
                "answer_config LONGTEXT" +
                "program_languages VARCHAR(255)" +
                "judge_case_json LONGTEXT" +
                "tags_json TEXT" +
                "analysis_hint TEXT" +
                "score INT NOT NULL DEFAULT 10" +
                "sort_order INT NOT NULL DEFAULT 0" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (exam_id) REFERENCES t_written_exam(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addColumnIfMissing("t_written_exam_question", "bank_question_id",
                "ALTER TABLE t_written_exam_question ADD COLUMN bank_question_id BIGINT AFTER exam_id");
        addColumnIfMissing("t_written_exam_question", "track_code",
                "ALTER TABLE t_written_exam_question ADD COLUMN track_code VARCHAR(50) AFTER question_type");
        addColumnIfMissing("t_written_exam_question", "difficulty",
                "ALTER TABLE t_written_exam_question ADD COLUMN difficulty VARCHAR(32) AFTER content");
        addColumnIfMissing("t_written_exam_question", "input_format",
                "ALTER TABLE t_written_exam_question ADD COLUMN input_format TEXT AFTER difficulty");
        addColumnIfMissing("t_written_exam_question", "output_format",
                "ALTER TABLE t_written_exam_question ADD COLUMN output_format TEXT AFTER input_format");
        addColumnIfMissing("t_written_exam_question", "sample_case_json",
                "ALTER TABLE t_written_exam_question ADD COLUMN sample_case_json TEXT AFTER output_format");
        addColumnIfMissing("t_written_exam_question", "tags_json",
                "ALTER TABLE t_written_exam_question ADD COLUMN tags_json TEXT AFTER judge_case_json");
        addColumnIfMissing("t_written_exam_question", "analysis_hint",
                "ALTER TABLE t_written_exam_question ADD COLUMN analysis_hint TEXT AFTER tags_json");
    }

    private void createWrittenExamSubmissionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam_submission (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "exam_id BIGINT NOT NULL" +
                "lab_id BIGINT NOT NULL" +
                "user_id BIGINT NOT NULL" +
                "answer_sheet_json LONGTEXT" +
                "total_score DECIMAL(6,2) NOT NULL DEFAULT 0" +
                "ai_remark TEXT" +
                "admin_remark VARCHAR(255)" +
                "status TINYINT NOT NULL DEFAULT 1" +
                "submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                "grade_time DATETIME" +
                "review_time DATETIME" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_written_exam_submission (exam_id, user_id)," +
                "FOREIGN KEY (exam_id) REFERENCES t_written_exam(id)," +
                "FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createWrittenExamProgressTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_written_exam_progress (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "exam_id BIGINT NOT NULL," +
                "student_id BIGINT NOT NULL," +
                "answers_json JSON NULL," +
                "remaining_seconds INT NULL," +
                "current_index INT NOT NULL DEFAULT 0," +
                "flagged_ids JSON NULL," +
                "save_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_exam_student (exam_id, student_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createSystemNotificationTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_system_notification (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                "user_id BIGINT NOT NULL" +
                "title VARCHAR(120) NOT NULL" +
                "content TEXT NOT NULL" +
                "notification_type VARCHAR(40) NOT NULL" +
                "related_id BIGINT" +
                "is_read TINYINT NOT NULL DEFAULT 0" +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
    private void seedCompetitionCoreData() {
        try {
            upsertDefaultCollege("CS", "School of Computer Science", "default seed");
            upsertDefaultCollege("AI", "School of AI and Data", "default seed");
            upsertDefaultCollege("EEE", "School of Electrical Engineering", "default seed");
            upsertDefaultCollege("ME", "School of Mechanical Engineering", "default seed");
            upsertDefaultCollege("MGT", "School of Management", "default seed");
            upsertDefaultCollege("ART", "School of Art and Design", "default seed");
            upsertDefaultCollege("FLA", "School of Liberal Arts", "default seed");
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
            normalizeSystemAccountCodes();
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

        seedLabSpaceFolder("Profiles", "profile", 10);
        seedLabSpaceFolder("Recruitment", "recruit", 20);
        seedLabSpaceFolder("Members", "member", 30);
        seedLabSpaceFolder("Attendance", "attendance", 40);
        seedLabSpaceFolder("Projects", "project", 50);
        seedLabSpaceFolder("Meetings", "meeting", 60);
        seedLabSpaceFolder("Achievements", "achievement", 70);
        seedLabSpaceFolder("Templates", "template", 80);
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
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '%Y-%m-%d'), 2, '閺夆晝鍠庨崺?10 闁告帒妫濋幐?, 2, NOW())," +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y-%m-%d'), 5, '閻炴稏鍎抽鐑芥焻濮樺磭绠?, 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y-%m-%d'), 4, '閻犲洤鍢叉禍锝夊嫉椤忓懎顥?, 2, NOW())," +
                        "(1, 5, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '%Y-%m-%d'), 1, NULL, 2, NOW())," +
                        "(1, 6, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '%Y-%m-%d'), 3, '閻犲洤澧介埢濂稿礃閼碱剛宕?, 2, NOW())," +
                        "(3, 8, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '%Y-%m-%d'), 1, NULL, 4, NOW())," +
                        "(3, 8, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d'), 2, '闁哄懏鑹鹃崺?, 4, NOW())"
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
