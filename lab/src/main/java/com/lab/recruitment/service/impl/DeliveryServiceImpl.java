package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.dto.DeliveryDTO;
import com.lab.recruitment.entity.Delivery;
import com.lab.recruitment.entity.Lab;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.DeliveryMapper;
import com.lab.recruitment.service.DeliveryService;
import com.lab.recruitment.service.LabService;
import com.lab.recruitment.service.LabMemberService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.service.WrittenExamService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryServiceImpl extends ServiceImpl<DeliveryMapper, Delivery> implements DeliveryService {

    private static final int AUDIT_PENDING = 0;
    private static final int AUDIT_APPROVED = 1;
    private static final int AUDIT_REJECTED = 2;
    private static final int AUDIT_WITHDRAWN = 3;
    private static final int ADMISSION_NONE = 0;
    private static final int ADMISSION_JOINED = 1;
    private static final int ADMISSION_PENDING_CONFIRM = 2;
    private static final int ADMISSION_DECLINED = 3;

    @Autowired
    private UserService userService;

    @Autowired
    private LabService labService;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private LabMemberService labMemberService;

    @Autowired
    private WrittenExamService writtenExamService;

    @Override
    @Transactional
    public boolean deliver(DeliveryDTO deliveryDTO, String username) {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("username", username);
        User user = userService.getOne(userQuery);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (userAccessService.resolveManagedLabId(user) != null) {
            throw new RuntimeException("你已加入实验室，如需重新投递请先提交退出申请。");
        }

        Lab lab = labService.getById(deliveryDTO.getLabId());
        if (lab == null) {
            throw new RuntimeException("实验室不存在");
        }

        if (lab.getStatus() == null || lab.getStatus() != 1) {
            throw new RuntimeException("该实验室当前未开启招新");
        }
        String attachmentUrl = resolveAttachmentUrl(deliveryDTO, user);
        checkDeliveryEligibility(deliveryDTO.getLabId(), user);

        QueryWrapper<Delivery> deliveryQuery = new QueryWrapper<>();
        deliveryQuery.eq("user_id", user.getId());
        deliveryQuery.eq("lab_id", deliveryDTO.getLabId());
        deliveryQuery.eq("deleted", 0);

        Delivery existing = this.getOne(deliveryQuery);
        if (existing == null) {
            Delivery delivery = new Delivery();
            BeanUtils.copyProperties(deliveryDTO, delivery);
            delivery.setUserId(user.getId());
            delivery.setSkillTags(trimToNull(deliveryDTO.getSkillTags()));
            delivery.setStudyProgress(trimToNull(deliveryDTO.getStudyProgress()));
            delivery.setAttachmentUrl(attachmentUrl);
            delivery.setAuditStatus(AUDIT_PENDING);
            delivery.setIsAdmitted(ADMISSION_NONE);
            delivery.setDeliveryAttemptCount(1);
            delivery.setWithdrawCount(0);

            return this.save(delivery);
        }

        Integer attemptCount = existing.getDeliveryAttemptCount() == null ? 1 : existing.getDeliveryAttemptCount();

        if (Integer.valueOf(AUDIT_WITHDRAWN).equals(existing.getAuditStatus())) {
            if (attemptCount >= 2) {
                throw new RuntimeException("你已用完投递次数（最多 2 次）");
            }

            existing.setSkillTags(trimToNull(deliveryDTO.getSkillTags()));
            existing.setStudyProgress(trimToNull(deliveryDTO.getStudyProgress()));
            existing.setAttachmentUrl(attachmentUrl);
            existing.setDeliveryTime(LocalDateTime.now());
            existing.setAuditStatus(AUDIT_PENDING);
            existing.setAuditRemark(null);
            existing.setAuditTime(null);
            existing.setIsAdmitted(ADMISSION_NONE);
            existing.setAdmitTime(null);
            existing.setDeliveryAttemptCount(attemptCount + 1);
            return this.updateById(existing);
        }

        throw new RuntimeException("你已投递过该实验室");
    }

    @Override
    @Transactional
    public boolean withdraw(Long deliveryId, Long userId) {
        Delivery delivery = this.getById(deliveryId);
        if (delivery == null || Integer.valueOf(1).equals(delivery.getDeleted())) {
            throw new RuntimeException("投递记录不存在");
        }

        if (!delivery.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该投递记录");
        }

        Integer attemptCount = delivery.getDeliveryAttemptCount() == null ? 1 : delivery.getDeliveryAttemptCount();
        Integer withdrawCount = delivery.getWithdrawCount() == null ? 0 : delivery.getWithdrawCount();

        if (withdrawCount >= 1) {
            throw new RuntimeException("撤销次数已用完（最多 1 次）");
        }
        if (attemptCount >= 2) {
            throw new RuntimeException("已无剩余投递次数（最多 2 次）");
        }

        if (!Integer.valueOf(ADMISSION_NONE).equals(delivery.getIsAdmitted())) {
            throw new RuntimeException("当前投递已进入 offer 流程，无法撤销");
        }

        if (!Integer.valueOf(AUDIT_PENDING).equals(delivery.getAuditStatus())
                && !Integer.valueOf(AUDIT_APPROVED).equals(delivery.getAuditStatus())) {
            throw new RuntimeException("当前状态不支持撤销");
        }

        delivery.setAuditStatus(AUDIT_WITHDRAWN);
        delivery.setWithdrawCount(withdrawCount + 1);
        delivery.setAuditRemark(appendRemark(delivery.getAuditRemark(), "学生撤销本次投递。"));
        delivery.setAuditTime(LocalDateTime.now());
        return this.updateById(delivery);
    }

    @Override
    public Page<Map<String, Object>> getDeliveryPage(Integer pageNum, Integer pageSize, Long labId,
                                                     String realName, String studentId, Integer auditStatus) {
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectDeliveryPage(page, labId, realName, studentId, auditStatus);
    }

    @Override
    public Page<Map<String, Object>> getMyDeliveryPage(Integer pageNum, Integer pageSize, String username,
                                                       String labName, Integer auditStatus) {
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectMyDeliveryPage(page, username, labName, auditStatus);
    }

    @Override
    @Transactional
    public boolean audit(Long deliveryId, Integer auditStatus, String auditRemark) {
        Delivery delivery = this.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("Delivery record not found");
        }

        delivery.setAuditStatus(auditStatus);
        delivery.setAuditRemark(auditRemark);
        delivery.setAuditTime(LocalDateTime.now());
        return this.updateById(delivery);
    }

    @Override
    @Transactional
    public boolean admit(Long deliveryId) {
        Delivery delivery = this.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("Delivery record not found");
        }

        if (!Integer.valueOf(AUDIT_APPROVED).equals(delivery.getAuditStatus())) {
            throw new RuntimeException("只有审核通过的投递才能发放 offer");
        }

        if (delivery.getIsAdmitted() != null && !Integer.valueOf(ADMISSION_NONE).equals(delivery.getIsAdmitted())) {
            throw new RuntimeException("该投递的 offer 已处理");
        }

        User user = userService.getById(delivery.getUserId());
        if (user != null && userAccessService.resolveManagedLabId(user) != null) {
            throw new RuntimeException("该学生已加入其他实验室");
        }

        delivery.setIsAdmitted(ADMISSION_PENDING_CONFIRM);
        delivery.setAdmitTime(LocalDateTime.now());
        delivery.setAuditRemark(appendRemark(delivery.getAuditRemark(),
                "已发送 offer，等待学生确认。"));
        return this.updateById(delivery);
    }

    @Override
    @Transactional
    public boolean studentConfirm(Long deliveryId, Long userId) {
        Delivery delivery = this.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("投递记录不存在");
        }

        if (!delivery.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该投递记录");
        }

        if (!Integer.valueOf(ADMISSION_PENDING_CONFIRM).equals(delivery.getIsAdmitted())) {
            throw new RuntimeException("当前 offer 不处于待确认状态");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (userAccessService.resolveManagedLabId(user) != null) {
            throw new RuntimeException("你已加入其他实验室");
        }

        Lab lab = labService.getById(delivery.getLabId());
        if (lab == null) {
            throw new RuntimeException("实验室不存在");
        }

        int currentMemberCount = labService.countCurrentMembers(delivery.getLabId());
        if (lab.getRecruitNum() != null && lab.getRecruitNum() > 0 && currentMemberCount >= lab.getRecruitNum()) {
            throw new RuntimeException("该实验室名额已满");
        }

        delivery.setIsAdmitted(ADMISSION_JOINED);
        delivery.setAdmitTime(LocalDateTime.now());
        delivery.setAuditRemark(appendRemark(delivery.getAuditRemark(),
                "学生接受 offer 并加入实验室。"));
        this.updateById(delivery);

        labMemberService.activateMember(delivery.getLabId(), userId, "member", null, "Accepted delivery offer");
        userAccessService.refreshCompatibilityAccess(userId);
        labService.syncCurrentMemberCount(delivery.getLabId());

        QueryWrapper<Delivery> otherOfferQuery = new QueryWrapper<>();
        otherOfferQuery.eq("user_id", userId)
                .eq("is_admitted", ADMISSION_PENDING_CONFIRM)
                .ne("id", deliveryId);

        List<Delivery> otherOffers = this.list(otherOfferQuery);
        for (Delivery otherOffer : otherOffers) {
            otherOffer.setIsAdmitted(ADMISSION_DECLINED);
            otherOffer.setAdmitTime(LocalDateTime.now());
            otherOffer.setAuditRemark(appendRemark(otherOffer.getAuditRemark(),
                    "学生已接受其他实验室 offer，系统自动关闭该 offer。"));
        }
        if (!otherOffers.isEmpty()) {
            this.updateBatchById(otherOffers);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean studentRejectOffer(Long deliveryId, Long userId) {
        Delivery delivery = this.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("投递记录不存在");
        }

        if (!delivery.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该投递记录");
        }

        if (!Integer.valueOf(ADMISSION_PENDING_CONFIRM).equals(delivery.getIsAdmitted())) {
            throw new RuntimeException("当前 offer 不处于待处理状态");
        }

        delivery.setIsAdmitted(ADMISSION_DECLINED);
        delivery.setAdmitTime(LocalDateTime.now());
        delivery.setAuditRemark(appendRemark(delivery.getAuditRemark(),
                "学生拒绝该 offer。"));
        return this.updateById(delivery);
    }

    @Override
    public Map<String, Object> getOfferSummary(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        QueryWrapper<Delivery> countQuery = new QueryWrapper<>();
        countQuery.eq("user_id", user.getId());
        countQuery.eq("is_admitted", ADMISSION_PENDING_CONFIRM);

        Map<String, Object> summary = new HashMap<>();
        summary.put("pendingOfferCount", Math.toIntExact(this.count(countQuery)));
        summary.put("offers", baseMapper.selectPendingOfferList(username));
        summary.put("joinedLabId", userAccessService.resolveManagedLabId(user));
        return summary;
    }

    @Override
    public Map<String, Object> getStatistics(Long labId) {
        Map<String, Object> statistics = new HashMap<>();

        QueryWrapper<Delivery> totalQuery = new QueryWrapper<>();
        totalQuery.eq("lab_id", labId);
        statistics.put("totalCount", this.count(totalQuery));

        statistics.put("pendingCount", baseMapper.countByLabIdAndAuditStatus(labId, AUDIT_PENDING));
        statistics.put("approvedCount", baseMapper.countByLabIdAndAuditStatus(labId, AUDIT_APPROVED));
        statistics.put("rejectedCount", baseMapper.countByLabIdAndAuditStatus(labId, AUDIT_REJECTED));
        statistics.put("admittedCount", labService.countCurrentMembers(labId));

        return statistics;
    }

    private String appendRemark(String originalRemark, String message) {
        if (!StringUtils.hasText(originalRemark)) {
            return message;
        }
        return originalRemark + System.lineSeparator() + message;
    }

    private String resolveAttachmentUrl(DeliveryDTO deliveryDTO, User user) {
        String attachmentUrl = normalizeUrls(deliveryDTO.getAttachmentUrl());
        if (!StringUtils.hasText(attachmentUrl)) {
            attachmentUrl = normalizeUrls(user.getResume());
        }
        if (!StringUtils.hasText(attachmentUrl)) {
            throw new RuntimeException("请先上传简历后再投递");
        }
        return attachmentUrl;
    }

    private void checkDeliveryEligibility(Long labId, User user) {
        if (!userAccessService.isStudentIdentity(user)) {
            throw new RuntimeException("仅学生账号可以投递实验室");
        }
        if (!writtenExamService.canEnterInterview(labId, user.getId())) {
            throw new RuntimeException(writtenExamService.getInterviewRequirementMessage(labId, user.getId()));
        }
    }

    private String normalizeUrls(String rawUrls) {
        if (!StringUtils.hasText(rawUrls)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String item : rawUrls.split(",")) {
            String value = trimToNull(item);
            if (value == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
