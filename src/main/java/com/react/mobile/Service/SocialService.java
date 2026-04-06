package com.react.mobile.Service;

import com.react.mobile.DTO.response.ActivityFeedResponse;
import com.react.mobile.DTO.response.UserPublicProfileResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface SocialService {
    void follow(AuthUser currentUser, Long targetUserId);
    void unfollow(AuthUser currentUser, Long targetUserId);
    List<UserPublicProfileResponse> getFollowers(AuthUser currentUser, int page, int size);
    List<UserPublicProfileResponse> getFollowing(AuthUser currentUser, int page, int size);
    List<UserPublicProfileResponse> searchUsers(AuthUser currentUser, String query, int limit);
    UserPublicProfileResponse getUserProfile(AuthUser currentUser, Long userId);
    List<ActivityFeedResponse> getFeed(AuthUser currentUser, int page, int size);
    void recordActivity(AuthUser actor, String actionType, String targetType, String targetId, String targetName, String metadata);
}
