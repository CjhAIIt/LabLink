package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.entity.LabMember;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface LabMemberService extends IService<LabMember> {

    Page<Map<String, Object>> getMemberPage(Integer pageNum, Integer pageSize, Long labId, String status,
                                            String memberRole, String keyword, User currentUser);

    List<Map<String, Object>> getActiveMembers(Long labId, User currentUser);

    void activateMember(Long labId, Long userId, String memberRole, Long appointedBy, String remark);

    boolean appointLeader(Long memberId, User currentUser);

    boolean removeMember(Long memberId, String remark, User currentUser);
}
