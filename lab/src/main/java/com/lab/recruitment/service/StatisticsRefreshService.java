package com.lab.recruitment.service;

import com.lab.recruitment.messaging.PlatformEventPublisher;
import com.lab.recruitment.messaging.StatisticsRefreshMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StatisticsRefreshService {

    @Autowired
    private PlatformEventPublisher platformEventPublisher;

    public void refreshAsync(String domain, Long collegeId, Long labId, Long operatorUserId, String reason) {
        StatisticsRefreshMessage message = new StatisticsRefreshMessage();
        message.setDomain(domain);
        message.setCollegeId(collegeId);
        message.setLabId(labId);
        message.setOperatorUserId(operatorUserId);
        message.setReason(reason);
        message.setOccurredAt(LocalDateTime.now());
        platformEventPublisher.publishStatisticsRefresh(message);
    }
}
