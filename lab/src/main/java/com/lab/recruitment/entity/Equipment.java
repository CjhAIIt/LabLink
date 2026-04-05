package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_equipment")
public class Equipment {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("lab_id")
    private Long labId;
    
    @TableField("name")
    private String name;
    
    @TableField("type")
    private String type;
    
    @TableField("serial_number")
    private String serialNumber;
    
    @TableField("image_url")
    private String imageUrl;
    
    @TableField("description")
    private String description;
    
    @TableField("status")
    private Integer status; // 0-空闲, 1-借用中, 2-维修中
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
