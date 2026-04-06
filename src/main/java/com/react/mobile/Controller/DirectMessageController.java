package com.react.mobile.Controller;

import com.react.mobile.DTO.request.SendDirectMessageRequest;
import com.react.mobile.DTO.response.DirectConversationResponse;
import com.react.mobile.DTO.response.DirectMessageResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final AuthUserRepository authUserRepository;

    @GetMapping("/conversations")
    public ResponseEntity<List<DirectConversationResponse>> listConversations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(directMessageService.listConversations(resolveUser(userDetails)));
    }

    @PostMapping("/conversations/{userId}")
    public ResponseEntity<DirectConversationResponse> createConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                directMessageService.createOrGetConversation(resolveUser(userDetails), userId));
    }

    @GetMapping("/conversations/{convId}/messages")
    public ResponseEntity<List<DirectMessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long convId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(
                directMessageService.getMessages(resolveUser(userDetails), convId, limit));
    }

    @PostMapping("/conversations/{convId}/messages")
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long convId,
            @RequestBody SendDirectMessageRequest request
    ) {
        return ResponseEntity.ok(
                directMessageService.sendMessage(resolveUser(userDetails), convId, request));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
