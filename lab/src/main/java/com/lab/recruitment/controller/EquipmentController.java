package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Equipment;
import com.lab.recruitment.entity.EquipmentBorrow;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.service.EquipmentBorrowService;
import com.lab.recruitment.service.EquipmentService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private com.lab.recruitment.service.UserService userService;
    
    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentBorrowService borrowService;

    @Autowired
    private AuditLogService auditLogService;

    // --- 设备管理 ---

    @GetMapping("/list")
    public Result<Page<Equipment>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) Long labId,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!"anonymousUser".equals(username)) {
            com.lab.recruitment.entity.User user = userService.findByUsername(username);
            if (user != null && "admin".equals(user.getRole())) {
                labId = user.getLabId();
            }
        }
        
        Page<Equipment> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Equipment> query = new QueryWrapper<>();
        if (labId != null) query.eq("lab_id", labId);
        if (name != null && !name.isEmpty()) query.like("name", name);
        if (status != null) query.eq("status", status);
        query.orderByDesc("create_time");
        return Result.success(equipmentService.page(page, query));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> add(@RequestBody Equipment equipment) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        equipment.setLabId(user.getLabId());
        return Result.success(equipmentService.save(equipment));
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> update(@RequestBody Equipment equipment) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        Equipment exist = equipmentService.getById(equipment.getId());
        if (exist == null) return Result.error("设备不存在");
        if (!exist.getLabId().equals(user.getLabId())) return Result.error("无权操作其他实验室设备");
        
        return Result.success(equipmentService.updateById(equipment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        Equipment exist = equipmentService.getById(id);
        if (exist == null) return Result.error("设备不存在");
        if (!exist.getLabId().equals(user.getLabId())) return Result.error("无权操作其他实验室设备");
        
        return Result.success(equipmentService.removeById(id));
    }

    // --- 借用管理 ---

    @PostMapping("/borrow")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> borrow(@RequestBody EquipmentBorrow borrow) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        if (user.getLabId() == null) {
            return Result.error("您尚未加入任何实验室，无法借用设备");
        }
        
        return Result.success(borrowService.apply(borrow.getEquipmentId(), borrow.getReason(), borrow.getExpectedReturnTime(), username));
    }

    @GetMapping("/borrow/list")
    public Result<Page<EquipmentBorrow>> listBorrow(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) Long equipmentId,
                                                  @RequestParam(required = false) Long labId,
                                                  @RequestParam(required = false) Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!"anonymousUser".equals(username)) {
            com.lab.recruitment.entity.User user = userService.findByUsername(username);
            if (user != null && "admin".equals(user.getRole())) {
                labId = user.getLabId();
            }
        }
                                                      
        Page<EquipmentBorrow> page = new Page<>(pageNum, pageSize);
        QueryWrapper<EquipmentBorrow> query = new QueryWrapper<>();
        if (userId != null) query.eq("user_id", userId);
        if (equipmentId != null) query.eq("equipment_id", equipmentId);
        if (status != null) query.eq("status", status);
        
        if (labId != null) {
            // Find all equipments in this lab
            java.util.List<Equipment> equipments = equipmentService.list(new QueryWrapper<Equipment>().eq("lab_id", labId));
            if (equipments.isEmpty()) {
                // If no equipment, return empty page
                return Result.success(page);
            }
            java.util.List<Long> equipmentIds = equipments.stream().map(Equipment::getId).collect(java.util.stream.Collectors.toList());
            query.in("equipment_id", equipmentIds);
        }
        
        query.orderByDesc("create_time");
        return Result.success(borrowService.page(page, query));
    }
    
    @GetMapping("/borrow/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<EquipmentBorrow>> listMyBorrow(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        return listBorrow(pageNum, pageSize, user.getId(), null, null, status);
    }

    @PostMapping("/borrow/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> auditBorrow(@RequestBody EquipmentBorrow borrow) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        EquipmentBorrow exist = borrowService.getById(borrow.getId());
        if (exist == null) return Result.error("记录不存在");
        
        Equipment equipment = equipmentService.getById(exist.getEquipmentId());
        if (!equipment.getLabId().equals(user.getLabId())) return Result.error("无权操作其他实验室记录");
        
        boolean success = borrowService.audit(borrow.getId(), borrow.getStatus());
        if (success) {
            auditLogService.record(user == null ? null : user.getId(),
                    "equipment_borrow_audit",
                    "equipment_borrow",
                    borrow.getId(),
                    "status=" + borrow.getStatus());
        }
        return Result.success(success);
    }

    @PostMapping("/borrow/return")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> returnEquipment(@RequestBody EquipmentBorrow borrow) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        com.lab.recruitment.entity.User user = userService.findByUsername(username);
        
        EquipmentBorrow exist = borrowService.getById(borrow.getId());
        if (exist == null) return Result.error("记录不存在");
        
        Equipment equipment = equipmentService.getById(exist.getEquipmentId());
        if (!equipment.getLabId().equals(user.getLabId())) return Result.error("无权操作其他实验室记录");

        boolean success = borrowService.returnEquipment(borrow.getId());
        if (success) {
            auditLogService.record(user == null ? null : user.getId(),
                    "equipment_borrow_return",
                    "equipment_borrow",
                    borrow.getId(),
                    null);
        }
        return Result.success(success);
    }
}
