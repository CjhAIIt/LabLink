UPDATE t_lab l
LEFT JOIN t_college c ON c.id = l.college_id
SET l.college_id = NULL
WHERE l.college_id IS NOT NULL AND c.id IS NULL;

UPDATE t_user u
LEFT JOIN t_lab l ON l.id = u.lab_id
SET u.lab_id = NULL
WHERE u.lab_id IS NOT NULL AND l.id IS NULL;

DELETE d
FROM t_delivery d
LEFT JOIN t_user u ON u.id = d.user_id
WHERE u.id IS NULL;

DELETE d
FROM t_delivery d
LEFT JOIN t_lab l ON l.id = d.lab_id
WHERE l.id IS NULL;

DELETE rp
FROM t_recruit_plan rp
LEFT JOIN t_lab l ON l.id = rp.lab_id
WHERE l.id IS NULL;

UPDATE t_lab_apply la
LEFT JOIN t_recruit_plan rp ON rp.id = la.recruit_plan_id
SET la.recruit_plan_id = NULL
WHERE la.recruit_plan_id IS NOT NULL AND rp.id IS NULL;

DELETE la
FROM t_lab_apply la
LEFT JOIN t_lab l ON l.id = la.lab_id
WHERE l.id IS NULL;

DELETE la
FROM t_lab_apply la
LEFT JOIN t_user u ON u.id = la.student_user_id
WHERE u.id IS NULL;

DELETE lca
FROM t_lab_create_apply lca
LEFT JOIN t_user u ON u.id = lca.applicant_user_id
WHERE u.id IS NULL;

DELETE lca
FROM t_lab_create_apply lca
LEFT JOIN t_college c ON c.id = lca.college_id
WHERE c.id IS NULL;

DELETE lm
FROM t_lab_member lm
LEFT JOIN t_lab l ON l.id = lm.lab_id
WHERE l.id IS NULL;

DELETE lm
FROM t_lab_member lm
LEFT JOIN t_user u ON u.id = lm.user_id
WHERE u.id IS NULL;

DELETE lsf
FROM t_lab_space_folder lsf
LEFT JOIN t_lab l ON l.id = lsf.lab_id
WHERE l.id IS NULL;

UPDATE t_lab_space_file lsf
LEFT JOIN t_user u ON u.id = lsf.upload_user_id
SET lsf.upload_user_id = NULL
WHERE lsf.upload_user_id IS NOT NULL AND u.id IS NULL;

DELETE lsf
FROM t_lab_space_file lsf
LEFT JOIN t_lab l ON l.id = lsf.lab_id
WHERE l.id IS NULL;

DELETE lsf
FROM t_lab_space_file lsf
LEFT JOIN t_lab_space_folder folder ON folder.id = lsf.folder_id
WHERE folder.id IS NULL;

DELETE la
FROM t_lab_attendance la
LEFT JOIN t_lab l ON l.id = la.lab_id
WHERE l.id IS NULL;

DELETE la
FROM t_lab_attendance la
LEFT JOIN t_user u ON u.id = la.user_id
WHERE u.id IS NULL;

DELETE lea
FROM t_lab_exit_application lea
LEFT JOIN t_lab l ON l.id = lea.lab_id
WHERE l.id IS NULL;

DELETE lea
FROM t_lab_exit_application lea
LEFT JOIN t_user u ON u.id = lea.user_id
WHERE u.id IS NULL;

