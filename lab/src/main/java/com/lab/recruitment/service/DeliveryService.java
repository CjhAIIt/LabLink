package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.dto.DeliveryDTO;
import com.lab.recruitment.entity.Delivery;

import java.util.Map;

public interface DeliveryService extends IService<Delivery> {

    boolean deliver(DeliveryDTO deliveryDTO, String username);

    boolean withdraw(Long deliveryId, Long userId);

    Page<Map<String, Object>> getDeliveryPage(Integer pageNum, Integer pageSize, Long labId,
                                              String realName, String studentId, Integer auditStatus);

    Page<Map<String, Object>> getMyDeliveryPage(Integer pageNum, Integer pageSize, String username,
                                                String labName, Integer auditStatus);

    boolean audit(Long deliveryId, Integer auditStatus, String auditRemark);

    boolean admit(Long deliveryId);

    boolean studentConfirm(Long deliveryId, Long userId);

    boolean studentRejectOffer(Long deliveryId, Long userId);

    Map<String, Object> getOfferSummary(String username);

    Map<String, Object> getStatistics(Long labId);
}
