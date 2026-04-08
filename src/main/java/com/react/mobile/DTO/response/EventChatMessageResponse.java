package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChatMessageResponse {

    private Long id;
    private Long eventId;
    private String scope;
    private String kind;
    private Long senderId;
    private String senderName;
    private String senderPublicKey;
    private String senderProfilePictureUrl;
    private Long recipientId;
    private String ciphertext;
    private String contentNonce;
    private String encryptedKey;
    private String keyNonce;
    private Boolean pinned;
    private String pinnedAt;
    private Long pinnedById;
    private String pinnedByName;
    private String createdAt;
}
