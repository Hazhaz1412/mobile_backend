package com.react.mobile.Controller;

import com.react.mobile.DTO.request.ItineraryRequest;
import com.react.mobile.DTO.request.SmartSearchRequest;
import com.react.mobile.DTO.response.DiscoveryItemResponse;
import com.react.mobile.DTO.response.ItineraryResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.SmartSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class SmartSearchController {

    private final SmartSearchService smartSearchService;
    private final AuthUserRepository authUserRepository;

    @GetMapping("/similar/{placeId}")
    public ResponseEntity<List<DiscoveryItemResponse>> similarPlaces(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String placeId,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(
                smartSearchService.similarPlaces(resolveUser(userDetails), placeId, limit));
    }

    @PostMapping("/smart-search")
    public ResponseEntity<List<DiscoveryItemResponse>> smartSearch(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SmartSearchRequest request
    ) {
        return ResponseEntity.ok(
                smartSearchService.smartSearch(resolveUser(userDetails), request));
    }

    @PostMapping("/itinerary")
    public ResponseEntity<ItineraryResponse> generateItinerary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ItineraryRequest request
    ) {
        return ResponseEntity.ok(
                smartSearchService.generateItinerary(resolveUser(userDetails), request));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
