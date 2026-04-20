package com.lab.recruitment.messaging;

import lombok.Data;

@Data
public class SystemNotificationMessage {

    private Long userId;
    private String title;
    private String content;
    private String notificationType;
    private Long relatedId;
    private String redirectPath;
}
