package com.react.mobile.Entity;

import com.react.mobile.Entity.Enums.TokenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // --- THÊM CỘT NÀY ---
    @Enumerated(EnumType.STRING) // Lưu chữ "PASSWORD_RESET" vào DB cho dễ đọc
    @Column(nullable = false)
    private TokenType type; 

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser user;
}