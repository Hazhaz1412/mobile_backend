package com.react.mobile.Service;

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

import java.util.List;

public interface TravelPlanService {

    List<TravelPlanSummaryResponse> listPlans(AuthUser user);

    TravelPlanDetailResponse createPlan(AuthUser user, CreateTravelPlanRequest request);

    TravelPlanDetailResponse getPlan(AuthUser user, Long planId);

    TravelPlanDetailResponse updatePlan(AuthUser user, Long planId, UpdateTravelPlanRequest request);

    void deletePlan(AuthUser user, Long planId);

    TravelPlanItemResponse addItem(AuthUser user, Long planId, CreateTravelPlanItemRequest request);

    TravelPlanItemResponse updateItem(AuthUser user, Long planId, Long itemId, UpdateTravelPlanItemRequest request);

    void deleteItem(AuthUser user, Long planId, Long itemId);

    TravelPlanShareResponse sharePlan(AuthUser user, Long planId);

    TravelPlanDetailResponse getSharedPlan(String shareToken);

    TravelRouteOptimizationResponse optimizeRoute(AuthUser user, Long planId, OptimizeRouteRequest request);
}
