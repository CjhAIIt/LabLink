package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabCreateApplyAuditDTO;
import com.lab.recruitment.dto.LabCreateApplyCreateDTO;
import com.lab.recruitment.entity.User;

import java.util.Map;

public interface LabCreateApplyService {

    boolean createApply(LabCreateApplyCreateDTO createDTO, User currentUser);

    Page<Map<String, Object>> getApplyPage(Integer pageNum, Integer pageSize, String status, String keyword, User currentUser);

    boolean auditApply(Long id, LabCreateApplyAuditDTO auditDTO, User currentUser);
}
