package com.react.mobile.Controller;

import com.react.mobile.DTO.request.SendEventChatMessageRequest;
import com.react.mobile.DTO.request.UpsertChatPublicKeyRequest;
import com.react.mobile.DTO.response.ChatPublicKeyResponse;
import com.react.mobile.DTO.response.EventChatMessageResponse;
import com.react.mobile.DTO.response.EventChatParticipantResponse;
import com.react.mobile.DTO.response.EventChatSummaryResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthUserRepository authUserRepository;

    @GetMapping("/keys/me")
    public ResponseEntity<ChatPublicKeyResponse> getMyKey(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.getMyPublicKey(resolveUser(userDetails)));
    }

    @PutMapping("/keys/me")
    public ResponseEntity<ChatPublicKeyResponse> upsertMyKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpsertChatPublicKeyRequest request
    ) {
        return ResponseEntity.ok(chatService.upsertMyPublicKey(resolveUser(userDetails), request.getPublicKey()));
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventChatSummaryResponse>> getChatEvents(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.getChatEvents(resolveUser(userDetails)));
    }

    @GetMapping("/events/{eventId}/participants")
    public ResponseEntity<List<EventChatParticipantResponse>> getParticipants(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(chatService.getParticipants(resolveUser(userDetails), eventId));
    }

    @GetMapping("/events/{eventId}/messages")
    public ResponseEntity<List<EventChatMessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "GROUP") String scope,
            @RequestParam(required = false) Long counterpartUserId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(chatService.getMessages(resolveUser(userDetails), eventId, scope, counterpartUserId, limit));
    }

    @GetMapping("/events/{eventId}/pins")
    public ResponseEntity<List<EventChatMessageResponse>> getPins(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(chatService.getPinnedMessages(resolveUser(userDetails), eventId));
    }

    @PostMapping("/events/{eventId}/messages")
    public ResponseEntity<EventChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @Valid @RequestBody SendEventChatMessageRequest request
    ) {
        return ResponseEntity.ok(chatService.sendMessage(resolveUser(userDetails), eventId, request));
    }

    @PostMapping("/events/{eventId}/messages/{messageId}/pin")
    public ResponseEntity<EventChatMessageResponse> pinMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @PathVariable Long messageId
    ) {
        return ResponseEntity.ok(chatService.pinMessage(resolveUser(userDetails), eventId, messageId));
    }

    @DeleteMapping("/events/{eventId}/messages/{messageId}/pin")
    public ResponseEntity<Map<String, String>> unpinMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @PathVariable Long messageId
    ) {
        chatService.unpinMessage(resolveUser(userDetails), eventId, messageId);
        return ResponseEntity.ok(Map.of("message", "Message unpinned"));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
