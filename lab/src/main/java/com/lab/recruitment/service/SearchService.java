package com.lab.recruitment.service;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.vo.SearchResultItemVO;

import java.util.List;
import java.util.Map;

public interface SearchService {

    Map<String, Object> globalSearch(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchLabs(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchUsers(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchProfiles(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchDevices(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchFiles(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchNotices(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);

    List<SearchResultItemVO> searchAttendanceTasks(String keyword, Integer limit, Long collegeId, Long labId, User currentUser);
}