DELETE n
FROM t_notice n
LEFT JOIN t_user u ON u.id = n.publisher_id
WHERE u.id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab ADD CONSTRAINT fk_lab_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_user' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_user ADD CONSTRAINT fk_user_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_delivery' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_delivery ADD CONSTRAINT fk_delivery_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_delivery' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_delivery ADD CONSTRAINT fk_delivery_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_recruit_plan' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_recruit_plan ADD CONSTRAINT fk_recruit_plan_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_apply' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_apply ADD CONSTRAINT fk_lab_apply_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_apply' AND column_name = 'student_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_apply ADD CONSTRAINT fk_lab_apply_student_user FOREIGN KEY (student_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_apply' AND column_name = 'recruit_plan_id'
      AND referenced_table_name = 't_recruit_plan' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_apply ADD CONSTRAINT fk_lab_apply_recruit_plan FOREIGN KEY (recruit_plan_id) REFERENCES t_recruit_plan(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_create_apply' AND column_name = 'applicant_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_create_apply ADD CONSTRAINT fk_lab_create_apply_user FOREIGN KEY (applicant_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_create_apply' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_create_apply ADD CONSTRAINT fk_lab_create_apply_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_member' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_member ADD CONSTRAINT fk_lab_member_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_member' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_member ADD CONSTRAINT fk_lab_member_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_folder' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_folder ADD CONSTRAINT fk_lab_space_folder_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_file' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_file ADD CONSTRAINT fk_lab_space_file_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_file' AND column_name = 'folder_id'
      AND referenced_table_name = 't_lab_space_folder' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_file ADD CONSTRAINT fk_lab_space_file_folder FOREIGN KEY (folder_id) REFERENCES t_lab_space_folder(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_space_file' AND column_name = 'upload_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_space_file ADD CONSTRAINT fk_lab_space_file_upload_user FOREIGN KEY (upload_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_attendance' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_attendance ADD CONSTRAINT fk_lab_attendance_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_attendance' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_attendance ADD CONSTRAINT fk_lab_attendance_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_exit_application' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_exit_application ADD CONSTRAINT fk_lab_exit_application_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_exit_application' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_exit_application ADD CONSTRAINT fk_lab_exit_application_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_notice' AND column_name = 'publisher_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_notice ADD CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE sp
FROM t_student_profile sp
LEFT JOIN t_user u ON u.id = sp.user_id
WHERE u.id IS NULL;

UPDATE t_student_profile sp
LEFT JOIN t_lab l ON l.id = sp.lab_id
SET sp.lab_id = NULL
WHERE sp.lab_id IS NOT NULL AND l.id IS NULL;

UPDATE t_student_profile sp
LEFT JOIN t_college c ON c.id = sp.college_id
SET sp.college_id = NULL
WHERE sp.college_id IS NOT NULL AND c.id IS NULL;

DELETE spr
FROM t_student_profile_review spr
LEFT JOIN t_student_profile sp ON sp.id = spr.profile_id
WHERE sp.id IS NULL;

UPDATE t_student_profile_review spr
LEFT JOIN t_user u ON u.id = spr.reviewer_id
SET spr.reviewer_id = NULL
WHERE spr.reviewer_id IS NOT NULL AND u.id IS NULL;

DELETE spa
FROM t_student_profile_archive spa
LEFT JOIN t_student_profile sp ON sp.id = spa.profile_id
WHERE sp.id IS NULL;

UPDATE t_student_profile_archive spa
LEFT JOIN t_user u ON u.id = spa.archived_by
SET spa.archived_by = NULL
WHERE spa.archived_by IS NOT NULL AND u.id IS NULL;

DELETE ec
FROM t_equipment_category ec
LEFT JOIN t_lab l ON l.id = ec.lab_id
WHERE l.id IS NULL;

UPDATE t_equipment e
LEFT JOIN t_equipment_category ec ON ec.id = e.category_id
SET e.category_id = NULL
WHERE e.category_id IS NOT NULL AND ec.id IS NULL;

DELETE e
FROM t_equipment e
LEFT JOIN t_lab l ON l.id = e.lab_id
WHERE l.id IS NULL;

DELETE eb
FROM t_equipment_borrow eb
LEFT JOIN t_equipment e ON e.id = eb.equipment_id
WHERE e.id IS NULL;

DELETE eb
FROM t_equipment_borrow eb
LEFT JOIN t_user u ON u.id = eb.user_id
WHERE u.id IS NULL;

DELETE em
FROM t_equipment_maintenance em
LEFT JOIN t_equipment e ON e.id = em.equipment_id
WHERE e.id IS NULL;

DELETE em
FROM t_equipment_maintenance em
LEFT JOIN t_lab l ON l.id = em.lab_id
WHERE l.id IS NULL;

DELETE sn
FROM t_system_notification sn
LEFT JOIN t_user u ON u.id = sn.user_id
WHERE u.id IS NULL;

DELETE ui
FROM t_user_identity ui
LEFT JOIN t_user u ON u.id = ui.user_id
WHERE u.id IS NULL;

DELETE pp
FROM t_platform_post pp
LEFT JOIN t_user u ON u.id = pp.user_id
WHERE u.id IS NULL;

DELETE ltr
FROM t_lab_teacher_relation ltr
LEFT JOIN t_lab l ON l.id = ltr.lab_id
WHERE l.id IS NULL;

DELETE ltr
FROM t_lab_teacher_relation ltr
LEFT JOIN t_user u ON u.id = ltr.user_id
WHERE u.id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile ADD CONSTRAINT fk_student_profile_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile ADD CONSTRAINT fk_student_profile_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile ADD CONSTRAINT fk_student_profile_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_review' AND column_name = 'profile_id'
      AND referenced_table_name = 't_student_profile' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_review ADD CONSTRAINT fk_student_profile_review_profile FOREIGN KEY (profile_id) REFERENCES t_student_profile(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_review' AND column_name = 'reviewer_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_review ADD CONSTRAINT fk_student_profile_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_archive' AND column_name = 'profile_id'
      AND referenced_table_name = 't_student_profile' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_archive ADD CONSTRAINT fk_student_profile_archive_profile FOREIGN KEY (profile_id) REFERENCES t_student_profile(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_student_profile_archive' AND column_name = 'archived_by'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_student_profile_archive ADD CONSTRAINT fk_student_profile_archive_archived_by FOREIGN KEY (archived_by) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_category' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_category ADD CONSTRAINT fk_equipment_category_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment ADD CONSTRAINT fk_equipment_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment' AND column_name = 'category_id'
      AND referenced_table_name = 't_equipment_category' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment ADD CONSTRAINT fk_equipment_category FOREIGN KEY (category_id) REFERENCES t_equipment_category(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'equipment_id'
      AND referenced_table_name = 't_equipment' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_borrow ADD CONSTRAINT fk_equipment_borrow_equipment FOREIGN KEY (equipment_id) REFERENCES t_equipment(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_borrow' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_borrow ADD CONSTRAINT fk_equipment_borrow_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_maintenance' AND column_name = 'equipment_id'
      AND referenced_table_name = 't_equipment' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_maintenance ADD CONSTRAINT fk_equipment_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES t_equipment(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_equipment_maintenance' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_equipment_maintenance ADD CONSTRAINT fk_equipment_maintenance_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_system_notification' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_system_notification ADD CONSTRAINT fk_system_notification_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_user_identity' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_user_identity ADD CONSTRAINT fk_user_identity_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_platform_post' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_platform_post ADD CONSTRAINT fk_platform_post_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_teacher_relation' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_teacher_relation ADD CONSTRAINT fk_lab_teacher_relation_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_lab_teacher_relation' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_lab_teacher_relation ADD CONSTRAINT fk_lab_teacher_relation_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE att
FROM t_attendance_task att
LEFT JOIN t_college c ON c.id = att.college_id
WHERE c.id IS NULL;

DELETE att
FROM t_attendance_task att
LEFT JOIN t_user u ON u.id = att.created_by
WHERE u.id IS NULL;

DELETE ats
FROM t_attendance_schedule ats
LEFT JOIN t_attendance_task att ON att.id = ats.task_id
WHERE att.id IS NULL;

DELETE ase
FROM t_attendance_session ase
LEFT JOIN t_attendance_task att ON att.id = ase.task_id
WHERE att.id IS NULL;

DELETE ase
FROM t_attendance_session ase
LEFT JOIN t_attendance_schedule ats ON ats.id = ase.schedule_id
WHERE ats.id IS NULL;

DELETE ase
FROM t_attendance_session ase
LEFT JOIN t_lab l ON l.id = ase.lab_id
WHERE l.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_attendance_session ase ON ase.id = ar.session_id
WHERE ase.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_attendance_task att ON att.id = ar.task_id
WHERE att.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_lab l ON l.id = ar.lab_id
WHERE l.id IS NULL;

DELETE ar
FROM t_attendance_record ar
LEFT JOIN t_user u ON u.id = ar.user_id
WHERE u.id IS NULL;

DELETE ap
FROM t_attendance_photo ap
LEFT JOIN t_attendance_session ase ON ase.id = ap.session_id
WHERE ase.id IS NULL;

DELETE ap
FROM t_attendance_photo ap
LEFT JOIN t_lab l ON l.id = ap.lab_id
WHERE l.id IS NULL;

DELETE ap
FROM t_attendance_photo ap
LEFT JOIN t_user u ON u.id = ap.uploader_id
WHERE u.id IS NULL;

UPDATE t_attendance_duty ad
LEFT JOIN t_user backup_user ON backup_user.id = ad.backup_admin_user_id
SET ad.backup_admin_user_id = NULL
WHERE ad.backup_admin_user_id IS NOT NULL AND backup_user.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_attendance_task att ON att.id = ad.task_id
WHERE att.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_attendance_session ase ON ase.id = ad.session_id
WHERE ase.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_college c ON c.id = ad.college_id
WHERE c.id IS NULL;

DELETE ad
FROM t_attendance_duty ad
LEFT JOIN t_user u ON u.id = ad.duty_admin_user_id
WHERE u.id IS NULL;

UPDATE t_attendance_leave al
LEFT JOIN t_user reviewer ON reviewer.id = al.reviewed_by
SET al.reviewed_by = NULL
WHERE al.reviewed_by IS NOT NULL AND reviewer.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_attendance_session ase ON ase.id = al.session_id
WHERE ase.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_attendance_task att ON att.id = al.task_id
WHERE att.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_lab l ON l.id = al.lab_id
WHERE l.id IS NULL;

DELETE al
FROM t_attendance_leave al
LEFT JOIN t_user u ON u.id = al.user_id
WHERE u.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_attendance_session ase ON ase.id = acl.session_id
WHERE ase.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_attendance_record ar ON ar.id = acl.record_id
WHERE ar.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_lab l ON l.id = acl.lab_id
WHERE l.id IS NULL;

DELETE acl
FROM t_attendance_change_log acl
LEFT JOIN t_user u ON u.id = acl.user_id
WHERE u.id IS NULL;

DELETE bfr
FROM t_business_file_relation bfr
LEFT JOIN t_file_object fo ON fo.id = bfr.file_id
WHERE fo.id IS NULL;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_task' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_task ADD CONSTRAINT fk_attendance_task_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_task' AND column_name = 'created_by'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_task ADD CONSTRAINT fk_attendance_task_creator FOREIGN KEY (created_by) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_schedule' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_schedule ADD CONSTRAINT fk_attendance_schedule_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_session ADD CONSTRAINT fk_attendance_session_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND column_name = 'schedule_id'
      AND referenced_table_name = 't_attendance_schedule' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_session ADD CONSTRAINT fk_attendance_session_schedule FOREIGN KEY (schedule_id) REFERENCES t_attendance_schedule(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_session' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_session ADD CONSTRAINT fk_attendance_session_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_record' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_record ADD CONSTRAINT fk_attendance_record_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_photo' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_photo ADD CONSTRAINT fk_attendance_photo_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_photo' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_photo ADD CONSTRAINT fk_attendance_photo_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_photo' AND column_name = 'uploader_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_photo ADD CONSTRAINT fk_attendance_photo_uploader FOREIGN KEY (uploader_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'college_id'
      AND referenced_table_name = 't_college' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_college FOREIGN KEY (college_id) REFERENCES t_college(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_duty' AND column_name = 'duty_admin_user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_duty ADD CONSTRAINT fk_attendance_duty_admin FOREIGN KEY (duty_admin_user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'task_id'
      AND referenced_table_name = 't_attendance_task' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_task FOREIGN KEY (task_id) REFERENCES t_attendance_task(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_leave' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_leave ADD CONSTRAINT fk_attendance_leave_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'session_id'
      AND referenced_table_name = 't_attendance_session' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_session FOREIGN KEY (session_id) REFERENCES t_attendance_session(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'record_id'
      AND referenced_table_name = 't_attendance_record' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_record FOREIGN KEY (record_id) REFERENCES t_attendance_record(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'lab_id'
      AND referenced_table_name = 't_lab' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_lab FOREIGN KEY (lab_id) REFERENCES t_lab(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_attendance_change_log' AND column_name = 'user_id'
      AND referenced_table_name = 't_user' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_attendance_change_log ADD CONSTRAINT fk_attendance_change_log_user FOREIGN KEY (user_id) REFERENCES t_user(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 't_business_file_relation' AND column_name = 'file_id'
      AND referenced_table_name = 't_file_object' AND referenced_column_name = 'id'
);
SET @ddl = IF(@fk_exists = 0, 'ALTER TABLE t_business_file_relation ADD CONSTRAINT fk_business_file_relation_file FOREIGN KEY (file_id) REFERENCES t_file_object(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
