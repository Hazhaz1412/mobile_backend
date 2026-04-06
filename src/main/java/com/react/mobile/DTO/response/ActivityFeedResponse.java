package com.react.mobile.DTO.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityFeedResponse {
    private Long id;
    private Long actorId;
    private String actorUsername;
    private String actorProfilePictureUrl;
    private String actionType;
    private String targetType;
    private String targetId;
    private String targetName;
    private String metadata;
    private String createdAt;
}
