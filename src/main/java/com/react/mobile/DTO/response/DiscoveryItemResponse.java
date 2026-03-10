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
public class DiscoveryItemResponse {
    private String id;
    private String name;
    private String category;
    private List<String> tags;
    private String shortDescription;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Double rating;
    private Integer reviewCount;
    private Integer priceLevel;
    private Integer popularityScore;
    private String thumbnailUrl;
    private String pricingText;
    private String operationalHours;
    private Boolean openNow;
    private String availabilityLabel;
    private String directionsUrl;
    private Boolean bookmarked;
}
