package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "direct_conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}),
        indexes = {
                @Index(name = "idx_dc_user1", columnList = "user1_id,last_message_at"),
                @Index(name = "idx_dc_user2", columnList = "user2_id,last_message_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    @ToString.Exclude
    private AuthUser user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    @ToString.Exclude
    private AuthUser user2;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
