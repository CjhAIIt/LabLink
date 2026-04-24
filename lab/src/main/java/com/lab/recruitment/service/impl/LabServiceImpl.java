package com.lab.recruitment.service.impl;

import com.lab.recruitment.config.PlatformCacheNames;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.dto.LabWithAdminDTO;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.LabSpaceService;
import com.lab.recruitment.service.PlatformCacheService;
import com.lab.recruitment.service.StatisticsRefreshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabServiceImpl extends ServiceImpl<LabMapper, Lab> implements LabService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Autowired
    private LabSpaceService labSpaceService;

    @Autowired
    private PlatformCacheService platformCacheService;

    @Autowired
    private StatisticsRefreshService statisticsRefreshService;

    @Override
    public Page<Lab> getLabPage(Integer pageNum, Integer pageSize, Long collegeId, String labName, Integer status) {
        Page<Lab> page = new Page<>(pageNum, pageSize);
        Page<Lab> result = baseMapper.selectLabPage(page, collegeId, labName, status);
        populateCurrentMemberCounts(result.getRecords());
        populateLiveDisplayFields(result.getRecords());
        return result;
    }

    @Override
    @Cacheable(cacheNames = PlatformCacheNames.LAB_DETAIL, key = "#id", condition = "#id != null")
    public Lab getLabById(Long id) {
        Lab lab = this.getById(id);
        if (lab != null) {
            populateCurrentMemberCounts(Collections.singletonList(lab));
            populateLiveDisplayFields(Collections.singletonList(lab));
        }
        return lab;
    }

    @Override
    public boolean createLab(Lab lab) {
        lab.setCurrentNum(0);
        syncTeacherDisplayField(lab);
        if (!StringUtils.hasText(lab.getLabCode())) {
            lab.setLabCode("LAB-" + System.currentTimeMillis());
        }
        boolean created = this.save(lab);
        if (created) {
            labSpaceService.initializeDefaultFolders(lab.getId(), null);
            platformCacheService.evictLabDetailCache(lab.getId());
            statisticsRefreshService.refreshAsync("lab", lab.getCollegeId(), lab.getId(), null, "lab_create");
        }
        return created;
    }

    @Override
    public boolean updateLab(Lab lab) {
        if (lab == null || lab.getId() == null) {
            throw new RuntimeException("实验室 ID 不能为空");
        }

        int currentMembers = countCurrentMembers(lab.getId());
        if (lab.getRecruitNum() != null && lab.getRecruitNum() < currentMembers) {
            throw new RuntimeException("招募名额不能低于当前成员人数");
        }

        lab.setCurrentNum(currentMembers);
        syncLiveWritableFields(lab);
        boolean success = this.updateById(lab);
        if (success) {
            platformCacheService.evictLabDetailCache(lab.getId());
            statisticsRefreshService.refreshAsync("lab", lab.getCollegeId(), lab.getId(), null, "lab_update");
        }
        return success;
    }

    @Override
    public boolean deleteLab(Long id) {
        Lab existing = id == null ? null : this.getById(id);
        boolean success = this.removeById(id);
        if (success) {
            platformCacheService.evictLabDetailCache(id);
            statisticsRefreshService.refreshAsync(
                    "lab",
                    existing == null ? null : existing.getCollegeId(),
                    id,
                    null,
                    "lab_delete"
            );
        }
        return success;
    }

    @Override
    public List<LabWithAdminDTO> getLabsWithAdmin() {
        List<Lab> labs = this.list();
        populateCurrentMemberCounts(labs);
        populateLiveDisplayFields(labs);
        List<LabWithAdminDTO> result = new ArrayList<>();

        for (Lab lab : labs) {
            LabWithAdminDTO dto = new LabWithAdminDTO();
            dto.setLab(lab);
            dto.setAdmin(resolveCurrentLabAdmin(lab.getId()));
            dto.setCreateTime(lab.getCreateTime());
            dto.setUpdateTime(lab.getUpdateTime());
            result.add(dto);
        }

        return result;
    }

    @Override
    public int countCurrentMembers(Long labId) {
        if (labId == null) {
            return 0;
        }

        QueryWrapper<LabMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id")
                .eq("lab_id", labId)
                .eq("status", "active")
                .eq("deleted", 0);
        return (int) labMemberMapper.selectList(queryWrapper).stream()
                .map(LabMember::getUserId)
                .filter(id -> id != null)
                .distinct()
                .count();
    }

    @Override
    public void syncCurrentMemberCount(Long labId) {
        if (labId == null) {
            return;
        }

        Lab lab = new Lab();
        lab.setId(labId);
        lab.setCurrentNum(countCurrentMembers(labId));
        this.updateById(lab);
        platformCacheService.evictLabDetailCache(labId);
    }

    private void populateCurrentMemberCounts(List<Lab> labs) {
        if (labs == null || labs.isEmpty()) {
            return;
        }

        List<Long> labIds = labs.stream()
                .map(Lab::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (labIds.isEmpty()) {
            return;
        }

        QueryWrapper<LabMember> memberQuery = new QueryWrapper<>();
        memberQuery.select("id", "lab_id", "user_id")
                .in("lab_id", labIds)
                .eq("status", "active")
                .eq("deleted", 0);

        Map<Long, Integer> memberCountMap = new HashMap<>();
        Map<Long, List<Long>> countedUsers = new HashMap<>();
        for (LabMember member : labMemberMapper.selectList(memberQuery)) {
            if (member.getLabId() == null || member.getUserId() == null) {
                continue;
            }
            List<Long> userIds = countedUsers.computeIfAbsent(member.getLabId(), ignored -> new ArrayList<>());
            if (userIds.contains(member.getUserId())) {
                continue;
            }
            userIds.add(member.getUserId());
            memberCountMap.merge(member.getLabId(), 1, Integer::sum);
        }

        for (Lab lab : labs) {
            if (lab == null || lab.getId() == null) {
                continue;
            }
            lab.setCurrentNum(memberCountMap.getOrDefault(lab.getId(), 0));
        }
    }

    private void populateLiveDisplayFields(List<Lab> labs) {
        if (labs == null || labs.isEmpty()) {
            return;
        }

        for (Lab lab : labs) {
            if (lab == null) {
                continue;
            }
            syncTeacherDisplayField(lab);
            if (lab.getId() != null) {
                lab.setCurrentAdmins(resolveCurrentLabAdminName(lab.getId()));
            }
        }
    }

    private void syncLiveWritableFields(Lab lab) {
        if (lab == null) {
            return;
        }
        syncTeacherDisplayField(lab);
        if (lab.getId() != null) {
            lab.setCurrentAdmins(resolveCurrentLabAdminName(lab.getId()));
        }
    }

    private void syncTeacherDisplayField(Lab lab) {
        if (lab == null || lab.getTeacherName() == null) {
            return;
        }
        lab.setAdvisors(lab.getTeacherName().trim());
    }

    private String resolveCurrentLabAdminName(Long labId) {
        return displayUserName(resolveCurrentLabAdmin(labId));
    }

    private String displayUserName(User user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "";
    }

    private User resolveCurrentLabAdmin(Long labId) {
        if (labId == null) {
            return null;
        }

        QueryWrapper<LabMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("status", "active")
                .eq("member_role", "lab_admin")
                .orderByDesc("id")
                .last("LIMIT 1");
        LabMember adminMember = labMemberMapper.selectOne(memberQuery);
        if (adminMember != null && adminMember.getUserId() != null) {
            User memberAdmin = userMapper.selectById(adminMember.getUserId());
            if (memberAdmin != null && (memberAdmin.getDeleted() == null || memberAdmin.getDeleted() == 0)) {
                memberAdmin.setPassword(null);
                return memberAdmin;
            }
        }

        QueryWrapper<User> fallbackQuery = new QueryWrapper<>();
        fallbackQuery.eq("role", "admin")
                .eq("lab_id", labId)
                .eq("deleted", 0)
                .orderByDesc("id")
                .last("LIMIT 1");
        User fallbackAdmin = userMapper.selectOne(fallbackQuery);
        if (fallbackAdmin != null) {
            fallbackAdmin.setPassword(null);
        }
        return fallbackAdmin;
    }
}
