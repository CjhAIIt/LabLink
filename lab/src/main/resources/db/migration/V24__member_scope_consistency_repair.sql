-- Keep t_lab_member(status='active') as the single source of truth for membership.
-- t_user.lab_id remains a compatibility cache only.

-- Close duplicate active rows in the same lab for the same user, keeping the earliest row.
UPDATE t_lab_member m
JOIN (
    SELECT id
    FROM (
        SELECT m1.id
        FROM t_lab_member m1
        JOIN (
            SELECT user_id, lab_id, MIN(id) AS keep_id
            FROM t_lab_member
            WHERE deleted = 0 AND status = 'active'
            GROUP BY user_id, lab_id
            HAVING COUNT(*) > 1
        ) keeper ON keeper.user_id = m1.user_id AND keeper.lab_id = m1.lab_id
        WHERE m1.deleted = 0
          AND m1.status = 'active'
          AND m1.id <> keeper.keep_id
    ) duplicate_rows
) duplicate_rows ON duplicate_rows.id = m.id
SET m.status = 'inactive',
    m.quit_date = COALESCE(m.quit_date, CURDATE()),
    m.remark = COALESCE(NULLIF(m.remark, ''), 'duplicate active membership closed by repair script');

-- If historical data contains multiple active labs for one user, keep the earliest active relation.
UPDATE t_lab_member m
JOIN (
    SELECT id
    FROM (
        SELECT m1.id
        FROM t_lab_member m1
        JOIN (
            SELECT user_id, MIN(id) AS keep_id
            FROM t_lab_member
            WHERE deleted = 0 AND status = 'active'
            GROUP BY user_id
            HAVING COUNT(DISTINCT lab_id) > 1
        ) keeper ON keeper.user_id = m1.user_id
        WHERE m1.deleted = 0
          AND m1.status = 'active'
          AND m1.id <> keeper.keep_id
    ) duplicate_lab_rows
) duplicate_lab_rows ON duplicate_lab_rows.id = m.id
SET m.status = 'inactive',
    m.quit_date = COALESCE(m.quit_date, CURDATE()),
    m.remark = COALESCE(NULLIF(m.remark, ''), 'cross-lab active membership closed by repair script');

UPDATE t_user u
LEFT JOIN (
    SELECT user_id, MIN(lab_id) AS active_lab_id
    FROM t_lab_member
    WHERE deleted = 0 AND status = 'active'
    GROUP BY user_id
) active_member ON active_member.user_id = u.id
SET u.lab_id = active_member.active_lab_id
WHERE u.deleted = 0
  AND u.student_id IS NOT NULL
  AND active_member.active_lab_id IS NOT NULL
  AND (u.lab_id IS NULL OR u.lab_id <> active_member.active_lab_id);

UPDATE t_user u
LEFT JOIN (
    SELECT user_id, MIN(lab_id) AS active_lab_id
    FROM t_lab_member
    WHERE deleted = 0 AND status = 'active'
    GROUP BY user_id
) active_member ON active_member.user_id = u.id
SET u.lab_id = NULL
WHERE u.deleted = 0
  AND u.student_id IS NOT NULL
  AND u.lab_id IS NOT NULL
  AND active_member.active_lab_id IS NULL;

UPDATE t_lab l
LEFT JOIN (
    SELECT lab_id, COUNT(DISTINCT user_id) AS member_count
    FROM t_lab_member
    WHERE deleted = 0 AND status = 'active'
    GROUP BY lab_id
) active_count ON active_count.lab_id = l.id
SET l.current_num = COALESCE(active_count.member_count, 0)
WHERE l.deleted = 0;
