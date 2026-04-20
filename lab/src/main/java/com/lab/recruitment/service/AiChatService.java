package com.lab.recruitment.service;

import java.util.List;
import java.util.Map;

public interface AiChatService {

    /**
     * 发送对话请求给 AI，返回 AI 回复
     */
    String chat(String moduleName, String mode, List<Map<String, String>> chatHistory, String promptTemplate);

    /**
     * 生成面试评估报告
     */
    Map<String, Object> generateReport(String moduleName, List<Map<String, String>> chatHistory);
}
