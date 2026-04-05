package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabWithAdminDTO;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.service.AdminManagementService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实验室管理控制器
 * 负责处理实验室相关的增删改查操作
 * 提供实验室列表查询、详情查询、创建、更新、删除等功能
 */
@RestController
@RequestMapping("/labs")
public class LabController {

    @Autowired
    private com.lab.recruitment.service.UserService userService;

    @Autowired
    private LabService labService;
    
    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ... (existing code) ...

    /**
     * 更新实验室详细信息（仅实验室管理员可访问）
     */
    @PutMapping("/update-info")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> updateLabInfo(@RequestBody Lab lab) {
        try {
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            com.lab.recruitment.entity.User user = userService.findByUsername(username);
            
            if (user.getLabId() == null) {
                return Result.error("当前管理员未绑定实验室");
            }
            
            if (!user.getLabId().equals(lab.getId())) {
                return Result.error("无权修改其他实验室信息");
            }
            
            boolean success = labService.updateLab(lab);
            if (success) {
                return Result.success("实验室信息更新成功");
            } else {
                return Result.error("实验室信息更新失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取实验室列表（分页）
     * 支持按实验室名称和状态进行筛选
     * @param pageNum 页码，默认为1
     * @param pageSize 每页数量，默认为10
     * @param labName 实验室名称（可选，模糊查询）
     * @param status 实验室状态（可选，0-未开始，1-进行中，2-已结束）
     * @return 分页的实验室列表
     */
    @GetMapping("/list")
    public Result<Page<Lab>> getLabList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String labName,
            @RequestParam(required = false) Integer status) {
        try {
            Page<Lab> labPage = labService.getLabPage(pageNum, pageSize, collegeId, labName, status);
            return Result.success(labPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getLabStats() {
        try {
            Map<String, Object> result = new HashMap<>();
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_lab WHERE deleted = 0", Long.class);
            result.put("total", total == null ? 0L : total);
            List<Map<String, Object>> byCollege = jdbcTemplate.queryForList(
                    "SELECT l.college_id AS collegeId, c.college_name AS collegeName, COUNT(*) AS labCount " +
                            "FROM t_lab l " +
                            "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                            "WHERE l.deleted = 0 " +
                            "GROUP BY l.college_id, c.college_name " +
                            "ORDER BY labCount DESC"
            );
            result.put("byCollege", byCollege);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取实验室详情
     * @param id 实验室ID
     * @return 实验室详情信息
     */
    @GetMapping("/{id}")
    public Result<Lab> getLabById(@PathVariable Long id) {
        try {
            Lab lab = labService.getLabById(id);
            if (lab == null) {
                return Result.error("实验室不存在");
            }
            return Result.success(lab);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/detail/{id}")
    public Result<Lab> getLabDetailById(@PathVariable Long id) {
        return getLabById(id);
    }

    /**
     * 创建新实验室
     * 仅总负责人（SUPER_ADMIN）可访问
     * @param lab 实验室信息
     * @return 创建结果
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> createLab(@RequestBody Lab lab) {
        try {
            boolean success = labService.createLab(lab);
            if (success) {
                return Result.success("实验室创建成功");
            } else {
                return Result.error("实验室创建失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新实验室信息
     * 仅总负责人（SUPER_ADMIN）可访问
     * @param id 实验室ID
     * @param lab 更新的实验室信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> updateLab(@PathVariable Long id, @RequestBody Lab lab) {
        try {
            lab.setId(id);
            boolean success = labService.updateLab(lab);
            if (success) {
                return Result.success("实验室更新成功");
            } else {
                return Result.error("实验室更新失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除实验室
     * 仅总负责人（SUPER_ADMIN）可访问
     * @param id 实验室ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> deleteLab(@PathVariable Long id) {
        try {
            boolean success = labService.deleteLab(id);
            if (success) {
                return Result.success("实验室删除成功");
            } else {
                return Result.error("实验室删除失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取实验室及其管理员信息列表
     * 仅总负责人（SUPER_ADMIN）可访问
     * @return 实验室和管理员信息列表
     */
    @GetMapping("/list-with-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<LabWithAdminDTO>> getLabsWithAdmin() {
        try {
            List<LabWithAdminDTO> labsWithAdmin = labService.getLabsWithAdmin();
            return Result.success(labsWithAdmin);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
