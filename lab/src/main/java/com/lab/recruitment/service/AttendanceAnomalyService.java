package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AttendanceAnomalyHandleDTO;
import com.lab.recruitment.entity.AttendanceRecord;
import com.lab.recruitment.entity.User;

import java.time.LocalDate;

public interface AttendanceAnomalyService {

    Page<AttendanceRecord> listAnomalies(Long labId, LocalDate date, Integer pageNum, Integer pageSize, User currentUser);

    boolean handleAnomalies(AttendanceAnomalyHandleDTO dto, User currentUser);
}
