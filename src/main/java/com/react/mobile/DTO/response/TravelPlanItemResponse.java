package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanItemResponse {
    private Long id;
    private Integer dayNumber;
    private Integer sortOrder;
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
    private String createdAt;
    private String updatedAt;
}
