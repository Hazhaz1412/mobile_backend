package com.react.mobile.DTO.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPublicProfileResponse {
    private Long id;
    private String username;
    private String profilePictureUrl;
    private String bio;
    private long followerCount;
    private long followingCount;
    private boolean followedByCurrentUser;
}
