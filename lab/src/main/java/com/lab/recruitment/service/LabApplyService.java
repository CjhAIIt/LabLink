package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.dto.LabApplyAuditDTO;
import com.lab.recruitment.dto.LabApplyCreateDTO;
import com.lab.recruitment.entity.LabApply;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface LabApplyService extends IService<LabApply> {

    boolean createApply(LabApplyCreateDTO createDTO, User currentUser);

    Page<Map<String, Object>> getApplyPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                           String keyword, User currentUser);

    Page<Map<String, Object>> getMyApplyPage(Integer pageNum, Integer pageSize, String status, User currentUser);

    boolean auditApply(Long applyId, LabApplyAuditDTO auditDTO, User currentUser);

    List<Map<String, Object>> getLatestApplies(Long labId, int limit);
}
