package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.service.LabMemberService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class LabMemberServiceImpl extends ServiceImpl<LabMemberMapper, LabMember> implements LabMemberService {

    private static final String MEMBER_STATUS_ACTIVE = "active";
    private static final String MEMBER_STATUS_INACTIVE = "inactive";
    private static final String MEMBER_ROLE_MEMBER = "member";
    private static final String MEMBER_ROLE_LAB_ADMIN = "lab_admin";
    private static final String MEMBER_ROLE_LAB_LEADER = "lab_leader";

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public Page<Map<String, Object>> getMemberPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                                   String memberRole, String keyword, User currentUser) {
        Long scopedLabId = labId;
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
        }
        return baseMapper.selectMemberPage(new Page<>(pageNum, pageSize), scopedLabId,
                trimToNull(status), trimToNull(memberRole), trimToNull(keyword));
    }

    @Override
    public List<Map<String, Object>> getActiveMembers(Long labId, User currentUser) {
        Long scopedLabId = labId;
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser, labId);
        }
        if (scopedLabId == null) {
            throw new RuntimeException("Lab scope is required");
        }
        return baseMapper.selectActiveMembersByLabId(scopedLabId);
    }

    @Override
    @Transactional
    public void activateMember(Long labId, Long userId, String memberRole, Long appointedBy, String remark) {
        if (labId == null || userId == null) {
            throw new RuntimeException("Lab and user are required");
        }

        List<LabMember> activeMemberships = listActiveMembershipsByUser(userId);
        for (LabMember activeMembership : activeMemberships) {
            if (!labId.equals(activeMembership.getLabId())) {
                throw new RuntimeException("该学生已存在其他实验室的有效成员关系");
            }
        }

        List<LabMember> sameLabActiveMemberships = listActiveMemberships(labId, userId);
        if (!sameLabActiveMemberships.isEmpty()) {
            LabMember primary = sameLabActiveMemberships.get(0);
            primary.setMemberRole(defaultMemberRole(memberRole));
            primary.setRemark(trimToNull(remark));
            primary.setAppointedBy(appointedBy);
            primary.setQuitDate(null);
            this.updateById(primary);
            deactivateDuplicateMemberships(sameLabActiveMemberships, primary.getId(), appointedBy);
            syncMembershipSnapshot(userId, labId);
            return;
        }

        QueryWrapper<LabMember> historyWrapper = new QueryWrapper<>();
        historyWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        LabMember historyMember = this.getOne(historyWrapper, false);
        if (historyMember != null) {
            historyMember.setStatus(MEMBER_STATUS_ACTIVE);
            historyMember.setMemberRole(defaultMemberRole(memberRole));
            historyMember.setJoinDate(LocalDate.now());
            historyMember.setQuitDate(null);
            historyMember.setRemark(trimToNull(remark));
            historyMember.setAppointedBy(appointedBy);
            this.updateById(historyMember);
            syncMembershipSnapshot(userId, labId);
            return;
        }

        LabMember member = new LabMember();
        member.setLabId(labId);
        member.setUserId(userId);
        member.setMemberRole(defaultMemberRole(memberRole));
        member.setJoinDate(LocalDate.now());
        member.setStatus(MEMBER_STATUS_ACTIVE);
        member.setAppointedBy(appointedBy);
        member.setRemark(trimToNull(remark));
        this.save(member);
        syncMembershipSnapshot(userId, labId);
    }

    @Override
    @Transactional
    public boolean appointLeader(Long memberId, User currentUser) {
        LabMember member = this.getById(memberId);
        if (member == null) {
            throw new RuntimeException("Member record does not exist");
        }
        currentUserAccessor.assertLabScope(currentUser, member.getLabId());
        if (!MEMBER_STATUS_ACTIVE.equalsIgnoreCase(member.getStatus())) {
            throw new RuntimeException("Only active lab members can be promoted");
        }

        QueryWrapper<LabMember> leaderWrapper = new QueryWrapper<>();
        leaderWrapper.eq("lab_id", member.getLabId())
                .eq("status", MEMBER_STATUS_ACTIVE)
                .eq("member_role", MEMBER_ROLE_LAB_LEADER);
        List<LabMember> leaders = this.list(leaderWrapper);
        for (LabMember leader : leaders) {
            leader.setMemberRole(MEMBER_ROLE_MEMBER);
        }
        if (!leaders.isEmpty()) {
            this.updateBatchById(leaders);
        }

        member.setMemberRole(MEMBER_ROLE_LAB_LEADER);
        member.setAppointedBy(currentUser.getId());
        member.setRemark("appointed as lab leader");
        return this.updateById(member);
    }

    @Override
    @Transactional
    public boolean removeMember(Long memberId, String remark, User currentUser) {
        LabMember member = this.getById(memberId);
        if (member == null) {
            throw new RuntimeException("Member record does not exist");
        }
        currentUserAccessor.assertLabScope(currentUser, member.getLabId());
        if (!MEMBER_STATUS_ACTIVE.equalsIgnoreCase(member.getStatus())) {
            throw new RuntimeException("The member has already left the lab");
        }
        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(member.getUserId())) {
            throw new RuntimeException("You cannot remove yourself from the lab here");
        }
        if (hasActiveLabAdminMembership(member.getLabId(), member.getUserId())) {
            throw new RuntimeException("Current lab admin must be revoked before member removal");
        }

        List<LabMember> activeMemberships = listActiveMemberships(member.getLabId(), member.getUserId());
        if (activeMemberships.isEmpty()) {
            throw new RuntimeException("No active membership was found for this user");
        }

        String normalizedRemark = trimToNull(remark);
        LocalDate quitDate = LocalDate.now();
        boolean updated = false;
        for (LabMember activeMembership : activeMemberships) {
            activeMembership.setStatus(MEMBER_STATUS_INACTIVE);
            activeMembership.setQuitDate(quitDate);
            activeMembership.setRemark(normalizedRemark);
            updated = this.updateById(activeMembership) || updated;
        }

        User user = userService.getById(member.getUserId());
        if (user != null && member.getLabId() != null && member.getLabId().equals(user.getLabId())) {
            user.setLabId(null);
            userService.updateById(user);
        }
        userAccessService.refreshCompatibilityAccess(member.getUserId());
        labService.syncCurrentMemberCount(member.getLabId());
        systemNotificationService.createNotification(
                member.getUserId(),
                "实验室成员变更通知",
                "你已被移出当前实验室，个人页和顶部实验室信息已同步更新。",
                "lab_member_removed",
                member.getLabId(),
                "/student/lab"
        );
        auditLogService.record(currentUser == null ? null : currentUser.getId(), "lab_member_remove",
                "lab_member", member.getId(), "labId=" + member.getLabId() + ", userId=" + member.getUserId());
        return updated;
    }

    private List<LabMember> listActiveMembershipsByUser(Long userId) {
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .orderByAsc("id");
        return this.list(queryWrapper);
    }

    private List<LabMember> listActiveMemberships(Long labId, Long userId) {
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .orderByDesc("id");
        return this.list(queryWrapper);
    }

    private void deactivateDuplicateMemberships(List<LabMember> memberships, Long primaryId, Long operatorId) {
        if (memberships == null || memberships.size() <= 1) {
            return;
        }
        LocalDate quitDate = LocalDate.now();
        for (LabMember membership : memberships) {
            if (membership == null || membership.getId() == null || membership.getId().equals(primaryId)) {
                continue;
            }
            membership.setStatus(MEMBER_STATUS_INACTIVE);
            membership.setQuitDate(quitDate);
            membership.setAppointedBy(operatorId);
            membership.setRemark("duplicate active membership closed automatically");
            this.updateById(membership);
        }
    }

    private void syncMembershipSnapshot(Long userId, Long labId) {
        userAccessService.refreshCompatibilityAccess(userId);
        labService.syncCurrentMemberCount(labId);
    }

    private boolean hasActiveLabAdminMembership(Long labId, Long userId) {
        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .eq("status", MEMBER_STATUS_ACTIVE)
                .eq("member_role", MEMBER_ROLE_LAB_ADMIN)
                .last("LIMIT 1");
        return this.getOne(queryWrapper, false) != null;
    }

    private String defaultMemberRole(String memberRole) {
        return StringUtils.hasText(memberRole) ? memberRole.trim().toLowerCase() : MEMBER_ROLE_MEMBER;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
