package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Boolean isPublic;
    private Long itemCount;
    private Long dayCount;
    private String createdAt;
    private String updatedAt;
}
