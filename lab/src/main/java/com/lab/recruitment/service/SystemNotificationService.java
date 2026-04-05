package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.SystemNotification;

public interface SystemNotificationService extends IService<SystemNotification> {

    SystemNotification createNotification(Long userId, String title, String content, String type, Long relatedId);
}
