package com.react.mobile.DTO.response;

import com.react.mobile.Entity.Enums.InterestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDataExportResponse {
    private Long userId;
    private String username;
    private String email;
    private Boolean isActive;
    private LocalDateTime dateJoined;
    private LocalDateTime lastLogin;
    
    // Profile info
    private String firstName;
    private String lastName;
    private LocalDateTime dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String address;
    private String travelStyle;
    private Set<InterestType> interests;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime profileCreatedAt;
    private LocalDateTime profileUpdatedAt;
    
    // Preferences
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private Boolean profileVisibility;
    private String language;
    private String timezone;
    private Boolean darkMode;
    private LocalDateTime preferencesCreatedAt;
    private LocalDateTime preferencesUpdatedAt;
    
    private LocalDateTime exportedAt;
}
