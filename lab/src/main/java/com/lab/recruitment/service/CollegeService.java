package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.College;

import java.util.List;

public interface CollegeService extends IService<College> {

    Page<College> getCollegePage(Integer pageNum, Integer pageSize, String keyword, Integer status);

    List<College> getEnabledColleges();
}
