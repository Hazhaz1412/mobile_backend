package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "activity_feed_items",
        indexes = {
                @Index(name = "idx_afi_actor", columnList = "actor_id,created_at"),
                @Index(name = "idx_afi_created", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    @ToString.Exclude
    private AuthUser actor;

    /** e.g. REVIEW, EVENT_JOIN, BOOKMARK, FOLLOW */
    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    /** e.g. PLACE, EVENT, USER */
    @Column(name = "target_type", length = 40)
    private String targetType;

    @Column(name = "target_id", length = 255)
    private String targetId;

    @Column(name = "target_name", length = 500)
    private String targetName;

    /** Additional JSON metadata (rating, etc.) */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
