package com.react.mobile.Entity;

import com.react.mobile.Entity.Enums.EventChatMessageKind;
import com.react.mobile.Entity.Enums.EventChatScope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_chat_messages",
        indexes = {
                @Index(name = "idx_event_chat_event_scope_created", columnList = "event_id,scope,created_at"),
                @Index(name = "idx_event_chat_pinned", columnList = "event_id,pinned,pinned_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    private AuthUser sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    @ToString.Exclude
    private AuthUser recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private EventChatScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private EventChatMessageKind kind;

    @Column(name = "ciphertext", nullable = false, columnDefinition = "TEXT")
    private String ciphertext;

    @Column(name = "content_nonce", nullable = false, length = 255)
    private String contentNonce;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by_id")
    @ToString.Exclude
    private AuthUser pinnedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
