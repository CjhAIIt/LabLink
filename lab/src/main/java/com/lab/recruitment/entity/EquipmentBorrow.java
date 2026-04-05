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
    
    @TableField("reason")
    private String reason;
    
    @TableField("status")
    private Integer status; // 0-申请中, 1-已借出, 2-已拒绝, 3-已归还
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
