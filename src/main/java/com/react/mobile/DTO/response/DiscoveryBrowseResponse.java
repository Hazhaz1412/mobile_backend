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
public class DiscoveryBrowseResponse {
    private String query;
    private String category;
    private Double minRating;
    private Integer maxPriceLevel;
    private Integer minPopularity;
    private Double maxDistanceKm;
    private String sortBy;
    private Double referenceLatitude;
    private Double referenceLongitude;
    private List<String> autocompleteSuggestions;
    private List<DiscoveryItemResponse> items;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer totalItems = 0;
    @Builder.Default
    private Integer totalPages = 1;
    @Builder.Default
    private Boolean hasNext = false;
}
