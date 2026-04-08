package com.react.mobile.Controller;

import com.react.mobile.DTO.response.ActivityFeedResponse;
import com.react.mobile.DTO.response.UserPublicProfileResponse;
import com.react.mobile.DTO.request.ReportUserRequest;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final AuthUserRepository authUserRepository;

    @PostMapping("/follow/{userId}")
    public ResponseEntity<Map<String, String>> follow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId
    ) {
        socialService.follow(resolveUser(userDetails), userId);
        return ResponseEntity.ok(Map.of("message", "Followed"));
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<Map<String, String>> unfollow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId
    ) {
        socialService.unfollow(resolveUser(userDetails), userId);
        return ResponseEntity.ok(Map.of("message", "Unfollowed"));
    }

    @GetMapping("/followers")
    public ResponseEntity<List<UserPublicProfileResponse>> followers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(socialService.getFollowers(resolveUser(userDetails), page, size));
    }

    @GetMapping("/following")
    public ResponseEntity<List<UserPublicProfileResponse>> following(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(socialService.getFollowing(resolveUser(userDetails), page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserPublicProfileResponse>> searchUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(socialService.searchUsers(resolveUser(userDetails), query, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserPublicProfileResponse> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(socialService.getUserProfile(resolveUser(userDetails), userId));
    }

    @PostMapping("/user/{userId}/report")
    public ResponseEntity<Map<String, String>> reportUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @RequestBody ReportUserRequest request
    ) {
        AuthUser currentUser = resolveUser(userDetails);
        socialService.reportUser(currentUser, userId, request);
        return ResponseEntity.ok(Map.of("message", "User has been reported"));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<ActivityFeedResponse>> feed(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(socialService.getFeed(resolveUser(userDetails), page, size));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
