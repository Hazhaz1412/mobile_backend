package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.UpdateProfileRequest;
import com.react.mobile.DTO.request.UpdatePreferencesRequest;
import com.react.mobile.DTO.response.UserProfileResponse;
import com.react.mobile.DTO.response.UserPreferencesResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Entity.UserPreferences;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Repository.UserPreferencesRepository;
import com.react.mobile.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    @Override
    @Transactional
    public UserProfileResponse updateProfile(AuthUser authUser, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByAuthUser(authUser)
                .orElseGet(() -> UserProfile.builder()
                        .authUser(authUser)
                        .build());

        // Update only non-null fields
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getTravelStyle() != null) {
            profile.setTravelStyle(request.getTravelStyle());
        }
        if (request.getInterests() != null) {
            profile.setInterests(request.getInterests());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getProfilePictureUrl() != null) {
            profile.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToProfileResponse(savedProfile);
    }

    @Override
    @Transactional
    public UserPreferencesResponse updatePreferences(AuthUser authUser, UpdatePreferencesRequest request) {
        UserPreferences preferences = userPreferencesRepository.findByAuthUser(authUser)
                .orElseGet(() -> UserPreferences.builder()
                        .authUser(authUser)
                        .build());

        // Update only non-null fields
        if (request.getEmailNotifications() != null) {
            preferences.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getPushNotifications() != null) {
            preferences.setPushNotifications(request.getPushNotifications());
        }
        if (request.getSmsNotifications() != null) {
            preferences.setSmsNotifications(request.getSmsNotifications());
        }
        if (request.getProfileVisibility() != null) {
            preferences.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getLanguage() != null) {
            preferences.setLanguage(request.getLanguage());
        }
        if (request.getTimezone() != null) {
            preferences.setTimezone(request.getTimezone());
        }
        if (request.getDarkMode() != null) {
            preferences.setDarkMode(request.getDarkMode());
        }

        UserPreferences savedPreferences = userPreferencesRepository.save(preferences);
        return mapToPreferencesResponse(savedPreferences);
    }

    @Override
    @Transactional
    public UserProfileResponse getProfile(AuthUser authUser) {
        UserProfile profile = userProfileRepository.findByAuthUser(authUser)
            .orElseGet(() -> userProfileRepository.save(
                UserProfile.builder()
                    .authUser(authUser)
                    .build()
            ));
        return mapToProfileResponse(profile);
    }

    @Override
    @Transactional
    public UserPreferencesResponse getPreferences(AuthUser authUser) {
        UserPreferences preferences = userPreferencesRepository.findByAuthUser(authUser)
            .orElseGet(() -> userPreferencesRepository.save(
                UserPreferences.builder()
                    .authUser(authUser)
                    .build()
            ));
        return mapToPreferencesResponse(preferences);
    }

    private UserProfileResponse mapToProfileResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .phoneNumber(profile.getPhoneNumber())
                .address(profile.getAddress())
                .travelStyle(profile.getTravelStyle())
                .interests(profile.getInterests())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl()) 
                .build();
    }

    private UserPreferencesResponse mapToPreferencesResponse(UserPreferences preferences) {
        return UserPreferencesResponse.builder()
                .id(preferences.getId())
                .emailNotifications(preferences.getEmailNotifications())
                .pushNotifications(preferences.getPushNotifications())
                .smsNotifications(preferences.getSmsNotifications())
                .profileVisibility(preferences.getProfileVisibility())
                .language(preferences.getLanguage())
                .timezone(preferences.getTimezone())
                .darkMode(preferences.getDarkMode()) 
                .build();
    }
}
 