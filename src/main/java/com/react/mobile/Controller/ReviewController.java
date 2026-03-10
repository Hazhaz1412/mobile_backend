package com.react.mobile.Controller;

import com.react.mobile.DTO.request.CreateReviewRequest;
import com.react.mobile.DTO.request.ReportReviewRequest;
import com.react.mobile.DTO.request.ReviewReplyRequest;
import com.react.mobile.DTO.response.ReviewHelpfulResponse;
import com.react.mobile.DTO.response.ReviewListResponse;
import com.react.mobile.DTO.response.ReviewResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthUserRepository authUserRepository;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateReviewRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.createReview(user, request));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody CreateReviewRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.updateReview(user, reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        AuthUser user = resolveUser(userDetails);
        reviewService.deleteReview(user, reviewId);
        return ResponseEntity.ok(Map.of("message", "Review deleted"));
    }

    @GetMapping
    public ResponseEntity<ReviewListResponse> listReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NEWEST") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.listReviews(user, targetType, targetId, search, sortBy, page, size));
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<ReviewListResponse> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.getMyReviews(user, page, size));
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyToReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody ReviewReplyRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.replyToReview(user, reviewId, request));
    }

    @PostMapping("/{reviewId}/report")
    public ResponseEntity<Map<String, String>> reportReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody ReportReviewRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        reviewService.reportReview(user, reviewId, request);
        return ResponseEntity.ok(Map.of("message", "Review has been reported"));
    }

    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<ReviewHelpfulResponse> toggleHelpfulVote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.toggleHelpfulVote(user, reviewId));
    }

    @GetMapping("/flagged")
    public ResponseEntity<ReviewListResponse> getFlaggedReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.getFlaggedReviews(user, page, size));
    }

    @PostMapping("/flagged/{reviewId}/approve")
    public ResponseEntity<ReviewResponse> approveFlaggedReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(reviewService.approveFlaggedReview(user, reviewId));
    }

    @DeleteMapping("/flagged/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteFlaggedReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        AuthUser user = resolveUser(userDetails);
        reviewService.deleteFlaggedReview(user, reviewId);
        return ResponseEntity.ok(Map.of("message", "Flagged review deleted"));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Unauthorized");
        }
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
