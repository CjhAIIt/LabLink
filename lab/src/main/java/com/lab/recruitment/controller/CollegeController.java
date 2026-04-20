package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.College;
import com.lab.recruitment.service.CollegeService;
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

@RestController
@RequestMapping("/colleges")
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<College>> getCollegePage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        try {
            return Result.success(collegeService.getCollegePage(pageNum, pageSize, keyword, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/options")
    public Result<List<College>> getCollegeOptions() {
        try {
            return Result.success(collegeService.getEnabledColleges());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> createCollege(@RequestBody College college) {
        try {
            currentUserAccessor.assertSuperAdmin(currentUserAccessor.getCurrentUser());
            return collegeService.save(college) ? Result.success("学院创建成功", null) : Result.error("学院创建失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> updateCollege(@PathVariable Long id, @RequestBody College college) {
        try {
            currentUserAccessor.assertSuperAdmin(currentUserAccessor.getCurrentUser());
            college.setId(id);
            return collegeService.updateById(college) ? Result.success("学院更新成功", null) : Result.error("学院更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> deleteCollege(@PathVariable Long id) {
        try {
            currentUserAccessor.assertSuperAdmin(currentUserAccessor.getCurrentUser());
            return collegeService.removeById(id) ? Result.success("学院删除成功", null) : Result.error("学院删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
