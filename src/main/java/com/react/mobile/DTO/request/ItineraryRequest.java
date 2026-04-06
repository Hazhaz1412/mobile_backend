package com.react.mobile.DTO.request;

import lombok.Data;

@Data
public class ItineraryRequest {
    private Double durationHours;
    private String startTime;
    private String mood;
    private Double budget;
    private Double latitude;
    private Double longitude;
    private Double maxDistanceKm;
}
