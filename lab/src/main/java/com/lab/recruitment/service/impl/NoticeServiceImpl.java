package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.Notice;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.NoticeMapper;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.NoticeService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private LabService labService;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.search.notice-fulltext-enabled:true}")
    private boolean noticeFullTextEnabled;

    @Override
    public Page<Map<String, Object>> getNoticePage(Integer pageNum, Integer pageSize, String publishScope,
                                                   Long collegeId, Long labId, String keyword, User currentUser) {
        Long scopedLabId = labId;
        Long scopedCollegeId = collegeId;
        String normalizedScope = normalizeScope(publishScope);
        String normalizedKeyword = trimToNull(keyword);
        if (!currentUserAccessor.isSuperAdmin(currentUser) && currentUserAccessor.isCollegeManager(currentUser)) {
            DataScope scope = currentUserAccessor.resolveManagementScope(currentUser, collegeId, labId);
            scopedCollegeId = scope.getCollegeId();
            scopedLabId = scope.getLabId();
        } else if (!currentUserAccessor.isSuperAdmin(currentUser) && currentUserAccessor.isLabScopedManager(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
            Lab currentLab = labService.getById(scopedLabId);
            scopedCollegeId = currentLab == null ? collegeId : currentLab.getCollegeId();
        }
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        if (!noticeFullTextEnabled || !StringUtils.hasText(normalizedKeyword)) {
            return baseMapper.selectNoticePageByLike(page, normalizedScope, scopedCollegeId, scopedLabId, normalizedKeyword);
        }
        try {
            return baseMapper.selectNoticePageByFullText(page, normalizedScope, scopedCollegeId, scopedLabId, normalizedKeyword);
        } catch (Exception ex) {
            log.warn("notice fulltext search fallback to like query, reason: {}", ex.getMessage());
            return baseMapper.selectNoticePageByLike(page, normalizedScope, scopedCollegeId, scopedLabId, normalizedKeyword);
        }
    }

    @Override
    public List<Map<String, Object>> getLatestNotices(User currentUser, int limit) {
        Long labId = null;
        Long collegeId = null;
        if (currentUser != null && currentUserAccessor.isSuperAdmin(currentUser)) {
            return baseMapper.selectNoticePageByLike(new Page<>(1, limit), null, null, null, null).getRecords();
        }
        if (currentUser != null) {
            DataScope scope = currentUserAccessor.buildDataScope(currentUser);
            labId = scope.getLabId();
            collegeId = scope.getCollegeId();
        }
        return baseMapper.selectLatestNotices(Math.max(limit, 1), collegeId, labId);
    }

    @Override
    @Transactional
    public boolean createNotice(Notice notice, User currentUser) {
        prepareNotice(notice, currentUser, true);
        notice.setPublisherId(currentUser.getId());
        notice.setPublishTime(LocalDateTime.now());
        notice.setStatus(notice.getStatus() == null ? 1 : notice.getStatus());
        boolean saved = this.save(notice);
        if (saved && Integer.valueOf(1).equals(notice.getStatus())) {
            notifyNoticeReceivers(notice);
        }
        return saved;
    }

    @Override
    @Transactional
    public boolean updateNotice(Notice notice, User currentUser) {
        if (notice == null || notice.getId() == null) {
            throw new RuntimeException("公告ID不能为空");
        }
        Notice existing = this.getById(notice.getId());
        if (existing == null) {
            throw new RuntimeException("公告不存在");
        }
        assertCanManageNotice(existing, currentUser);
        prepareNotice(notice, currentUser, false);
        return this.updateById(notice);
    }

    @Override
    @Transactional
    public boolean deleteNotice(Long id, User currentUser) {
        Notice existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("公告不存在");
        }
        assertCanManageNotice(existing, currentUser);
        return this.removeById(id);
    }

    private void prepareNotice(Notice notice, User currentUser, boolean creating) {
        if (notice == null) {
            throw new RuntimeException("公告内容不能为空");
        }
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new RuntimeException("公告标题不能为空");
        }
        if (!StringUtils.hasText(notice.getContent())) {
            throw new RuntimeException("公告内容不能为空");
        }
        String scope = normalizeScope(notice.getPublishScope());
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            prepareManagedNotice(notice, currentUser, scope);
            return;
        }
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            if (currentUser.getLabId() == null) {
                throw new RuntimeException("当前账号未绑定实验室");
            }
            scope = "lab";
            notice.setLabId(currentUser.getLabId());
            Lab lab = labService.getById(currentUser.getLabId());
            notice.setCollegeId(lab == null ? null : lab.getCollegeId());
        } else if ("school".equals(scope)) {
            notice.setCollegeId(null);
            notice.setLabId(null);
        } else if ("college".equals(scope)) {
            if (notice.getCollegeId() == null) {
                throw new RuntimeException("学院公告必须选择学院");
            }
            notice.setLabId(null);
        } else {
            if (notice.getLabId() == null) {
                throw new RuntimeException("实验室公告必须选择实验室");
            }
            Lab lab = labService.getById(notice.getLabId());
            if (lab == null) {
                throw new RuntimeException("实验室不存在");
            }
            notice.setCollegeId(lab.getCollegeId());
        }
        notice.setPublishScope(scope);
        if (!creating) {
            notice.setPublisherId(null);
            notice.setPublishTime(null);
        }
    }

    private void assertCanManageNotice(Notice notice, User currentUser) {
        if (currentUserAccessor.isCollegeManager(currentUser)) {
            if ("college".equalsIgnoreCase(notice.getPublishScope())) {
                currentUserAccessor.assertCollegeScope(currentUser, notice.getCollegeId());
                return;
            }
            currentUserAccessor.assertLabScope(currentUser, notice.getLabId());
            return;
        }
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            currentUserAccessor.assertLabScope(currentUser, notice.getLabId());
            return;
        }
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }
        if (currentUser.getLabId() == null || !currentUser.getLabId().equals(notice.getLabId())) {
            throw new RuntimeException("无权操作其他实验室公告");
        }
    }

    private void prepareManagedNotice(Notice notice, User currentUser, String scope) {
        if (currentUserAccessor.isCollegeManager(currentUser)) {
            Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
            if (managedCollegeId == null) {
                throw new RuntimeException("当前学院管理员未绑定学院");
            }
            if ("lab".equals(scope)) {
                Long scopedLabId = currentUserAccessor.resolveLabScope(currentUser, notice.getLabId());
                Lab lab = labService.getById(scopedLabId);
                notice.setPublishScope("lab");
                notice.setLabId(scopedLabId);
                notice.setCollegeId(lab == null ? managedCollegeId : lab.getCollegeId());
                return;
            }
            notice.setPublishScope("college");
            notice.setCollegeId(managedCollegeId);
            notice.setLabId(null);
            return;
        }

        Long scopedLabId = currentUserAccessor.resolveLabScope(currentUser, notice.getLabId());
        Lab lab = labService.getById(scopedLabId);
        notice.setPublishScope("lab");
        notice.setLabId(scopedLabId);
        notice.setCollegeId(lab == null ? null : lab.getCollegeId());
    }

    private void notifyNoticeReceivers(Notice notice) {
        Set<Long> receiverIds = resolveNoticeReceiverIds(notice);
        for (Long receiverId : receiverIds) {
            systemNotificationService.createNotificationAsync(
                    receiverId,
                    "新公告：" + notice.getTitle(),
                    "有一条新的公告，请及时查看。",
                    "notice",
                    notice.getId(),
                    "/student/notices"
            );
        }
    }

    private Set<Long> resolveNoticeReceiverIds(Notice notice) {
        Set<Long> receiverIds = new LinkedHashSet<>();
        if (notice == null) {
            return receiverIds;
        }
        if ("lab".equalsIgnoreCase(notice.getPublishScope()) && notice.getLabId() != null) {
            for (Map<String, Object> member : labMemberMapper.selectActiveMembersByLabId(notice.getLabId())) {
                Object userId = member.get("userId");
                if (userId instanceof Number) {
                    receiverIds.add(((Number) userId).longValue());
                }
            }
            return receiverIds;
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT m.user_id FROM t_lab_member m " +
                        "INNER JOIN t_lab l ON l.id = m.lab_id AND l.deleted = 0 " +
                        "WHERE m.deleted = 0 AND m.status = 'active'"
        );
        if ("college".equalsIgnoreCase(notice.getPublishScope())) {
            sql.append(" AND l.college_id = ?");
            args.add(notice.getCollegeId());
        }
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql.toString(), args.toArray())) {
            Object userId = row.get("user_id");
            if (userId instanceof Number) {
                receiverIds.add(((Number) userId).longValue());
            }
        }
        return receiverIds;
    }

    private String normalizeScope(String scope) {
        String value = trimToNull(scope);
        if (!StringUtils.hasText(value)) {
            return "school";
        }
        String normalized = value.toLowerCase();
        if (!"school".equals(normalized) && !"college".equals(normalized) && !"lab".equals(normalized)) {
            throw new RuntimeException("公告范围不合法");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
