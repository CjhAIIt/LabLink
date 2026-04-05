package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.AttendanceRecordReviewDTO;
import com.lab.recruitment.dto.AttendanceScheduleDTO;
import com.lab.recruitment.dto.AttendanceSignInDTO;
import com.lab.recruitment.dto.AttendanceTaskUpsertDTO;
import com.lab.recruitment.entity.AttendanceSchedule;
import com.lab.recruitment.entity.User;
import org.springframework.web.multipart.MultipartFile;

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

    Map<String, Object> getCurrentStudentSession(User currentUser);

    boolean studentSignIn(AttendanceSignInDTO signInDTO, User currentUser);

    Page<Map<String, Object>> getStudentHistory(Integer pageNum, Integer pageSize, User currentUser);
}
