package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.TeacherRegisterApplyAuditDTO;
import com.lab.recruitment.dto.TeacherRegisterDTO;
import com.lab.recruitment.dto.TeacherRegisterEmailCodeDTO;
import com.lab.recruitment.entity.User;

import java.util.Map;

public interface TeacherRegisterApplyService {

    void sendRegisterEmailCode(TeacherRegisterEmailCodeDTO registerEmailCodeDTO);

    boolean submitRegisterApply(TeacherRegisterDTO registerDTO);

    Page<Map<String, Object>> getApplyPage(Integer pageNum, Integer pageSize, String status, String keyword, User currentUser);

    boolean auditApply(Long id, TeacherRegisterApplyAuditDTO auditDTO, User currentUser);

    String resolveLoginBlockMessage(String username);
}
