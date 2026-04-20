package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AttendanceRecordReviewDTO;
import com.lab.recruitment.dto.AttendanceScheduleDTO;
import com.lab.recruitment.dto.AttendanceSignInDTO;
import com.lab.recruitment.dto.AttendanceTaskUpsertDTO;
import com.lab.recruitment.dto.AttendanceMakeupRequestDTO;
import com.lab.recruitment.dto.AttendanceLeaveApplyDTO;
import com.lab.recruitment.dto.AttendanceLeaveApprovalBatchDTO;
import com.lab.recruitment.dto.AttendanceLeaveReviewDTO;
import com.lab.recruitment.entity.AttendanceSchedule;
import com.lab.recruitment.entity.User;
import org.springframework.web.multipart.MultipartFile;
import com.lab.recruitment.dto.AttendanceDutyUpsertDTO;

import java.util.List;
import java.util.Map;

public interface AttendanceWorkflowService {

    Page<Map<String, Object>> getTaskPage(Integer pageNum, Integer pageSize, Long collegeId, String keyword, User currentUser);

    Map<String, Object> saveTask(AttendanceTaskUpsertDTO taskDTO, User currentUser);

    boolean publishTask(Long taskId, User currentUser);

    List<AttendanceSchedule> getTaskSchedules(Long taskId, User currentUser);

    List<AttendanceSchedule> saveTaskSchedules(Long taskId, List<AttendanceScheduleDTO> schedules, User currentUser);

    Map<String, Object> getAttendanceSummary(Long taskId, Long labId, User currentUser);

    Map<String, Object> getCurrentLabSession(User currentUser);

    List<Map<String, Object>> getCurrentLabSessionRecords(User currentUser);

    boolean reviewLabAttendanceRecord(AttendanceRecordReviewDTO reviewDTO, User currentUser);

    Map<String, Object> uploadCurrentSessionPhoto(MultipartFile file, String remark, User currentUser);

    Map<String, Object> setSessionDuty(Long sessionId, AttendanceDutyUpsertDTO dutyDTO, User currentUser);

    Map<String, Object> getCurrentStudentSession(User currentUser);

    boolean studentSignIn(AttendanceSignInDTO signInDTO, User currentUser);

    Map<String, Object> studentApplyLeave(AttendanceLeaveApplyDTO leaveDTO, User currentUser);

    boolean studentRequestMakeup(AttendanceMakeupRequestDTO requestDTO, User currentUser);

    Page<Map<String, Object>> getStudentHistory(Integer pageNum, Integer pageSize, User currentUser);

    Page<Map<String, Object>> getPendingLeavePage(Integer pageNum, Integer pageSize, Long labId,
                                                  String leaveStatus, String keyword, User currentUser);

    Map<String, Object> approveLeave(Long leaveId, AttendanceLeaveReviewDTO reviewDTO, User currentUser);

    Map<String, Object> rejectLeave(Long leaveId, AttendanceLeaveReviewDTO reviewDTO, User currentUser);

    void batchApproveLeave(AttendanceLeaveApprovalBatchDTO dto, User currentUser);
}
