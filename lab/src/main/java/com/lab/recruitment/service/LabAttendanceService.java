package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.LabAttendance;
import com.lab.recruitment.vo.LabAttendanceMemberVO;

import java.util.List;
import java.util.Map;

public interface LabAttendanceService extends IService<LabAttendance> {

    List<LabAttendanceMemberVO> getDailyAttendance(Long labId, String attendanceDate);

    boolean confirmAttendance(Long labId, Long adminId, Long userId, String attendanceDate, Integer status, String reason);

    Page<LabAttendance> getMyAttendancePage(Integer pageNum, Integer pageSize, Long userId);

    boolean studentSignIn(Long labId, Long userId, String attendanceDate, Integer status, String reason);

    Map<String, Object> getAttendanceSummary(Long labId, Long userId);
}
