package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.entity.ForumComment;
import com.lab.recruitment.entity.ForumLike;
import com.lab.recruitment.entity.ForumPost;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.ForumCommentMapper;
import com.lab.recruitment.mapper.ForumLikeMapper;
import com.lab.recruitment.mapper.ForumPostMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForumServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements ForumService {

    @Autowired
    private ForumCommentMapper commentMapper;

    @Autowired
    private ForumLikeMapper likeMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<ForumPost> getPostPage(Integer pageNum, Integer pageSize, String keyword, Boolean isEssence, Long currentUserId) {
        Page<ForumPost> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ForumPost> query = new QueryWrapper<>();
        
        query.eq("deleted", 0);
        
        if (isEssence != null && isEssence) {
            query.eq("is_essence", 1);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            query.and(wrapper -> wrapper.like("title", keyword).or().like("content", keyword));
        }
        
        // Ordering: Pinned first, then create_time desc
        query.orderByDesc("is_pinned", "create_time");
        
        Page<ForumPost> result = this.page(page, query);
        
        // Populate author info and like status
        result.getRecords().forEach(post -> {
            post.setAuthor(getUserSimpleInfo(post.getUserId()));
            if (currentUserId != null) {
                post.setIsLiked(checkIsLiked(post.getId(), currentUserId));
            } else {
                post.setIsLiked(false);
            }
        });
        
        return result;
    }

    @Override
    public ForumPost getPostDetail(Long id, Long currentUserId) {
        ForumPost post = this.getById(id);
        if (post == null) return null;
        
        post.setAuthor(getUserSimpleInfo(post.getUserId()));
        if (currentUserId != null) {
            post.setIsLiked(checkIsLiked(post.getId(), currentUserId));
        }
        
        // Increment view count (simple implementation, no strict unique check)
        post.setViewCount(post.getViewCount() + 1);
        this.updateById(post);
        
        return post;
    }

    @Override
    @Transactional
    public boolean createPost(ForumPost post, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        
        // Check limits for students
        if ("student".equals(user.getRole())) {
            // Count posts today
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            
            QueryWrapper<ForumPost> query = new QueryWrapper<>();
            query.eq("user_id", userId)
                 .ge("create_time", startOfDay)
                 .le("create_time", endOfDay);
            
            Long count = this.count(query);
            if (count >= 1) {
                throw new RuntimeException("普通成员每天只能发布一条帖子");
            }
        }
        
        post.setUserId(userId);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setIsPinned(false);
        post.setIsEssence(false);
        
        return this.save(post);
    }

    @Override
    public boolean deletePost(Long id, Long userId, boolean isAdmin) {
        ForumPost post = this.getById(id);
        if (post == null) return false;
        
        if (!isAdmin && !post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此贴");
        }
        
        return this.removeById(id);
    }

    @Override
    public boolean setPinned(Long id, boolean isPinned) {
        ForumPost post = this.getById(id);
        if (post == null) return false;
        post.setIsPinned(isPinned);
        return this.updateById(post);
    }

    @Override
    public boolean setEssence(Long id, boolean isEssence) {
        ForumPost post = this.getById(id);
        if (post == null) return false;
        post.setIsEssence(isEssence);
        return this.updateById(post);
    }

    // --- Comments ---

    @Override
    public Page<ForumComment> getCommentPage(Integer pageNum, Integer pageSize, Long postId) {
        Page<ForumComment> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ForumComment> query = new QueryWrapper<>();
        query.eq("post_id", postId).eq("deleted", 0);
        query.orderByAsc("create_time");
        
        Page<ForumComment> result = commentMapper.selectPage(page, query);
        result.getRecords().forEach(comment -> {
            comment.setAuthor(getUserSimpleInfo(comment.getUserId()));
        });
        
        return result;
    }

    @Override
    @Transactional
    public boolean createComment(ForumComment comment, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        
        // Check limits for students
        if ("student".equals(user.getRole())) {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            
            QueryWrapper<ForumComment> query = new QueryWrapper<>();
            query.eq("user_id", userId)
                 .ge("create_time", startOfDay)
                 .le("create_time", endOfDay);
            
            Long count = commentMapper.selectCount(query);
            if (count >= 100) {
                throw new RuntimeException("普通成员每天评论上限为100条");
            }
        }
        
        comment.setUserId(userId);
        int rows = commentMapper.insert(comment);
        
        if (rows > 0) {
            // Update post comment count
            ForumPost post = this.getById(comment.getPostId());
            if (post != null) {
                post.setCommentCount(post.getCommentCount() + 1);
                this.updateById(post);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteComment(Long id, Long userId, boolean isAdmin) {
        ForumComment comment = commentMapper.selectById(id);
        if (comment == null) return false;
        
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }
        
        // Logic delete
        comment.setDeleted(1);
        int rows = commentMapper.updateById(comment);
        
        if (rows > 0) {
            // Decrement post comment count
            ForumPost post = this.getById(comment.getPostId());
            if (post != null && post.getCommentCount() > 0) {
                post.setCommentCount(post.getCommentCount() - 1);
                this.updateById(post);
            }
            return true;
        }
        return false;
    }

    // --- Likes ---

    @Override
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        QueryWrapper<ForumLike> query = new QueryWrapper<>();
        query.eq("post_id", postId).eq("user_id", userId);
        ForumLike existingLike = likeMapper.selectOne(query);
        
        ForumPost post = this.getById(postId);
        if (post == null) return false;
        
        if (existingLike != null) {
            // Unlike
            likeMapper.deleteById(existingLike.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            // Like
            ForumLike like = new ForumLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
            
            // Auto essence check
            if (post.getLikeCount() >= 100) {
                post.setIsEssence(true);
            }
        }
        
        return this.updateById(post);
    }
    
    private boolean checkIsLiked(Long postId, Long userId) {
        QueryWrapper<ForumLike> query = new QueryWrapper<>();
        query.eq("post_id", postId).eq("user_id", userId);
        return likeMapper.selectCount(query) > 0;
    }
    
    private User getUserSimpleInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            User simple = new User();
            simple.setId(user.getId());
            simple.setUsername(user.getUsername());
            simple.setRealName(user.getRealName());
            simple.setAvatar(user.getAvatar());
            simple.setRole(user.getRole());
            return simple;
        }
        return null;
    }
}
