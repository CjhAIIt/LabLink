package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.College;
import com.lab.recruitment.mapper.CollegeMapper;
import com.lab.recruitment.service.CollegeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CollegeServiceImpl extends ServiceImpl<CollegeMapper, College> implements CollegeService {

    @Override
    public Page<College> getCollegePage(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<College> page = new Page<>(pageNum, pageSize);
        QueryWrapper<College> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like("college_name", keyword).or().like("college_code", keyword));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("college_code").orderByDesc("id");
        return this.page(page, wrapper);
    }

    @Override
    public List<College> getEnabledColleges() {
        QueryWrapper<College> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("college_code").orderByAsc("id");
        return this.list(wrapper);
    }
}
