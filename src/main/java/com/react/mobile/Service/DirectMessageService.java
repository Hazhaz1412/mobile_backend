package com.react.mobile.Service;

import com.react.mobile.DTO.request.SendDirectMessageRequest;
import com.react.mobile.DTO.response.DirectConversationResponse;
import com.react.mobile.DTO.response.DirectMessageResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface DirectMessageService {
    List<DirectConversationResponse> listConversations(AuthUser currentUser);
    DirectConversationResponse createOrGetConversation(AuthUser currentUser, Long peerUserId);
    List<DirectMessageResponse> getMessages(AuthUser currentUser, Long conversationId, int limit);
    DirectMessageResponse sendMessage(AuthUser currentUser, Long conversationId, SendDirectMessageRequest request);
}
