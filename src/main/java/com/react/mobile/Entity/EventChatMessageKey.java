package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "event_chat_message_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}),
        indexes = @Index(name = "idx_event_chat_message_keys_user", columnList = "user_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChatMessageKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @ToString.Exclude
    private EventChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private AuthUser user;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    @Column(name = "key_nonce", nullable = false, length = 255)
    private String keyNonce;
}
