package com.react.mobile.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
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

    private static final String GOOGLE_PROVIDER = "google";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Xác thực Google OAuth token từ React Native client
     */
    @Transactional
    public AuthenticationResponse verifyGoogleToken(GoogleOAuthRequest request) {
        try {
            // 1. Xác thực token từ Google
            GoogleIdToken idToken = verifyIdToken(request.getIdToken());
            
            if (idToken == null) {
                throw new RuntimeException("Google token xác thực thất bại");
            }

            // 2. Lấy thông tin user từ token
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleUserId = payload.getSubject();
            String email = payload.getEmail().toLowerCase(Locale.ROOT);
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            log.info("Google OAuth: User={}, Email={}", googleUserId, email);

            // 3. Kiểm tra xem user đã tồn tại chưa
            AuthUser user = findOrCreateUser(googleUserId, email, name, picture);

            // 4. Kiểm tra liên kết social auth
            linkOrUpdateSocialAuth(user, googleUserId, payload);

            // 5. Sinh JWT token
            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // 6. Lưu refresh token
            saveRefreshToken(user, refreshToken);

            // 7. Cập nhật last login
            user.setLastLogin(LocalDateTime.now());
            authUserRepository.save(user);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(userMapper.toResponse(user))
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            log.error("Lỗi xác thực Google token: {}", e.getMessage());
            throw new RuntimeException("Không thể xác thực Google token: " + e.getMessage());
        }
    }

    /**
     * Xác thực Google ID token sử dụng Google API
     */
    private GoogleIdToken verifyIdToken(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JSON_FACTORY)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            return verifier.verify(idTokenString);
        } catch (IllegalArgumentException e) {
            log.error("Invalid ID token format");
            return null;
        }
    }

    /**
     * Tìm hoặc tạo user mới từ Google account
     */
    private AuthUser findOrCreateUser(String googleUserId, String email, String name, String picture) {
        // 1. Kiểm tra xem user đã tồn tại dùng email
        var existingUser = authUserRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // 2. Nếu chưa có, tạo user mới từ Google
        String username = generateUniqueUsername(email);
        
        AuthUser newUser = AuthUser.builder()
                .username(username)
                .email(email)
                .password("") // Không có password vì dùng OAuth
                .uuid(UUID.randomUUID())
                .isActive(true) // OAuth users được active ngay
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
        var socialAuth = socialAuthUserRepository.findByProviderAndProviderUserId(GOOGLE_PROVIDER, googleUserId);

        if (socialAuth.isPresent()) {
            // Cập nhật existing record
            SocialAuthUser existing = socialAuth.get();
            existing.setUpdatedAt(LocalDateTime.now());
            socialAuthUserRepository.save(existing);
        } else {
            // Tạo mới
            SocialAuthUser newSocialAuth = SocialAuthUser.builder()
                    .provider(GOOGLE_PROVIDER)
                    .providerUserId(googleUserId)
                    .authUser(user)
                    .extraData(payload.toString()) // Lưu payload để dùng sau
                    .createdAt(LocalDateTime.now())
                    .build();
            socialAuthUserRepository.save(newSocialAuth);
        }
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
    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0];
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
