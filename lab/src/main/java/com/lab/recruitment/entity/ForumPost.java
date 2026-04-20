package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_forum_post")
public class ForumPost {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    
    private String title;
    
    private String content;
    
    private Boolean isPinned;
    
    private Boolean isEssence;
    
    private Integer likeCount;
    
    private Integer commentCount;
    
    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
    
    @TableField(exist = false)
    private User author;
    
    @TableField(exist = false)
    private Boolean isLiked; // Current user liked?
}
