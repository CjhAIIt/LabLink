package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.LabWithAdminDTO;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.LabInfoChangeReviewService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/labs")
public class LabController {

    @Autowired
    private LabService labService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Autowired
    private LabInfoChangeReviewService labInfoChangeReviewService;

    @PutMapping("/update-info")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<Object> updateLabInfo(@RequestBody Lab lab) {
        try {
            return Result.success("Lab info change submitted", labInfoChangeReviewService.submitChange(
                    lab,
                    currentUserAccessor.getCurrentUser()
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Page<Lab>> getLabList(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) Long collegeId,
                                        @RequestParam(required = false) String labName,
                                        @RequestParam(required = false) Integer status) {
        try {
            return Result.success(labService.getLabPage(pageNum, pageSize, collegeId, labName, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getLabStats() {
        try {
            Map<String, Object> result = new HashMap<>();
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_lab WHERE deleted = 0", Long.class);
            result.put("total", total == null ? 0L : total);
            List<Map<String, Object>> byCollege = jdbcTemplate.queryForList(
                    "SELECT l.college_id AS collegeId, c.college_name AS collegeName, COUNT(*) AS labCount " +
                            "FROM t_lab l " +
                            "LEFT JOIN t_college c ON c.id = l.college_id AND c.deleted = 0 " +
                            "WHERE l.deleted = 0 " +
                            "GROUP BY l.college_id, c.college_name " +
                            "ORDER BY labCount DESC"
            );
            result.put("byCollege", byCollege);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<Lab> getLabById(@PathVariable Long id) {
        try {
            Lab lab = labService.getLabById(id);
            if (lab == null) {
                return Result.error("Lab not found");
            }
            return Result.success(lab);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/detail/{id:\\d+}")
    public Result<Lab> getLabDetailById(@PathVariable Long id) {
        return getLabById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> createLab(@RequestBody Lab lab) {
        try {
            return labService.createLab(lab)
                    ? Result.success("Lab created", null)
                    : Result.error("Failed to create lab");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> updateLab(@PathVariable Long id, @RequestBody Lab lab) {
        try {
            lab.setId(id);
            return labService.updateLab(lab)
                    ? Result.success("Lab updated", null)
                    : Result.error("Failed to update lab");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Object> deleteLab(@PathVariable Long id) {
        try {
            return labService.deleteLab(id)
                    ? Result.success("Lab deleted", null)
                    : Result.error("Failed to delete lab");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list-with-admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<LabWithAdminDTO>> getLabsWithAdmin() {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            List<LabWithAdminDTO> labs = labService.getLabsWithAdmin();
            if (currentUserAccessor.isSuperAdmin(currentUser)) {
                return Result.success(labs);
            }
            if (currentUserAccessor.isCollegeManager(currentUser)) {
                Long managedCollegeId = currentUserAccessor.resolveManagedCollegeId(currentUser);
                List<LabWithAdminDTO> scoped = labs.stream()
                        .filter(item -> item != null && item.getLab() != null && managedCollegeId != null
                                && managedCollegeId.equals(item.getLab().getCollegeId()))
                        .collect(Collectors.toList());
                return Result.success(scoped);
            }
            return Result.error("Access denied");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
