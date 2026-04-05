package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.EquipmentBorrow;

public interface EquipmentBorrowService extends IService<EquipmentBorrow> {
    boolean apply(Long equipmentId, String reason, String username);
    boolean audit(Long borrowId, Integer status); // 1-pass, 2-reject
    boolean returnEquipment(Long borrowId);
}
