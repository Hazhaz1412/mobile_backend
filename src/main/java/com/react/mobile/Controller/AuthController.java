package com.react.mobile.Controller;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.request.LoginRequest;
import com.react.mobile.DTO.request.GoogleOAuthRequest;
import com.react.mobile.DTO.request.RefreshTokenRequest;
import com.react.mobile.DTO.request.ResendOtpRequest;
import com.react.mobile.DTO.request.ForgotPasswordRequest;
import com.react.mobile.DTO.request.ResetPasswordRequest;
import com.react.mobile.DTO.request.VerifyOtpRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.AuthService;
import com.react.mobile.Service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthUserRepository authUserRepository;
    private final GoogleOAuthService googleOAuthService;
 
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
 
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyUser(token));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String message = authService.verifyEmailOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<Map<String, String>> resendVerificationOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendVerificationOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP xác thực đã được gửi lại"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Nếu email tồn tại, OTP đã được gửi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        var user = authUserRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        authService.logout(user);
        return ResponseEntity.ok("Đã đăng xuất từ tất cả thiết bị!");
    }      
    @PostMapping("/login") 
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    /**
     * Google OAuth endpoint - React Native client gửi Google ID token lên
     */
    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> googleAuth(@Valid @RequestBody GoogleOAuthRequest request) {
        return ResponseEntity.ok(googleOAuthService.verifyGoogleToken(request));
    }

    /**
     * Disconnect Google account
     */
    @PostMapping("/google/disconnect")
    public ResponseEntity<String> disconnectGoogle(@AuthenticationPrincipal UserDetails userDetails) {
        var user = authUserRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        googleOAuthService.disconnectGoogle(user);
        return ResponseEntity.ok("Đã hủy liên kết tài khoản Google");
    }

    /**
     * Kiểm tra xem user có liên kết Google không
     */
    @GetMapping("/google/linked")
    public ResponseEntity<Boolean> isGoogleLinked(@AuthenticationPrincipal UserDetails userDetails) {
        var user = authUserRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(googleOAuthService.isGoogleLinked(user));
    }
}
