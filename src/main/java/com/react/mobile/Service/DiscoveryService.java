package com.react.mobile.Service;

import com.react.mobile.DTO.response.DiscoveryBrowseResponse;
import com.react.mobile.DTO.response.DiscoveryDetailResponse;
import com.react.mobile.DTO.response.DiscoveryItemResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface DiscoveryService {
    DiscoveryBrowseResponse browse(
            AuthUser authUser,
            String query,
            String category,
            Double minRating,
            Integer maxPriceLevel,
            Integer minPopularity,
            Double maxDistanceKm,
            String sortBy,
            Double latitude,
            Double longitude,
            Integer limit,
            Integer page
    );

    List<String> suggestions(
            AuthUser authUser,
            String query,
            Double latitude,
            Double longitude,
            Integer limit
    );

    DiscoveryDetailResponse getDetail(AuthUser authUser, String placeId, Double latitude, Double longitude);

    List<DiscoveryItemResponse> getBookmarks(AuthUser authUser, Double latitude, Double longitude);

    boolean setBookmark(AuthUser authUser, String placeId, boolean bookmarked);
}
