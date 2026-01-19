package com.react.mobile.Service.Impl; 
 
import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.VerificationToken;
import com.react.mobile.Entity.Enums.TokenType;  
import com.react.mobile.Mapper.UserMapper;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.VerificationTokenRepository;
import com.react.mobile.Service.AuthService;
import com.react.mobile.Service.EmailService;

import lombok.RequiredArgsConstructor;
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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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
}