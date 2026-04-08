package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.CreateReviewRequest;
import com.react.mobile.DTO.request.ReportReviewRequest;
import com.react.mobile.DTO.request.ReviewReplyRequest;
import com.react.mobile.DTO.response.RatingBarResponse;
import com.react.mobile.DTO.response.ReviewHelpfulResponse;
import com.react.mobile.DTO.response.ReviewListResponse;
import com.react.mobile.DTO.response.ReviewResponse;
import com.react.mobile.DTO.response.ReviewSummaryResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.ReviewModerationStatus;
import com.react.mobile.Entity.Enums.ReviewSortBy;
import com.react.mobile.Entity.Enums.ReviewTargetType;
import com.react.mobile.Entity.Review;
import com.react.mobile.Entity.ReviewHelpfulVote;
import com.react.mobile.Entity.ReviewReport;
import com.react.mobile.Repository.EventRepository;
import com.react.mobile.Repository.ReviewHelpfulVoteRepository;
import com.react.mobile.Repository.ReviewRepository;
import com.react.mobile.Repository.ReviewReportRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;
    private final EventRepository eventRepository;
    private final UserProfileRepository userProfileRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional
    public ReviewResponse createReview(AuthUser user, CreateReviewRequest req) {
        String targetType = normalizeTargetType(req.getTargetType());
        String targetName = requireText(req.getTargetName(), "Target name is required");
        String targetId = normalizeTargetId(req.getTargetId(), targetName);
        String comment = requireText(req.getComment(), "Review text is required");
        double rating = normalizeRating(req.getRating());

        if (reviewRepository.existsByUserIdAndTargetTypeAndTargetId(
                user.getId(), targetType, targetId)) {
            throw new IllegalArgumentException("You have already reviewed this item");
        }

        Review review = Review.builder()
                .user(user)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .rating(rating)
                .comment(comment)
                .photoUrl(cleanOptionalText(req.getPhotoUrl()))
                .build();

        review = reviewRepository.save(review);
        return toResponse(review, user, Map.of(), Map.of(), false);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(AuthUser user, Long reviewId, CreateReviewRequest req) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the author can update this review");
        }

        if (req.getRating() != null) {
            review.setRating(normalizeRating(req.getRating()));
        }
        if (req.getComment() != null) {
            review.setComment(requireText(req.getComment(), "Review text is required"));
        }
        if (req.getTargetName() != null) {
            review.setTargetName(requireText(req.getTargetName(), "Target name is required"));
        }
        if (req.getPhotoUrl() != null) {
            review.setPhotoUrl(cleanOptionalText(req.getPhotoUrl()));
        }

        review = reviewRepository.save(review);
        Map<Long, Boolean> helpfulMap = buildHelpfulVoteMap(user.getId(), List.of(review.getId()));
        return toResponse(review, user, helpfulMap, Map.of(), false);
    }

    @Override
    @Transactional
    public void deleteReview(AuthUser user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().getId().equals(user.getId()) && !isPrivileged(user)) {
            throw new AccessDeniedException("Only the author can delete this review");
        }

        reviewHelpfulVoteRepository.deleteByReviewId(reviewId);
        reviewReportRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
    }

    @Override
    public ReviewListResponse listReviews(
            AuthUser user,
            String targetType,
            String targetId,
            String search,
            String sortBy,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        String typeParam = normalizeTargetTypeForFilter(targetType);
        String idParam = (targetId != null && !targetId.isBlank()) ? targetId : null;
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : "";
        ReviewSortBy safeSort = parseSortBy(sortBy);

        Page<Review> pageResult = reviewRepository.findFiltered(
                typeParam,
                idParam,
                searchParam,
                ReviewModerationStatus.APPROVED,
                PageRequest.of(safePage, safeSize, resolveSort(safeSort))
        );

        List<Long> reviewIds = pageResult.getContent().stream()
                .map(Review::getId)
                .toList();
        Map<Long, Boolean> helpfulMap = buildHelpfulVoteMap(user.getId(), reviewIds);
        ReviewSummaryResponse summary = buildSummary(typeParam, idParam, searchParam, ReviewModerationStatus.APPROVED);

        return toListResponse(pageResult, safePage, safeSize, user, helpfulMap, Map.of(), false, summary);
    }

    @Override
    public ReviewListResponse getMyReviews(AuthUser user, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        Page<Review> pageResult = reviewRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(safePage, safeSize));

        List<Long> reviewIds = pageResult.getContent().stream()
                .map(Review::getId)
                .toList();
        Map<Long, Boolean> helpfulMap = buildHelpfulVoteMap(user.getId(), reviewIds);
        return toListResponse(pageResult, safePage, safeSize, user, helpfulMap, Map.of(), false, null);
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(AuthUser user, Long reviewId, ReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!canReply(user, review)) {
            throw new AccessDeniedException("Only organizers or business owners can reply");
        }

        String reply = requireText(request.getReply(), "Reply text is required");
        review.setOwnerReply(reply);
        review.setOwnerReplyAuthor(user.getUsername());
        review.setOwnerReplyAuthorId(user.getId());
        review.setOwnerReplyAt(java.time.LocalDateTime.now());
        review = reviewRepository.save(review);

        Map<Long, Boolean> helpfulMap = buildHelpfulVoteMap(user.getId(), List.of(review.getId()));
        return toResponse(review, user, helpfulMap, Map.of(), false);
    }

    @Override
    @Transactional
    public void reportReview(AuthUser user, Long reviewId, ReportReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot report your own review");
        }

        if (reviewReportRepository.existsByReviewIdAndReporterIdAndResolvedFalse(reviewId, user.getId())) {
            throw new IllegalArgumentException("You have already reported this review");
        }

        String reason = requireText(request.getReason(), "Report reason is required");
        String details = cleanOptionalText(request.getDetails());

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporter(user)
                .reason(reason)
                .details(details)
                .build();

        reviewReportRepository.save(report);
        long currentFlagCount = reviewReportRepository.countByReviewIdAndResolvedFalse(reviewId);
        review.setFlagCount(currentFlagCount);
        review.setModerationStatus(ReviewModerationStatus.FLAGGED);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public ReviewHelpfulResponse toggleHelpfulVote(AuthUser user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        Optional<ReviewHelpfulVote> existingVote =
                reviewHelpfulVoteRepository.findByReviewIdAndUserId(reviewId, user.getId());

        boolean helpfulByCurrentUser;
        if (existingVote.isPresent()) {
            reviewHelpfulVoteRepository.delete(existingVote.get());
            helpfulByCurrentUser = false;
        } else {
            ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                    .review(review)
                    .user(user)
                    .build();
            reviewHelpfulVoteRepository.save(vote);
            helpfulByCurrentUser = true;
        }

        long helpfulCount = reviewHelpfulVoteRepository.countByReviewId(reviewId);
        review.setHelpfulCount(helpfulCount);
        reviewRepository.save(review);

        return ReviewHelpfulResponse.builder()
                .reviewId(reviewId)
                .helpfulCount(helpfulCount)
                .helpfulByCurrentUser(helpfulByCurrentUser)
                .build();
    }

    @Override
    public ReviewListResponse getFlaggedReviews(AuthUser user, int page, int size) {
        ensureAdmin(user);

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        Page<Review> pageResult = reviewRepository.findByModerationStatusOrderByUpdatedAtDesc(
                ReviewModerationStatus.FLAGGED,
                PageRequest.of(safePage, safeSize)
        );

        List<Long> reviewIds = pageResult.getContent().stream()
                .map(Review::getId)
                .toList();
        Map<Long, Boolean> helpfulMap = buildHelpfulVoteMap(user.getId(), reviewIds);
        Map<Long, List<String>> reportReasonsMap = buildReportReasonMap(reviewIds);
        ReviewSummaryResponse summary = buildSummary(null, null, null, ReviewModerationStatus.FLAGGED);

        return toListResponse(
                pageResult,
                safePage,
                safeSize,
                user,
                helpfulMap,
                reportReasonsMap,
                true,
                summary
        );
    }

    @Override
    @Transactional
    public ReviewResponse approveFlaggedReview(AuthUser user, Long reviewId) {
        ensureAdmin(user);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setModerationStatus(ReviewModerationStatus.APPROVED);
        review.setFlagCount(0L);
        review = reviewRepository.save(review);
        resolveReportsForReview(reviewId);

        Map<Long, Boolean> helpfulMap = buildHelpfulVoteMap(user.getId(), List.of(review.getId()));
        return toResponse(review, user, helpfulMap, Map.of(), false);
    }

    @Override
    @Transactional
    public void deleteFlaggedReview(AuthUser user, Long reviewId) {
        ensureAdmin(user);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (review.getModerationStatus() != ReviewModerationStatus.FLAGGED) {
            throw new IllegalArgumentException("Review is not in flagged state");
        }

        reviewHelpfulVoteRepository.deleteByReviewId(reviewId);
        reviewReportRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
    }

    // ── Helpers ──

    private ReviewListResponse toListResponse(
            Page<Review> pageResult,
            int page,
            int size,
            AuthUser requester,
            Map<Long, Boolean> helpfulMap,
            Map<Long, List<String>> reportReasonsMap,
            boolean includeReportReasons,
            ReviewSummaryResponse summary
    ) {
        return ReviewListResponse.builder()
                .reviews(pageResult.getContent().stream()
                        .map(review -> toResponse(
                                review,
                                requester,
                                helpfulMap,
                                reportReasonsMap,
                                includeReportReasons
                        ))
                        .collect(Collectors.toList()))
                .summary(summary)
                .total(pageResult.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    private ReviewResponse toResponse(
            Review review,
            AuthUser requester,
            Map<Long, Boolean> helpfulMap,
            Map<Long, List<String>> reportReasonMap,
            boolean includeReportReasons
    ) {
        Long reviewId = review.getId();
        List<String> reasons = includeReportReasons
                ? reportReasonMap.getOrDefault(reviewId, Collections.emptyList())
                : null;

        return ReviewResponse.builder()
                .id(reviewId)
                .targetType(review.getTargetType())
                .targetId(review.getTargetId())
                .targetName(review.getTargetName())
                .rating(review.getRating())
                .comment(review.getComment())
                .photoUrl(review.getPhotoUrl())
                .authorUsername(review.getUser().getUsername())
                .authorId(review.getUser().getId())
            .authorProfilePictureUrl(userProfileRepository.findByAuthUserId(review.getUser().getId())
                .map(profile -> profile.getProfilePictureUrl())
                .orElse(null))
                .helpfulCount(defaultLong(review.getHelpfulCount()))
                .helpfulByCurrentUser(helpfulMap.getOrDefault(reviewId, false))
                .flagCount(defaultLong(review.getFlagCount()))
                .moderationStatus(review.getModerationStatus())
                .ownerReply(review.getOwnerReply())
                .ownerReplyAuthor(review.getOwnerReplyAuthor())
                .ownerReplyAuthorId(review.getOwnerReplyAuthorId())
                .ownerReplyAt(review.getOwnerReplyAt() != null ? review.getOwnerReplyAt().format(DT_FMT) : null)
                .canReply(canReply(requester, review))
                .canModerate(isPrivileged(requester))
                .reportReasons(reasons)
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DT_FMT) : null)
                .updatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().format(DT_FMT) : null)
                .build();
    }

    private ReviewSummaryResponse buildSummary(
            String targetType,
            String targetId,
            String search,
            ReviewModerationStatus status
    ) {
        List<Double> ratings = reviewRepository.findRatingsForSummary(targetType, targetId, search, status);
        if (ratings == null || ratings.isEmpty()) {
            return ReviewSummaryResponse.builder()
                    .totalReviews(0L)
                    .averageRating(0.0)
                    .ratingBars(emptyRatingBars())
                    .build();
        }

        long total = ratings.size();
        double average = ratings.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Map<Integer, Long> starsCounter = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            starsCounter.put(star, 0L);
        }

        for (Double rating : ratings) {
            int star = normalizeStarForSummary(rating);
            starsCounter.put(star, starsCounter.get(star) + 1L);
        }

        List<RatingBarResponse> bars = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            long count = starsCounter.getOrDefault(star, 0L);
            double percentage = total > 0 ? (count * 100.0) / total : 0.0;
            bars.add(RatingBarResponse.builder()
                    .stars(star)
                    .count(count)
                    .percentage(roundOne(percentage))
                    .build());
        }

        return ReviewSummaryResponse.builder()
                .totalReviews(total)
                .averageRating(roundOne(average))
                .ratingBars(bars)
                .build();
    }

    private List<RatingBarResponse> emptyRatingBars() {
        List<RatingBarResponse> bars = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            bars.add(RatingBarResponse.builder()
                    .stars(star)
                    .count(0L)
                    .percentage(0.0)
                    .build());
        }
        return bars;
    }

    private Map<Long, Boolean> buildHelpfulVoteMap(Long userId, List<Long> reviewIds) {
        if (userId == null || reviewIds == null || reviewIds.isEmpty()) {
            return Map.of();
        }

        return reviewHelpfulVoteRepository.findByUserIdAndReviewIdIn(userId, reviewIds)
                .stream()
                .collect(Collectors.toMap(v -> v.getReview().getId(), vote -> true, (a, b) -> a));
    }

    private Map<Long, List<String>> buildReportReasonMap(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Map.of();
        }

        return reviewReportRepository.findByReviewIdInAndResolvedFalse(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(
                        report -> report.getReview().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), reports -> reports.stream()
                                .map(ReviewReport::getReason)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(value -> !value.isEmpty())
                                .distinct()
                                .sorted(Comparator.naturalOrder())
                                .toList())
                ));
    }

    private void resolveReportsForReview(Long reviewId) {
        List<ReviewReport> activeReports = reviewReportRepository.findByReviewIdInAndResolvedFalse(List.of(reviewId));
        if (activeReports.isEmpty()) {
            return;
        }
        activeReports.forEach(report -> report.setResolved(true));
        reviewReportRepository.saveAll(activeReports);
    }

    private ReviewSortBy parseSortBy(String value) {
        if (value == null || value.isBlank()) {
            return ReviewSortBy.NEWEST;
        }
        try {
            return ReviewSortBy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ReviewSortBy.NEWEST;
        }
    }

    private Sort resolveSort(ReviewSortBy sortBy) {
        if (sortBy == ReviewSortBy.TOP_RATED) {
            return Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
        }
        if (sortBy == ReviewSortBy.MOST_HELPFUL) {
            return Sort.by(Sort.Order.desc("helpfulCount"), Sort.Order.desc("createdAt"));
        }
        return Sort.by(Sort.Order.desc("createdAt"));
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException("Target type is required");
        }
        String normalized = targetType.trim().toUpperCase(Locale.ROOT);
        try {
            return ReviewTargetType.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }

    private String normalizeTargetTypeForFilter(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return null;
        }
        if ("ALL".equalsIgnoreCase(targetType.trim())) {
            return null;
        }
        return normalizeTargetType(targetType);
    }

    private String normalizeTargetId(String targetId, String targetName) {
        if (targetId != null && !targetId.isBlank()) {
            return targetId.trim();
        }
        String slug = targetName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!slug.isBlank()) {
            return slug;
        }
        return UUID.randomUUID().toString();
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double normalizeRating(Double rating) {
        if (rating == null) {
            throw new IllegalArgumentException("Rating is required");
        }
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        double normalized = Math.round(rating);
        return normalized;
    }

    private int normalizeStarForSummary(Double rating) {
        if (rating == null) {
            return 1;
        }
        int normalized = (int) Math.round(rating);
        return Math.max(1, Math.min(normalized, 5));
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean canReply(AuthUser user, Review review) {
        if (user == null || review == null) {
            return false;
        }

        if (isPrivileged(user)) {
            return true;
        }

        if (!"EVENT".equalsIgnoreCase(review.getTargetType())) {
            return false;
        }

        Long eventId = parseLong(review.getTargetId());
        if (eventId != null) {
            return eventRepository.findById(eventId)
                    .map(event -> event.getOrganizer() != null
                            && event.getOrganizer().getId() != null
                            && event.getOrganizer().getId().equals(user.getId()))
                    .orElse(false);
        }

        if (review.getTargetName() == null || review.getTargetName().isBlank()) {
            return false;
        }

        return eventRepository.findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(review.getTargetName().trim())
                .map(event -> event.getOrganizer() != null
                        && event.getOrganizer().getId() != null
                        && event.getOrganizer().getId().equals(user.getId()))
                .orElse(false);
    }

    private void ensureAdmin(AuthUser user) {
        if (!isPrivileged(user)) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private boolean isPrivileged(AuthUser user) {
        return user != null
                && (Boolean.TRUE.equals(user.getIsSuperuser()) || Boolean.TRUE.equals(user.getIsStaff()));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
