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
public class TravelRouteOptimizationResponse {
    private Long planId;
    private Integer dayNumber;
    private String mode;
    private Double totalDistanceKm;
    private Integer estimatedMinutes;
    private List<TravelPlanItemResponse> optimizedItems;
    private List<RouteLeg> legs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteLeg {
        private String fromTitle;
        private String toTitle;
        private Double distanceKm;
        private Integer estimatedMinutes;
        private String googleMapsUrl;
        private String mapboxDirectionsUrl;
    }
}
