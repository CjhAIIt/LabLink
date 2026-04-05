package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.dto.LabWithAdminDTO;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.LabMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.LabSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private LabSpaceService labSpaceService;

    @Override
    public Page<Lab> getLabPage(Integer pageNum, Integer pageSize, String labName, Integer status) {
        Page<Lab> page = new Page<>(pageNum, pageSize);
        Page<Lab> result = baseMapper.selectLabPage(page, labName, status);
        populateCurrentMemberCounts(result.getRecords());
        return result;
    }

    @Override
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
        }
        return created;
    }

    @Override
    public boolean updateLab(Lab lab) {
        if (lab == null || lab.getId() == null) {
            throw new RuntimeException("Lab id is required");
        }

        int currentMembers = countCurrentMembers(lab.getId());
        if (lab.getRecruitNum() != null && lab.getRecruitNum() < currentMembers) {
            throw new RuntimeException("Recruit quota cannot be lower than the current member count");
        }

        lab.setCurrentNum(currentMembers);
        return this.updateById(lab);
    }

    @Override
    public boolean deleteLab(Long id) {
        return this.removeById(id);
    }

    @Override
    public List<LabWithAdminDTO> getLabsWithAdmin() {
        List<Lab> labs = this.list();
        populateCurrentMemberCounts(labs);
        List<LabWithAdminDTO> result = new ArrayList<>();

        for (Lab lab : labs) {
            LabWithAdminDTO dto = new LabWithAdminDTO();
            dto.setLab(lab);

            QueryWrapper<User> adminQuery = new QueryWrapper<>();
            adminQuery.eq("role", "admin")
                    .eq("lab_id", lab.getId())
                    .eq("deleted", 0);
            User admin = userMapper.selectOne(adminQuery);

            dto.setAdmin(admin);
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
}
