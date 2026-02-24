package com.react.mobile.Controller;

import com.react.mobile.DTO.request.LocationUpdateRequest;
import com.react.mobile.DTO.response.LocationDiscoveryResponse;
import com.react.mobile.DTO.response.LocationRouteResponse;
import com.react.mobile.DTO.response.LocationSnapshotResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final AuthUserRepository authUserRepository;

    @PostMapping("/realtime")
    public ResponseEntity<LocationSnapshotResponse> updateRealtimeLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LocationUpdateRequest request) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(locationService.updateRealtimeLocation(authUser, request));
    }

    @PostMapping("/manual-override")
    public ResponseEntity<LocationSnapshotResponse> updateManualLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LocationUpdateRequest request) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(locationService.updateManualLocation(authUser, request));
    }

    @GetMapping("/current")
    public ResponseEntity<LocationSnapshotResponse> getCurrentLocation(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(locationService.getCurrentLocation(authUser));
    }

    @GetMapping("/history")
    public ResponseEntity<List<LocationSnapshotResponse>> getLocationHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "20") Integer limit) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(locationService.getLocationHistory(authUser, limit));
    }

    @GetMapping("/discover")
    public ResponseEntity<LocationDiscoveryResponse> discoverNearby(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "8") Double radiusKm) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(locationService.discoverNearby(authUser, latitude, longitude, radiusKm));
    }

    @GetMapping("/route")
    public ResponseEntity<LocationRouteResponse> getRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Double fromLatitude,
            @RequestParam(required = false) Double fromLongitude,
            @RequestParam Double toLatitude,
            @RequestParam Double toLongitude,
            @RequestParam(defaultValue = "walking") String mode) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(locationService.getRoute(authUser, fromLatitude, fromLongitude, toLatitude, toLongitude, mode));
    }

    private AuthUser getCurrentUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
