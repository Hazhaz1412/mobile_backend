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
public class LocationDiscoveryResponse {
    private LocationSnapshotResponse referenceLocation;
    private Double radiusKm;
    private List<NearbyPlaceResponse> pointsOfInterest;
    private List<NearbyPlaceResponse> events;
    private List<NearbyPlaceResponse> recommendations;
}
