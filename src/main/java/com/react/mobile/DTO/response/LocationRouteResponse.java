package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRouteResponse {
    private Double fromLatitude;
    private Double fromLongitude;
    private Double toLatitude;
    private Double toLongitude;
    private Double distanceKm;
    private Integer estimatedMinutes;
    private String travelMode;
    private String googleMapsUrl;
    private String mapboxDirectionsUrl;
}
