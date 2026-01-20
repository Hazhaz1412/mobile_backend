package com.react.mobile.Controller;

import com.react.mobile.DTO.request.UpdateProfileRequest;
import com.react.mobile.DTO.request.UpdatePreferencesRequest;
import com.react.mobile.DTO.response.UserProfileResponse;
import com.react.mobile.DTO.response.UserPreferencesResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthUserRepository authUserRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserProfileResponse profile = userService.getProfile(authUser);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserProfileResponse updatedProfile = userService.updateProfile(authUser, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> getPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserPreferencesResponse preferences = userService.getPreferences(authUser);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdatePreferencesRequest request) {
        
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserPreferencesResponse updatedPreferences = userService.updatePreferences(authUser, request);
        return ResponseEntity.ok(updatedPreferences);
    }
}
