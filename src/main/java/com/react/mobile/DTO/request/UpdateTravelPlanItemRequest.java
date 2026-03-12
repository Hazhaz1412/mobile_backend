package com.react.mobile.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTravelPlanItemRequest {
    private Integer dayNumber;
    private String itemType;
    private String referenceId;
    private String title;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private String startTime;
    private String endTime;
    private String note;
    private String reminderAt;
    private Integer sortOrder;
}
