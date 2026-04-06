package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_public_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPublicKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private AuthUser user;

    @Column(name = "algorithm", nullable = false, length = 40)
    @Builder.Default
    private String algorithm = "nacl-box";

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
