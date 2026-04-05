package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.recruitment.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<User> {
}