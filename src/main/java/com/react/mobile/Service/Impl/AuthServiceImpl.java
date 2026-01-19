package com.react.mobile.Service.Impl; 
 
import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.request.LoginRequest;
import com.react.mobile.DTO.response.AuthenticationResponse;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.RefreshToken;
import com.react.mobile.Entity.VerificationToken;
import com.react.mobile.Entity.Enums.TokenType;  
import com.react.mobile.Mapper.UserMapper;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.RefreshTokenRepository;
import com.react.mobile.Repository.VerificationTokenRepository;
import com.react.mobile.Service.AuthService;
import com.react.mobile.Service.EmailService;
import com.react.mobile.Service.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final VerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 1. Validate trùng lặp
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại!");
        }
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 2. Map từ DTO sang Entity & Xử lý mật khẩu
        AuthUser newUser = userMapper.toEntity(request);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setIsActive(false); // Mặc định chưa kích hoạt

        // 3. Lưu User trước để có ID
        AuthUser savedUser = authUserRepository.save(newUser);

        // 4. Tạo Verification Token
        String tokenString = UUID.randomUUID().toString();
        
        VerificationToken token = VerificationToken.builder()
            .token(tokenString)
            .user(savedUser)
            .expiryDate(LocalDateTime.now().plusHours(24))
            .build();
        token.setType(TokenType.EMAIL_VERIFICATION);

        tokenRepository.save(token);

        // 5. Gửi Email
        emailService.sendVerificationEmail(savedUser.getEmail(), tokenString);

        // 6. Trả về
        return userMapper.toResponse(savedUser);
    }
    @Override
    public AuthenticationResponse login(LoginRequest request) {
        // 1. Xác thực username/password (Spring tự lo việc check hash password)
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // 2. Nếu vượt qua bước 1, tức là login thành công -> Tìm user trong DB
        var user = authUserRepository.findByUsername(request.getUsername())
                .orElseThrow();

        // 3. Sinh token
        var jwtToken = jwtService.generateToken((org.springframework.security.core.userdetails.UserDetails) user);
        var refreshToken = jwtService.generateRefreshToken((org.springframework.security.core.userdetails.UserDetails) user);
        
        // Lưu refresh token vào DB cho Force Logout
        saveRefreshToken(user, refreshToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(user))
                .build();
    }
    @Override
    public String verifyUser(String tokenString) {
        // 1. Tìm token
        VerificationToken token = tokenRepository.findByToken(tokenString);

        // 2. Kiểm tra lỗi
        if (token == null) {
            return "Mã xác thực không hợp lệ!";
        }

        if (!token.getType().equals(TokenType.EMAIL_VERIFICATION)) {
            return "Loại token không đúng chức năng kích hoạt tài khoản!";
        }

        if (token.getConfirmedAt() != null) {
            return "Tài khoản đã được kích hoạt trước đó rồi!";
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "Mã xác thực đã hết hạn! Vui lòng yêu cầu gửi lại.";
        }

        // 3. Kích hoạt User
        AuthUser user = token.getUser();
        user.setIsActive(true);
        authUserRepository.save(user);

        // 4. Đánh dấu token đã dùng
        token.setConfirmedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.";
    }

    // Lưu refresh token vào DB
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

    // Force Logout - xóa tất cả refresh token của user
    @Transactional
    public void logout(AuthUser user) {
        refreshTokenRepository.deleteByUser(user);
    }
}