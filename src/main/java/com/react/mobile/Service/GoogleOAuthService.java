package com.react.mobile.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.react.mobile.DTO.request.GoogleOAuthRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.RefreshToken;
import com.react.mobile.Entity.SocialAuthUser;
import com.react.mobile.Mapper.UserMapper;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.RefreshTokenRepository;
import com.react.mobile.Repository.SocialAuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    private final AuthUserRepository authUserRepository;
    private final SocialAuthUserRepository socialAuthUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;
    private final PasswordEncoder passwordEncoder;

    private static final String GOOGLE_PROVIDER = "google";

    /**
     * Xác thực Google OAuth token từ React Native client
     */
    @Transactional
    public AuthenticationResponse verifyGoogleToken(GoogleOAuthRequest request) {
        try {
            GoogleIdToken idToken = verifyIdToken(request.getIdToken());

            if (idToken == null) {
                throw new IllegalArgumentException("Google token không hợp lệ hoặc đã hết hạn");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleUserId = payload.getSubject();
            String rawEmail = payload.getEmail();
            String name = (String) payload.get("name");
            Object emailVerified = payload.getEmailVerified();

            if (rawEmail == null || rawEmail.isBlank()) {
                throw new IllegalArgumentException("Google account chưa cung cấp email hợp lệ");
            }
            if (Boolean.FALSE.equals(emailVerified)) {
                throw new IllegalArgumentException("Email Google chưa được xác minh");
            }
            String email = rawEmail.toLowerCase(Locale.ROOT);

            log.info("Google OAuth: User={}, Email={}", googleUserId, email);

            AuthUser user = findOrCreateUser(email, name);

            linkOrUpdateSocialAuth(user, googleUserId, payload);

            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            saveRefreshToken(user, refreshToken);

            user.setEmail(email);
            user.setIsActive(true);
            user.setLastLogin(LocalDateTime.now());
            authUserRepository.save(user);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(userMapper.toResponse(user))
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            log.error("Lỗi xác thực Google token: {}", e.getMessage());
            throw new IllegalStateException("Không thể xác thực Google token", e);
        }
    }

    /**
     * Xác thực Google ID token sử dụng Google API
     */
    private GoogleIdToken verifyIdToken(String idTokenString) throws GeneralSecurityException, IOException {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_OAUTH_CLIENT_ID chưa được cấu hình");
        }
        return googleIdTokenVerifierService.verify(idTokenString, googleClientId);
    }

    /**
     * Tìm hoặc tạo user mới từ Google account
     */
    private AuthUser findOrCreateUser(String email, String name) {
        var existingUser = authUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            AuthUser user = existingUser.get();
            user.setIsActive(true);
            return user;
        }

        String username = generateUniqueUsername(email, name);

        AuthUser newUser = AuthUser.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .uuid(UUID.randomUUID())
                .isActive(true)
                .isSuperuser(false)
                .isStaff(false)
                .dateJoined(LocalDateTime.now())
                .build();

        return authUserRepository.save(newUser);
    }

    /**
     * Liên kết hoặc cập nhật social auth record
     */
    private void linkOrUpdateSocialAuth(AuthUser user, String googleUserId, GoogleIdToken.Payload payload) {
        var socialAuthByGoogleUserId = socialAuthUserRepository
                .findByProviderAndProviderUserId(GOOGLE_PROVIDER, googleUserId);

        if (socialAuthByGoogleUserId.isPresent()) {
            SocialAuthUser existing = socialAuthByGoogleUserId.get();
            if (!existing.getAuthUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Google account này đã liên kết với tài khoản khác");
            }

            existing.setExtraData(payload.toString());
            existing.setUpdatedAt(LocalDateTime.now());
            socialAuthUserRepository.save(existing);
            return;
        }

        var socialAuthByUser = socialAuthUserRepository.findByProviderAndAuthUser(GOOGLE_PROVIDER, user);
        if (socialAuthByUser.isPresent()) {
            SocialAuthUser existing = socialAuthByUser.get();
            existing.setProviderUserId(googleUserId);
            existing.setExtraData(payload.toString());
            existing.setUpdatedAt(LocalDateTime.now());
            socialAuthUserRepository.save(existing);
            return;
        }

        SocialAuthUser newSocialAuth = SocialAuthUser.builder()
                .provider(GOOGLE_PROVIDER)
                .providerUserId(googleUserId)
                .authUser(user)
                .extraData(payload.toString())
                .createdAt(LocalDateTime.now())
                .build();
        socialAuthUserRepository.save(newSocialAuth);
    }

    /**
     * Lưu refresh token vào DB
     */
    @Transactional
    private void saveRefreshToken(AuthUser user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Tạo username duy nhất từ email
     */
    private String generateUniqueUsername(String email, String displayName) {
        String baseUsername = (displayName != null && !displayName.isBlank())
                ? displayName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "")
                : email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");

        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        String username = baseUsername;
        int counter = 1;

        while (authUserRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    /**
     * Disconnect Google account (revoke social auth link)
     */
    @Transactional
    public void disconnectGoogle(AuthUser user) {
        socialAuthUserRepository.deleteByProviderAndAuthUser(GOOGLE_PROVIDER, user);
        log.info("Disconnected Google account for user: {}", user.getUsername());
    }

    /**
     * Kiểm tra xem user có liên kết Google không
     */
    public boolean isGoogleLinked(AuthUser user) {
        return socialAuthUserRepository.existsByProviderAndAuthUser(GOOGLE_PROVIDER, user);
    }
}
