package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_forum_comment")
public class ForumComment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    
    private Long userId;
    
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
    
    @TableField(exist = false)
    private User author;
}
