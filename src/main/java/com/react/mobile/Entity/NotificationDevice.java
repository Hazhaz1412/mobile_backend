package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_device",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_token"}),
        indexes = {
                @Index(name = "idx_notification_device_user", columnList = "user_id,active")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private AuthUser user;

    @Column(name = "device_token", nullable = false, length = 255)
    private String deviceToken;

    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
