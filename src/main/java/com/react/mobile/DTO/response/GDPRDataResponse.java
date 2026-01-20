package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GDPRDataResponse {
    
    private Long userId;
    private LocalDateTime exportDate;
    private PersonalInfo personalInfo;
    private PreferencesInfo preferences;
    private UserActivityLog activityLogs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String email;
        private String fullName;
        private String phoneNumber;
        private String bio;
        private String avatarUrl;
        private LocalDateTime birthDate;
        private String gender;
        private String location;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferencesInfo {
        private String language;
        private String theme;
        private Boolean emailNotifications;
        private Boolean pushNotifications;
        private Boolean smsNotifications;
        private Boolean marketingEmails;
        private String privacyLevel;
        private Boolean twoFactorEnabled;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityLog {
        private List<LoginHistoryInfo> loginHistory;
        private Integer totalLogins;
        private LocalDateTime lastLoginAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginHistoryInfo {
        private LocalDateTime loginTime;
        private String ipAddress;
        private String userAgent;
        private String deviceType;
        private String location;
        private Boolean success;
    }
}
