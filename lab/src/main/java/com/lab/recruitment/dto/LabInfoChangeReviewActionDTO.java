package com.lab.recruitment.dto;

import javax.validation.constraints.NotBlank;

public class LabInfoChangeReviewActionDTO {

    @NotBlank(message = "Review comment is required")
    private String reviewComment;

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
