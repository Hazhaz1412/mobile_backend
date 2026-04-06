package com.react.mobile.DTO.request;

import lombok.Data;

@Data
public class SmartSearchRequest {
    private Double budget;
    private String mood;
    private Double freeHours;
    private Double maxDistanceKm;
    private Double latitude;
    private Double longitude;
    private String preferences;
}
