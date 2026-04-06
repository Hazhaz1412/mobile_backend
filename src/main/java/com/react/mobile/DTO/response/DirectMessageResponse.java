package com.react.mobile.DTO.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DirectMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderPublicKey;
    private String kind;
    private String ciphertext;
    private String contentNonce;
    private String encryptedKey;
    private String keyNonce;
    private String createdAt;
}
