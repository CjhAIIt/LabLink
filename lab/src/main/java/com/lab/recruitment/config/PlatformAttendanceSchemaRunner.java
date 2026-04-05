package com.lab.recruitment.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.PlatformPost;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.entity.UserIdentity;
import com.lab.recruitment.mapper.CollegeMapper;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.PlatformPostMapper;
import com.lab.recruitment.mapper.UserIdentityMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.service.impl.UserAccessServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(2)
public class PlatformAttendanceSchemaRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAttendanceSchemaRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserIdentityMapper userIdentityMapper;

    @Autowired
    private PlatformPostMapper platformPostMapper;

    @Autowired
    private LabMapper labMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private UserAccessService userAccessService;

    private final List<String> reportLines = new ArrayList<>();

    @Override
    public void run(String... args) {
        createAccessModelTables();
        createAttendanceModelTables();
        migrateLegacyAccessModel();
        seedAttendanceTaskTemplates();
        writeReport();
    }

    private void createAccessModelTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_user_identity (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "identity_type VARCHAR(32) NOT NULL," +
                "college_id BIGINT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'active'," +
                "remark VARCHAR(255) NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_user_identity_user_type_deleted (user_id, identity_type, deleted)," +
                "KEY idx_user_identity_user_status (user_id, status, deleted)," +
                "CONSTRAINT fk_user_identity_user FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_platform_post (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "post_code VARCHAR(64) NOT NULL," +
                "college_id BIGINT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'active'," +
                "start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "end_time DATETIME NULL," +
                "remark VARCHAR(255) NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "unique_scope VARCHAR(128) GENERATED ALWAYS AS (" +
                "CASE WHEN deleted = 0 AND status = 'active' THEN " +
                "CASE WHEN post_code = 'SCHOOL_DIRECTOR' THEN 'SCHOOL_DIRECTOR' " +
                "ELSE CONCAT(post_code, '#', COALESCE(CAST(college_id AS CHAR), 'GLOBAL')) END " +
                "ELSE NULL END" +
                ") STORED," +
                "UNIQUE KEY uk_platform_post_active_scope (unique_scope)," +
                "KEY idx_platform_post_user_status (user_id, status, deleted)," +
                "CONSTRAINT fk_platform_post_user FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_lab_teacher_relation (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "lab_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "is_primary TINYINT NOT NULL DEFAULT 1," +
                "status VARCHAR(32) NOT NULL DEFAULT 'active'," +
                "remark VARCHAR(255) NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_lab_teacher_relation (lab_id, user_id, deleted)," +
                "KEY idx_lab_teacher_relation_user (user_id, status, deleted)," +
                "CONSTRAINT fk_lab_teacher_relation_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "CONSTRAINT fk_lab_teacher_relation_user FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createAttendanceModelTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_attendance_task (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "college_id BIGINT NOT NULL," +
                "semester_name VARCHAR(64) NOT NULL," +
                "task_name VARCHAR(128) NOT NULL," +
                "description VARCHAR(512) NULL," +
                "start_date DATE NOT NULL," +
                "end_date DATE NOT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'draft'," +
                "published_by BIGINT NULL," +
                "published_time DATETIME NULL," +
                "created_by BIGINT NOT NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "active_scope VARCHAR(128) GENERATED ALWAYS AS (" +
                "CASE WHEN deleted = 0 AND status IN ('draft', 'published') " +
                "THEN CONCAT(CAST(college_id AS CHAR), '#', semester_name) ELSE NULL END" +
                ") STORED," +
                "UNIQUE KEY uk_attendance_task_active_scope (active_scope)," +
                "KEY idx_attendance_task_college_status (college_id, status, deleted)," +
                "CONSTRAINT fk_attendance_task_college FOREIGN KEY (college_id) REFERENCES t_college(id)," +
                "CONSTRAINT fk_attendance_task_creator FOREIGN KEY (created_by) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_attendance_schedule (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "task_id BIGINT NOT NULL," +
                "week_day TINYINT NOT NULL," +
                "sign_in_start TIME NOT NULL," +
                "sign_in_end TIME NOT NULL," +
                "late_threshold_minutes INT NOT NULL DEFAULT 15," +
                "sign_code_length INT NOT NULL DEFAULT 4," +
                "code_ttl_minutes INT NOT NULL DEFAULT 90," +
                "status TINYINT NOT NULL DEFAULT 1," +
                "remark VARCHAR(255) NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_attendance_schedule_week_day (task_id, week_day, deleted)," +
                "CONSTRAINT fk_attendance_schedule_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_attendance_session (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "task_id BIGINT NOT NULL," +
                "schedule_id BIGINT NOT NULL," +
                "lab_id BIGINT NOT NULL," +
                "session_date DATE NOT NULL," +
                "session_code VARCHAR(12) NOT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'pending'," +
                "sign_start_time DATETIME NOT NULL," +
                "sign_end_time DATETIME NOT NULL," +
                "late_time DATETIME NOT NULL," +
                "code_expire_time DATETIME NOT NULL," +
                "generated_by BIGINT NULL," +
                "publish_time DATETIME NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_attendance_session_lab_date (lab_id, session_date, deleted)," +
                "KEY idx_attendance_session_task_status (task_id, session_date, status, deleted)," +
                "CONSTRAINT fk_attendance_session_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)," +
                "CONSTRAINT fk_attendance_session_schedule FOREIGN KEY (schedule_id) REFERENCES t_attendance_schedule(id)," +
                "CONSTRAINT fk_attendance_session_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_attendance_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "session_id BIGINT NOT NULL," +
                "task_id BIGINT NOT NULL," +
                "lab_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "sign_status VARCHAR(32) NOT NULL DEFAULT 'normal'," +
                "sign_code VARCHAR(12) NULL," +
                "sign_time DATETIME NULL," +
                "remark VARCHAR(255) NULL," +
                "source VARCHAR(32) NOT NULL DEFAULT 'student'," +
                "reviewed_by BIGINT NULL," +
                "review_time DATETIME NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_attendance_record_session_user (session_id, user_id, deleted)," +
                "KEY idx_attendance_record_lab_status (lab_id, sign_status, deleted)," +
                "KEY idx_attendance_record_user_time (user_id, deleted, sign_time)," +
                "CONSTRAINT fk_attendance_record_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)," +
                "CONSTRAINT fk_attendance_record_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)," +
                "CONSTRAINT fk_attendance_record_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "CONSTRAINT fk_attendance_record_user FOREIGN KEY (user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_attendance_photo (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "session_id BIGINT NOT NULL," +
                "lab_id BIGINT NOT NULL," +
                "uploader_id BIGINT NOT NULL," +
                "photo_url VARCHAR(1024) NOT NULL," +
                "remark VARCHAR(255) NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "KEY idx_attendance_photo_session (session_id, deleted)," +
                "CONSTRAINT fk_attendance_photo_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)," +
                "CONSTRAINT fk_attendance_photo_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)," +
                "CONSTRAINT fk_attendance_photo_uploader FOREIGN KEY (uploader_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_attendance_duty (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "task_id BIGINT NOT NULL," +
                "session_id BIGINT NOT NULL," +
                "college_id BIGINT NOT NULL," +
                "duty_admin_user_id BIGINT NOT NULL," +
                "backup_admin_user_id BIGINT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'active'," +
                "remark VARCHAR(255) NULL," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT NOT NULL DEFAULT 0," +
                "UNIQUE KEY uk_attendance_duty_session (session_id, deleted)," +
                "KEY idx_attendance_duty_task (task_id, college_id, deleted)," +
                "CONSTRAINT fk_attendance_duty_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)," +
                "CONSTRAINT fk_attendance_duty_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)," +
                "CONSTRAINT fk_attendance_duty_college FOREIGN KEY (college_id) REFERENCES t_college(id)," +
                "CONSTRAINT fk_attendance_duty_admin FOREIGN KEY (duty_admin_user_id) REFERENCES t_user(id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void migrateLegacyAccessModel() {
        migrateUserIdentities();
        migrateSchoolDirector();
        migrateCollegeManagers();
        migrateTeacherRelations();
        migrateLabManagers();
        syncCollegeManagerMirror();
        refreshCompatibilityAccess();
    }

    private void migrateUserIdentities() {
        List<User> users = activeUsers();
        for (User user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            String identityType = inferIdentityType(user);
            QueryWrapper<UserIdentity> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", user.getId())
                    .eq("identity_type", identityType)
                    .eq("deleted", 0)
                    .last("LIMIT 1");
            if (userIdentityMapper.selectOne(wrapper) != null) {
                continue;
            }
            UserIdentity identity = new UserIdentity();
            identity.setUserId(user.getId());
            identity.setIdentityType(identityType);
            identity.setStatus(UserAccessServiceImpl.STATUS_ACTIVE);
            userIdentityMapper.insert(identity);
            reportLines.add("identity: created " + identityType + " for user " + user.getUsername());
        }
    }

    private void migrateSchoolDirector() {
        List<PlatformPost> directors = findActivePosts(UserAccessServiceImpl.POST_SCHOOL_DIRECTOR, null);
        if (directors.size() > 1) {
            PlatformPost keeper = directors.get(0);
            for (int index = 1; index < directors.size(); index++) {
                deactivatePost(directors.get(index), "demoted duplicate school director");
            }
            reportLines.add("cleanup: multiple school directors detected, kept user " + keeper.getUserId());
            return;
        }
        if (!directors.isEmpty()) {
            return;
        }

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("status", 1)
                .eq("role", UserAccessServiceImpl.ROLE_SUPER_ADMIN)
                .orderByAsc("id");
        List<User> legacyDirectors = userMapper.selectList(wrapper);
        if (legacyDirectors.isEmpty()) {
            return;
        }
        createPlatformPost(legacyDirectors.get(0).getId(), UserAccessServiceImpl.POST_SCHOOL_DIRECTOR, null,
                "migrated from legacy super_admin");
        reportLines.add("post: migrated school director user " + legacyDirectors.get(0).getUsername());
        if (legacyDirectors.size() > 1) {
            reportLines.add("cleanup: extra legacy super_admin users kept as non-director records: "
                    + legacyDirectors.stream().skip(1).map(User::getUsername).collect(Collectors.joining(", ")));
        }
    }

    private void migrateCollegeManagers() {
        List<Map<String, Object>> colleges = jdbcTemplate.queryForList(
                "SELECT id, college_name AS collegeName, admin_user_id AS adminUserId FROM t_college WHERE deleted = 0 ORDER BY id");
        for (Map<String, Object> college : colleges) {
            Long collegeId = toLong(college.get("id"));
            Long adminUserId = toLong(college.get("adminUserId"));
            List<PlatformPost> activePosts = findActivePosts(UserAccessServiceImpl.POST_COLLEGE_MANAGER, collegeId);
            if (activePosts.size() > 1) {
                PlatformPost keeper = activePosts.get(0);
                for (int index = 1; index < activePosts.size(); index++) {
                    deactivatePost(activePosts.get(index), "demoted duplicate college manager");
                }
                adminUserId = keeper.getUserId();
                reportLines.add("cleanup: duplicate college managers found for college " + collegeId);
            }
            if (adminUserId == null && !activePosts.isEmpty()) {
                adminUserId = activePosts.get(0).getUserId();
            }
            if (adminUserId == null) {
                continue;
            }
            if (activePosts.isEmpty()) {
                createPlatformPost(adminUserId, UserAccessServiceImpl.POST_COLLEGE_MANAGER, collegeId,
                        "migrated from t_college.admin_user_id");
                reportLines.add("post: migrated college manager for college " + collegeId + " user " + adminUserId);
            }
        }
    }

    private void migrateTeacherRelations() {
        List<Lab> labs = labMapper.selectList(new QueryWrapper<Lab>().eq("deleted", 0));
        for (Lab lab : labs) {
            if (lab == null || lab.getId() == null || !StringUtils.hasText(lab.getTeacherName())) {
                continue;
            }
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.eq("deleted", 0)
                    .eq("real_name", lab.getTeacherName().trim())
                    .last("LIMIT 1");
            User teacher = userMapper.selectOne(userQuery);
            if (teacher == null) {
                continue;
            }
            QueryWrapper<com.lab.recruitment.entity.LabTeacherRelation> relationQuery = new QueryWrapper<>();
            relationQuery.eq("lab_id", lab.getId())
                    .eq("user_id", teacher.getId())
                    .eq("deleted", 0)
                    .last("LIMIT 1");
            if (jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_lab_teacher_relation WHERE lab_id = ? AND user_id = ? AND deleted = 0",
                    Integer.class, lab.getId(), teacher.getId()) > 0) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO t_lab_teacher_relation (lab_id, user_id, is_primary, status, remark) VALUES (?, ?, 1, 'active', ?)",
                    lab.getId(), teacher.getId(), "migrated from t_lab.teacher_name");
            reportLines.add("teacher: linked lab " + lab.getId() + " to teacher user " + teacher.getUsername());
        }
    }

    private void migrateLabManagers() {
        List<Lab> labs = labMapper.selectList(new QueryWrapper<Lab>().eq("deleted", 0).orderByAsc("id"));
        for (Lab lab : labs) {
            User targetManager = pickLabManagerCandidate(lab.getId());
            if (targetManager == null) {
                continue;
            }

            List<LabMember> activeAdmins = labMemberMapper.selectList(new QueryWrapper<LabMember>()
                    .eq("lab_id", lab.getId())
                    .eq("deleted", 0)
                    .eq("status", UserAccessServiceImpl.STATUS_ACTIVE)
                    .eq("member_role", UserAccessServiceImpl.MEMBER_ROLE_LAB_ADMIN));
            for (LabMember activeAdmin : activeAdmins) {
                if (activeAdmin.getUserId() != null && activeAdmin.getUserId().equals(targetManager.getId())) {
                    continue;
                }
                activeAdmin.setMemberRole("member");
                labMemberMapper.updateById(activeAdmin);
            }

            ensureLabMemberRole(lab.getId(), targetManager.getId(), UserAccessServiceImpl.MEMBER_ROLE_LAB_ADMIN,
                    "migrated lab manager");
            userAccessService.refreshCompatibilityAccess(targetManager.getId());
        }
    }

    private void syncCollegeManagerMirror() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT college_id AS collegeId, user_id AS userId FROM t_platform_post " +
                        "WHERE deleted = 0 AND status = 'active' AND post_code = 'COLLEGE_MANAGER'");
        for (Map<String, Object> row : rows) {
            Long collegeId = toLong(row.get("collegeId"));
            Long userId = toLong(row.get("userId"));
            if (collegeId == null) {
                continue;
            }
            jdbcTemplate.update("UPDATE t_college SET admin_user_id = ? WHERE id = ?", userId, collegeId);
        }
    }

    private void refreshCompatibilityAccess() {
        for (User user : activeUsers()) {
            userAccessService.refreshCompatibilityAccess(user.getId());
        }
    }

    private void seedAttendanceTaskTemplates() {
        Integer existingTaskCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_attendance_task WHERE deleted = 0", Integer.class);
        if (existingTaskCount != null && existingTaskCount > 0) {
            return;
        }

        List<Map<String, Object>> collegeManagers = jdbcTemplate.queryForList(
                "SELECT p.college_id AS collegeId, p.user_id AS userId " +
                        "FROM t_platform_post p WHERE p.deleted = 0 AND p.status = 'active' AND p.post_code = 'COLLEGE_MANAGER'");
        LocalDate today = LocalDate.now();
        String semesterName = today.getMonthValue() <= 7 ? today.getYear() + "-spring" : today.getYear() + "-autumn";
        for (Map<String, Object> managerRow : collegeManagers) {
            Long collegeId = toLong(managerRow.get("collegeId"));
            Long userId = toLong(managerRow.get("userId"));
            if (collegeId == null || userId == null) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO t_attendance_task (college_id, semester_name, task_name, description, start_date, end_date, status, published_by, published_time, created_by) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 'published', ?, NOW(), ?)",
                    collegeId,
                    semesterName,
                    "Default attendance task",
                    "Auto-seeded attendance task for refactored workflow",
                    today.minusMonths(1),
                    today.plusMonths(2),
                    userId,
                    userId
            );
            Long taskId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            if (taskId == null) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO t_attendance_schedule (task_id, week_day, sign_in_start, sign_in_end, late_threshold_minutes, sign_code_length, code_ttl_minutes, status, remark) VALUES " +
                            "(?, 1, '19:00:00', '21:00:00', 20, 4, 120, 1, 'Monday evening session')," +
                            "(?, 3, '19:00:00', '21:00:00', 20, 4, 120, 1, 'Wednesday evening session')," +
                            "(?, 5, '19:00:00', '21:00:00', 20, 4, 120, 1, 'Friday evening session')",
                    taskId, taskId, taskId
            );
            reportLines.add("attendance: seeded default task for college " + collegeId);
        }
    }

    private User pickLabManagerCandidate(Long labId) {
        User legacyManager = findLegacyLabManager(labId);
        if (legacyManager != null) {
            return legacyManager;
        }

        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
                "SELECT m.user_id AS userId, m.member_role AS memberRole " +
                        "FROM t_lab_member m " +
                        "WHERE m.lab_id = ? AND m.deleted = 0 AND m.status = 'active' AND m.member_role = 'lab_admin' " +
                        "ORDER BY m.id ASC",
                labId
        );
        for (Map<String, Object> candidateRow : candidates) {
            Long userId = toLong(candidateRow.get("userId"));
            if (userId == null) {
                continue;
            }
            User candidate = userMapper.selectById(userId);
            if (candidate != null && userAccessService.isStudentIdentity(candidate)) {
                continue;
            }
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private User findLegacyLabManager(Long labId) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("lab_id", labId)
                .eq("role", UserAccessServiceImpl.ROLE_ADMIN)
                .orderByAsc("id");
        List<User> users = userMapper.selectList(wrapper);
        return users.isEmpty() ? null : users.get(0);
    }

    private void ensureLabMemberRole(Long labId, Long userId, String memberRole, String remark) {
        QueryWrapper<LabMember> wrapper = new QueryWrapper<>();
        wrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        LabMember member = labMemberMapper.selectOne(wrapper);
        if (member == null) {
            member = new LabMember();
            member.setLabId(labId);
            member.setUserId(userId);
            member.setJoinDate(LocalDate.now());
            member.setStatus(UserAccessServiceImpl.STATUS_ACTIVE);
            member.setMemberRole(memberRole);
            member.setRemark(remark);
            labMemberMapper.insert(member);
            reportLines.add("member: created " + memberRole + " record for user " + userId + " in lab " + labId);
            return;
        }
        member.setStatus(UserAccessServiceImpl.STATUS_ACTIVE);
        member.setMemberRole(memberRole);
        member.setQuitDate(null);
        member.setRemark(remark);
        labMemberMapper.updateById(member);
        reportLines.add("member: updated " + memberRole + " record for user " + userId + " in lab " + labId);
    }

    private void createPlatformPost(Long userId, String postCode, Long collegeId, String remark) {
        PlatformPost post = new PlatformPost();
        post.setUserId(userId);
        post.setPostCode(postCode);
        post.setCollegeId(collegeId);
        post.setStatus(UserAccessServiceImpl.STATUS_ACTIVE);
        post.setStartTime(LocalDateTime.now());
        post.setRemark(remark);
        platformPostMapper.insert(post);
    }

    private List<PlatformPost> findActivePosts(String postCode, Long collegeId) {
        QueryWrapper<PlatformPost> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("status", UserAccessServiceImpl.STATUS_ACTIVE)
                .eq("post_code", postCode)
                .orderByAsc("id");
        if (collegeId == null) {
            wrapper.isNull("college_id");
        } else {
            wrapper.eq("college_id", collegeId);
        }
        return platformPostMapper.selectList(wrapper);
    }

    private void deactivatePost(PlatformPost post, String remark) {
        if (post == null || post.getId() == null) {
            return;
        }
        post.setStatus("inactive");
        post.setEndTime(LocalDateTime.now());
        post.setRemark(remark);
        platformPostMapper.updateById(post);
    }

    private List<User> activeUsers() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("status", 1)
                .orderByAsc("id");
        return userMapper.selectList(wrapper);
    }

    private String inferIdentityType(User user) {
        if (user == null) {
            return UserAccessServiceImpl.IDENTITY_STUDENT;
        }
        if (StringUtils.hasText(user.getStudentId())
                || UserAccessServiceImpl.ROLE_STUDENT.equalsIgnoreCase(user.getRole())) {
            return UserAccessServiceImpl.IDENTITY_STUDENT;
        }
        return UserAccessServiceImpl.IDENTITY_TEACHER;
    }

    private void writeReport() {
        if (reportLines.isEmpty()) {
            return;
        }
        reportLines.sort(Comparator.naturalOrder());
        Path reportPath = Paths.get(System.getProperty("user.dir"), "logs", "platform_refactor_report.txt");
        try {
            Files.createDirectories(reportPath.getParent());
            Files.write(reportPath, reportLines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Failed to write platform refactor report", ex);
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
