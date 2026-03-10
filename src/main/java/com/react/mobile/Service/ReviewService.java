package com.react.mobile.Service;

import com.react.mobile.DTO.request.CreateReviewRequest;
import com.react.mobile.DTO.request.ReportReviewRequest;
import com.react.mobile.DTO.request.ReviewReplyRequest;
import com.react.mobile.DTO.response.ReviewHelpfulResponse;
import com.react.mobile.DTO.response.ReviewListResponse;
import com.react.mobile.DTO.response.ReviewResponse;
import com.react.mobile.Entity.AuthUser;

public interface ReviewService {

    ReviewResponse createReview(AuthUser user, CreateReviewRequest request);

    ReviewResponse updateReview(AuthUser user, Long reviewId, CreateReviewRequest request);

    void deleteReview(AuthUser user, Long reviewId);

    ReviewListResponse listReviews(
            AuthUser user,
            String targetType,
            String targetId,
            String search,
            String sortBy,
            int page,
            int size
    );

    ReviewListResponse getMyReviews(AuthUser user, int page, int size);

    ReviewResponse replyToReview(AuthUser user, Long reviewId, ReviewReplyRequest request);

    void reportReview(AuthUser user, Long reviewId, ReportReviewRequest request);

    ReviewHelpfulResponse toggleHelpfulVote(AuthUser user, Long reviewId);

    ReviewListResponse getFlaggedReviews(AuthUser user, int page, int size);

    ReviewResponse approveFlaggedReview(AuthUser user, Long reviewId);

    void deleteFlaggedReview(AuthUser user, Long reviewId);
}
