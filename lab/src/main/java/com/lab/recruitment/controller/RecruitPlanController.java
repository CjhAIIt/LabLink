package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.RecruitPlan;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.RecruitPlanService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recruit-plans")
public class RecruitPlanController {

    @Autowired
    private RecruitPlanService recruitPlanService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getPlanPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) Long labId,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(recruitPlanService.getPlanPage(pageNum, pageSize, labId, status, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getActivePlans(@RequestParam(required = false) Long labId) {
        try {
            return Result.success(recruitPlanService.getActivePlans(labId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> createPlan(@RequestBody RecruitPlan recruitPlan) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return recruitPlanService.createPlan(recruitPlan, currentUser) ? Result.success("招新计划创建成功", null) : Result.error("招新计划创建失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> updatePlan(@PathVariable Long id, @RequestBody RecruitPlan recruitPlan) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            recruitPlan.setId(id);
            return recruitPlanService.updatePlan(recruitPlan, currentUser) ? Result.success("招新计划更新成功", null) : Result.error("招新计划更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> deletePlan(@PathVariable Long id) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return recruitPlanService.deletePlan(id, currentUser) ? Result.success("招新计划删除成功", null) : Result.error("招新计划删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
