package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedDiscoveryResponse {
    private LocationSnapshotResponse referenceLocation;
    private Double radiusKm;
    private String timeOfDay;
    private String weather;
    private String season;
    private String travelStyle;
    private String modelVersion;
    private Double learningConfidence;
    private Map<String, Integer> learnedInterestWeights;
    private List<PersonalizedPlaceResponse> prioritizedResults;
    private List<SimilarAttractionGroupResponse> similarAttractions;
}
