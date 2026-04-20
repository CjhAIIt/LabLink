package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.DeliveryDTO;
import com.lab.recruitment.entity.Delivery;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.DeliveryService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private UserService userService;

    @PostMapping("/deliver")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> deliver(@Validated @RequestBody DeliveryDTO deliveryDTO) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            boolean success = deliveryService.deliver(deliveryDTO, username);
            return success ? Result.success("投递成功", null) : Result.error("投递失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/withdraw/{deliveryId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> withdraw(@PathVariable Long deliveryId) {
        try {
            Long userId = getCurrentUser().getId();
            boolean success = deliveryService.withdraw(deliveryId, userId);
            return success ? Result.success("撤销成功", null) : Result.error("撤销失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Page<Map<String, Object>>> getDeliveryList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) Integer auditStatus) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByUsername(username);

            if (user != null && "admin".equals(user.getRole())) {
                if (user.getLabId() == null) {
                    return Result.success(new Page<>());
                }
                labId = user.getLabId();
            }

            Page<Map<String, Object>> deliveryPage = deliveryService.getDeliveryPage(
                    pageNum, pageSize, labId, realName, studentId, auditStatus);
            return Result.success(deliveryPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<Map<String, Object>>> getMyDeliveryList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String labName,
            @RequestParam(required = false) Integer auditStatus) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Page<Map<String, Object>> deliveryPage = deliveryService.getMyDeliveryPage(
                    pageNum, pageSize, username, labName, auditStatus);
            return Result.success(deliveryPage);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/offer-summary")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getOfferSummary() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return Result.success(deliveryService.getOfferSummary(username));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/audit/{deliveryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> audit(@PathVariable Long deliveryId,
                                @RequestParam Integer auditStatus,
                                @RequestParam(required = false) String auditRemark) {
        try {
            assertCurrentAdminOwnsDelivery(deliveryId);
            boolean success = deliveryService.audit(deliveryId, auditStatus, auditRemark);
            return success ? Result.success("审核完成", null) : Result.error("审核失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/admit/{deliveryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> admit(@PathVariable Long deliveryId) {
        try {
            assertCurrentAdminOwnsDelivery(deliveryId);
            boolean success = deliveryService.admit(deliveryId);
            return success ? Result.success("已发送 offer", null) : Result.error("发送 offer 失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/confirm/{deliveryId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> studentConfirm(@PathVariable Long deliveryId) {
        try {
            Long userId = getCurrentUser().getId();
            boolean success = deliveryService.studentConfirm(deliveryId, userId);
            return success ? Result.success("已接受 offer", null) : Result.error("接受 offer 失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reject-offer/{deliveryId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Object> studentRejectOffer(@PathVariable Long deliveryId) {
        try {
            Long userId = getCurrentUser().getId();
            boolean success = deliveryService.studentRejectOffer(deliveryId, userId);
            return success ? Result.success("已拒绝 offer", null) : Result.error("拒绝 offer 失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics/{labId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> getStatistics(@PathVariable Long labId) {
        try {
            return Result.success(deliveryService.getStatistics(labId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private void assertCurrentAdminOwnsDelivery(Long deliveryId) {
        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole())) {
            throw new RuntimeException("只有实验室管理员可以审核投递");
        }

        Delivery delivery = deliveryService.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("投递记录不存在");
        }

        if (currentUser.getLabId() == null || !currentUser.getLabId().equals(delivery.getLabId())) {
            throw new RuntimeException("只能审核自己实验室的投递");
        }
    }
}
