# Flyway Migration Fix

## What changed

- `V1` now creates the foundational tables required by later business migrations, including `t_college`, `t_lab`, `t_user`, and the phase-2 base business tables.
- `V3`, `V4`, `V5`, and `V7` now create their business tables without cross-table foreign keys.
- `V4` also brings `t_user_identity`, `t_platform_post`, and `t_lab_teacher_relation` into Flyway.
- `V5` also brings `t_attendance_photo` and `t_attendance_duty` into Flyway.
- `V9__restore_foreign_keys.sql` cleans orphan rows and restores foreign keys after all referenced tables exist.

## Why

The previous chain depended on tables created outside Flyway (`init.sql` or runtime schema updaters). On a truly empty database, later migrations referenced tables that did not exist yet and failed midway.

## Result

The migration order is now:

1. Base schema
2. Domain tables
3. Secondary indexes
4. Foreign key restoration

This makes fresh-schema initialization deterministic while keeping all required foreign keys effective at the end of the chain.
