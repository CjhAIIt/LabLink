package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_equipment_borrow")
public class EquipmentBorrow {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("equipment_id")
    private Long equipmentId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("borrow_time")
    private LocalDateTime borrowTime;
    
    @TableField("return_time")
    private LocalDateTime returnTime;
    
    @TableField("expected_return_time")
    private LocalDateTime expectedReturnTime;

    @TableField("pickup_time")
    private LocalDateTime pickupTime;

    @TableField("pickup_confirmed_by")
    private Long pickupConfirmedBy;

    @TableField("return_apply_time")
    private LocalDateTime returnApplyTime;

    @TableField("return_confirmed_by")
    private Long returnConfirmedBy;

    @TableField("return_confirm_time")
    private LocalDateTime returnConfirmTime;

    @TableField("acceptance_checklist")
    private String acceptanceChecklist;

    @TableField("reason")
    private String reason;
    
    @TableField("status")
    private Integer status; // 0-申请中, 1-已借出, 2-已拒绝, 3-已归还, 4-已领用, 5-归还待验收, 6-逾期
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
