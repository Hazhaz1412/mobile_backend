package com.react.mobile.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTravelPlanRequest {
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Boolean isPublic;
}
