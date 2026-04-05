package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.RecruitPlan;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface RecruitPlanService extends IService<RecruitPlan> {

    Page<Map<String, Object>> getPlanPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                          String keyword, User currentUser);

    List<Map<String, Object>> getActivePlans(Long labId);

    boolean createPlan(RecruitPlan recruitPlan, User currentUser);

    boolean updatePlan(RecruitPlan recruitPlan, User currentUser);

    boolean deletePlan(Long id, User currentUser);
}
