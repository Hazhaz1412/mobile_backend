package com.react.mobile.Controller;

import com.react.mobile.DTO.request.AdminEventModerationRequest;
import com.react.mobile.DTO.response.AdminAnalyticsResponse;
import com.react.mobile.DTO.response.AdminReviewListResponse;
import com.react.mobile.DTO.response.AdminUserListResponse;
import com.react.mobile.DTO.response.EventListResponse;
import com.react.mobile.DTO.response.EventResponse;
import com.react.mobile.DTO.response.ReviewListResponse;
import com.react.mobile.DTO.response.ReviewResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.AdminDashboardService;
import com.react.mobile.Service.EventService;
import com.react.mobile.Service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthUserRepository authUserRepository;
    private final AdminDashboardService adminDashboardService;
    private final EventService eventService;
    private final ReviewService reviewService;

    @GetMapping("/users")
    public ResponseEntity<AdminUserListResponse> listUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(adminDashboardService.listUsers(user, search, page, size));
    }

    @GetMapping("/events")
    public ResponseEntity<EventListResponse> listEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String moderationStatus,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.listEventsForAdmin(
                user,
                status,
                eventType,
                moderationStatus,
                isFree,
                search,
                page,
                size
        ));
    }

    @PostMapping("/events/{eventId}/approve")
    public ResponseEntity<EventResponse> approveEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.approveEvent(user, eventId));
    }

    @PostMapping("/events/{eventId}/reject")
    public ResponseEntity<EventResponse> rejectEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @RequestBody(required = false) AdminEventModerationRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(eventService.rejectEvent(user, eventId, reason));
    }

    @GetMapping("/reviews")
    public ResponseEntity<AdminReviewListResponse> listReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String moderationStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(adminDashboardService.listReviews(user, moderationStatus, search, page, size));
    }

    @GetMapping("/reviews/flagged")
    public ResponseEntity<ReviewListResponse> flaggedReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.getFlaggedReviews(user, page, size));
    }

    @PostMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<ReviewResponse> approveFlaggedReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.approveFlaggedReview(user, reviewId));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteFlaggedReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        AuthUser user = resolveUser(userDetails);
        reviewService.deleteFlaggedReview(user, reviewId);
        return ResponseEntity.ok(Map.of("message", "Flagged review deleted"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsResponse> analytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer windowDays
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(adminDashboardService.getAnalytics(user, windowDays));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Unauthorized");
        }
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
