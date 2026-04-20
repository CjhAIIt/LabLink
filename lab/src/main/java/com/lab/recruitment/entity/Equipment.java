package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_equipment")
public class Equipment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("lab_id")
    private Long labId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("device_code")
    private String deviceCode;

    @TableField("serial_number")
    private String serialNumber;

    @TableField("brand")
    private String brand;

    @TableField("model")
    private String model;

    @TableField("purchase_date")
    private LocalDate purchaseDate;

    @TableField("location")
    private String location;

    @TableField("image_url")
    private String imageUrl;

    @TableField("description")
    private String description;

    @TableField("remark")
    private String remark;

    @TableField("status")
    private Integer status;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
