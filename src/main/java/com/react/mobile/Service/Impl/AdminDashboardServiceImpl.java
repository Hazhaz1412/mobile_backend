package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.response.AdminAnalyticsResponse;
import com.react.mobile.DTO.response.AdminReviewListResponse;
import com.react.mobile.DTO.response.AdminReviewResponse;
import com.react.mobile.DTO.response.AdminUserListResponse;
import com.react.mobile.DTO.response.AdminUserProfileResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.EventModerationStatus;
import com.react.mobile.Entity.Enums.ReviewModerationStatus;
import com.react.mobile.Entity.LoginHistory;
import com.react.mobile.Entity.Review;
import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.DiscoveryBookmarkRepository;
import com.react.mobile.Repository.EventBookmarkRepository;
import com.react.mobile.Repository.EventRepository;
import com.react.mobile.Repository.LocationHistoryRepository;
import com.react.mobile.Repository.LoginHistoryRepository;
import com.react.mobile.Repository.ReviewRepository;
import com.react.mobile.Repository.UserReportRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AuthUserRepository authUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final DiscoveryBookmarkRepository discoveryBookmarkRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final UserReportRepository userReportRepository;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    @Transactional(readOnly = true)
    public AdminUserListResponse listUsers(AuthUser user, String search, int page, int size) {
        ensureAdmin(user);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String searchParam = search == null ? "" : search.trim();

        Page<AuthUser> pageResult = authUserRepository.searchUsers(
                searchParam,
                PageRequest.of(safePage, safeSize)
        );

        List<Long> userIds = pageResult.getContent().stream()
                .map(AuthUser::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, UserProfile> profileByUserId = userProfileRepository.findByAuthUserIdIn(userIds).stream()
                .filter(profile -> profile.getAuthUser() != null && profile.getAuthUser().getId() != null)
                .collect(Collectors.toMap(
                        profile -> profile.getAuthUser().getId(),
                        profile -> profile,
                        (left, right) -> left
                ));

        List<AdminUserProfileResponse> users = pageResult.getContent().stream()
                .map(item -> {
                    UserProfile profile = profileByUserId.get(item.getId());
                long reportCount = userReportRepository.countByReportedUserIdAndResolvedFalse(item.getId());
                    return AdminUserProfileResponse.builder()
                            .id(item.getId())
                            .username(item.getUsername())
                            .email(item.getEmail())
                            .isActive(item.getIsActive())
                            .isSuperuser(item.getIsSuperuser())
                            .isStaff(item.getIsStaff())
                    .reportCount(reportCount)
                            .dateJoined(formatDateTime(item.getDateJoined()))
                            .lastLogin(formatDateTime(item.getLastLogin()))
                            .firstName(profile != null ? profile.getFirstName() : null)
                            .lastName(profile != null ? profile.getLastName() : null)
                            .age(profile != null ? profile.getAge() : null)
                            .gender(profile != null ? profile.getGender() : null)
                            .travelStyle(profile != null ? profile.getTravelStyle() : null)
                            .bio(profile != null ? profile.getBio() : null)
                            .profilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                            .build();
                })
                .toList();

        return AdminUserListResponse.builder()
                .users(users)
                .total(pageResult.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    @Override
    @Transactional
    public AdminUserListResponse banUser(AuthUser user, Long userId) {
        return setUserActiveState(user, userId, false);
    }

    @Override
    @Transactional
    public AdminUserListResponse unbanUser(AuthUser user, Long userId) {
        return setUserActiveState(user, userId, true);
    }

    private AdminUserListResponse setUserActiveState(AuthUser user, Long userId, boolean active) {
        ensureAdmin(user);

        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }

        if (Objects.equals(user.getId(), userId)) {
            throw new IllegalArgumentException("You cannot change your own account status");
        }

        AuthUser target = authUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        target.setIsActive(active);
        authUserRepository.save(target);

        return listUsers(user, null, 0, 10);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReviewListResponse listReviews(AuthUser user, String moderationStatus, String search, int page, int size) {
        ensureAdmin(user);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String searchParam = search == null ? "" : search.trim();

        ReviewModerationStatus statusFilter = parseReviewModerationStatus(moderationStatus);

        Page<Review> pageResult = reviewRepository.findFiltered(
                null,
                null,
                searchParam,
                statusFilter,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("updatedAt")))
        );

        List<AdminReviewResponse> reviews = pageResult.getContent().stream()
                .map(item -> AdminReviewResponse.builder()
                        .id(item.getId())
                        .targetType(item.getTargetType())
                        .targetId(item.getTargetId())
                        .targetName(item.getTargetName())
                        .rating(item.getRating())
                        .comment(item.getComment())
                        .authorUsername(item.getUser() != null ? item.getUser().getUsername() : null)
                        .authorId(item.getUser() != null ? item.getUser().getId() : null)
                        .helpfulCount(defaultLong(item.getHelpfulCount()))
                        .flagCount(defaultLong(item.getFlagCount()))
                        .moderationStatus(item.getModerationStatus() != null ? item.getModerationStatus().name() : null)
                        .createdAt(formatDateTime(item.getCreatedAt()))
                        .updatedAt(formatDateTime(item.getUpdatedAt()))
                        .build())
                .toList();

        return AdminReviewListResponse.builder()
                .reviews(reviews)
                .total(pageResult.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics(AuthUser user, Integer windowDays) {
        ensureAdmin(user);

        int safeWindowDays = windowDays == null ? 14 : Math.min(Math.max(windowDays, 1), 90);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusDays(safeWindowDays);

        List<LoginHistory> loginHistoryInWindow = loginHistoryRepository.findByLoginTimeAfterOrderByLoginTimeAsc(windowStart);

        Map<Long, Long> successfulLoginsByUser = loginHistoryInWindow.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getSuccess()))
                .filter(entry -> entry.getUserId() != null)
                .collect(Collectors.groupingBy(LoginHistory::getUserId, Collectors.counting()));

        long activeUsersInWindow = successfulLoginsByUser.keySet().size();

        List<AdminAnalyticsResponse.UserActivityItem> userActivity = successfulLoginsByUser.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Long userId = entry.getKey();
                    AuthUser account = authUserRepository.findById(userId).orElse(null);
                    long reviewsCreated = reviewRepository.countByUserIdAndCreatedAtAfter(userId, windowStart);
                    long eventsCreated = eventRepository.countByOrganizerIdAndCreatedAtAfter(userId, windowStart);

                    return AdminAnalyticsResponse.UserActivityItem.builder()
                            .userId(userId)
                            .username(account != null ? account.getUsername() : null)
                            .email(account != null ? account.getEmail() : null)
                            .successfulLogins(entry.getValue())
                            .reviewsCreated(reviewsCreated)
                            .eventsCreated(eventsCreated)
                            .build();
                })
                .toList();

        Map<String, Long> topPlacesScore = new HashMap<>();
        mergeTopEntries(topPlacesScore, reviewRepository.findTopTargetNames(PageRequest.of(0, 10)));
        mergeTopEntries(topPlacesScore, eventRepository.findTopLocationNames(EventModerationStatus.APPROVED, PageRequest.of(0, 10)));
        mergeTopEntries(topPlacesScore, locationHistoryRepository.findTopLocations(PageRequest.of(0, 10)));

        // Discovery bookmark only has placeId. Keep it visible but tagged so admin knows it is an ID.
        for (Object[] row : discoveryBookmarkRepository.findTopPlaceIds(PageRequest.of(0, 10))) {
            String placeId = asString(row[0]);
            long score = asLong(row[1]);
            if (placeId == null || placeId.isBlank()) continue;
            topPlacesScore.merge("ID: " + placeId, score, Long::sum);
        }

        List<AdminAnalyticsResponse.TopPlaceItem> topPlaces = topPlacesScore.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(item -> AdminAnalyticsResponse.TopPlaceItem.builder()
                        .place(item.getKey())
                        .score(item.getValue())
                        .build())
                .toList();

        Map<LocalDate, Long> loginByDate = loginHistoryInWindow.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getSuccess()))
                .filter(entry -> entry.getLoginTime() != null)
                .collect(Collectors.groupingBy(entry -> entry.getLoginTime().toLocalDate(), Collectors.counting()));

        Map<LocalDate, Long> eventsByDate = eventRepository.findByCreatedAtAfterOrderByCreatedAtAsc(windowStart).stream()
                .filter(event -> event.getCreatedAt() != null)
                .collect(Collectors.groupingBy(event -> event.getCreatedAt().toLocalDate(), Collectors.counting()));

        Map<LocalDate, Long> reviewsByDate = reviewRepository.findByCreatedAtAfterOrderByCreatedAtAsc(windowStart).stream()
                .filter(review -> review.getCreatedAt() != null)
                .collect(Collectors.groupingBy(review -> review.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<AdminAnalyticsResponse.TrafficStatItem> trafficStats = new ArrayList<>();
        LocalDate day = now.toLocalDate().minusDays(safeWindowDays - 1L);
        LocalDate today = now.toLocalDate();
        while (!day.isAfter(today)) {
            trafficStats.add(AdminAnalyticsResponse.TrafficStatItem.builder()
                    .date(day.format(DATE_FMT))
                    .successfulLogins(loginByDate.getOrDefault(day, 0L))
                    .eventsCreated(eventsByDate.getOrDefault(day, 0L))
                    .reviewsCreated(reviewsByDate.getOrDefault(day, 0L))
                    .build());
            day = day.plusDays(1);
        }

        long pendingEvents = eventRepository.countByModerationStatus(EventModerationStatus.PENDING);
        long approvedEvents = eventRepository.countByModerationStatus(EventModerationStatus.APPROVED)
                + eventRepository.countByModerationStatusIsNull();
        long rejectedEvents = eventRepository.countByModerationStatus(EventModerationStatus.REJECTED);

        return AdminAnalyticsResponse.builder()
                .totalUsers(authUserRepository.count())
                .activeUsersInWindow(activeUsersInWindow)
                .totalEvents(eventRepository.count())
                .pendingEvents(pendingEvents)
                .approvedEvents(approvedEvents)
                .rejectedEvents(rejectedEvents)
                .totalReviews(reviewRepository.count())
                .flaggedReviews(reviewRepository.countByModerationStatus(ReviewModerationStatus.FLAGGED))
                .totalDiscoveryBookmarks(discoveryBookmarkRepository.count())
                .totalEventBookmarks(eventBookmarkRepository.count())
                .totalLogins(loginHistoryRepository.count())
                .successfulLoginsInWindow(defaultLong(loginHistoryRepository.countBySuccessTrueAndLoginTimeAfter(windowStart)))
                .windowDays(safeWindowDays)
                .userActivity(userActivity)
                .topPlaces(topPlaces)
                .trafficStats(trafficStats)
                .build();
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

    private ReviewModerationStatus parseReviewModerationStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return ReviewModerationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported moderation status: " + value);
        }
    }

    private void mergeTopEntries(Map<String, Long> target, List<Object[]> rows) {
        for (Object[] row : rows) {
            String key = asString(row[0]);
            long score = asLong(row[1]);
            if (key == null || key.isBlank()) continue;
            target.merge(key, score, Long::sum);
        }
    }

    private String asString(Object raw) {
        return raw == null ? null : String.valueOf(raw).trim();
    }

    private long asLong(Object raw) {
        if (raw == null) return 0L;
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FMT);
    }
}
