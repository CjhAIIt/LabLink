package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Equipment;
import com.lab.recruitment.entity.EquipmentBorrow;
import com.lab.recruitment.entity.EquipmentCategory;
import com.lab.recruitment.entity.EquipmentMaintenanceRecord;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.EquipmentManagementService;
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
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentManagementService equipmentManagementService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) Long collegeId,
                                                  @RequestParam(required = false) Long labId,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) Integer status) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.getEquipmentPage(
                    pageNum, pageSize, collegeId, labId, categoryId, name, status, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> listCategories(@RequestParam(required = false) Long collegeId,
                                                            @RequestParam(required = false) Long labId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.getCategoryOptions(collegeId, labId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> createCategory(@RequestBody EquipmentCategory category) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.saveCategory(category, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> updateCategory(@PathVariable Long categoryId,
                                                      @RequestBody EquipmentCategory category) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            category.setId(categoryId);
            return Result.success(equipmentManagementService.saveCategory(category, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> deleteCategory(@PathVariable Long categoryId) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            equipmentManagementService.deleteCategory(categoryId, currentUser);
            return Result.success(Boolean.TRUE);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> add(@RequestBody Equipment equipment) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.saveEquipment(equipment, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> update(@RequestBody Equipment equipment) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.saveEquipment(equipment, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            equipmentManagementService.deleteEquipment(id, currentUser);
            return Result.success(Boolean.TRUE);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/borrow")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> borrow(@RequestBody EquipmentBorrow borrow) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.applyBorrow(borrow, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/borrow/list")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Map<String, Object>>> listBorrow(@RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) Long collegeId,
                                                        @RequestParam(required = false) Long labId,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) Long equipmentId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Integer status) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.getBorrowPage(
                    pageNum, pageSize, collegeId, labId, userId, equipmentId, keyword, status, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/borrow/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<Map<String, Object>>> listMyBorrow(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) Integer status) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.getBorrowPage(
                    pageNum, pageSize, null, null, currentUser.getId(), null, null, status, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/borrow/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> auditBorrow(@RequestBody EquipmentBorrow borrow) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.auditBorrow(borrow, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/borrow/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> returnEquipment(@RequestBody EquipmentBorrow borrow) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.returnBorrow(borrow.getId(), currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/maintenance/list")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Map<String, Object>>> listMaintenance(@RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                                             @RequestParam(required = false) Long collegeId,
                                                             @RequestParam(required = false) Long labId,
                                                             @RequestParam(required = false) Long equipmentId,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.getMaintenancePage(
                    pageNum, pageSize, collegeId, labId, equipmentId, keyword, status, currentUser
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> createMaintenance(@RequestBody EquipmentMaintenanceRecord record) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.createMaintenance(record, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/maintenance/handle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> handleMaintenance(@RequestBody EquipmentMaintenanceRecord record) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(equipmentManagementService.handleMaintenance(record, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
