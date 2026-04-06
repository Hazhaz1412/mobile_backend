package com.react.mobile.Service;

import com.react.mobile.Config.ChatSessionRegistry;
import com.react.mobile.DTO.response.EventChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatRealtimeNotifier {

    private final ChatSessionRegistry chatSessionRegistry;

    public void notifyNewMessages(Map<Long, EventChatMessageResponse> payloadsByUserId) {
        payloadsByUserId.forEach((userId, payload) -> chatSessionRegistry.send(userId, "chat.message", payload));
    }

    public void notifyPinnedMessages(Map<Long, EventChatMessageResponse> payloadsByUserId) {
        payloadsByUserId.forEach((userId, payload) -> chatSessionRegistry.send(userId, "chat.pin", payload));
    }

    public void notifyUnpinnedMessage(Long userId, Long eventId, Long messageId) {
        chatSessionRegistry.send(userId, "chat.unpin", Map.of(
                "eventId", eventId,
                "messageId", messageId
        ));
    }
}
