package com.lab.recruitment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Notice;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.NoticeService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Map<String, Object>>> getNoticePage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) String publishScope,
                                                           @RequestParam(required = false) Long collegeId,
                                                           @RequestParam(required = false) Long labId,
                                                           @RequestParam(required = false) String keyword) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(noticeService.getNoticePage(pageNum, pageSize, publishScope, collegeId, labId, keyword, currentUser));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getLatestNotices(@RequestParam(defaultValue = "6") Integer limit) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return Result.success(noticeService.getLatestNotices(currentUser, limit));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> createNotice(@RequestBody Notice notice) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return noticeService.createNotice(notice, currentUser) ? Result.success("公告发布成功", null) : Result.error("公告发布失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> updateNotice(@PathVariable Long id, @RequestBody Notice notice) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            notice.setId(id);
            return noticeService.updateNotice(notice, currentUser) ? Result.success("公告更新成功", null) : Result.error("公告更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Object> deleteNotice(@PathVariable Long id) {
        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            return noticeService.deleteNotice(id, currentUser) ? Result.success("公告删除成功", null) : Result.error("公告删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
