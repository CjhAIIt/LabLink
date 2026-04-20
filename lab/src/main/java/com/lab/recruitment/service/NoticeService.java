package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.Notice;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface NoticeService extends IService<Notice> {

    Page<Map<String, Object>> getNoticePage(Integer pageNum, Integer pageSize, String publishScope,
                                            Long collegeId, Long labId, String keyword, User currentUser);

    List<Map<String, Object>> getLatestNotices(User currentUser, int limit);

    boolean createNotice(Notice notice, User currentUser);

    boolean updateNotice(Notice notice, User currentUser);

    boolean deleteNotice(Long id, User currentUser);
}
