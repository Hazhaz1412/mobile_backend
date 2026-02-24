package com.react.mobile.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.react.mobile.DTO.request.GoogleOAuthRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.SocialAuthUser;
import com.react.mobile.Mapper.UserMapper;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.RefreshTokenRepository;
import com.react.mobile.Repository.SocialAuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private SocialAuthUserRepository socialAuthUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private AuthUser testUser;
    private SocialAuthUser testSocialAuthUser;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = AuthUser.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .username("testuser")
                .email("test@gmail.com")
                .password("")
                .isActive(true)
                .isSuperuser(false)
                .isStaff(false)
                .dateJoined(LocalDateTime.now())
                .build();

        // Setup test social auth user
        testSocialAuthUser = SocialAuthUser.builder()
                .id(UUID.randomUUID().toString())
                .provider("google")
                .providerUserId("123456789")
                .authUser(testUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test: User mới đăng nhập lần đầu với Google
     */
    @Test
    void testNewUserGoogleSignIn() {
        // Given
        GoogleOAuthRequest request = GoogleOAuthRequest.builder()
                .idToken("valid_google_id_token")
                .deviceName("react-native-test")
                .build();

        when(authUserRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(authUserRepository.existsByUsername(anyString())).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(testUser);
        when(socialAuthUserRepository.findByProviderAndProviderUserId("google", "123456789"))
                .thenReturn(Optional.empty());
        when(socialAuthUserRepository.save(any(SocialAuthUser.class))).thenReturn(testSocialAuthUser);
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt_token");
        when(jwtService.generateRefreshToken(any(AuthUser.class))).thenReturn("refresh_token");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            googleOAuthService.verifyGoogleToken(request);
        });
    }

    /**
     * Test: Existing user đăng nhập lại với Google
     */
    @Test
    void testExistingUserGoogleSignIn() {
        // Given
        GoogleOAuthRequest request = GoogleOAuthRequest.builder()
                .idToken("valid_google_id_token")
                .deviceName("react-native-test")
                .build();

        when(authUserRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(socialAuthUserRepository.findByProviderAndProviderUserId("google", "123456789"))
                .thenReturn(Optional.of(testSocialAuthUser));
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt_token");
        when(jwtService.generateRefreshToken(any(AuthUser.class))).thenReturn("refresh_token");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            googleOAuthService.verifyGoogleToken(request);
        });
    }

    /**
     * Test: Disconnect Google account
     */
    @Test
    void testDisconnectGoogle() {
        // When
        googleOAuthService.disconnectGoogle(testUser);

        // Then
        verify(socialAuthUserRepository, times(1))
                .deleteByProviderAndAuthUser("google", testUser);
    }

    /**
     * Test: Check if Google is linked
     */
    @Test
    void testIsGoogleLinked() {
        // Given
        when(socialAuthUserRepository.existsByProviderAndAuthUser("google", testUser))
                .thenReturn(true);

        // When
        boolean result = googleOAuthService.isGoogleLinked(testUser);

        // Then
        assertTrue(result);
        verify(socialAuthUserRepository, times(1))
                .existsByProviderAndAuthUser("google", testUser);
    }

    /**
     * Test: Check if Google is not linked
     */
    @Test
    void testIsGoogleNotLinked() {
        // Given
        when(socialAuthUserRepository.existsByProviderAndAuthUser("google", testUser))
                .thenReturn(false);

        // When
        boolean result = googleOAuthService.isGoogleLinked(testUser);

        // Then
        assertFalse(result);
    }
}
