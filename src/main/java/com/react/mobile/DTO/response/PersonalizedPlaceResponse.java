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
public class PersonalizedPlaceResponse {
    private String id;
    private String name;
    private String type;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Double averageRating;
    private Integer reviewCount;
    private Integer personalizationScore;
    private Integer proximityScore;
    private Integer reviewPriorityScore;
    private String navigationUrl;
    private List<String> reasons;
}
