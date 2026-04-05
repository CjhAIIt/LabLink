package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.Equipment;
import com.lab.recruitment.entity.EquipmentBorrow;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.EquipmentBorrowMapper;
import com.lab.recruitment.service.EquipmentBorrowService;
import com.lab.recruitment.service.EquipmentService;
import com.lab.recruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EquipmentBorrowServiceImpl extends ServiceImpl<EquipmentBorrowMapper, EquipmentBorrow> implements EquipmentBorrowService {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public boolean apply(Long equipmentId, String reason, String username) {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("username", username);
        User user = userService.getOne(userQuery);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Equipment equipment = equipmentService.getById(equipmentId);
        if (equipment == null) {
            throw new RuntimeException("Equipment not found");
        }
        if (user.getLabId() == null || !user.getLabId().equals(equipment.getLabId())) {
            throw new RuntimeException("You can only borrow equipment from your own lab");
        }
        if (equipment.getStatus() != 0) {
            throw new RuntimeException("The equipment is not available");
        }

        EquipmentBorrow borrow = new EquipmentBorrow();
        borrow.setEquipmentId(equipmentId);
        borrow.setUserId(user.getId());
        borrow.setReason(reason);
        borrow.setStatus(0);
        return this.save(borrow);
    }

    @Override
    @Transactional
    public boolean audit(Long borrowId, Integer status) {
        EquipmentBorrow borrow = this.getById(borrowId);
        if (borrow == null) {
            throw new RuntimeException("Borrow record not found");
        }
        if (borrow.getStatus() != 0) {
            throw new RuntimeException("This request has already been reviewed");
        }

        Equipment equipment = equipmentService.getById(borrow.getEquipmentId());
        if (equipment == null) {
            throw new RuntimeException("Equipment not found");
        }

        if (status == 1) {
            if (equipment.getStatus() != 0) {
                throw new RuntimeException("The equipment is already borrowed or under maintenance");
            }
            borrow.setStatus(1);
            borrow.setBorrowTime(LocalDateTime.now());
            equipment.setStatus(1);
            equipmentService.updateById(equipment);
        } else {
            borrow.setStatus(2);
        }

        return this.updateById(borrow);
    }

    @Override
    @Transactional
    public boolean returnEquipment(Long borrowId) {
        EquipmentBorrow borrow = this.getById(borrowId);
        if (borrow == null) {
            throw new RuntimeException("Borrow record not found");
        }
        if (borrow.getStatus() != 1) {
            throw new RuntimeException("The borrow status is invalid");
        }

        borrow.setStatus(3);
        borrow.setReturnTime(LocalDateTime.now());
        this.updateById(borrow);

        Equipment equipment = equipmentService.getById(borrow.getEquipmentId());
        if (equipment != null) {
            equipment.setStatus(0);
            equipmentService.updateById(equipment);
        }

        return true;
    }
}
