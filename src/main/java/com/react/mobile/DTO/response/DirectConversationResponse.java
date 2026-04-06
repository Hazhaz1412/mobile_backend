package com.react.mobile.DTO.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DirectConversationResponse {
    private Long id;
    private Long peerId;
    private String peerUsername;
    private String peerProfilePictureUrl;
    private String peerPublicKey;
    private String lastMessageAt;
    private String createdAt;
}
