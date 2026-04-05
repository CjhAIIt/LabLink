package com.lab.recruitment.service;

import com.lab.recruitment.dto.GrowthAssessmentSubmitDTO;
import com.lab.recruitment.entity.User;

import java.util.List;
import java.util.Map;

public interface GrowthCenterService {

    Map<String, Object> getDashboard(User user);

    Map<String, Object> getAssessmentQuestions();

    Map<String, Object> submitAssessment(User user, GrowthAssessmentSubmitDTO submitDTO);

    List<Map<String, Object>> getTracks(User user, String category);

    Map<String, Object> getTrackDetail(User user, String code);
}
