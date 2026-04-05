package com.lab.recruitment.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GradPathBridgeService {

    private final RestTemplate restTemplate;

    @Value("${gradpath.base-url:http://localhost:8080}")
    private String gradPathBaseUrl;

    @Value("${gradpath.ws-url:ws://localhost:8080/ws/interview}")
    private String gradPathWsUrl;

    public GradPathBridgeService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Map<String, String> getConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("baseUrl", trimBaseUrl());
        config.put("wsUrl", gradPathWsUrl);
        return config;
    }

    public Object getQuestionList(Integer pageNum, Integer pageSize, String keyword, String orderBy,
                                  String orderDirection, String trackCode) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("pageNum", String.valueOf(pageNum == null ? 1 : pageNum));
        queryParams.add("pageSize", String.valueOf(pageSize == null ? 9 : pageSize));
        if (trackCode != null && !trackCode.trim().isEmpty()) {
            queryParams.add("trackCode", trackCode.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryParams.add("keyword", keyword.trim());
        }
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            queryParams.add("orderBy", orderBy.trim());
        }
        if (orderDirection != null && !orderDirection.trim().isEmpty()) {
            queryParams.add("orderDirection", orderDirection.trim());
        }
        return exchange("/api/ai/list", HttpMethod.GET, null, queryParams);
    }

    public Object getQuestionDetail(Long questionId) {
        return exchange("/api/ai/detail/" + questionId, HttpMethod.GET, null, null);
    }

    public Object generateQuestion(String keyword, String trackCode) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("keyword", keyword);
        if (trackCode != null && !trackCode.trim().isEmpty()) {
            queryParams.add("trackCode", trackCode.trim());
        }
        return exchange("/api/ai/generate", HttpMethod.POST, null, queryParams);
    }

    public Object debugCode(Map<String, Object> payload) {
        return exchange("/api/judge/debug", HttpMethod.POST, payload, null);
    }

    public Object submitCode(Map<String, Object> payload) {
        return exchange("/api/judge/submit", HttpMethod.POST, payload, null);
    }

    public Object analyzeCode(Map<String, Object> payload) {
        return exchange("/api/judge/analyze", HttpMethod.POST, payload, null);
    }

    private Object exchange(String path, HttpMethod method, Object body, MultiValueMap<String, String> queryParams) {
        String url = buildUrl(path, queryParams);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body == null ? null : JSON.toJSONString(body), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            return extractData(response.getBody());
        } catch (HttpStatusCodeException exception) {
            String responseBody = exception.getResponseBodyAsString();
            throw new RuntimeException(parseRemoteErrorMessage(responseBody, exception.getStatusCode().toString()));
        } catch (Exception exception) {
            throw new RuntimeException("GradPath service request failed: " + exception.getMessage());
        }
    }

    private Object extractData(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new RuntimeException("GradPath service returned an empty response");
        }

        JSONObject response = JSON.parseObject(responseBody);
        if (response == null) {
            throw new RuntimeException("GradPath service returned an invalid response");
        }

        Integer code = response.getInteger("code");
        if (code == null || code != 1) {
            throw new RuntimeException(parseRemoteErrorMessage(responseBody, "GradPath service error"));
        }
        return response.get("data");
    }

    private String parseRemoteErrorMessage(String responseBody, String fallbackMessage) {
        try {
            JSONObject response = JSON.parseObject(responseBody);
            if (response != null) {
                String message = response.getString("msg");
                if (message == null || message.trim().isEmpty()) {
                    message = response.getString("message");
                }
                if (message != null && !message.trim().isEmpty()) {
                    return message.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return fallbackMessage;
    }

    private String buildUrl(String path, MultiValueMap<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trimBaseUrl() + path);
        if (queryParams != null && !queryParams.isEmpty()) {
            builder.queryParams(queryParams);
        }
        return builder.build(true).toUriString();
    }

    private String trimBaseUrl() {
        if (gradPathBaseUrl == null || gradPathBaseUrl.trim().isEmpty()) {
            return "http://localhost:8080";
        }
        String value = gradPathBaseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
