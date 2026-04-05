package com.lab.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_forum_like")
public class ForumLike {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
