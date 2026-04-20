package com.lab.recruitment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lab.recruitment.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    // PLACEHOLDER_CHAT_METHOD

    @Override
    public String chat(String moduleName, String mode, List<Map<String, String>> chatHistory, String promptTemplate) {
        String systemPrompt = buildChatPrompt(moduleName, mode, promptTemplate);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (chatHistory != null) {
            messages.addAll(chatHistory);
        }
        return callApi(messages, 0.7, 500);
    }

    @Override
    public Map<String, Object> generateReport(String moduleName, List<Map<String, String>> chatHistory) {
        String systemPrompt = "你是一位技术面试评估专家。请根据以下面试对话记录，对候选人进行评估。\n" +
                "面试模块：" + moduleName + "\n\n" +
                "请严格按照以下 JSON 格式输出评估结果，不要输出任何其他内容：\n" +
                "{\"score\": 0-100的整数, \"tags\": [\"标签1\",\"标签2\",\"标签3\"], " +
                "\"summary\": \"100字左右的综合评价\", " +
                "\"strengths\": \"候选人的优势\", " +
                "\"weaknesses\": \"候选人的不足\", " +
                "\"suggestions\": \"学习建议\"}";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (chatHistory != null) {
            for (Map<String, String> msg : chatHistory) {
                messages.add(Map.of("role", msg.get("role"), "content", msg.get("content")));
            }
        }
        messages.add(Map.of("role", "user", "content", "请根据以上对话生成评估报告。"));

        String reply = callApi(messages, 0.3, 800);
        try {
            return JSON.parseObject(reply, Map.class);
        } catch (Exception e) {
            // 尝试提取 JSON
            int start = reply.indexOf('{');
            int end = reply.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return JSON.parseObject(reply.substring(start, end + 1), Map.class);
                } catch (Exception ignored) {}
            }
            log.error("AI 报告解析失败: {}", reply);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("score", 0);
            fallback.put("tags", List.of());
            fallback.put("summary", "报告生成异常，请联系管理员。");
            return fallback;
        }
    }

    private String buildChatPrompt(String moduleName, String mode, String promptTemplate) {
        if (promptTemplate != null && !promptTemplate.isBlank()) {
            return promptTemplate.replace("{module}", moduleName).replace("{mode}", mode);
        }
        String modeDesc = "formal".equals(mode) ? "正式面试" : "模拟练习";
        return "你是一位资深的实验室技术面试官，正在进行" + modeDesc + "。\n" +
                "当前面试模块：" + moduleName + "\n\n" +
                "面试规则：\n" +
                "1. 每次只提一个问题\n" +
                "2. 根据候选人的回答进行追问，考察理解深度\n" +
                "3. 问题难度从基础到进阶递进\n" +
                "4. 最多提问 10 个问题\n" +
                "5. 面试结束时说「本次面试到此结束，感谢你的参加！」\n" +
                "6. 不要在对话中直接给出评分\n\n" +
                "请开始面试，先向候选人打招呼并提出第一个问题。";
    }

    private String callApi(List<Map<String, String>> messages, double temperature, int maxTokens) {
        String url = baseUrl + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JSONObject json = JSON.parseObject(response.getBody());
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (Exception e) {
            log.error("AI API 调用失败", e);
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试");
        }
    }
}
