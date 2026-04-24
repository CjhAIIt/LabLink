package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.recruitment.entity.AttendanceRecord;
import com.lab.recruitment.entity.AttendanceSession;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AttendanceRecordMapper;
import com.lab.recruitment.mapper.AttendanceSessionMapper;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AttendanceStatsService;
import com.lab.recruitment.vo.AttendanceStatsVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceStatsServiceImpl implements AttendanceStatsService {

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Override
    public AttendanceStatsVO getStats(Long labId, Long userId, LocalDate startDate, LocalDate endDate, User currentUser) {
        User targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new RuntimeException("用户不存在");
        }
        return buildStatsVO(labId, targetUser, startDate, endDate);
    }

    @Override
    public List<AttendanceStatsVO> getBatchStats(Long labId, LocalDate startDate, LocalDate endDate, User currentUser) {
        List<User> members = listActiveMemberUsers(labId);
        List<AttendanceStatsVO> result = new ArrayList<>();
        for (User member : members) {
            result.add(buildStatsVO(labId, member, startDate, endDate));
        }
        return result;
    }

    @Override
    public byte[] exportStats(Long labId, LocalDate startDate, LocalDate endDate, User currentUser) {
        List<AttendanceStatsVO> statsList = getBatchStats(labId, startDate, endDate, currentUser);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("出勤统计");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("应到天数");
            header.createCell(3).setCellValue("已签到");
            header.createCell(4).setCellValue("迟到");
            header.createCell(5).setCellValue("缺勤");
            header.createCell(6).setCellValue("请假");
            header.createCell(7).setCellValue("出勤率(%)");

            int rowIdx = 1;
            for (AttendanceStatsVO s : statsList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getStudentId() != null ? s.getStudentId() : "");
                row.createCell(1).setCellValue(s.getRealName() != null ? s.getRealName() : "");
                row.createCell(2).setCellValue(s.getTotalDays());
                row.createCell(3).setCellValue(s.getSignedDays());
                row.createCell(4).setCellValue(s.getLateDays());
                row.createCell(5).setCellValue(s.getAbsentDays());
                row.createCell(6).setCellValue(s.getLeaveDays());
                row.createCell(7).setCellValue(s.getAttendanceRate());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出出勤统计失败", e);
        }
    }

    private AttendanceStatsVO buildStatsVO(Long labId, User user, LocalDate startDate, LocalDate endDate) {
        // 查询时间范围内的会话
        List<AttendanceSession> sessions = attendanceSessionMapper.selectList(
                new LambdaQueryWrapper<AttendanceSession>()
                        .eq(AttendanceSession::getLabId, labId)
                        .ge(AttendanceSession::getSessionDate, startDate)
                        .le(AttendanceSession::getSessionDate, endDate)
                        .ne(AttendanceSession::getStatus, "CANCELLED"));

        List<Long> sessionIds = sessions.stream()
                .map(AttendanceSession::getId)
                .collect(Collectors.toList());

        // 查询该用户的考勤记录
        Map<Long, AttendanceRecord> recordBySession = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                    new LambdaQueryWrapper<AttendanceRecord>()
                            .eq(AttendanceRecord::getUserId, user.getId())
                            .in(AttendanceRecord::getSessionId, sessionIds));
            for (AttendanceRecord r : records) {
                recordBySession.put(r.getSessionId(), r);
            }
        }

        // 按日期聚合（同一天多个会话取最差状态）
        Map<LocalDate, String> dailyStatus = new HashMap<>();
        Map<LocalDate, String> dailySignTime = new HashMap<>();
        for (AttendanceSession session : sessions) {
            LocalDate date = session.getSessionDate();
            AttendanceRecord record = recordBySession.get(session.getId());
            String status = record != null ? record.getSignStatus() : "ABSENT";
            String signTime = (record != null && record.getSignTime() != null)
                    ? record.getSignTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    : null;

            String existing = dailyStatus.get(date);
            if (existing == null || statusPriority(status) > statusPriority(existing)) {
                dailyStatus.put(date, status);
                dailySignTime.put(date, signTime);
            }
        }

        int signed = 0, late = 0, absent = 0, leave = 0;
        List<AttendanceStatsVO.DailyRecord> dailyRecords = new ArrayList<>();
        for (Map.Entry<LocalDate, String> entry : dailyStatus.entrySet()) {
            AttendanceStatsVO.DailyRecord dr = new AttendanceStatsVO.DailyRecord();
            dr.setDate(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE));
            dr.setStatus(entry.getValue());
            dr.setSignTime(dailySignTime.get(entry.getKey()));
            dailyRecords.add(dr);

            switch (entry.getValue()) {
                case "SIGNED": signed++; break;
                case "LATE": late++; break;
                case "ABSENT": absent++; break;
                case "LEAVE": leave++; break;
                default: break;
            }
        }
        dailyRecords.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        int totalDays = dailyStatus.size();
        double rate = totalDays > 0
                ? Math.round((signed + late + leave) * 1000.0 / totalDays) / 10.0
                : 0.0;

        AttendanceStatsVO vo = new AttendanceStatsVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setStudentId(user.getStudentId());
        vo.setTotalDays(totalDays);
        vo.setSignedDays(signed);
        vo.setLateDays(late);
        vo.setAbsentDays(absent);
        vo.setLeaveDays(leave);
        vo.setAttendanceRate(rate);
        vo.setDailyRecords(dailyRecords);
        return vo;
    }

    private int statusPriority(String status) {
        switch (status) {
            case "ABSENT": return 3;
            case "LATE": return 2;
            case "LEAVE": return 1;
            case "SIGNED": return 0;
            default: return -1;
        }
    }

    private List<User> listActiveMemberUsers(Long labId) {
        return labMemberMapper.selectActiveMembersByLabId(labId).stream()
                .map(row -> {
                    User user = new User();
                    user.setId(toLong(row.get("userId")));
                    user.setRealName(toStringValue(row.get("realName")));
                    user.setStudentId(toStringValue(row.get("studentId")));
                    return user;
                })
                .filter(user -> user.getId() != null)
                .collect(Collectors.toList());
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
