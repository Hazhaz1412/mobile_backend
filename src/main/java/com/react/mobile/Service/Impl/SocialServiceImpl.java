package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.response.ActivityFeedResponse;
import com.react.mobile.DTO.response.UserPublicProfileResponse;
import com.react.mobile.Entity.ActivityFeedItem;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.UserFollow;
import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Repository.ActivityFeedItemRepository;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.UserFollowRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final UserFollowRepository userFollowRepository;
    private final ActivityFeedItemRepository activityFeedItemRepository;
    private final AuthUserRepository authUserRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public void follow(AuthUser currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        AuthUser target = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (userFollowRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)) {
            return;
        }

        userFollowRepository.save(UserFollow.builder()
                .follower(currentUser)
                .following(target)
                .build());

        recordActivity(currentUser, "FOLLOW", "USER", String.valueOf(targetUserId), target.getUsername(), null);
    }

    @Override
    @Transactional
    public void unfollow(AuthUser currentUser, Long targetUserId) {
        userFollowRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicProfileResponse> getFollowers(AuthUser currentUser, int page, int size) {
        Page<UserFollow> follows = userFollowRepository.findFollowersByUserId(
                currentUser.getId(), PageRequest.of(page, Math.min(size, 50)));
        return follows.getContent().stream()
                .map(uf -> toPublicProfile(currentUser, uf.getFollower()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicProfileResponse> getFollowing(AuthUser currentUser, int page, int size) {
        Page<UserFollow> follows = userFollowRepository.findFollowingByUserId(
                currentUser.getId(), PageRequest.of(page, Math.min(size, 50)));
        return follows.getContent().stream()
                .map(uf -> toPublicProfile(currentUser, uf.getFollowing()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicProfileResponse> searchUsers(AuthUser currentUser, String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<AuthUser> users = authUserRepository.findByUsernameContainingIgnoreCase(query, PageRequest.of(0, Math.min(limit, 20)));
        return users.stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(u -> toPublicProfile(currentUser, u))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getUserProfile(AuthUser currentUser, Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toPublicProfile(currentUser, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityFeedResponse> getFeed(AuthUser currentUser, int page, int size) {
        List<Long> followingIds = userFollowRepository.findFollowingIdsByUserId(currentUser.getId());
        // Include the user's own activity + followed users' activity
        List<Long> actorIds = new java.util.ArrayList<>(followingIds);
        if (!actorIds.contains(currentUser.getId())) {
            actorIds.add(currentUser.getId());
        }
        Page<ActivityFeedItem> items = activityFeedItemRepository.findByActorIdIn(
                actorIds, PageRequest.of(page, Math.min(size, 50)));
        return items.getContent().stream()
                .map(this::toFeedResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordActivity(AuthUser actor, String actionType, String targetType,
                               String targetId, String targetName, String metadata) {
        activityFeedItemRepository.save(ActivityFeedItem.builder()
                .actor(actor)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .metadata(metadata)
                .build());
    }

    private UserPublicProfileResponse toPublicProfile(AuthUser viewer, AuthUser user) {
        UserProfile profile = userProfileRepository.findByAuthUserId(user.getId()).orElse(null);
        boolean followed = !viewer.getId().equals(user.getId()) &&
                userFollowRepository.existsByFollowerIdAndFollowingId(viewer.getId(), user.getId());

        return UserPublicProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .bio(profile != null ? profile.getBio() : null)
                .followerCount(userFollowRepository.countByFollowingId(user.getId()))
                .followingCount(userFollowRepository.countByFollowerId(user.getId()))
                .followedByCurrentUser(followed)
                .build();
    }

    private ActivityFeedResponse toFeedResponse(ActivityFeedItem item) {
        AuthUser actor = item.getActor();
        UserProfile profile = userProfileRepository.findByAuthUserId(actor.getId()).orElse(null);

        return ActivityFeedResponse.builder()
                .id(item.getId())
                .actorId(actor.getId())
                .actorUsername(actor.getUsername())
                .actorProfilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .actionType(item.getActionType())
                .targetType(item.getTargetType())
                .targetId(item.getTargetId())
                .targetName(item.getTargetName())
                .metadata(item.getMetadata())
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null)
                .build();
    }
}
