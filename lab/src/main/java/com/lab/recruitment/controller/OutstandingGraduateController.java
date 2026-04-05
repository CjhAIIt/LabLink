package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.OutstandingGraduate;
import com.lab.recruitment.service.OutstandingGraduateService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/graduate")
public class OutstandingGraduateController {

    @Autowired
    private com.lab.recruitment.service.UserService userService;
    
    @Autowired
    private OutstandingGraduateService graduateService;

    @GetMapping("/list")
    public Result<Page<OutstandingGraduate>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) Long labId) {
        Page<OutstandingGraduate> page = new Page<>(pageNum, pageSize);
        QueryWrapper<OutstandingGraduate> query = new QueryWrapper<>();
        if (labId != null) query.eq("lab_id", labId);
        query.orderByDesc("graduation_year");
        return Result.success(graduateService.page(page, query));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> add(@RequestBody OutstandingGraduate graduate) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        graduate.setLabId(user.getLabId());
        return Result.success(graduateService.save(graduate));
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> update(@RequestBody OutstandingGraduate graduate) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        OutstandingGraduate exist = graduateService.getById(graduate.getId());
        if (exist == null) return Result.error("记录不存在");
        if (!exist.getLabId().equals(user.getLabId())) return Result.error("无权操作其他实验室数据");
        
        return Result.success(graduateService.updateById(graduate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        OutstandingGraduate exist = graduateService.getById(id);
        if (exist == null) return Result.error("记录不存在");
        if (!exist.getLabId().equals(user.getLabId())) return Result.error("无权操作其他实验室数据");
        
        return Result.success(graduateService.removeById(id));
    }
}
