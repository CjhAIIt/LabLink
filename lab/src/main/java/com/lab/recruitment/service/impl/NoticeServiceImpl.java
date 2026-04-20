package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.Notice;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.NoticeMapper;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.NoticeService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private LabService labService;

    @Value("${app.search.notice-fulltext-enabled:true}")
    private boolean noticeFullTextEnabled;

    @Override
    public Page<Map<String, Object>> getNoticePage(Integer pageNum, Integer pageSize, String publishScope,
                                                   Long collegeId, Long labId, String keyword, User currentUser) {
        Long scopedLabId = labId;
        Long scopedCollegeId = collegeId;
        String normalizedScope = normalizeScope(publishScope);
        String normalizedKeyword = trimToNull(keyword);
        if (!currentUserAccessor.isSuperAdmin(currentUser) && currentUserAccessor.isLabScopedManager(currentUser)) {
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
        if (currentUser != null && currentUser.getLabId() != null) {
            labId = currentUser.getLabId();
            Lab lab = labService.getById(labId);
            if (lab != null) {
                collegeId = lab.getCollegeId();
            }
        }
        if (currentUser != null && currentUserAccessor.isSuperAdmin(currentUser)) {
            return baseMapper.selectNoticePageByLike(new Page<>(1, limit), null, null, null, null).getRecords();
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
        return this.save(notice);
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
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            return;
        }
        if (currentUser.getLabId() == null || !currentUser.getLabId().equals(notice.getLabId())) {
            throw new RuntimeException("无权操作其他实验室公告");
        }
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
