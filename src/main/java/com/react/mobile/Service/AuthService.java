package com.react.mobile.Service;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.request.LoginRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Entity.AuthUser;

// Đây là Interface, không cần @Service
public interface AuthService {
    UserResponse register(RegisterRequest request);
    
    AuthenticationResponse login(LoginRequest request);
    
    String verifyUser(String token);
    
    void logout(AuthUser user);
}