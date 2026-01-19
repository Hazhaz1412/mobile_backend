package com.react.mobile.Controller;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
 
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
 
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyUser(token));
    }

    

}