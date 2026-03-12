package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewResponse {
    private Long id;
    private String targetType;
    private String targetId;
    private String targetName;
    private Double rating;
    private String comment;
    private String authorUsername;
    private Long authorId;
    private Long helpfulCount;
    private Long flagCount;
    private String moderationStatus;
    private String createdAt;
    private String updatedAt;
}
