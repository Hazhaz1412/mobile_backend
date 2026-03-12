package com.react.mobile.Controller;

import com.react.mobile.DTO.request.CreateTravelPlanItemRequest;
import com.react.mobile.DTO.request.CreateTravelPlanRequest;
import com.react.mobile.DTO.request.OptimizeRouteRequest;
import com.react.mobile.DTO.request.UpdateTravelPlanItemRequest;
import com.react.mobile.DTO.request.UpdateTravelPlanRequest;
import com.react.mobile.DTO.response.TravelPlanDetailResponse;
import com.react.mobile.DTO.response.TravelPlanItemResponse;
import com.react.mobile.DTO.response.TravelPlanShareResponse;
import com.react.mobile.DTO.response.TravelPlanSummaryResponse;
import com.react.mobile.DTO.response.TravelRouteOptimizationResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanService travelPlanService;
    private final AuthUserRepository authUserRepository;

    @GetMapping
    public ResponseEntity<List<TravelPlanSummaryResponse>> listPlans(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.listPlans(user));
    }

    @PostMapping
    public ResponseEntity<TravelPlanDetailResponse> createPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateTravelPlanRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.createPlan(user, request));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<TravelPlanDetailResponse> getPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.getPlan(user, planId));
    }

    @PutMapping("/{planId}")
    public ResponseEntity<TravelPlanDetailResponse> updatePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @RequestBody UpdateTravelPlanRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.updatePlan(user, planId, request));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Map<String, String>> deletePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId
    ) {
        AuthUser user = resolveUser(userDetails);
        travelPlanService.deletePlan(user, planId);
        return ResponseEntity.ok(Map.of("message", "Travel plan deleted"));
    }

    @PostMapping("/{planId}/items")
    public ResponseEntity<TravelPlanItemResponse> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @RequestBody CreateTravelPlanItemRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.addItem(user, planId, request));
    }

    @PutMapping("/{planId}/items/{itemId}")
    public ResponseEntity<TravelPlanItemResponse> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @PathVariable Long itemId,
            @RequestBody UpdateTravelPlanItemRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.updateItem(user, planId, itemId, request));
    }

    @DeleteMapping("/{planId}/items/{itemId}")
    public ResponseEntity<Map<String, String>> deleteItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @PathVariable Long itemId
    ) {
        AuthUser user = resolveUser(userDetails);
        travelPlanService.deleteItem(user, planId, itemId);
        return ResponseEntity.ok(Map.of("message", "Travel plan item deleted"));
    }

    @PostMapping("/{planId}/share")
    public ResponseEntity<TravelPlanShareResponse> sharePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.sharePlan(user, planId));
    }

    @PostMapping("/{planId}/optimize-route")
    public ResponseEntity<TravelRouteOptimizationResponse> optimizeRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @RequestBody OptimizeRouteRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(travelPlanService.optimizeRoute(user, planId, request));
    }

    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<TravelPlanDetailResponse> getSharedPlan(
            @PathVariable String shareToken
    ) {
        return ResponseEntity.ok(travelPlanService.getSharedPlan(shareToken));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Unauthorized");
        }
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
