package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.ForumComment;
import com.lab.recruitment.entity.ForumPost;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.ForumService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/forum")
public class ForumController {

    @Autowired
    private ForumService forumService;
    
    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(username)) return null;
        return userService.findByUsername(username);
    }

    // --- Posts ---

    @GetMapping("/post/list")
    public Result<Page<ForumPost>> getPostList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isEssence) {
        
        User currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        return Result.success(forumService.getPostPage(pageNum, pageSize, keyword, isEssence, userId));
    }

    @GetMapping("/post/{id}")
    public Result<ForumPost> getPostDetail(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        return Result.success(forumService.getPostDetail(id, userId));
    }

    @PostMapping("/post/add")
    public Result<Boolean> addPost(@RequestBody ForumPost post) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("请先登录");
        
        try {
            return Result.success(forumService.createPost(post, currentUser.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/post/{id}")
    public Result<Boolean> deletePost(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("请先登录");
        
        boolean isAdmin = "admin".equals(currentUser.getRole()) || "super_admin".equals(currentUser.getRole());
        try {
            return Result.success(forumService.deletePost(id, currentUser.getId(), isAdmin));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/post/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> setPinned(@PathVariable Long id, @RequestParam Boolean isPinned) {
        return Result.success(forumService.setPinned(id, isPinned));
    }

    @PutMapping("/post/{id}/essence")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Boolean> setEssence(@PathVariable Long id, @RequestParam Boolean isEssence) {
        return Result.success(forumService.setEssence(id, isEssence));
    }

    // --- Comments ---

    @GetMapping("/comment/list")
    public Result<Page<ForumComment>> getCommentList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long postId) {
        return Result.success(forumService.getCommentPage(pageNum, pageSize, postId));
    }

    @PostMapping("/comment/add")
    public Result<Boolean> addComment(@RequestBody ForumComment comment) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("请先登录");
        
        try {
            return Result.success(forumService.createComment(comment, currentUser.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/comment/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("请先登录");
        
        boolean isAdmin = "admin".equals(currentUser.getRole()) || "super_admin".equals(currentUser.getRole());
        try {
            return Result.success(forumService.deleteComment(id, currentUser.getId(), isAdmin));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // --- Likes ---

    @PostMapping("/post/{id}/like")
    public Result<Boolean> toggleLike(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("请先登录");
        
        return Result.success(forumService.toggleLike(id, currentUser.getId()));
    }
}
