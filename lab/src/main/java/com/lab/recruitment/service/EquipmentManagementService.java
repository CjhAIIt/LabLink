package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Equipment;
import com.lab.recruitment.entity.EquipmentBorrow;
import com.lab.recruitment.entity.EquipmentCategory;
import com.lab.recruitment.entity.EquipmentMaintenanceRecord;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface EquipmentManagementService {

    Page<Map<String, Object>> getEquipmentPage(Integer pageNum, Integer pageSize, Long collegeId, Long labId,
                                               Long categoryId, String keyword, Integer status, User currentUser);

    Map<String, Object> saveEquipment(Equipment equipment, User currentUser);

    void deleteEquipment(Long equipmentId, User currentUser);

    List<Map<String, Object>> getCategoryOptions(Long collegeId, Long labId, User currentUser);

    Map<String, Object> saveCategory(EquipmentCategory category, User currentUser);

    void deleteCategory(Long categoryId, User currentUser);

    Page<Map<String, Object>> getBorrowPage(Integer pageNum, Integer pageSize, Long collegeId, Long labId,
                                            Long userId, Long equipmentId, String keyword, Integer status,
                                            User currentUser);

    Map<String, Object> applyBorrow(EquipmentBorrow borrow, User currentUser);

    Map<String, Object> auditBorrow(EquipmentBorrow borrow, User currentUser);

    Map<String, Object> returnBorrow(Long borrowId, User currentUser);

    Page<Map<String, Object>> getMaintenancePage(Integer pageNum, Integer pageSize, Long collegeId, Long labId,
                                                 Long equipmentId, String keyword, String status, User currentUser);

    Map<String, Object> createMaintenance(EquipmentMaintenanceRecord record, User currentUser);

    Map<String, Object> handleMaintenance(EquipmentMaintenanceRecord record, User currentUser);
}
