package com.react.mobile.Service;

import com.react.mobile.DTO.response.AdminAnalyticsResponse;
import com.react.mobile.DTO.response.AdminReviewListResponse;
import com.react.mobile.DTO.response.AdminUserListResponse;
import com.react.mobile.Entity.AuthUser;

public interface AdminDashboardService {

    AdminUserListResponse listUsers(AuthUser user, String search, int page, int size);

    AdminUserListResponse banUser(AuthUser user, Long userId);

    AdminUserListResponse unbanUser(AuthUser user, Long userId);

    AdminReviewListResponse listReviews(AuthUser user, String moderationStatus, String search, int page, int size);

    AdminAnalyticsResponse getAnalytics(AuthUser user, Integer windowDays);
}
