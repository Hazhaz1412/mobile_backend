package com.react.mobile.Controller;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.request.LoginRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthUserRepository authUserRepository;
 
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
 
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyUser(token));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        var user = authUserRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        authService.logout(user);
        return ResponseEntity.ok("Đã đăng xuất từ tất cả thiết bị!");
    }      
    @PostMapping("/login") 
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

}