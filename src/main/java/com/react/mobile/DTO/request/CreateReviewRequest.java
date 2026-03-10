package com.react.mobile.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {
    private String targetType;
    private String targetId;
    private String targetName;
    private Double rating;
    private String comment;
    private String photoUrl;
}
