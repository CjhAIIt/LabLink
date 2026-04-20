package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.LabExitApplication;
import com.lab.recruitment.entity.User;

public interface LabExitApplicationService extends IService<LabExitApplication> {

    boolean submit(Long userId, String reason);

    Page<LabExitApplication> getMyApplicationPage(Integer pageNum, Integer pageSize, Long userId);

    Page<LabExitApplication> getLabApplicationPage(Integer pageNum, Integer pageSize, Long labId, Integer status, String realName);

    boolean audit(Long applicationId, Integer status, String auditRemark, User admin);
}
