package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.recruitment.entity.AttendanceRecord;
import com.lab.recruitment.entity.AttendanceSession;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.AttendanceRecordMapper;
import com.lab.recruitment.mapper.AttendanceSessionMapper;
import com.lab.recruitment.mapper.LabMemberMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.AttendanceDashboardService;
import com.lab.recruitment.vo.AttendanceDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceDashboardServiceImpl implements AttendanceDashboardService {

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LabMemberMapper labMemberMapper;

    @Override
    public AttendanceDashboardVO getTodayDashboard(Long labId, User currentUser) {
        LocalDate today = LocalDate.now();
        AttendanceDashboardVO vo = new AttendanceDashboardVO();
        vo.setDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // 成员以 t_lab_member 的 active 关系为准，避免踢出后 user.lab_id 残留影响考勤看板。
        List<User> members = listActiveMemberUsers(labId);

        // 查询今日所有签到会话
        List<AttendanceSession> todaySessions = attendanceSessionMapper.selectList(
                new LambdaQueryWrapper<AttendanceSession>()
                        .eq(AttendanceSession::getLabId, labId)
                        .eq(AttendanceSession::getSessionDate, today)
                        .ne(AttendanceSession::getStatus, "CANCELLED"));

        List<Long> sessionIds = todaySessions.stream()
                .map(AttendanceSession::getId)
                .collect(Collectors.toList());

        // 查询今日所有考勤记录
        Map<Long, AttendanceRecord> recordMap = new java.util.HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                    new LambdaQueryWrapper<AttendanceRecord>()
                            .in(AttendanceRecord::getSessionId, sessionIds));
            // 每个用户取最新一条记录
            for (AttendanceRecord r : records) {
                recordMap.merge(r.getUserId(), r, (old, cur) ->
                        cur.getCreateTime().isAfter(old.getCreateTime()) ? cur : old);
            }
        }

        // 组装成员状态列表
        int signed = 0, late = 0, absent = 0, leave = 0;
        List<AttendanceDashboardVO.MemberStatusVO> memberList = new ArrayList<>();

        for (User member : members) {
            AttendanceDashboardVO.MemberStatusVO msv = new AttendanceDashboardVO.MemberStatusVO();
            msv.setUserId(member.getId());
            msv.setRealName(member.getRealName());
            msv.setStudentId(member.getStudentId());

            AttendanceRecord record = recordMap.get(member.getId());
            if (record != null) {
                msv.setStatus(record.getSignStatus());
                if (record.getSignTime() != null) {
                    msv.setSignTime(record.getSignTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                }
                switch (record.getSignStatus()) {
                    case "SIGNED": signed++; break;
                    case "LATE": late++; break;
                    case "ABSENT": absent++; break;
                    case "LEAVE": leave++; break;
                    default: break;
                }
            } else {
                msv.setStatus(sessionIds.isEmpty() ? "NOT_STARTED" : "ABSENT");
                if (!sessionIds.isEmpty()) {
                    absent++;
                }
            }
            memberList.add(msv);
        }

        vo.setTotal(members.size());
        vo.setSigned(signed);
        vo.setLate(late);
        vo.setAbsent(absent);
        vo.setLeave(leave);
        vo.setMembers(memberList);
        return vo;
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
