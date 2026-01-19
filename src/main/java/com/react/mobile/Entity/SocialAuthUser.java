package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
// import org.hibernate.validator.constraints.UUID; <-- XÓA DÒNG NÀY (Không cần thiết)

import java.time.LocalDateTime;
import com.react.mobile.Entity.AuthUser;

@Entity
@Table(name = "social_auth_user") // Tên bảng trong DB
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid") 
    private String id; // Đổi thành String (hoặc java.util.UUID) để chứa UUID

    @Column(name = "provider", nullable = false, length = 55)
    private String provider;

    @Column(name = "uid", nullable = false, length = 255) 
    private String providerUserId; 
 
    @ManyToOne(fetch = FetchType.LAZY)  
    @JoinColumn(name = "user_id", nullable = false) 
    private AuthUser authUser;

    @Column(name = "extra_data", columnDefinition = "TEXT")  
    private String extraData;

    @CreationTimestamp
    @Column(name = "created", updatable = false)  
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified")  
    private LocalDateTime updatedAt;
}