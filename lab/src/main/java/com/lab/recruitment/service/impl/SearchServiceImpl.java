package com.lab.recruitment.service.impl;

import com.lab.recruitment.config.PlatformCacheNames;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.SearchService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import com.lab.recruitment.vo.SearchResultItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 20;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Override
    @Cacheable(
            cacheNames = PlatformCacheNames.SEARCH_GLOBAL,
            key = "#currentUser.id + ':' + #keyword.trim().toLowerCase() + ':' + (#limit == null ? 0 : #limit) + ':' + (#collegeId == null ? 'all' : #collegeId) + ':' + (#labId == null ? 'all' : #labId)",
            condition = "#currentUser != null && #currentUser.id != null && #keyword != null"
    )
    public Map<String, Object> globalSearch(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int pageLimit = normalizeLimit(limit);
        DataScope scope = currentUserAccessor.resolveManagementScope(currentUser, collegeId, labId);

        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(group("labs", "实验室", searchLabs(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));
        groups.add(group("users", "成员", searchUsers(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));
        groups.add(group("profiles", "资料", searchProfiles(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));
        groups.add(group("devices", "设备", searchDevices(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));
        groups.add(group("files", "文件", searchFiles(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));
        groups.add(group("notices", "公告", searchNotices(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));
        groups.add(group("attendance", "考勤", searchAttendanceTasks(normalizedKeyword, pageLimit, scope.getCollegeId(), scope.getLabId(), currentUser)));

        long total = 0L;
        for (Map<String, Object> group : groups) {
            Object value = group.get("total");
            if (value instanceof Number) {
                total += ((Number) value).longValue();
            }
        }

        logKeyword(normalizedKeyword, scope);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", normalizedKeyword);
        result.put("scopeLevel", scope.getLevel() == null ? null : scope.getLevel().name());
        result.put("collegeId", scope.getCollegeId());
        result.put("labId", scope.getLabId());
        result.put("total", total);
        result.put("groups", groups);
        return result;
    }

    @Override
    public List<SearchResultItemVO> searchLabs(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT l.id, l.lab_name AS title, CONCAT(COALESCE(l.teacher_name, '-'), ' / ', COALESCE(l.location, '-')) AS subtitle, " +
                        "l.status, c.college_name AS collegeName " +
                        "FROM t_lab l " +
                        "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                        "WHERE l.deleted = 0 AND (l.lab_name LIKE ? OR l.teacher_name LIKE ? OR l.location LIKE ?)"
        );
        addKeywordArgs(args, keyword, 3);
        appendLabScope(sql, args, collegeId, labId, "l");
        sql.append(" ORDER BY l.create_time DESC, l.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("labs");
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("title"));
            item.setSubtitle(joinSubtitle(rs.getString("subtitle"), rs.getString("collegeName")));
            item.setStatus(rs.getString("status"));
            item.setExtra(mapOf("path", "/admin/labs"));
            return item;
        });
    }

    @Override
    public List<SearchResultItemVO> searchUsers(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT u.id, u.real_name AS realName, u.student_id AS studentId, u.major, " +
                        "COALESCE(l.lab_name, '-') AS labName, u.role " +
                        "FROM t_user u " +
                        "LEFT JOIN t_lab_member lm ON lm.user_id = u.id AND lm.deleted = 0 AND lm.status = 'active' " +
                        "LEFT JOIN t_lab l ON l.id = lm.lab_id AND l.deleted = 0 " +
                        "WHERE u.deleted = 0 AND (u.real_name LIKE ? OR u.student_id LIKE ? OR u.username LIKE ?)"
        );
        addKeywordArgs(args, keyword, 3);
        appendLabScope(sql, args, collegeId, labId, "l");
        sql.append(" ORDER BY u.create_time DESC, u.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("users");
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("realName"));
            item.setSubtitle(joinSubtitle(rs.getString("studentId"), rs.getString("major"), rs.getString("labName")));
            item.setStatus(rs.getString("role"));
            item.setExtra(mapOf("path", "/admin/members"));
            return item;
        });
    }

    @Override
    public List<SearchResultItemVO> searchProfiles(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.real_name AS realName, p.student_no AS studentNo, p.major, p.status, " +
                        "COALESCE(l.lab_name, '-') AS labName " +
                        "FROM t_student_profile p " +
                        "LEFT JOIN t_lab_member lm ON lm.user_id = p.user_id AND lm.deleted = 0 AND lm.status = 'active' " +
                        "LEFT JOIN t_lab l ON l.id = lm.lab_id AND l.deleted = 0 " +
                        "WHERE p.deleted = 0 AND (p.real_name LIKE ? OR p.student_no LIKE ? OR p.major LIKE ?)"
        );
        addKeywordArgs(args, keyword, 3);
        if (labId != null) {
            sql.append(" AND l.id = ?");
            args.add(labId);
        } else if (collegeId != null) {
            sql.append(" AND p.college_id = ?");
            args.add(collegeId);
        }
        sql.append(" ORDER BY p.updated_at DESC, p.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("profiles");
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("realName"));
            item.setSubtitle(joinSubtitle(rs.getString("studentNo"), rs.getString("major"), rs.getString("labName")));
            item.setStatus(rs.getString("status"));
            item.setExtra(mapOf("path", "/admin/profiles"));
            return item;
        });
    }

    @Override
    public List<SearchResultItemVO> searchDevices(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT e.id, e.device_name AS deviceName, e.device_code AS deviceCode, e.status, " +
                        "CONCAT(COALESCE(e.brand, '-'), ' / ', COALESCE(e.model, '-')) AS subtitle, " +
                        "l.lab_name AS labName " +
                        "FROM t_equipment e " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "WHERE e.deleted = 0 AND (e.device_name LIKE ? OR e.device_code LIKE ? OR e.brand LIKE ? OR e.model LIKE ?)"
        );
        addKeywordArgs(args, keyword, 4);
        appendLabScope(sql, args, collegeId, labId, "l");
        sql.append(" ORDER BY e.update_time DESC, e.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("devices");
            item.setId(rs.getLong("id"));
            item.setTitle(joinSubtitle(rs.getString("deviceName"), rs.getString("deviceCode")));
            item.setSubtitle(joinSubtitle(rs.getString("subtitle"), rs.getString("labName")));
            item.setStatus(rs.getString("status"));
            item.setExtra(mapOf("path", "/admin/devices"));
            return item;
        });
    }

    @Override
    public List<SearchResultItemVO> searchFiles(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT f.id, f.original_name AS originalName, f.content_type AS contentType, f.file_size AS fileSize, " +
                        "f.uploaded_at AS uploadedAt, COALESCE(u.real_name, u.username, '-') AS uploaderName, " +
                        "COALESCE(l.lab_name, '-') AS labName " +
                        "FROM t_file_object f " +
                        "LEFT JOIN t_user u ON u.id = f.uploaded_by AND u.deleted = 0 " +
                        "LEFT JOIN t_lab_member lm ON lm.user_id = u.id AND lm.deleted = 0 AND lm.status = 'active' " +
                        "LEFT JOIN t_lab l ON l.id = lm.lab_id AND l.deleted = 0 " +
                        "WHERE f.deleted = 0 AND (f.original_name LIKE ? OR f.file_name LIKE ? OR COALESCE(f.content_type, '') LIKE ?)"
        );
        addKeywordArgs(args, keyword, 3);
        appendLabScope(sql, args, collegeId, labId, "l");
        sql.append(" ORDER BY f.uploaded_at DESC, f.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("files");
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("originalName"));
            item.setSubtitle(joinSubtitle(rs.getString("contentType"), readableFileSize(rs.getLong("fileSize")), rs.getString("labName")));
            item.setStatus("已上传");
            item.setExtra(mapOf("uploaderName", rs.getString("uploaderName")));
            return item;
        });
    }

    @Override
    public List<SearchResultItemVO> searchNotices(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT n.id, n.title, n.publish_scope AS publishScope, n.status, " +
                        "LEFT(COALESCE(n.content, ''), 120) AS contentSnippet " +
                        "FROM t_notice n " +
                        "WHERE n.deleted = 0 AND (n.title LIKE ? OR n.content LIKE ?)"
        );
        addKeywordArgs(args, keyword, 2);
        appendNoticeScope(sql, args, collegeId, labId);
        sql.append(" ORDER BY n.publish_time DESC, n.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("notices");
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("title"));
            item.setSubtitle(joinSubtitle(rs.getString("publishScope"), rs.getString("contentSnippet")));
            item.setStatus(rs.getString("status"));
            item.setExtra(mapOf("path", "/admin/notices"));
            return item;
        });
    }

    @Override
    public List<SearchResultItemVO> searchAttendanceTasks(String keyword, Integer limit, Long collegeId, Long labId, User currentUser) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT t.id, t.task_name AS taskName, t.semester_name AS semesterName, t.status, c.college_name AS collegeName " +
                        "FROM t_attendance_task t " +
                        "LEFT JOIN t_college c ON c.id = t.college_id AND c.deleted = 0 " +
                        "WHERE t.deleted = 0 AND (t.task_name LIKE ? OR t.semester_name LIKE ?)"
        );
        addKeywordArgs(args, keyword, 2);
        if (labId != null) {
            sql.append(" AND t.college_id = (SELECT college_id FROM t_lab WHERE id = ? AND deleted = 0)");
            args.add(labId);
        } else if (collegeId != null) {
            sql.append(" AND t.college_id = ?");
            args.add(collegeId);
        }
        sql.append(" ORDER BY t.update_time DESC, t.id DESC LIMIT ?");
        args.add(normalizeLimit(limit));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            SearchResultItemVO item = new SearchResultItemVO();
            item.setType("attendance");
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("taskName"));
            item.setSubtitle(joinSubtitle(rs.getString("semesterName"), rs.getString("collegeName")));
            item.setStatus(rs.getString("status"));
            item.setExtra(mapOf("path", "/admin/attendance-tasks"));
            return item;
        });
    }

    private Map<String, Object> group(String type, String label, List<SearchResultItemVO> items) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("type", type);
        group.put("label", label);
        group.put("total", items == null ? 0 : items.size());
        group.put("items", items == null ? new ArrayList<>() : items);
        return group;
    }

    private void appendLabScope(StringBuilder sql, List<Object> args, Long collegeId, Long labId, String labAlias) {
        if (labId != null) {
            sql.append(" AND ").append(labAlias).append(".id = ?");
            args.add(labId);
        } else if (collegeId != null) {
            sql.append(" AND ").append(labAlias).append(".college_id = ?");
            args.add(collegeId);
        }
    }

    private void appendNoticeScope(StringBuilder sql, List<Object> args, Long collegeId, Long labId) {
        if (labId != null) {
            sql.append(" AND (n.publish_scope = 'school' OR (n.publish_scope = 'college' AND n.college_id = ?) OR (n.publish_scope = 'lab' AND n.lab_id = ?))");
            args.add(currentUserAccessor.resolveCollegeIdByLabId(labId));
            args.add(labId);
            return;
        }
        if (collegeId != null) {
            sql.append(" AND (n.publish_scope = 'school' OR (n.publish_scope = 'college' AND n.college_id = ?))");
            args.add(collegeId);
        }
    }

    private void addKeywordArgs(List<Object> args, String keyword, int count) {
        for (int index = 0; index < count; index++) {
            args.add("%" + keyword + "%");
        }
    }

    private void logKeyword(String keyword, DataScope scope) {
        if (!StringUtils.hasText(keyword) || scope == null || scope.getLevel() == null) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO t_search_keyword_hot (keyword, scope_level, college_id, lab_id, search_count, deleted) " +
                            "VALUES (?, ?, ?, ?, 1, 0) " +
                            "ON DUPLICATE KEY UPDATE search_count = search_count + 1, updated_at = CURRENT_TIMESTAMP",
                    keyword,
                    scope.getLevel().name(),
                    scope.getCollegeId(),
                    scope.getLabId()
            );
        } catch (Exception ignored) {
            // Search should still work even if the hot-key table is not available yet.
        }
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new RuntimeException("搜索关键词不能为空");
        }
        return keyword.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String joinSubtitle(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!StringUtils.hasText(value) || "-".equals(value.trim())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(value.trim());
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (values == null) {
            return result;
        }
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    private String readableFileSize(long fileSize) {
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        double kb = fileSize / 1024D;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024D;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        double gb = mb / 1024D;
        return String.format(Locale.ROOT, "%.1f GB", gb);
    }
}
