UPDATE t_lab_attendance
SET status = 3,
    update_time = NOW()
WHERE deleted = 0
  AND status = 2
  AND tag_type = 'leave';

UPDATE t_lab_attendance
SET status = 4,
    update_time = NOW()
WHERE deleted = 0
  AND status = 3
  AND tag_type = 'forgot';
