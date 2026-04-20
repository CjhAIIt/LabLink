package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.service.LabMemberService;
import com.lab.recruitment.service.LabService;
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

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Override
    public Page<Map<String, Object>> getMemberPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                                   String memberRole, String keyword, User currentUser) {
        Long scopedLabId = labId;
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser,
                    labId != null ? labId : currentUser.getLabId());
        }
        return baseMapper.selectMemberPage(new Page<>(pageNum, pageSize), scopedLabId,
                trimToNull(status), trimToNull(memberRole), trimToNull(keyword));
    }

    @Override
    public List<Map<String, Object>> getActiveMembers(Long labId, User currentUser) {
        Long scopedLabId = labId;
        if (!currentUserAccessor.isSuperAdmin(currentUser)) {
            scopedLabId = currentUserAccessor.resolveLabScope(currentUser,
                    labId != null ? labId : currentUser.getLabId());
        }
        if (scopedLabId == null) {
            throw new RuntimeException("实验室不能为空");
        }
        return baseMapper.selectActiveMembersByLabId(scopedLabId);
    }

    @Override
    @Transactional
    public void activateMember(Long labId, Long userId, String memberRole, Long appointedBy, String remark) {
        QueryWrapper<LabMember> activeWrapper = new QueryWrapper<>();
        activeWrapper.eq("lab_id", labId).eq("user_id", userId).eq("status", "active");
        LabMember activeMember = this.getOne(activeWrapper, false);
        if (activeMember != null) {
            activeMember.setMemberRole(defaultMemberRole(memberRole));
            activeMember.setRemark(trimToNull(remark));
            activeMember.setAppointedBy(appointedBy);
            activeMember.setQuitDate(null);
            this.updateById(activeMember);
            return;
        }

        QueryWrapper<LabMember> historyWrapper = new QueryWrapper<>();
        historyWrapper.eq("lab_id", labId).eq("user_id", userId).orderByDesc("id").last("LIMIT 1");
        LabMember historyMember = this.getOne(historyWrapper, false);
        if (historyMember != null) {
            historyMember.setStatus("active");
            historyMember.setMemberRole(defaultMemberRole(memberRole));
            historyMember.setJoinDate(LocalDate.now());
            historyMember.setQuitDate(null);
            historyMember.setRemark(trimToNull(remark));
            historyMember.setAppointedBy(appointedBy);
            this.updateById(historyMember);
            return;
        }

        LabMember member = new LabMember();
        member.setLabId(labId);
        member.setUserId(userId);
        member.setMemberRole(defaultMemberRole(memberRole));
        member.setJoinDate(LocalDate.now());
        member.setStatus("active");
        member.setAppointedBy(appointedBy);
        member.setRemark(trimToNull(remark));
        this.save(member);
    }

    @Override
    @Transactional
    public boolean appointLeader(Long memberId, User currentUser) {
        LabMember member = this.getById(memberId);
        if (member == null) {
            throw new RuntimeException("成员记录不存在");
        }
        currentUserAccessor.assertLabScope(currentUser, member.getLabId());
        if (!"active".equalsIgnoreCase(member.getStatus())) {
            throw new RuntimeException("仅可任命在组成员为负责人");
        }

        QueryWrapper<LabMember> leaderWrapper = new QueryWrapper<>();
        leaderWrapper.eq("lab_id", member.getLabId()).eq("status", "active").eq("member_role", "lab_leader");
        List<LabMember> leaders = this.list(leaderWrapper);
        for (LabMember leader : leaders) {
            leader.setMemberRole("member");
        }
        if (!leaders.isEmpty()) {
            this.updateBatchById(leaders);
        }

        member.setMemberRole("lab_leader");
        member.setAppointedBy(currentUser.getId());
        member.setRemark("已任命为实验室负责人");
        return this.updateById(member);
    }

    @Override
    @Transactional
    public boolean removeMember(Long memberId, String remark, User currentUser) {
        LabMember member = this.getById(memberId);
        if (member == null) {
            throw new RuntimeException("成员记录不存在");
        }
        currentUserAccessor.assertLabScope(currentUser, member.getLabId());
        if (!"active".equalsIgnoreCase(member.getStatus())) {
            throw new RuntimeException("该成员已不在组内");
        }

        member.setStatus("inactive");
        member.setQuitDate(LocalDate.now());
        member.setRemark(trimToNull(remark));
        boolean updated = this.updateById(member);

        User user = userService.getById(member.getUserId());
        if (user != null && member.getLabId().equals(user.getLabId())) {
            user.setLabId(null);
            userService.updateById(user);
        }
        labService.syncCurrentMemberCount(member.getLabId());
        return updated;
    }

    private String defaultMemberRole(String memberRole) {
        return StringUtils.hasText(memberRole) ? memberRole.trim().toLowerCase() : "member";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
