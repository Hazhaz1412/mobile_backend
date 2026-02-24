package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.LoginRequest;
import com.react.mobile.DTO.request.RefreshTokenRequest;
import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.request.ResetPasswordRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.TokenType;
import com.react.mobile.Entity.LoginHistory;
import com.react.mobile.Entity.RefreshToken;
import com.react.mobile.Entity.VerificationToken;
import com.react.mobile.Mapper.UserMapper;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.LoginHistoryRepository;
import com.react.mobile.Repository.RefreshTokenRepository;
import com.react.mobile.Repository.VerificationTokenRepository;
import com.react.mobile.Service.AuthService;
import com.react.mobile.Service.EmailService;
import com.react.mobile.Service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final VerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final int OTP_EXPIRATION_MINUTES = 15;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = generateUniqueUsername(
                normalizedEmail,
                request.getUsername()
        );

        if (authUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (authUserRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        AuthUser newUser = userMapper.toEntity(request);
        newUser.setUsername(username);
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setIsActive(false);

        AuthUser savedUser = authUserRepository.save(newUser);
        tokenRepository.deleteByUserAndType(savedUser, TokenType.EMAIL_VERIFICATION);
        VerificationToken token = createToken(savedUser, TokenType.EMAIL_VERIFICATION, true);
        emailService.sendVerificationEmail(savedUser.getEmail(), token.getToken(), token.getOtpCode());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.resolveIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập username hoặc email");
        }

        Optional<AuthUser> foundUser = findByIdentifier(identifier);
        String usernameForAuthentication = foundUser
                .map(AuthUser::getUsername)
                .orElse(identifier);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            usernameForAuthentication,
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            foundUser.ifPresent(user -> saveLoginHistory(user, false, "ACCOUNT_NOT_VERIFIED", httpRequest));
            throw new IllegalArgumentException("Tài khoản chưa được xác thực email");
        } catch (AuthenticationException e) {
            foundUser.ifPresent(user -> saveLoginHistory(user, false, e.getMessage(), httpRequest));
            throw e;
        }

        AuthUser user = authUserRepository.findByUsername(usernameForAuthentication).orElseThrow();

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);
        user.setLastLogin(LocalDateTime.now());
        authUserRepository.save(user);
        saveLoginHistory(user, true, null, httpRequest);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public String verifyUser(String tokenString) {
        VerificationToken token = tokenRepository
                .findByTokenAndType(tokenString, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new IllegalArgumentException("Mã xác thực không hợp lệ"));
        activateUserWithToken(token);
        return "Kích hoạt tài khoản thành công";
    }

    @Override
    @Transactional
    public String verifyEmailOtp(String email, String otp) {
        AuthUser user = authUserRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        VerificationToken token = tokenRepository
                .findTopByUserAndTypeAndOtpCodeAndConfirmedAtIsNullOrderByCreatedAtDesc(
                        user,
                        TokenType.EMAIL_VERIFICATION,
                        otp
                )
                .orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ"));

        activateUserWithToken(token);
        return "Xác thực OTP thành công";
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        AuthUser user = authUserRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã xác thực trước đó");
        }

        tokenRepository.deleteByUserAndType(user, TokenType.EMAIL_VERIFICATION);
        VerificationToken token = createToken(user, TokenType.EMAIL_VERIFICATION, true);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken(), token.getOtpCode());
    }

    @Override
    @Transactional
    public void sendForgotPasswordOtp(String email) {
        Optional<AuthUser> optionalUser = authUserRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (optionalUser.isEmpty()) {
            return;
        }

        AuthUser user = optionalUser.get();
        tokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        VerificationToken token = createToken(user, TokenType.PASSWORD_RESET, true);
        emailService.sendPasswordResetEmail(user.getEmail(), token.getOtpCode());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không hợp lệ"));

        VerificationToken token = tokenRepository
                .findTopByUserAndTypeAndOtpCodeAndConfirmedAtIsNullOrderByCreatedAtDesc(
                        user,
                        TokenType.PASSWORD_RESET,
                        request.getOtp()
                )
                .orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn"));

        validateTokenExpiry(token);
        token.setConfirmedAt(LocalDateTime.now());
        tokenRepository.save(token);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        authUserRepository.save(user);

        tokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        AuthUser user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ"));

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ"));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new IllegalArgumentException("Refresh token đã hết hạn");
        }

        if (!jwtService.isTokenValid(refreshToken, user)) {
            refreshTokenRepository.delete(storedToken);
            throw new IllegalArgumentException("Refresh token không hợp lệ");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        saveRefreshToken(user, newRefreshToken);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userMapper.toResponse(user))
                .build();
    }

    @Transactional
    public void saveRefreshToken(AuthUser user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void logout(AuthUser user) {
        refreshTokenRepository.deleteByUser(user);
    }

    private void activateUserWithToken(VerificationToken token) {
        if (token.getConfirmedAt() != null) {
            throw new IllegalArgumentException("OTP đã được sử dụng");
        }

        validateTokenExpiry(token);

        AuthUser user = token.getUser();
        user.setIsActive(true);
        authUserRepository.save(user);

        token.setConfirmedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private void validateTokenExpiry(VerificationToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP đã hết hạn, vui lòng yêu cầu mã mới");
        }
    }

    private VerificationToken createToken(AuthUser user, TokenType type, boolean withOtp) {
        VerificationToken token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .otpCode(withOtp ? generateOtp() : null)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES))
                .type(type)
                .build();
        return tokenRepository.save(token);
    }

    private String generateOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    private String generateUniqueUsername(String email, String preferredUsername) {
        String baseUsername = (preferredUsername != null && !preferredUsername.isBlank())
                ? preferredUsername.trim()
                : email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");

        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        String candidate = baseUsername;
        int suffix = 1;
        while (authUserRepository.existsByUsername(candidate)) {
            candidate = baseUsername + suffix++;
        }
        return candidate;
    }

    private Optional<AuthUser> findByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return authUserRepository.findByEmail(identifier.trim().toLowerCase(Locale.ROOT));
        }
        return authUserRepository.findByUsername(identifier.trim());
    }

    private void saveLoginHistory(
            AuthUser user,
            boolean success,
            String failureReason,
            HttpServletRequest request
    ) {
        if (request == null) {
            return;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        LoginHistory history = LoginHistory.builder()
                .userId(user.getId())
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceType(detectDeviceType(userAgent))
                .success(success)
                .failureReason(failureReason)
                .build();
        loginHistoryRepository.save(history);
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN";
        }

        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("android") || ua.contains("iphone") || ua.contains("mobile")) {
            return "MOBILE";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "TABLET";
        }
        return "DESKTOP";
    }
}
