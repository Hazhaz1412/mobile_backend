package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.response.GDPRDataResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.LoginHistory;
import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Entity.UserPreferences;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.LoginHistoryRepository;
import com.react.mobile.Repository.RefreshTokenRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Repository.UserPreferencesRepository;
import com.react.mobile.Repository.VerificationTokenRepository;
import com.react.mobile.Repository.SocialAuthUserRepository;
import com.react.mobile.Service.GDPRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GDPRServiceImpl implements GDPRService {

    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final SocialAuthUserRepository socialAuthUserRepository;

    @Override
    @Transactional(readOnly = true)
    public GDPRDataResponse exportAllData(AuthUser authUser) {
        log.info("Exporting GDPR data for user ID: {}", authUser.getId());
        
        // 1. Lấy thông tin profile
        UserProfile profile = userProfileRepository.findByAuthUser(authUser).orElse(null);
        
        // 2. Lấy preferences
        UserPreferences preferences = userPreferencesRepository.findByAuthUser(authUser).orElse(null);
        
        // 3. Lấy lịch sử đăng nhập (50 lần gần nhất)
        List<LoginHistory> loginLogs = loginHistoryRepository.findTop50ByUserIdOrderByLoginTimeDesc(authUser.getId());
        Long totalLogins = loginHistoryRepository.countByUserId(authUser.getId());
        
        // 4. Tìm lần đăng nhập cuối
        LocalDateTime lastLogin = loginLogs.isEmpty() ? null : loginLogs.get(0).getLoginTime();

        // 5. Tổng hợp dữ liệu
        return GDPRDataResponse.builder()
                .userId(authUser.getId())
                .exportDate(LocalDateTime.now())
                .personalInfo(mapToPersonalInfo(authUser, profile))
                .preferences(mapToPreferencesInfo(preferences))
                .activityLogs(
                    GDPRDataResponse.UserActivityLog.builder()
                        .loginHistory(mapToLoginHistoryInfo(loginLogs))
                        .totalLogins(totalLogins.intValue())
                        .lastLoginAt(lastLogin)
                        .build()
                )
                .build();
    }

    @Override
    @Transactional
    public void deleteAccount(AuthUser authUser, String deleteType) {
        log.info("Deleting account for user ID: {} with type: {}", authUser.getId(), deleteType);
        
        if ("soft".equalsIgnoreCase(deleteType)) {
            // Soft delete: Vô hiệu hóa tài khoản
            authUser.setIsActive(false);
            authUser.setEmail("deleted_" + authUser.getId() + "@deleted.com");
            authUserRepository.save(authUser);
            log.info("Soft deleted user ID: {}", authUser.getId());
            
        } else if ("hard".equalsIgnoreCase(deleteType)) {
            // Hard delete: Xóa hoàn toàn
            Long userId = authUser.getId();
            
            // Xóa theo thứ tự: dependencies trước, main entity sau
            try {
                // 1. Xóa refresh tokens
                refreshTokenRepository.deleteByUser(authUser);
                log.debug("Deleted refresh tokens for user ID: {}", userId);
                
                // 2. Xóa verification tokens (nếu có)
                var verificationToken = verificationTokenRepository.findAll().stream()
                    .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                    .findFirst();
                verificationToken.ifPresent(verificationTokenRepository::delete);
                log.debug("Deleted verification tokens for user ID: {}", userId);
                
                // 3. Xóa social auth users (nếu có)
                var socialAuthUsers = socialAuthUserRepository.findAll().stream()
                    .filter(s -> s.getAuthUser() != null && s.getAuthUser().getId().equals(userId))
                    .toList();
                socialAuthUserRepository.deleteAll(socialAuthUsers);
                log.debug("Deleted social auth for user ID: {}", userId);
                
                // 4. Xóa user preferences
                userPreferencesRepository.findByAuthUser(authUser)
                    .ifPresent(userPreferencesRepository::delete);
                log.debug("Deleted preferences for user ID: {}", userId);
                
                // 5. Xóa user profile
                userProfileRepository.findByAuthUser(authUser)
                    .ifPresent(userProfileRepository::delete);
                log.debug("Deleted profile for user ID: {}", userId);
                
                // 6. Xóa login history
                loginHistoryRepository.deleteByUserId(userId);
                log.debug("Deleted login history for user ID: {}", userId);
                
                // 7. Cuối cùng xóa auth user
                authUserRepository.delete(authUser);
                log.info("Hard deleted user ID: {}", userId);
                
            } catch (Exception e) {
                log.error("Error during hard delete for user ID: {}", userId, e);
                throw new RuntimeException("Failed to delete account: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Invalid delete type. Use 'soft' or 'hard'");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GDPRDataResponse.UserActivityLog getActivityLogs(AuthUser authUser, Integer limit) {
        log.info("Getting activity logs for user ID: {} with limit: {}", authUser.getId(), limit);
        
        List<LoginHistory> loginLogs = (limit != null && limit > 0) 
            ? loginHistoryRepository.findTop50ByUserIdOrderByLoginTimeDesc(authUser.getId())
            : loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(authUser.getId());
        
        Long totalLogins = loginHistoryRepository.countByUserId(authUser.getId());
        LocalDateTime lastLogin = loginLogs.isEmpty() ? null : loginLogs.get(0).getLoginTime();
        
        return GDPRDataResponse.UserActivityLog.builder()
                .loginHistory(mapToLoginHistoryInfo(loginLogs))
                .totalLogins(totalLogins.intValue())
                .lastLoginAt(lastLogin)
                .build();
    }

    // Helper methods
    private GDPRDataResponse.PersonalInfo mapToPersonalInfo(AuthUser authUser, UserProfile profile) {
        if (profile == null) {
            return GDPRDataResponse.PersonalInfo.builder()
                    .email(authUser.getEmail())
                    .createdAt(authUser.getDateJoined())
                    .build();
        }
        
        String fullName = null;
        if (profile.getFirstName() != null || profile.getLastName() != null) {
            fullName = (profile.getFirstName() != null ? profile.getFirstName() : "") + 
                      " " + 
                      (profile.getLastName() != null ? profile.getLastName() : "");
            fullName = fullName.trim();
        }
        
        return GDPRDataResponse.PersonalInfo.builder()
                .email(authUser.getEmail())
                .fullName(fullName)
                .phoneNumber(profile.getPhoneNumber())
                .bio(profile.getBio())
                .avatarUrl(profile.getProfilePictureUrl())
                .birthDate(profile.getDateOfBirth())
                .gender(profile.getGender())
                .location(profile.getAddress())
                .createdAt(authUser.getDateJoined())
                .updatedAt(null)
                .build();
    }

    private GDPRDataResponse.PreferencesInfo mapToPreferencesInfo(UserPreferences preferences) {
        if (preferences == null) {
            return null;
        }
        
        return GDPRDataResponse.PreferencesInfo.builder()
                .language(preferences.getLanguage())
                .theme(preferences.getDarkMode() ? "dark" : "light")
                .emailNotifications(preferences.getEmailNotifications())
                .pushNotifications(preferences.getPushNotifications())
                .smsNotifications(preferences.getSmsNotifications())
                .marketingEmails(false) // Không có field này trong entity
                .privacyLevel(preferences.getProfileVisibility() ? "public" : "private")
                .twoFactorEnabled(false) // Không có field này trong entity
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }

    private List<GDPRDataResponse.LoginHistoryInfo> mapToLoginHistoryInfo(List<LoginHistory> loginHistories) {
        return loginHistories.stream()
                .map(lh -> GDPRDataResponse.LoginHistoryInfo.builder()
                        .loginTime(lh.getLoginTime())
                        .ipAddress(lh.getIpAddress())
                        .userAgent(lh.getUserAgent())
                        .deviceType(lh.getDeviceType())
                        .location(lh.getLocation())
                        .success(lh.getSuccess())
                        .build())
                .collect(Collectors.toList());
    }
}