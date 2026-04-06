package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChatParticipantResponse {

    private Long userId;
    private String username;
    private String role;
    private Boolean organizer;
    private Boolean currentUser;
    private Boolean directAllowed;
    private Boolean hasChatPublicKey;
    private String publicKey;
}
