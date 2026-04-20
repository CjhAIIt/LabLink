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
        return result;
    }

    @Override
    @Cacheable(cacheNames = PlatformCacheNames.LAB_DETAIL, key = "#id", condition = "#id != null")
    public Lab getLabById(Long id) {
        Lab lab = this.getById(id);
        if (lab != null) {
            populateCurrentMemberCounts(Collections.singletonList(lab));
        }
        return lab;
    }

    @Override
    public boolean createLab(Lab lab) {
        lab.setCurrentNum(0);
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

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("role", "student")
                .eq("deleted", 0);
        return Math.toIntExact(userMapper.selectCount(queryWrapper));
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

        QueryWrapper<User> memberQuery = new QueryWrapper<>();
        memberQuery.select("id", "lab_id")
                .in("lab_id", labIds)
                .eq("role", "student")
                .eq("deleted", 0);

        Map<Long, Integer> memberCountMap = new HashMap<>();
        for (User user : userMapper.selectList(memberQuery)) {
            if (user.getLabId() == null) {
                continue;
            }
            memberCountMap.merge(user.getLabId(), 1, Integer::sum);
        }

        for (Lab lab : labs) {
            if (lab == null || lab.getId() == null) {
                continue;
            }
            lab.setCurrentNum(memberCountMap.getOrDefault(lab.getId(), 0));
        }
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
