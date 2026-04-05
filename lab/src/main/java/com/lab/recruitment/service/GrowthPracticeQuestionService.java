package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.dto.PracticeQuestionSubmitDTO;
import com.lab.recruitment.dto.WrittenExamQuestionDTO;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface GrowthPracticeQuestionService {

    Map<String, Object> getStudentQuestionPage(User user,
                                               Integer pageNum,
                                               Integer pageSize,
                                               String trackCode,
                                               String questionType,
                                               String keyword);

    Map<String, Object> getStudentQuestionDetail(User user, Long questionId);

    Map<String, Object> submitPracticeAnswer(User user, PracticeQuestionSubmitDTO submitDTO);

    Page<Map<String, Object>> getAdminQuestionPage(User user,
                                                   Integer pageNum,
                                                   Integer pageSize,
                                                   String trackCode,
                                                   String questionType,
                                                   String keyword);

    Map<String, Object> getAdminQuestionDetail(User user, Long questionId);

    Map<String, Object> saveAdminQuestion(User user, WrittenExamQuestionDTO questionDTO);

    boolean deleteAdminQuestion(User user, Long questionId);

    List<WrittenExamQuestionDTO> getQuestionSnapshots(List<Long> questionIds, boolean includeAnswerConfig);
}
