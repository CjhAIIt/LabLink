package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.LabAttendance;
import com.lab.recruitment.entity.LabExitApplication;
import com.lab.recruitment.entity.LabSpaceFolder;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.LabAttendanceService;
import com.lab.recruitment.service.LabExitApplicationService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.LabSpaceService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.LabAttendanceMemberVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lab-space")
public class LabSpaceController {

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Autowired
    private LabAttendanceService labAttendanceService;

    @Autowired
    private LabExitApplicationService labExitApplicationService;

    @Autowired
    private LabSpaceService labSpaceService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getOverview(@RequestParam(required = false) Long labId) {
        try {
            User currentUser = getCurrentUser();
            Long scopedLabId = resolveReadableLabId(currentUser, labId);
            Lab lab = labService.getById(scopedLabId);
            if (lab == null) {
                return Result.error("Lab not found");
            }

            QueryWrapper<User> memberQuery = new QueryWrapper<>();
            memberQuery.eq("lab_id", scopedLabId)
                    .eq("role", "student")
                    .orderByAsc("student_id")
                    .orderByAsc("id");
            List<User> members = userService.list(memberQuery);
            List<Map<String, Object>> memberItems = new ArrayList<>();
            for (User member : members) {
                Map<String, Object> memberItem = new HashMap<>();
                memberItem.put("id", member.getId());
                memberItem.put("username", member.getUsername());
                memberItem.put("realName", member.getRealName());
                memberItem.put("studentId", member.getStudentId());
                memberItem.put("major", member.getMajor());
                memberItems.add(memberItem);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("lab", lab);
            data.put("memberCount", members.size());
            data.put("members", memberItems);
            data.put("attendanceSummary", labAttendanceService.getAttendanceSummary(scopedLabId,
                    currentUserAccessor.isAdmin(currentUser) || currentUserAccessor.isTeacherIdentity(currentUser)
                            ? null
                            : currentUser.getId()));
            data.put("recentFiles", labSpaceService.getRecentFiles(scopedLabId, 5, currentUser));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/attendance/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<LabAttendanceMemberVO>> getDailyAttendance(@RequestParam(required = false) Long labId,
                                                                  @RequestParam(required = false) String attendanceDate) {
        try {
            User currentUser = getCurrentUser();
            Long scopedLabId = resolveReadableLabId(currentUser, labId);
            String currentDate = StringUtils.hasText(attendanceDate) ? attendanceDate : LocalDate.now().toString();
            return Result.success(labAttendanceService.getDailyAttendance(scopedLabId, currentDate));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/attendance/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> confirmAttendance(@RequestBody Map<String, Object> request) {
        try {
            Long requestedLabId = getLongValue(request.get("labId"));
            User currentAdmin = getCurrentLabOperator(requestedLabId);
            Long scopedLabId = currentUserAccessor.resolveLabScope(currentAdmin, requestedLabId);
            Long userId = getLongValue(request.get("userId"));
            Integer status = getIntegerValue(request.get("status"));
            String attendanceDate = request.get("attendanceDate") == null
                    ? LocalDate.now().toString()
                    : String.valueOf(request.get("attendanceDate"));
            String reason = request.get("reason") == null ? null : String.valueOf(request.get("reason"));

            return Result.success(labAttendanceService.confirmAttendance(
                    scopedLabId,
                    currentAdmin.getId(),
                    userId,
                    attendanceDate,
                    status,
                    reason
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/attendance/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<LabAttendance>> getMyAttendance(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labAttendanceService.getMyAttendancePage(pageNum, pageSize, currentUser.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/attendance/summary")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getAttendanceSummary(@RequestParam(required = false) Long labId) {
        try {
            User currentUser = getCurrentUser();
            if (!currentUserAccessor.isAdmin(currentUser) && !currentUserAccessor.isTeacherIdentity(currentUser)) {
                if (currentUser.getLabId() == null) {
                    return Result.error("You have not joined any lab");
                }
                return Result.success(labAttendanceService.getAttendanceSummary(currentUser.getLabId(), currentUser.getId()));
            }
            Long scopedLabId = resolveReadableLabId(currentUser, labId);
            return Result.success(labAttendanceService.getAttendanceSummary(scopedLabId, null));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/attendance/sign-in")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> signIn(@RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getLabId() == null) {
                return Result.error("You have not joined any lab");
            }
            String attendanceDate = request.get("attendanceDate") == null
                    ? LocalDate.now().toString()
                    : String.valueOf(request.get("attendanceDate"));
            Integer status = getIntegerValue(request.get("status"));
            String reason = request.get("reason") == null ? null : String.valueOf(request.get("reason"));
            return Result.success(labAttendanceService.studentSignIn(
                    currentUser.getLabId(),
                    currentUser.getId(),
                    attendanceDate,
                    status,
                    reason
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/folders")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> getFolders(@RequestParam(required = false) Long labId) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labSpaceService.getFolderTree(labId, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/folders")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<LabSpaceFolder> createFolder(@RequestBody LabSpaceFolder folder) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labSpaceService.saveFolder(folder, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/folders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<LabSpaceFolder> updateFolder(@PathVariable Long id, @RequestBody LabSpaceFolder folder) {
        try {
            User currentUser = getCurrentUser();
            folder.setId(id);
            return Result.success(labSpaceService.saveFolder(folder, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/files")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Page<Map<String, Object>>> getFiles(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) Long labId,
                                                      @RequestParam(required = false) Long folderId,
                                                      @RequestParam(required = false) Integer archiveFlag,
                                                      @RequestParam(required = false) String keyword) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labSpaceService.getFilePage(pageNum, pageSize, labId, folderId, archiveFlag, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/files/recent")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> getRecentFiles(@RequestParam(required = false) Long labId,
                                                            @RequestParam(defaultValue = "6") Integer limit) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labSpaceService.getRecentFiles(labId, limit, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/files/upload")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> uploadFile(@RequestParam(value = "labId", required = false) Long labId,
                                                  @RequestParam("folderId") Long folderId,
                                                  @RequestParam(value = "archiveFlag", required = false) Integer archiveFlag,
                                                  @RequestParam(value = "accessScope", required = false) String accessScope,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labSpaceService.uploadFile(labId, folderId, archiveFlag, accessScope, file, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/files/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> updateArchive(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            Integer archiveFlag = getIntegerValue(request.get("archiveFlag"));
            return Result.success(labSpaceService.updateArchiveFlag(id, archiveFlag, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exit-application")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> submitExitApplication(@RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            String reason = request.get("reason") == null ? null : String.valueOf(request.get("reason"));
            return Result.success(labExitApplicationService.submit(currentUser.getId(), reason));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/exit-application/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<LabExitApplication>> getMyExitApplications(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            User currentUser = getCurrentUser();
            return Result.success(labExitApplicationService.getMyApplicationPage(pageNum, pageSize, currentUser.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/exit-application/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<LabExitApplication>> getLabExitApplications(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                                   @RequestParam(required = false) Long labId,
                                                                   @RequestParam(required = false) Integer status,
                                                                   @RequestParam(required = false) String realName) {
        try {
            User currentAdmin = getCurrentLabOperator(labId);
            return Result.success(labExitApplicationService.getLabApplicationPage(
                    pageNum,
                    pageSize,
                    currentUserAccessor.resolveLabScope(currentAdmin, labId),
                    status,
                    realName
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exit-application/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> auditExitApplication(@RequestBody Map<String, Object> request) {
        try {
            Long id = getLongValue(request.get("id"));
            User currentAdmin = resolveExitAuditOperator(id);
            Integer status = getIntegerValue(request.get("status"));
            String auditRemark = request.get("auditRemark") == null ? null : String.valueOf(request.get("auditRemark"));
            return Result.success(labExitApplicationService.audit(id, status, auditRemark, currentAdmin));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private User getCurrentLabOperator(Long labId) {
        User currentUser = getCurrentUser();
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            if (labId == null) {
                throw new RuntimeException("Lab id is required");
            }
            currentUser.setLabId(currentUserAccessor.resolveLabScope(currentUser, labId));
            return currentUser;
        }
        if (!currentUserAccessor.isLabManager(currentUser)) {
            throw new RuntimeException("Only lab managers can access this function");
        }
        currentUser.setLabId(currentUserAccessor.resolveLabScope(currentUser, null));
        return currentUser;
    }

    private User resolveExitAuditOperator(Long exitApplicationId) {
        if (exitApplicationId == null) {
            throw new RuntimeException("Exit application id is required");
        }
        User currentUser = getCurrentUser();
        if (currentUserAccessor.isLabManager(currentUser)) {
            currentUser.setLabId(currentUserAccessor.resolveLabScope(currentUser, null));
            return currentUser;
        }
        if (currentUserAccessor.isSuperAdmin(currentUser)) {
            LabExitApplication application = labExitApplicationService.getById(exitApplicationId);
            if (application == null) {
                throw new RuntimeException("Exit application not found");
            }
            currentUser.setLabId(currentUserAccessor.resolveLabScope(currentUser, application.getLabId()));
            return currentUser;
        }
        throw new RuntimeException("Only lab managers can access this function");
    }

    private User getCurrentUser() {
        User user = currentUserAccessor.getCurrentUser();
        user.setPassword(null);
        return user;
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long resolveReadableLabId(User currentUser, Long requestedLabId) {
        if (!currentUserAccessor.isAdmin(currentUser) && !currentUserAccessor.isTeacherIdentity(currentUser)) {
            if (currentUser.getLabId() == null) {
                throw new RuntimeException("Current account is not bound to any lab");
            }
            if (requestedLabId != null && !currentUser.getLabId().equals(requestedLabId)) {
                throw new RuntimeException("No permission to access another lab");
            }
            return currentUser.getLabId();
        }
        return currentUserAccessor.resolveLabScope(currentUser, requestedLabId);
    }
}
