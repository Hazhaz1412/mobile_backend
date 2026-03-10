package com.react.mobile.Entity;

import com.react.mobile.Entity.Enums.ReviewModerationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private AuthUser user;

    /** PLACE, EVENT, ATTRACTION, CUISINE, ACTIVITY */
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    /** discovery place id or event id */
    @Column(name = "target_id", nullable = false, length = 200)
    private String targetId;

    @Column(name = "target_name", length = 300)
    private String targetName;

    @Column(name = "photo_url", length = 700)
    private String photoUrl;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Long helpfulCount = 0L;

    @Column(name = "flag_count", nullable = false)
    @Builder.Default
    private Long flagCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewModerationStatus moderationStatus = ReviewModerationStatus.APPROVED;

    @Column(name = "owner_reply", columnDefinition = "TEXT")
    private String ownerReply;

    @Column(name = "owner_reply_author", length = 100)
    private String ownerReplyAuthor;

    @Column(name = "owner_reply_author_id")
    private Long ownerReplyAuthorId;

    @Column(name = "owner_reply_at")
    private LocalDateTime ownerReplyAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
