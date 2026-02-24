package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyPlaceResponse {
    private String id;
    private String name;
    private String type;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Integer recommendationScore;
    private String navigationUrl;
}
