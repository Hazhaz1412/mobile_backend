package com.react.mobile.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventRequest {
    private String title;
    private String description;
    private String eventType;
    private Boolean isFree;
    private Double price;
    private String currency;
    private String startDate;
    private String endDate;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private Integer maxAttendees;
    private String imageUrl;
}
