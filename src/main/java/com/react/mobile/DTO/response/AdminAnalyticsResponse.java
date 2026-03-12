package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsResponse {
    private Long totalUsers;
    private Long activeUsersInWindow;
    private Long totalEvents;
    private Long pendingEvents;
    private Long approvedEvents;
    private Long rejectedEvents;
    private Long totalReviews;
    private Long flaggedReviews;
    private Long totalDiscoveryBookmarks;
    private Long totalEventBookmarks;
    private Long totalLogins;
    private Long successfulLoginsInWindow;
    private Integer windowDays;

    private List<UserActivityItem> userActivity;
    private List<TopPlaceItem> topPlaces;
    private List<TrafficStatItem> trafficStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserActivityItem {
        private Long userId;
        private String username;
        private String email;
        private Long successfulLogins;
        private Long reviewsCreated;
        private Long eventsCreated;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopPlaceItem {
        private String place;
        private Long score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrafficStatItem {
        private String date;
        private Long successfulLogins;
        private Long eventsCreated;
        private Long reviewsCreated;
    }
}
