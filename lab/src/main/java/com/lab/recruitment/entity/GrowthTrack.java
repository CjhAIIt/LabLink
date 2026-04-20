package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_growth_track")
public class GrowthTrack {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("short_name")
    private String shortName;

    @TableField("category")
    private String category;

    @TableField("subtitle")
    private String subtitle;

    @TableField("description")
    private String description;

    @TableField("fit_scene")
    private String fitScene;

    @TableField("salary_range")
    private String salaryRange;

    @TableField("recommended_keyword")
    private String recommendedKeyword;

    @TableField("interview_position")
    private String interviewPosition;

    @TableField("icon_key")
    private String iconKey;

    @TableField("difficulty_label")
    private String difficultyLabel;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("courses_json")
    private String coursesJson;

    @TableField("books_json")
    private String booksJson;

    @TableField("competitions_json")
    private String competitionsJson;

    @TableField("certificates_json")
    private String certificatesJson;

    @TableField("competency_json")
    private String competencyJson;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
