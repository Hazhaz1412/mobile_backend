package com.react.mobile.Service;

import com.react.mobile.DTO.request.ItineraryRequest;
import com.react.mobile.DTO.request.SmartSearchRequest;
import com.react.mobile.DTO.response.DiscoveryItemResponse;
import com.react.mobile.DTO.response.ItineraryResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface SmartSearchService {
    List<DiscoveryItemResponse> similarPlaces(AuthUser user, String placeId, int limit);
    List<DiscoveryItemResponse> smartSearch(AuthUser user, SmartSearchRequest request);
    ItineraryResponse generateItinerary(AuthUser user, ItineraryRequest request);
}
