package com.react.mobile.Service;

import com.react.mobile.DTO.request.UpdateProfileRequest;
import com.react.mobile.DTO.request.UpdatePreferencesRequest;
import com.react.mobile.DTO.response.UserProfileResponse;
import com.react.mobile.DTO.response.UserPreferencesResponse;
import com.react.mobile.Entity.AuthUser;

public interface UserService {
    UserProfileResponse updateProfile(AuthUser authUser, UpdateProfileRequest request);
    
    UserPreferencesResponse updatePreferences(AuthUser authUser, UpdatePreferencesRequest request);
    
    UserProfileResponse getProfile(AuthUser authUser);
    
    UserPreferencesResponse getPreferences(AuthUser authUser);
}
