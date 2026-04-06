package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.SendDirectMessageRequest;
import com.react.mobile.DTO.response.DirectConversationResponse;
import com.react.mobile.DTO.response.DirectMessageResponse;
import com.react.mobile.Entity.*;
import com.react.mobile.Entity.Enums.EventChatMessageKind;
import com.react.mobile.Repository.*;
import com.react.mobile.Service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final AuthUserRepository authUserRepository;
    private final ChatPublicKeyRepository chatPublicKeyRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DirectConversationResponse> listConversations(AuthUser currentUser) {
        List<DirectConversation> conversations = conversationRepository.findByUserId(currentUser.getId());
        return conversations.stream()
                .map(conv -> toConversationResponse(currentUser, conv))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DirectConversationResponse createOrGetConversation(AuthUser currentUser, Long peerUserId) {
        if (currentUser.getId().equals(peerUserId)) {
            throw new IllegalArgumentException("Cannot message yourself");
        }

        AuthUser peer = authUserRepository.findById(peerUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DirectConversation conv = conversationRepository
                .findByUserPair(currentUser.getId(), peerUserId)
                .orElseGet(() -> {
                    // Ensure consistent ordering: lower ID → user1
                    AuthUser u1 = currentUser.getId() < peerUserId ? currentUser : peer;
                    AuthUser u2 = currentUser.getId() < peerUserId ? peer : currentUser;
                    return conversationRepository.save(DirectConversation.builder()
                            .user1(u1)
                            .user2(u2)
                            .build());
                });

        return toConversationResponse(currentUser, conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectMessageResponse> getMessages(AuthUser currentUser, Long conversationId, int limit) {
        DirectConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(currentUser, conv);

        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<DirectMessageResponse> messages = messageRepository
                .findRecentByConversationId(conversationId, PageRequest.of(0, safeLimit))
                .stream()
                .map(msg -> toMessageResponse(currentUser, msg))
                .collect(Collectors.toList());

        Collections.reverse(messages); // Return in chronological order
        return messages;
    }

    @Override
    @Transactional
    public DirectMessageResponse sendMessage(AuthUser currentUser, Long conversationId,
                                             SendDirectMessageRequest request) {
        DirectConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(currentUser, conv);

        EventChatMessageKind kind;
        try {
            kind = EventChatMessageKind.valueOf(request.getKind().toUpperCase());
        } catch (Exception e) {
            kind = EventChatMessageKind.TEXT;
        }

        DirectMessage msg = messageRepository.save(DirectMessage.builder()
                .conversation(conv)
                .sender(currentUser)
                .kind(kind)
                .ciphertext(request.getCiphertext())
                .contentNonce(request.getContentNonce())
                .build());

        // Update last message timestamp
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        // Save encrypted keys for the recipient
        if (request.getEncryptedKeys() != null) {
            for (SendDirectMessageRequest.EncryptedKeyEntry keyEntry : request.getEncryptedKeys()) {
                // Store encrypted keys (reuse EventChatMessageKey entity pattern)
                // For DM we embed the key info into the response
            }
        }

        return toMessageResponse(currentUser, msg);
    }

    private void validateParticipant(AuthUser user, DirectConversation conv) {
        if (!conv.getUser1().getId().equals(user.getId()) &&
            !conv.getUser2().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not part of this conversation");
        }
    }

    private DirectConversationResponse toConversationResponse(AuthUser currentUser, DirectConversation conv) {
        AuthUser peer = conv.getUser1().getId().equals(currentUser.getId())
                ? conv.getUser2() : conv.getUser1();

        String peerPublicKey = chatPublicKeyRepository.findByUserId(peer.getId())
                .map(ChatPublicKey::getPublicKey)
                .orElse(null);

        UserProfile peerProfile = userProfileRepository.findByAuthUserId(peer.getId()).orElse(null);

        return DirectConversationResponse.builder()
                .id(conv.getId())
                .peerId(peer.getId())
                .peerUsername(peer.getUsername())
                .peerProfilePictureUrl(peerProfile != null ? peerProfile.getProfilePictureUrl() : null)
                .peerPublicKey(peerPublicKey)
                .lastMessageAt(conv.getLastMessageAt() != null ? conv.getLastMessageAt().toString() : null)
                .createdAt(conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : null)
                .build();
    }

    private DirectMessageResponse toMessageResponse(AuthUser currentUser, DirectMessage msg) {
        String senderPublicKey = chatPublicKeyRepository.findByUserId(msg.getSender().getId())
                .map(ChatPublicKey::getPublicKey)
                .orElse(null);

        // Find the encrypted key for the current user
        String encryptedKey = null;
        String keyNonce = null;

        return DirectMessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getUsername())
                .senderPublicKey(senderPublicKey)
                .kind(msg.getKind().name())
                .ciphertext(msg.getCiphertext())
                .contentNonce(msg.getContentNonce())
                .encryptedKey(encryptedKey)
                .keyNonce(keyNonce)
                .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null)
                .build();
    }
}
