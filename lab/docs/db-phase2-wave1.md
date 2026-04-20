# Phase 2 Wave 1 Database Delivery

This document summarizes the Phase 2 Wave 1 database delivery already added to the repository.

## 1. Flyway Migration Order

1. `V1__search_and_list_indexes.sql`
2. `V2__student_profile_scope.sql`
3. `V3__equipment_management_upgrade.sql`
4. `V4__audit_notification_center.sql`
5. `V5__attendance_leave_and_change_log.sql`
6. `V6__search_keyword_hot.sql`
7. `V7__file_object_and_relation.sql`
8. `V8__phase2_core_indexes.sql`

## 2. New Core Tables

### Profiles

- `t_student_profile`
- `t_student_profile_review`
- `t_student_profile_archive`

### Attendance

- `t_attendance_leave`
- `t_attendance_change_log`

### Devices

- `t_equipment_category`
- `t_equipment_maintenance`

### Search

- `t_search_keyword_hot`

### Files

- `t_file_object`
- `t_business_file_relation`

## 3. Upgraded Existing Tables

- `t_equipment`
- `t_audit_log`
- `t_system_notification`

## 4. Index Delivery Highlights

### Earlier migrations

- Notice list and notice full-text indexes
- Lab list indexes
- User list indexes
- Search hot keyword indexes
- Audit log and notification indexes
- Profile, review, archive indexes
- Equipment and maintenance indexes

### `V8__phase2_core_indexes.sql`

- `idx_user_lab_role_status_create`
- `idx_lab_member_lab_status_deleted`
- `idx_attendance_session_lab_status_date`
- `idx_attendance_record_session_status`
- `idx_equipment_borrow_user_status_time`
- `idx_equipment_borrow_equipment_status_time`

These indexes target current high-frequency filters in:

- management user queries
- lab member count/statistics
- attendance current session and session record lookups
- borrow/return and maintenance management pages

## 5. Storage Rules

- Core business data persists in MySQL
- Redis is acceleration only
- RabbitMQ is async decoupling only
- Core data keeps logical delete fields
- File binary and file metadata are separated

## 6. Middleware Flags

- `LABLINK_CACHE_REDIS_ENABLED`
- `LABLINK_MQ_RABBIT_ENABLED`
- `APP_SCHEMA_MIGRATION_ENABLED`

## 7. Runtime Schema Note

The project still contains runtime schema compensation logic in:

- `DatabaseSchemaUpdate`
- `PlatformAttendanceSchemaRunner`

Current recommendation:

- keep Flyway as the main delivery path for new schema changes
- gradually shrink runtime schema mutation in later cleanup work
