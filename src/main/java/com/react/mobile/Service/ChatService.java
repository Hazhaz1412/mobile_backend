package com.react.mobile.Service;

import com.react.mobile.DTO.request.SendEventChatMessageRequest;
import com.react.mobile.DTO.response.ChatPublicKeyResponse;
import com.react.mobile.DTO.response.EventChatMessageResponse;
import com.react.mobile.DTO.response.EventChatParticipantResponse;
import com.react.mobile.DTO.response.EventChatSummaryResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface ChatService {

    ChatPublicKeyResponse getMyPublicKey(AuthUser currentUser);

    ChatPublicKeyResponse upsertMyPublicKey(AuthUser currentUser, String publicKey);

    List<EventChatSummaryResponse> getChatEvents(AuthUser currentUser);

    List<EventChatParticipantResponse> getParticipants(AuthUser currentUser, Long eventId);

    List<EventChatMessageResponse> getMessages(AuthUser currentUser, Long eventId, String scope, Long counterpartUserId, int limit);

    List<EventChatMessageResponse> getPinnedMessages(AuthUser currentUser, Long eventId);

    EventChatMessageResponse sendMessage(AuthUser currentUser, Long eventId, SendEventChatMessageRequest request);

    EventChatMessageResponse pinMessage(AuthUser currentUser, Long eventId, Long messageId);

    void unpinMessage(AuthUser currentUser, Long eventId, Long messageId);
}
