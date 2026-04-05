package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.ForumComment;
import com.lab.recruitment.entity.ForumPost;

public interface ForumService extends IService<ForumPost> {
    
    // Post methods
    Page<ForumPost> getPostPage(Integer pageNum, Integer pageSize, String keyword, Boolean isEssence, Long currentUserId);
    
    ForumPost getPostDetail(Long id, Long currentUserId);
    
    boolean createPost(ForumPost post, Long userId);
    
    boolean deletePost(Long id, Long userId, boolean isAdmin);
    
    boolean setPinned(Long id, boolean isPinned);
    
    boolean setEssence(Long id, boolean isEssence);
    
    // Comment methods
    Page<ForumComment> getCommentPage(Integer pageNum, Integer pageSize, Long postId);
    
    boolean createComment(ForumComment comment, Long userId);
    
    boolean deleteComment(Long id, Long userId, boolean isAdmin);
    
    // Like methods
    boolean toggleLike(Long postId, Long userId);
}
