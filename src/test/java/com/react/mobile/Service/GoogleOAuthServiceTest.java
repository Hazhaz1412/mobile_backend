package com.react.mobile.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.react.mobile.DTO.request.GoogleOAuthRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.SocialAuthUser;
import com.react.mobile.DTO.response.UserResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private GoogleIdTokenVerifierService googleIdTokenVerifierService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private AuthUser testUser;
    private SocialAuthUser testSocialAuthUser;
    private GoogleIdToken googleIdToken;
    private GoogleIdToken.Payload payload;

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
                .id(UUID.randomUUID())
                .provider("google")
                .providerUserId("123456789")
                .authUser(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        googleIdToken = mock(GoogleIdToken.class);
        payload = mock(GoogleIdToken.Payload.class);

        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("123456789");
        when(payload.getEmail()).thenReturn("test@gmail.com");
        when(payload.get("name")).thenReturn("Test User");
        when(payload.get("picture")).thenReturn("https://example.com/avatar.png");
        when(payload.getEmailVerified()).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@gmail.com");
        userResponse.setIsActive(true);
        when(userMapper.toResponse(any(AuthUser.class))).thenReturn(userResponse);

        org.springframework.test.util.ReflectionTestUtils.setField(
                googleOAuthService,
                "googleClientId",
                "test-client-id"
        );
    }

    /**
     * Test: User mới đăng nhập lần đầu với Google
     */
    @Test
    void testNewUserGoogleSignIn() throws Exception {
        // Given
        GoogleOAuthRequest request = GoogleOAuthRequest.builder()
                .idToken("valid_google_id_token")
                .deviceName("react-native-test")
                .build();

        when(googleIdTokenVerifierService.verify("valid_google_id_token", "test-client-id"))
                .thenReturn(googleIdToken);
        when(authUserRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(authUserRepository.existsByUsername(anyString())).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(testUser);
        when(socialAuthUserRepository.findByProviderAndProviderUserId("google", "123456789"))
                .thenReturn(Optional.empty());
        when(socialAuthUserRepository.findByProviderAndAuthUser("google", testUser))
                .thenReturn(Optional.empty());
        when(socialAuthUserRepository.save(any(SocialAuthUser.class))).thenReturn(testSocialAuthUser);
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt_token");
        when(jwtService.generateRefreshToken(any(AuthUser.class))).thenReturn("refresh_token");

        AuthenticationResponse response = googleOAuthService.verifyGoogleToken(request);

        assertEquals("jwt_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertNotNull(response.getUser());
        verify(refreshTokenRepository, times(1)).save(any());
    }

    /**
     * Test: Existing user đăng nhập lại với Google
     */
    @Test
    void testExistingUserGoogleSignIn() throws Exception {
        // Given
        GoogleOAuthRequest request = GoogleOAuthRequest.builder()
                .idToken("valid_google_id_token")
                .deviceName("react-native-test")
                .build();

        when(googleIdTokenVerifierService.verify("valid_google_id_token", "test-client-id"))
                .thenReturn(googleIdToken);
        when(authUserRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(socialAuthUserRepository.findByProviderAndProviderUserId("google", "123456789"))
                .thenReturn(Optional.of(testSocialAuthUser));
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt_token");
        when(jwtService.generateRefreshToken(any(AuthUser.class))).thenReturn("refresh_token");

        AuthenticationResponse response = googleOAuthService.verifyGoogleToken(request);

        assertEquals("jwt_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        verify(socialAuthUserRepository, times(1)).save(testSocialAuthUser);
    }

    @Test
    void testRejectsUnverifiedEmail() throws Exception {
        GoogleOAuthRequest request = GoogleOAuthRequest.builder()
                .idToken("valid_google_id_token")
                .build();

        when(googleIdTokenVerifierService.verify("valid_google_id_token", "test-client-id"))
                .thenReturn(googleIdToken);
        when(payload.getEmailVerified()).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> googleOAuthService.verifyGoogleToken(request)
        );

        assertTrue(exception.getMessage().contains("Email Google chưa được xác minh"));
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
