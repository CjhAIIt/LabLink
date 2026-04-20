package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AttendanceAnomalyHandleDTO;
import com.lab.recruitment.entity.AttendanceChangeLog;
import com.lab.recruitment.entity.AttendanceRecord;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AttendanceChangeLogMapper;
import com.lab.recruitment.mapper.AttendanceRecordMapper;
import com.lab.recruitment.service.AttendanceAnomalyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceAnomalyServiceImpl implements AttendanceAnomalyService {

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private AttendanceChangeLogMapper attendanceChangeLogMapper;

    @Override
    public Page<AttendanceRecord> listAnomalies(Long labId, LocalDate date, Integer pageNum, Integer pageSize, User currentUser) {
        Page<AttendanceRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getSignStatus, "ABSENT")
                .isNull(AttendanceRecord::getHandledBy)
                .orderByDesc(AttendanceRecord::getCreateTime);
        if (labId != null) {
            wrapper.eq(AttendanceRecord::getLabId, labId);
        }
        return attendanceRecordMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAnomalies(AttendanceAnomalyHandleDTO dto, User currentUser) {
        String targetStatus = dto.getTargetStatus();
        if (!"LEAVE".equals(targetStatus) && !"SIGNED".equals(targetStatus)) {
            throw new RuntimeException("目标状态只能为 LEAVE 或 SIGNED");
        }

        List<AttendanceRecord> records = attendanceRecordMapper.selectBatchIds(dto.getRecordIds());
        if (records.isEmpty()) {
            throw new RuntimeException("未找到对应的考勤记录");
        }

        LocalDateTime now = LocalDateTime.now();
        for (AttendanceRecord record : records) {
            if (!"ABSENT".equals(record.getSignStatus())) {
                continue;
            }
            String beforeStatus = record.getSignStatus();
            record.setSignStatus(targetStatus);
            record.setHandleRemark(dto.getRemark());
            record.setHandledBy(currentUser.getId());
            record.setHandledAt(now);
            attendanceRecordMapper.updateById(record);

            // 写入变更日志
            AttendanceChangeLog log = new AttendanceChangeLog();
            log.setSessionId(record.getSessionId());
            log.setRecordId(record.getId());
            log.setLabId(record.getLabId());
            log.setUserId(record.getUserId());
            log.setBeforeStatus(beforeStatus);
            log.setAfterStatus(targetStatus);
            log.setChangedBy(currentUser.getId());
            log.setChangedReason(dto.getRemark());
            attendanceChangeLogMapper.insert(log);
        }
        return true;
    }
}
