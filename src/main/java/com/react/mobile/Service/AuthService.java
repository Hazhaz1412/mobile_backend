package com.react.mobile.Service;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.request.LoginRequest;
import com.react.mobile.DTO.request.RefreshTokenRequest;
import com.react.mobile.DTO.request.ResetPasswordRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Entity.AuthUser;
import jakarta.servlet.http.HttpServletRequest;

// Đây là Interface, không cần @Service
public interface AuthService {
    UserResponse register(RegisterRequest request);
    
    AuthenticationResponse login(LoginRequest request, HttpServletRequest httpRequest);
    
    String verifyUser(String token);

    String verifyEmailOtp(String email, String otp);

    void resendVerificationOtp(String email);

    void sendForgotPasswordOtp(String email);

    void resetPassword(ResetPasswordRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);
    
    void logout(AuthUser user);
}
