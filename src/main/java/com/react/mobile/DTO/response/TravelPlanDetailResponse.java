package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Boolean isPublic;
    private String shareToken;
    private String sharePath;
    private String ownerUsername;
    private String createdAt;
    private String updatedAt;
    private List<TravelPlanItemResponse> items;
}
