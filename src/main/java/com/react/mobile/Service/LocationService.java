package com.react.mobile.Service;

import com.react.mobile.DTO.request.LocationUpdateRequest;
import com.react.mobile.DTO.response.LocationDiscoveryResponse;
import com.react.mobile.DTO.response.LocationRouteResponse;
import com.react.mobile.DTO.response.LocationSnapshotResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface LocationService {
    LocationSnapshotResponse updateRealtimeLocation(AuthUser authUser, LocationUpdateRequest request);

    LocationSnapshotResponse updateManualLocation(AuthUser authUser, LocationUpdateRequest request);

    LocationSnapshotResponse getCurrentLocation(AuthUser authUser);

    List<LocationSnapshotResponse> getLocationHistory(AuthUser authUser, Integer limit);

    LocationDiscoveryResponse discoverNearby(AuthUser authUser, Double latitude, Double longitude, Double radiusKm);

    LocationRouteResponse getRoute(AuthUser authUser, Double fromLatitude, Double fromLongitude, Double toLatitude, Double toLongitude, String mode);
}
