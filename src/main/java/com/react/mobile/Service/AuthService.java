package com.react.mobile.Service;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.response.UserResponse;

// Đây là Interface, không cần @Service
public interface AuthService {
    UserResponse register(RegisterRequest request);
    
    String verifyUser(String token);
}