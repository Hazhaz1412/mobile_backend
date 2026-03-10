package com.react.mobile.Controller;

import com.react.mobile.DTO.response.DiscoveryBrowseResponse;
import com.react.mobile.DTO.response.DiscoveryDetailResponse;
import com.react.mobile.DTO.response.DiscoveryItemResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final AuthUserRepository authUserRepository;

    @GetMapping("/browse")
    public ResponseEntity<DiscoveryBrowseResponse> browse(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer maxPriceLevel,
            @RequestParam(required = false) Integer minPopularity,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(defaultValue = "RELEVANCE") String sort,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(
                discoveryService.browse(
                        authUser,
                        query,
                        category,
                        minRating,
                        maxPriceLevel,
                        minPopularity,
                        maxDistanceKm,
                        sort,
                        latitude,
                        longitude,
                        limit,
                        page
                )
        );
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "6") Integer limit
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(discoveryService.suggestions(authUser, query, latitude, longitude, limit));
    }

    @GetMapping("/detail/{placeId}")
    public ResponseEntity<DiscoveryDetailResponse> detail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String placeId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(discoveryService.getDetail(authUser, placeId, latitude, longitude));
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<List<DiscoveryItemResponse>> bookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(discoveryService.getBookmarks(authUser, latitude, longitude));
    }

    @PostMapping("/bookmarks/{placeId}")
    public ResponseEntity<String> addBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String placeId
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        discoveryService.setBookmark(authUser, placeId, true);
        return ResponseEntity.ok("Bookmarked");
    }

    @DeleteMapping("/bookmarks/{placeId}")
    public ResponseEntity<String> removeBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String placeId
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        discoveryService.setBookmark(authUser, placeId, false);
        return ResponseEntity.ok("Bookmark removed");
    }

    private AuthUser getCurrentUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
