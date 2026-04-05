package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.dto.LabWithAdminDTO;
import com.lab.recruitment.entity.Lab;

import java.util.List;

public interface LabService extends IService<Lab> {
    
    Page<Lab> getLabPage(Integer pageNum, Integer pageSize, String labName, Integer status);
    
    Lab getLabById(Long id);
    
    boolean createLab(Lab lab);
    
    boolean updateLab(Lab lab);
    
    boolean deleteLab(Long id);
    
    List<LabWithAdminDTO> getLabsWithAdmin();

    int countCurrentMembers(Long labId);

    void syncCurrentMemberCount(Long labId);
}
