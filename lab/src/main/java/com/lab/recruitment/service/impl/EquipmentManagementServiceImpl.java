package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Equipment;
import com.lab.recruitment.entity.EquipmentBorrow;
import com.lab.recruitment.entity.EquipmentCategory;
import com.lab.recruitment.entity.EquipmentMaintenanceRecord;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.EquipmentBorrowMapper;
import com.lab.recruitment.mapper.EquipmentCategoryMapper;
import com.lab.recruitment.mapper.EquipmentMaintenanceRecordMapper;
import com.lab.recruitment.mapper.EquipmentMapper;
import com.lab.recruitment.service.AuditLogService;
import com.lab.recruitment.service.EquipmentManagementService;
import com.lab.recruitment.service.StatisticsRefreshService;
import com.lab.recruitment.service.SystemNotificationService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.support.DataScope;
import com.lab.recruitment.support.DataScopeLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class EquipmentManagementServiceImpl implements EquipmentManagementService {

    private static final int EQUIPMENT_STATUS_IDLE = 0;
    private static final int EQUIPMENT_STATUS_BORROWED = 1;
    private static final int EQUIPMENT_STATUS_MAINTAINING = 2;
    private static final int EQUIPMENT_STATUS_SCRAPPED = 3;

    private static final int BORROW_STATUS_PENDING = 0;
    private static final int BORROW_STATUS_APPROVED = 1;
    private static final int BORROW_STATUS_REJECTED = 2;
    private static final int BORROW_STATUS_RETURNED = 3;

    private static final String MAINTENANCE_PENDING = "PENDING";
    private static final String MAINTENANCE_IN_PROGRESS = "IN_PROGRESS";
    private static final String MAINTENANCE_RESOLVED = "RESOLVED";

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private EquipmentBorrowMapper equipmentBorrowMapper;

    @Autowired
    private EquipmentCategoryMapper equipmentCategoryMapper;

    @Autowired
    private EquipmentMaintenanceRecordMapper equipmentMaintenanceRecordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private StatisticsRefreshService statisticsRefreshService;

    @Override
    public Page<Map<String, Object>> getEquipmentPage(Integer pageNum, Integer pageSize, Long collegeId, Long labId,
                                                      Long categoryId, String keyword, Integer status,
                                                      User currentUser) {
        DataScope scope = resolveReadScope(currentUser, collegeId, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE e.deleted = 0");
        appendScopeCondition(where, args, scope, "e.lab_id", "l.college_id");

        if (categoryId != null) {
            where.append(" AND e.category_id = ?");
            args.add(categoryId);
        }
        if (status != null) {
            where.append(" AND e.status = ?");
            args.add(status);
        }
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            where.append(" AND (e.name LIKE ? OR e.device_code LIKE ? OR e.serial_number LIKE ? OR e.brand LIKE ? OR e.model LIKE ? OR e.location LIKE ?)");
            String likeValue = "%" + normalizedKeyword + "%";
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_equipment e " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_equipment_category c ON c.id = e.category_id AND c.deleted = 0" +
                        where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT e.id, e.lab_id AS labId, l.lab_name AS labName, e.category_id AS categoryId, " +
                        "c.name AS categoryName, e.name, e.type, e.device_code AS deviceCode, " +
                        "e.serial_number AS serialNumber, e.brand, e.model, e.purchase_date AS purchaseDate, " +
                        "e.location, e.image_url AS imageUrl, e.description, e.remark, e.status, " +
                        "e.create_time AS createTime, e.update_time AS updateTime " +
                        "FROM t_equipment e " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_equipment_category c ON c.id = e.category_id AND c.deleted = 0" +
                        where +
                        " ORDER BY e.update_time DESC, e.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    @Transactional
    public Map<String, Object> saveEquipment(Equipment equipment, User currentUser) {
        if (equipment == null) {
            throw new RuntimeException("Equipment payload is required");
        }

        Equipment target;
        boolean isNew = equipment.getId() == null;
        Long operatorId = currentUser == null ? null : currentUser.getId();

        if (isNew) {
            target = new Equipment();
            target.setLabId(resolveWritableLabId(currentUser, equipment.getLabId()));
            target.setCreatedBy(operatorId);
        } else {
            target = mustGetEquipment(equipment.getId());
            assertManagementLabScope(currentUser, target.getLabId());
        }

        String name = trimToNull(equipment.getName());
        String deviceCode = trimToNull(equipment.getDeviceCode());
        if (name == null) {
            throw new RuntimeException("Equipment name is required");
        }
        if (deviceCode == null) {
            throw new RuntimeException("Device code is required");
        }

        EquipmentCategory category = null;
        if (equipment.getCategoryId() != null) {
            category = mustGetCategory(equipment.getCategoryId());
            if (!Objects.equals(category.getLabId(), target.getLabId())) {
                throw new RuntimeException("The selected category does not belong to the current lab");
            }
        }

        assertUniqueDeviceCode(target.getLabId(), deviceCode, target.getId());

        target.setCategoryId(category == null ? null : category.getId());
        target.setName(name);
        target.setDeviceCode(deviceCode);
        target.setType(category != null ? category.getName() : defaultValue(trimToNull(equipment.getType()), "General"));
        target.setSerialNumber(defaultValue(trimToNull(equipment.getSerialNumber()), deviceCode));
        target.setBrand(trimToNull(equipment.getBrand()));
        target.setModel(trimToNull(equipment.getModel()));
        target.setPurchaseDate(equipment.getPurchaseDate());
        target.setLocation(trimToNull(equipment.getLocation()));
        target.setImageUrl(trimToNull(equipment.getImageUrl()));
        target.setDescription(trimToNull(equipment.getDescription()));
        target.setRemark(trimToNull(equipment.getRemark()));
        target.setStatus(normalizeEquipmentStatus(equipment.getStatus(), isNew ? EQUIPMENT_STATUS_IDLE : target.getStatus()));
        target.setUpdatedBy(operatorId);

        if (isNew) {
            equipmentMapper.insert(target);
            auditLogService.record(operatorId, "equipment_create", "equipment", target.getId(), "deviceCode=" + deviceCode);
        } else {
            equipmentMapper.updateById(target);
            auditLogService.record(operatorId, "equipment_update", "equipment", target.getId(), "deviceCode=" + deviceCode);
        }
        publishEquipmentStatisticsRefresh(target.getLabId(), operatorId, isNew ? "equipment_create" : "equipment_update");
        return getEquipmentView(target.getId());
    }

    @Override
    @Transactional
    public void deleteEquipment(Long equipmentId, User currentUser) {
        Equipment equipment = mustGetEquipment(equipmentId);
        assertManagementLabScope(currentUser, equipment.getLabId());

        QueryWrapper<EquipmentBorrow> borrowQuery = new QueryWrapper<>();
        borrowQuery.eq("equipment_id", equipmentId)
                .eq("deleted", 0)
                .in("status", BORROW_STATUS_PENDING, BORROW_STATUS_APPROVED);
        if (equipmentBorrowMapper.selectCount(borrowQuery) > 0) {
            throw new RuntimeException("The equipment still has pending or active borrow records");
        }

        QueryWrapper<EquipmentMaintenanceRecord> maintenanceQuery = new QueryWrapper<>();
        maintenanceQuery.eq("equipment_id", equipmentId)
                .eq("deleted", 0)
                .ne("maintenance_status", MAINTENANCE_RESOLVED);
        if (equipmentMaintenanceRecordMapper.selectCount(maintenanceQuery) > 0) {
            throw new RuntimeException("The equipment still has unfinished maintenance records");
        }

        equipmentMapper.deleteById(equipmentId);
        auditLogService.record(currentUser == null ? null : currentUser.getId(),
                "equipment_delete", "equipment", equipmentId, equipment.getDeviceCode());
        publishEquipmentStatisticsRefresh(equipment.getLabId(), currentUser == null ? null : currentUser.getId(), "equipment_delete");
    }

    @Override
    public List<Map<String, Object>> getCategoryOptions(Long collegeId, Long labId, User currentUser) {
        DataScope scope = resolveReadScope(currentUser, collegeId, labId);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE c.deleted = 0");
        appendScopeCondition(where, args, scope, "c.lab_id", "l.college_id");

        return jdbcTemplate.queryForList(
                "SELECT c.id, c.lab_id AS labId, l.lab_name AS labName, c.name, c.description, " +
                        "c.create_time AS createTime, COUNT(e.id) AS equipmentCount " +
                        "FROM t_equipment_category c " +
                        "LEFT JOIN t_lab l ON l.id = c.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_equipment e ON e.category_id = c.id AND e.deleted = 0" +
                        where +
                        " GROUP BY c.id, c.lab_id, l.lab_name, c.name, c.description, c.create_time " +
                        " ORDER BY c.name ASC, c.id ASC",
                args.toArray()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> saveCategory(EquipmentCategory category, User currentUser) {
        if (category == null) {
            throw new RuntimeException("Category payload is required");
        }

        EquipmentCategory target;
        boolean isNew = category.getId() == null;
        Long operatorId = currentUser == null ? null : currentUser.getId();

        if (isNew) {
            target = new EquipmentCategory();
            target.setLabId(resolveWritableLabId(currentUser, category.getLabId()));
            target.setCreatedBy(operatorId);
        } else {
            target = mustGetCategory(category.getId());
            assertManagementLabScope(currentUser, target.getLabId());
        }

        String name = trimToNull(category.getName());
        if (name == null) {
            throw new RuntimeException("Category name is required");
        }

        QueryWrapper<EquipmentCategory> duplicateQuery = new QueryWrapper<>();
        duplicateQuery.eq("lab_id", target.getLabId())
                .eq("deleted", 0)
                .eq("name", name);
        if (target.getId() != null) {
            duplicateQuery.ne("id", target.getId());
        }
        if (equipmentCategoryMapper.selectCount(duplicateQuery) > 0) {
            throw new RuntimeException("The category name already exists in the current lab");
        }

        target.setName(name);
        target.setDescription(trimToNull(category.getDescription()));
        target.setUpdatedBy(operatorId);

        if (isNew) {
            equipmentCategoryMapper.insert(target);
            auditLogService.record(operatorId, "equipment_category_create", "equipment_category", target.getId(), name);
        } else {
            equipmentCategoryMapper.updateById(target);
            auditLogService.record(operatorId, "equipment_category_update", "equipment_category", target.getId(), name);
        }
        publishEquipmentStatisticsRefresh(target.getLabId(), operatorId, isNew ? "equipment_category_create" : "equipment_category_update");
        return getCategoryView(target.getId());
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, User currentUser) {
        EquipmentCategory category = mustGetCategory(categoryId);
        assertManagementLabScope(currentUser, category.getLabId());

        QueryWrapper<Equipment> equipmentQuery = new QueryWrapper<>();
        equipmentQuery.eq("category_id", categoryId).eq("deleted", 0);
        if (equipmentMapper.selectCount(equipmentQuery) > 0) {
            throw new RuntimeException("The category is still referenced by equipment records");
        }

        equipmentCategoryMapper.deleteById(categoryId);
        auditLogService.record(currentUser == null ? null : currentUser.getId(),
                "equipment_category_delete", "equipment_category", categoryId, category.getName());
        publishEquipmentStatisticsRefresh(category.getLabId(), currentUser == null ? null : currentUser.getId(), "equipment_category_delete");
    }

    @Override
    public Page<Map<String, Object>> getBorrowPage(Integer pageNum, Integer pageSize, Long collegeId, Long labId,
                                                   Long userId, Long equipmentId, String keyword, Integer status,
                                                   User currentUser) {
        DataScope scope = resolveReadScope(currentUser, collegeId, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE b.deleted = 0 AND e.deleted = 0");
        appendScopeCondition(where, args, scope, "e.lab_id", "l.college_id");

        if (currentUserAccessor.isStudentIdentity(currentUser)) {
            where.append(" AND b.user_id = ?");
            args.add(currentUser.getId());
        } else if (userId != null) {
            where.append(" AND b.user_id = ?");
            args.add(userId);
        }

        if (equipmentId != null) {
            where.append(" AND b.equipment_id = ?");
            args.add(equipmentId);
        }
        if (status != null) {
            where.append(" AND b.status = ?");
            args.add(status);
        }
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            String likeValue = "%" + normalizedKeyword + "%";
            where.append(" AND (e.name LIKE ? OR e.device_code LIKE ? OR u.real_name LIKE ? OR u.username LIKE ? OR b.reason LIKE ?)");
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_equipment_borrow b " +
                        "LEFT JOIN t_equipment e ON e.id = b.equipment_id " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user u ON u.id = b.user_id AND u.deleted = 0" +
                        where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT b.id, b.equipment_id AS equipmentId, b.user_id AS userId, e.lab_id AS labId, " +
                        "l.lab_name AS labName, e.name AS equipmentName, e.device_code AS deviceCode, " +
                        "u.username, u.real_name AS userRealName, b.reason, b.status, " +
                        "b.borrow_time AS borrowTime, b.return_time AS returnTime, " +
                        "b.expected_return_time AS expectedReturnTime, b.create_time AS createTime " +
                        "FROM t_equipment_borrow b " +
                        "LEFT JOIN t_equipment e ON e.id = b.equipment_id " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user u ON u.id = b.user_id AND u.deleted = 0" +
                        where +
                        " ORDER BY b.create_time DESC, b.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    @Transactional
    public Map<String, Object> applyBorrow(EquipmentBorrow borrow, User currentUser) {
        if (!currentUserAccessor.isStudentIdentity(currentUser)) {
            throw new RuntimeException("Only student accounts can submit borrow requests");
        }
        if (borrow == null || borrow.getEquipmentId() == null) {
            throw new RuntimeException("Equipment is required");
        }

        DataScope scope = currentUserAccessor.getCurrentDataScope();
        if (scope.getLabId() == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }

        Equipment equipment = mustGetEquipment(borrow.getEquipmentId());
        if (!Objects.equals(scope.getLabId(), equipment.getLabId())) {
            throw new RuntimeException("You can only borrow equipment from your own lab");
        }
        if (!Objects.equals(equipment.getStatus(), EQUIPMENT_STATUS_IDLE)) {
            throw new RuntimeException("The equipment is not available");
        }

        String reason = trimToNull(borrow.getReason());
        if (reason == null) {
            throw new RuntimeException("Borrow reason is required");
        }

        EquipmentBorrow target = new EquipmentBorrow();
        target.setEquipmentId(equipment.getId());
        target.setUserId(currentUser.getId());
        target.setReason(reason);
        target.setExpectedReturnTime(borrow.getExpectedReturnTime());
        target.setStatus(BORROW_STATUS_PENDING);
        equipmentBorrowMapper.insert(target);

        auditLogService.record(currentUser.getId(), "equipment_borrow_submit", "equipment_borrow", target.getId(),
                "equipmentId=" + equipment.getId());
        return getBorrowView(target.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> auditBorrow(EquipmentBorrow borrow, User currentUser) {
        if (borrow == null || borrow.getId() == null) {
            throw new RuntimeException("Borrow record is required");
        }
        if (borrow.getStatus() == null || (borrow.getStatus() != BORROW_STATUS_APPROVED && borrow.getStatus() != BORROW_STATUS_REJECTED)) {
            throw new RuntimeException("Invalid borrow review status");
        }

        EquipmentBorrow target = mustGetBorrow(borrow.getId());
        if (!Objects.equals(target.getStatus(), BORROW_STATUS_PENDING)) {
            throw new RuntimeException("The borrow record has already been reviewed");
        }

        Equipment equipment = mustGetEquipment(target.getEquipmentId());
        assertManagementLabScope(currentUser, equipment.getLabId());

        if (Objects.equals(borrow.getStatus(), BORROW_STATUS_APPROVED)) {
            if (!Objects.equals(equipment.getStatus(), EQUIPMENT_STATUS_IDLE)) {
                throw new RuntimeException("The equipment is not in an idle state");
            }
            target.setStatus(BORROW_STATUS_APPROVED);
            target.setBorrowTime(LocalDateTime.now());
            equipment.setStatus(EQUIPMENT_STATUS_BORROWED);
            equipment.setUpdatedBy(currentUser.getId());
            equipmentMapper.updateById(equipment);
        } else {
            target.setStatus(BORROW_STATUS_REJECTED);
        }

        equipmentBorrowMapper.updateById(target);
        auditLogService.record(currentUser.getId(), "equipment_borrow_review", "equipment_borrow", target.getId(),
                "status=" + target.getStatus());
        systemNotificationService.createNotificationAsync(
                target.getUserId(),
                Objects.equals(target.getStatus(), BORROW_STATUS_APPROVED) ? "Borrow request approved" : "Borrow request rejected",
                Objects.equals(target.getStatus(), BORROW_STATUS_APPROVED)
                        ? "Your equipment borrow request has been approved."
                        : "Your equipment borrow request was rejected.",
                "EQUIPMENT_BORROW",
                target.getId(),
                "/student/my-lab"
        );
        publishEquipmentStatisticsRefresh(equipment.getLabId(), currentUser.getId(), "equipment_borrow_review");
        return getBorrowView(target.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> returnBorrow(Long borrowId, User currentUser) {
        EquipmentBorrow target = mustGetBorrow(borrowId);
        if (!Objects.equals(target.getStatus(), BORROW_STATUS_APPROVED)) {
            throw new RuntimeException("Only active borrow records can be returned");
        }

        Equipment equipment = mustGetEquipment(target.getEquipmentId());
        assertManagementLabScope(currentUser, equipment.getLabId());

        target.setStatus(BORROW_STATUS_RETURNED);
        target.setReturnTime(LocalDateTime.now());
        target.setReturnConfirmedBy(currentUser.getId());
        target.setReturnConfirmTime(LocalDateTime.now());
        equipmentBorrowMapper.updateById(target);

        if (!Objects.equals(equipment.getStatus(), EQUIPMENT_STATUS_SCRAPPED)) {
            equipment.setStatus(EQUIPMENT_STATUS_IDLE);
        }
        equipment.setUpdatedBy(currentUser.getId());
        equipmentMapper.updateById(equipment);

        auditLogService.record(currentUser.getId(), "equipment_borrow_return", "equipment_borrow", target.getId(), null);
        systemNotificationService.createNotificationAsync(
                target.getUserId(),
                "Equipment returned",
                "Your borrowed equipment has been marked as returned.",
                "EQUIPMENT_BORROW",
                target.getId(),
                "/student/my-lab"
        );
        publishEquipmentStatisticsRefresh(equipment.getLabId(), currentUser.getId(), "equipment_borrow_return");
        return getBorrowView(target.getId());
    }

    @Override
    public Page<Map<String, Object>> getMaintenancePage(Integer pageNum, Integer pageSize, Long collegeId, Long labId,
                                                        Long equipmentId, String keyword, String status,
                                                        User currentUser) {
        DataScope scope = resolveReadScope(currentUser, collegeId, labId);
        long current = Math.max(pageNum == null ? 1 : pageNum, 1);
        long size = Math.max(pageSize == null ? 10 : pageSize, 1);
        long offset = (current - 1) * size;

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE m.deleted = 0 AND e.deleted = 0");
        appendScopeCondition(where, args, scope, "e.lab_id", "l.college_id");

        if (equipmentId != null) {
            where.append(" AND m.equipment_id = ?");
            args.add(equipmentId);
        }
        String normalizedStatus = normalizeMaintenanceStatus(status, false);
        if (normalizedStatus != null) {
            where.append(" AND m.maintenance_status = ?");
            args.add(normalizedStatus);
        }
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            String likeValue = "%" + normalizedKeyword + "%";
            where.append(" AND (e.name LIKE ? OR e.device_code LIKE ? OR m.issue_desc LIKE ? OR reporter.real_name LIKE ?)");
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_equipment_maintenance m " +
                        "LEFT JOIN t_equipment e ON e.id = m.equipment_id " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user reporter ON reporter.id = m.report_user_id AND reporter.deleted = 0" +
                        where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(offset);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT m.id, m.equipment_id AS equipmentId, e.lab_id AS labId, l.lab_name AS labName, " +
                        "e.name AS equipmentName, e.device_code AS deviceCode, m.issue_desc AS issueDesc, " +
                        "m.maintenance_status AS maintenanceStatus, m.result_desc AS resultDesc, " +
                        "m.report_user_id AS reportUserId, reporter.real_name AS reportUserName, " +
                        "m.handled_by AS handledBy, handler.real_name AS handledByName, " +
                        "m.handled_at AS handledAt, m.create_time AS createTime, m.update_time AS updateTime " +
                        "FROM t_equipment_maintenance m " +
                        "LEFT JOIN t_equipment e ON e.id = m.equipment_id " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user reporter ON reporter.id = m.report_user_id AND reporter.deleted = 0 " +
                        "LEFT JOIN t_user handler ON handler.id = m.handled_by AND handler.deleted = 0" +
                        where +
                        " ORDER BY m.create_time DESC, m.id DESC LIMIT ?, ?",
                queryArgs.toArray()
        );

        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total == null ? 0L : total);
        return page;
    }

    @Override
    @Transactional
    public Map<String, Object> createMaintenance(EquipmentMaintenanceRecord record, User currentUser) {
        if (record == null || record.getEquipmentId() == null) {
            throw new RuntimeException("Equipment is required");
        }
        Equipment equipment = mustGetEquipment(record.getEquipmentId());
        assertManagementLabScope(currentUser, equipment.getLabId());

        String issueDesc = trimToNull(record.getIssueDesc());
        if (issueDesc == null) {
            throw new RuntimeException("Issue description is required");
        }
        if (Objects.equals(equipment.getStatus(), EQUIPMENT_STATUS_BORROWED)) {
            throw new RuntimeException("Borrowed equipment cannot be marked for maintenance");
        }

        EquipmentMaintenanceRecord target = new EquipmentMaintenanceRecord();
        target.setEquipmentId(equipment.getId());
        target.setLabId(equipment.getLabId());
        target.setReportUserId(currentUser.getId());
        target.setIssueDesc(issueDesc);
        target.setMaintenanceStatus(normalizeMaintenanceStatus(record.getMaintenanceStatus(), true));
        target.setResultDesc(trimToNull(record.getResultDesc()));
        target.setCreatedBy(currentUser.getId());
        target.setUpdatedBy(currentUser.getId());
        equipmentMaintenanceRecordMapper.insert(target);

        equipment.setStatus(EQUIPMENT_STATUS_MAINTAINING);
        equipment.setUpdatedBy(currentUser.getId());
        equipmentMapper.updateById(equipment);

        auditLogService.record(currentUser.getId(), "equipment_maintenance_create", "equipment_maintenance", target.getId(),
                "equipmentId=" + equipment.getId());
        publishEquipmentStatisticsRefresh(equipment.getLabId(), currentUser.getId(), "equipment_maintenance_create");
        return getMaintenanceView(target.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> handleMaintenance(EquipmentMaintenanceRecord record, User currentUser) {
        if (record == null || record.getId() == null) {
            throw new RuntimeException("Maintenance record is required");
        }

        EquipmentMaintenanceRecord target = mustGetMaintenance(record.getId());
        Equipment equipment = mustGetEquipment(target.getEquipmentId());
        assertManagementLabScope(currentUser, equipment.getLabId());

        String maintenanceStatus = normalizeMaintenanceStatus(record.getMaintenanceStatus(), false);
        if (maintenanceStatus == null) {
            throw new RuntimeException("Maintenance status is required");
        }

        target.setMaintenanceStatus(maintenanceStatus);
        target.setResultDesc(trimToNull(record.getResultDesc()));
        target.setHandledBy(currentUser.getId());
        target.setHandledAt(LocalDateTime.now());
        target.setUpdatedBy(currentUser.getId());
        equipmentMaintenanceRecordMapper.updateById(target);

        if (MAINTENANCE_RESOLVED.equals(maintenanceStatus)) {
            if (!Objects.equals(equipment.getStatus(), EQUIPMENT_STATUS_SCRAPPED)) {
                equipment.setStatus(EQUIPMENT_STATUS_IDLE);
            }
        } else {
            equipment.setStatus(EQUIPMENT_STATUS_MAINTAINING);
        }
        equipment.setUpdatedBy(currentUser.getId());
        equipmentMapper.updateById(equipment);

        auditLogService.record(currentUser.getId(), "equipment_maintenance_handle", "equipment_maintenance", target.getId(),
                "status=" + maintenanceStatus);
        publishEquipmentStatisticsRefresh(equipment.getLabId(), currentUser.getId(), "equipment_maintenance_handle");
        return getMaintenanceView(target.getId());
    }

    private void publishEquipmentStatisticsRefresh(Long labId, Long operatorUserId, String reason) {
        statisticsRefreshService.refreshAsync(
                "equipment",
                currentUserAccessor.resolveCollegeIdByLabId(labId),
                labId,
                operatorUserId,
                reason
        );
    }

    private DataScope resolveReadScope(User currentUser, Long requestedCollegeId, Long requestedLabId) {
        if (currentUserAccessor.isAdmin(currentUser)) {
            return currentUserAccessor.resolveManagementScope(currentUser, requestedCollegeId, requestedLabId);
        }

        DataScope currentScope = currentUserAccessor.getCurrentDataScope();
        if (currentScope.getLabId() == null) {
            throw new RuntimeException("Current account is not bound to a lab");
        }
        Long collegeId = currentUserAccessor.resolveCollegeIdByLabId(currentScope.getLabId());
        if (requestedLabId != null && !Objects.equals(requestedLabId, currentScope.getLabId())) {
            throw new RuntimeException("No access to another lab");
        }
        if (requestedCollegeId != null && !Objects.equals(requestedCollegeId, collegeId)) {
            throw new RuntimeException("No access to another college");
        }

        DataScope scope = new DataScope();
        scope.setLevel(DataScopeLevel.LAB);
        scope.setUserId(currentUser == null ? null : currentUser.getId());
        scope.setDisplayRole(currentScope.getDisplayRole());
        scope.setCollegeId(collegeId);
        scope.setLabId(currentScope.getLabId());
        return scope;
    }

    private Long resolveWritableLabId(User currentUser, Long requestedLabId) {
        if (!currentUserAccessor.isAdmin(currentUser)) {
            throw new RuntimeException("Current account cannot manage equipment data");
        }
        return currentUserAccessor.resolveLabScope(currentUser, requestedLabId);
    }

    private void assertManagementLabScope(User currentUser, Long labId) {
        if (!currentUserAccessor.isAdmin(currentUser)) {
            throw new RuntimeException("Current account cannot manage equipment data");
        }
        currentUserAccessor.assertLabScope(currentUser, labId);
    }

    private void appendScopeCondition(StringBuilder where, List<Object> args, DataScope scope,
                                      String labColumn, String collegeColumn) {
        if (scope == null) {
            return;
        }
        if (scope.getCollegeId() != null) {
            where.append(" AND ").append(collegeColumn).append(" = ?");
            args.add(scope.getCollegeId());
        }
        if (scope.getLabId() != null) {
            where.append(" AND ").append(labColumn).append(" = ?");
            args.add(scope.getLabId());
        }
    }

    private Equipment mustGetEquipment(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null || Objects.equals(equipment.getDeleted(), 1)) {
            throw new RuntimeException("Equipment does not exist");
        }
        return equipment;
    }

    private EquipmentBorrow mustGetBorrow(Long borrowId) {
        EquipmentBorrow borrow = equipmentBorrowMapper.selectById(borrowId);
        if (borrow == null || Objects.equals(borrow.getDeleted(), 1)) {
            throw new RuntimeException("Borrow record does not exist");
        }
        return borrow;
    }

    private EquipmentCategory mustGetCategory(Long categoryId) {
        EquipmentCategory category = equipmentCategoryMapper.selectById(categoryId);
        if (category == null || Objects.equals(category.getDeleted(), 1)) {
            throw new RuntimeException("Equipment category does not exist");
        }
        return category;
    }

    private EquipmentMaintenanceRecord mustGetMaintenance(Long recordId) {
        EquipmentMaintenanceRecord record = equipmentMaintenanceRecordMapper.selectById(recordId);
        if (record == null || Objects.equals(record.getDeleted(), 1)) {
            throw new RuntimeException("Maintenance record does not exist");
        }
        return record;
    }

    private void assertUniqueDeviceCode(Long labId, String deviceCode, Long excludeId) {
        QueryWrapper<Equipment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lab_id", labId)
                .eq("deleted", 0)
                .eq("device_code", deviceCode);
        if (excludeId != null) {
            queryWrapper.ne("id", excludeId);
        }
        if (equipmentMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("The device code already exists in the current lab");
        }
    }

    private Integer normalizeEquipmentStatus(Integer requestedStatus, Integer fallbackStatus) {
        int status = requestedStatus == null ? (fallbackStatus == null ? EQUIPMENT_STATUS_IDLE : fallbackStatus) : requestedStatus;
        if (status != EQUIPMENT_STATUS_IDLE
                && status != EQUIPMENT_STATUS_BORROWED
                && status != EQUIPMENT_STATUS_MAINTAINING
                && status != EQUIPMENT_STATUS_SCRAPPED) {
            throw new RuntimeException("Invalid equipment status");
        }
        return status;
    }

    private String normalizeMaintenanceStatus(String status, boolean allowDefault) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return allowDefault ? MAINTENANCE_PENDING : null;
        }
        String upperValue = normalized.toUpperCase(Locale.ROOT);
        if (!MAINTENANCE_PENDING.equals(upperValue)
                && !MAINTENANCE_IN_PROGRESS.equals(upperValue)
                && !MAINTENANCE_RESOLVED.equals(upperValue)) {
            throw new RuntimeException("Invalid maintenance status");
        }
        return upperValue;
    }

    private Map<String, Object> getEquipmentView(Long equipmentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT e.id, e.lab_id AS labId, l.lab_name AS labName, e.category_id AS categoryId, " +
                        "c.name AS categoryName, e.name, e.type, e.device_code AS deviceCode, " +
                        "e.serial_number AS serialNumber, e.brand, e.model, e.purchase_date AS purchaseDate, " +
                        "e.location, e.image_url AS imageUrl, e.description, e.remark, e.status, " +
                        "e.create_time AS createTime, e.update_time AS updateTime " +
                        "FROM t_equipment e " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_equipment_category c ON c.id = e.category_id AND c.deleted = 0 " +
                        "WHERE e.id = ? AND e.deleted = 0 LIMIT 1",
                equipmentId
        );
        if (rows.isEmpty()) {
            throw new RuntimeException("Equipment does not exist");
        }
        return rows.get(0);
    }

    private Map<String, Object> getCategoryView(Long categoryId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.id, c.lab_id AS labId, l.lab_name AS labName, c.name, c.description, " +
                        "c.create_time AS createTime, c.update_time AS updateTime " +
                        "FROM t_equipment_category c " +
                        "LEFT JOIN t_lab l ON l.id = c.lab_id AND l.deleted = 0 " +
                        "WHERE c.id = ? AND c.deleted = 0 LIMIT 1",
                categoryId
        );
        if (rows.isEmpty()) {
            throw new RuntimeException("Equipment category does not exist");
        }
        return rows.get(0);
    }

    private Map<String, Object> getBorrowView(Long borrowId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT b.id, b.equipment_id AS equipmentId, b.user_id AS userId, e.lab_id AS labId, " +
                        "l.lab_name AS labName, e.name AS equipmentName, e.device_code AS deviceCode, " +
                        "u.username, u.real_name AS userRealName, b.reason, b.status, " +
                        "b.borrow_time AS borrowTime, b.return_time AS returnTime, " +
                        "b.expected_return_time AS expectedReturnTime, b.create_time AS createTime " +
                        "FROM t_equipment_borrow b " +
                        "LEFT JOIN t_equipment e ON e.id = b.equipment_id " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user u ON u.id = b.user_id AND u.deleted = 0 " +
                        "WHERE b.id = ? AND b.deleted = 0 LIMIT 1",
                borrowId
        );
        if (rows.isEmpty()) {
            throw new RuntimeException("Borrow record does not exist");
        }
        return rows.get(0);
    }

    private Map<String, Object> getMaintenanceView(Long recordId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT m.id, m.equipment_id AS equipmentId, e.lab_id AS labId, l.lab_name AS labName, " +
                        "e.name AS equipmentName, e.device_code AS deviceCode, m.issue_desc AS issueDesc, " +
                        "m.maintenance_status AS maintenanceStatus, m.result_desc AS resultDesc, " +
                        "m.report_user_id AS reportUserId, reporter.real_name AS reportUserName, " +
                        "m.handled_by AS handledBy, handler.real_name AS handledByName, " +
                        "m.handled_at AS handledAt, m.create_time AS createTime, m.update_time AS updateTime " +
                        "FROM t_equipment_maintenance m " +
                        "LEFT JOIN t_equipment e ON e.id = m.equipment_id " +
                        "LEFT JOIN t_lab l ON l.id = e.lab_id AND l.deleted = 0 " +
                        "LEFT JOIN t_user reporter ON reporter.id = m.report_user_id AND reporter.deleted = 0 " +
                        "LEFT JOIN t_user handler ON handler.id = m.handled_by AND handler.deleted = 0 " +
                        "WHERE m.id = ? AND m.deleted = 0 LIMIT 1",
                recordId
        );
        if (rows.isEmpty()) {
            throw new RuntimeException("Maintenance record does not exist");
        }
        return rows.get(0);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
