package com.lab.recruitment.vo;

import lombok.Data;

import java.util.Map;

@Data
public class SearchResultItemVO {

    private String type;

    private Long id;

    private String title;

    private String subtitle;

    private String status;

    private Map<String, Object> extra;
}
