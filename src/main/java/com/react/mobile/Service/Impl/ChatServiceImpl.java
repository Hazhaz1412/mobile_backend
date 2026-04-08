package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.EventChatMessageKeyRequest;
import com.react.mobile.DTO.request.SendEventChatMessageRequest;
import com.react.mobile.DTO.response.ChatPublicKeyResponse;
import com.react.mobile.DTO.response.EventChatMessageResponse;
import com.react.mobile.DTO.response.EventChatParticipantResponse;
import com.react.mobile.DTO.response.EventChatSummaryResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.ChatPublicKey;
import com.react.mobile.Entity.Enums.EventChatMessageKind;
import com.react.mobile.Entity.Enums.EventChatScope;
import com.react.mobile.Entity.Enums.EventParticipantRole;
import com.react.mobile.Entity.Event;
import com.react.mobile.Entity.EventChatMessage;
import com.react.mobile.Entity.EventChatMessageKey;
import com.react.mobile.Entity.EventParticipant;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.ChatPublicKeyRepository;
import com.react.mobile.Repository.EventChatMessageKeyRepository;
import com.react.mobile.Repository.EventChatMessageRepository;
import com.react.mobile.Repository.EventParticipantRepository;
import com.react.mobile.Repository.EventRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Service.ChatRealtimeNotifier;
import com.react.mobile.Service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final AuthUserRepository authUserRepository;
    private final ChatPublicKeyRepository chatPublicKeyRepository;
    private final EventChatMessageRepository eventChatMessageRepository;
    private final EventChatMessageKeyRepository eventChatMessageKeyRepository;
    private final ChatRealtimeNotifier chatRealtimeNotifier;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public ChatPublicKeyResponse getMyPublicKey(AuthUser currentUser) {
        return chatPublicKeyRepository.findByUserId(currentUser.getId())
                .map(this::toKeyResponse)
                .orElse(ChatPublicKeyResponse.builder()
                        .userId(currentUser.getId())
                        .username(currentUser.getUsername())
                        .algorithm("nacl-box")
                        .publicKey(null)
                        .updatedAt(null)
                        .build());
    }

    @Override
    @Transactional
    public ChatPublicKeyResponse upsertMyPublicKey(AuthUser currentUser, String publicKey) {
        String normalized = normalizeRequired(publicKey, "Public key is required");
        ChatPublicKey entity = chatPublicKeyRepository.findByUserId(currentUser.getId())
                .orElseGet(ChatPublicKey::new);

        entity.setUser(currentUser);
        entity.setAlgorithm("nacl-box");
        entity.setPublicKey(normalized);
        return toKeyResponse(chatPublicKeyRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventChatSummaryResponse> getChatEvents(AuthUser currentUser) {
        Map<Long, Event> events = new LinkedHashMap<>();

        eventRepository.findByOrganizerIdOrderByCreatedAtDesc(currentUser.getId())
                .forEach(event -> events.put(event.getId(), event));

        eventParticipantRepository.findByUserIdOrderByJoinedAtDesc(currentUser.getId()).stream()
                .map(EventParticipant::getEvent)
                .forEach(event -> events.put(event.getId(), event));

        return events.values().stream()
                .filter(event -> canAccessEvent(event, currentUser))
                .sorted(Comparator.comparing((Event event) -> {
                    LocalDateTime lastMessage = eventChatMessageRepository.findLatestCreatedAtByEventIdAndScope(event.getId(), EventChatScope.GROUP);
                    return lastMessage != null ? lastMessage : event.getStartDate();
                }).reversed())
                .map(event -> toSummary(event, currentUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventChatParticipantResponse> getParticipants(AuthUser currentUser, Long eventId) {
        Event event = getAccessibleEvent(eventId, currentUser);
        List<AuthUser> members = resolveGroupMembers(event);
        Map<Long, ChatPublicKey> publicKeys = chatPublicKeyRepository.findByUserIdIn(
                        members.stream().map(AuthUser::getId).toList()
                ).stream()
                .collect(Collectors.toMap(item -> item.getUser().getId(), Function.identity()));
        Map<Long, String> profilePictureMap = userProfileRepository.findByAuthUserIdIn(
                members.stream().map(AuthUser::getId).toList()
            ).stream()
            .collect(Collectors.toMap(item -> item.getAuthUser().getId(), item -> item.getProfilePictureUrl()));

        return members.stream()
                .map(member -> EventChatParticipantResponse.builder()
                        .userId(member.getId())
                        .username(member.getUsername())
                        .role(resolveParticipantRole(event, member).name())
                        .organizer(Objects.equals(event.getOrganizer().getId(), member.getId()))
                        .currentUser(Objects.equals(currentUser.getId(), member.getId()))
                        .directAllowed(canDirectMessage(event, currentUser, member))
                        .hasChatPublicKey(publicKeys.containsKey(member.getId()))
                        .publicKey(publicKeys.containsKey(member.getId()) ? publicKeys.get(member.getId()).getPublicKey() : null)
                        .profilePictureUrl(profilePictureMap.get(member.getId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventChatMessageResponse> getMessages(AuthUser currentUser, Long eventId, String scope, Long counterpartUserId, int limit) {
        Event event = getAccessibleEvent(eventId, currentUser);
        EventChatScope parsedScope = parseScope(scope);
        int safeLimit = Math.min(Math.max(1, limit), 100);

        List<EventChatMessage> messages;
        if (parsedScope == EventChatScope.GROUP) {
            messages = eventChatMessageRepository.findRecentGroupMessages(
                    eventId,
                    EventChatScope.GROUP,
                    PageRequest.of(0, safeLimit)
            );
        } else {
            AuthUser counterpart = resolveDirectCounterpart(event, currentUser, counterpartUserId);
            messages = eventChatMessageRepository.findRecentDirectMessages(
                    eventId,
                    EventChatScope.DIRECT,
                    currentUser.getId(),
                    counterpart.getId(),
                    PageRequest.of(0, safeLimit)
            );
        }

        List<EventChatMessage> ordered = new ArrayList<>(messages);
        ordered.sort(Comparator.comparing(EventChatMessage::getCreatedAt));
        return toResponsesForViewer(ordered, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventChatMessageResponse> getPinnedMessages(AuthUser currentUser, Long eventId) {
        getAccessibleEvent(eventId, currentUser);
        return toResponsesForViewer(
                eventChatMessageRepository.findPinnedMessages(eventId, EventChatScope.GROUP),
                currentUser.getId()
        );
    }

    @Override
    @Transactional
    public EventChatMessageResponse sendMessage(AuthUser currentUser, Long eventId, SendEventChatMessageRequest request) {
        Event event = getAccessibleEvent(eventId, currentUser);
        EventChatScope scope = parseScope(request.getScope());
        EventChatMessageKind kind = parseKind(request.getKind());
        String ciphertext = normalizeRequired(request.getCiphertext(), "Ciphertext is required");
        String contentNonce = normalizeRequired(request.getContentNonce(), "Content nonce is required");

        AuthUser directCounterpart = null;
        Set<Long> recipientIds = new LinkedHashSet<>();
        if (scope == EventChatScope.GROUP) {
            recipientIds.addAll(resolveGroupMembers(event).stream().map(AuthUser::getId).toList());
        } else {
            directCounterpart = resolveDirectCounterpart(event, currentUser, request.getCounterpartUserId());
            recipientIds.add(currentUser.getId());
            recipientIds.add(directCounterpart.getId());
        }

        Map<Long, EventChatMessageKeyRequest> keyRequests = validateEncryptedKeys(request.getEncryptedKeys(), recipientIds);

        EventChatMessage savedMessage = eventChatMessageRepository.save(
                EventChatMessage.builder()
                        .event(event)
                        .sender(currentUser)
                        .recipient(directCounterpart)
                        .scope(scope)
                        .kind(kind)
                        .ciphertext(ciphertext)
                        .contentNonce(contentNonce)
                        .pinned(false)
                        .build()
        );

        Map<Long, AuthUser> recipientsById = authUserRepository.findAllById(recipientIds).stream()
                .collect(Collectors.toMap(AuthUser::getId, Function.identity()));

        List<EventChatMessageKey> keys = recipientIds.stream()
                .map(userId -> {
                    EventChatMessageKeyRequest keyRequest = keyRequests.get(userId);
                    return EventChatMessageKey.builder()
                            .message(savedMessage)
                            .user(recipientsById.get(userId))
                            .encryptedKey(keyRequest.getEncryptedKey())
                            .keyNonce(keyRequest.getKeyNonce())
                            .build();
                })
                .toList();
        eventChatMessageKeyRepository.saveAll(keys);

        Map<Long, EventChatMessageResponse> payloadsByUserId = buildPayloadsByRecipient(savedMessage, recipientIds);
        chatRealtimeNotifier.notifyNewMessages(payloadsByUserId);

        return payloadsByUserId.get(currentUser.getId());
    }

    @Override
    @Transactional
    public EventChatMessageResponse pinMessage(AuthUser currentUser, Long eventId, Long messageId) {
        Event event = getAccessibleEvent(eventId, currentUser);
        ensureOrganizerOrStaff(event, currentUser);

        EventChatMessage message = eventChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!Objects.equals(message.getEvent().getId(), eventId) || message.getScope() != EventChatScope.GROUP) {
            throw new RuntimeException("Only group messages from this event can be pinned");
        }

        message.setPinned(true);
        message.setPinnedAt(LocalDateTime.now());
        message.setPinnedBy(currentUser);
        message = eventChatMessageRepository.save(message);

        Set<Long> recipients = resolveGroupMembers(event).stream().map(AuthUser::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, EventChatMessageResponse> payloadsByUserId = buildPayloadsByRecipient(message, recipients);
        chatRealtimeNotifier.notifyPinnedMessages(payloadsByUserId);
        return payloadsByUserId.get(currentUser.getId());
    }

    @Override
    @Transactional
    public void unpinMessage(AuthUser currentUser, Long eventId, Long messageId) {
        Event event = getAccessibleEvent(eventId, currentUser);
        ensureOrganizerOrStaff(event, currentUser);

        EventChatMessage message = eventChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!Objects.equals(message.getEvent().getId(), eventId) || message.getScope() != EventChatScope.GROUP) {
            throw new RuntimeException("Only group messages from this event can be unpinned");
        }

        message.setPinned(false);
        message.setPinnedAt(null);
        message.setPinnedBy(null);
        eventChatMessageRepository.save(message);

        resolveGroupMembers(event).forEach(member ->
                chatRealtimeNotifier.notifyUnpinnedMessage(member.getId(), eventId, messageId)
        );
    }

    private Event getAccessibleEvent(Long eventId, AuthUser currentUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!canAccessEvent(event, currentUser)) {
            throw new AccessDeniedException("You do not have access to this chat");
        }
        return event;
    }

    private boolean canAccessEvent(Event event, AuthUser currentUser) {
        if (Objects.equals(event.getOrganizer().getId(), currentUser.getId())) {
            return true;
        }
        return eventParticipantRepository.existsByEventIdAndUserId(event.getId(), currentUser.getId());
    }

    private List<AuthUser> resolveGroupMembers(Event event) {
        Map<Long, AuthUser> members = new LinkedHashMap<>();
        if (event.getOrganizer() != null) {
            members.put(event.getOrganizer().getId(), event.getOrganizer());
        }
        eventParticipantRepository.findByEventIdOrderByJoinedAtAsc(event.getId()).forEach(participant ->
                members.put(participant.getUser().getId(), participant.getUser())
        );
        return new ArrayList<>(members.values());
    }

    private EventParticipantRole resolveParticipantRole(Event event, AuthUser member) {
        if (Objects.equals(event.getOrganizer().getId(), member.getId())) {
            return EventParticipantRole.ORGANIZER;
        }
        return eventParticipantRepository.findByEventIdAndUserId(event.getId(), member.getId())
                .map(EventParticipant::getRole)
                .orElse(EventParticipantRole.ATTENDEE);
    }

    private boolean canDirectMessage(Event event, AuthUser actor, AuthUser counterpart) {
        if (Objects.equals(actor.getId(), counterpart.getId())) {
            return false;
        }

        Long organizerId = event.getOrganizer().getId();
        boolean actorIsOrganizer = Objects.equals(actor.getId(), organizerId);
        boolean counterpartIsOrganizer = Objects.equals(counterpart.getId(), organizerId);
        boolean actorIsParticipant = eventParticipantRepository.existsByEventIdAndUserId(event.getId(), actor.getId()) || actorIsOrganizer;
        boolean counterpartIsParticipant = eventParticipantRepository.existsByEventIdAndUserId(event.getId(), counterpart.getId()) || counterpartIsOrganizer;

        return actorIsParticipant
                && counterpartIsParticipant
                && actorIsOrganizer != counterpartIsOrganizer;
    }

    private AuthUser resolveDirectCounterpart(Event event, AuthUser currentUser, Long counterpartUserId) {
        if (counterpartUserId == null) {
            throw new IllegalArgumentException("counterpartUserId is required for direct messages");
        }
        AuthUser counterpart = authUserRepository.findById(counterpartUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!canDirectMessage(event, currentUser, counterpart)) {
            throw new AccessDeniedException("Direct messaging is only available between organizer and event attendees");
        }
        return counterpart;
    }

    private Map<Long, EventChatMessageKeyRequest> validateEncryptedKeys(
            List<EventChatMessageKeyRequest> encryptedKeys,
            Set<Long> expectedRecipientIds
    ) {
        if (encryptedKeys == null || encryptedKeys.isEmpty()) {
            throw new IllegalArgumentException("Encrypted keys are required");
        }

        Map<Long, EventChatMessageKeyRequest> requestsByUserId = new LinkedHashMap<>();
        for (EventChatMessageKeyRequest item : encryptedKeys) {
            if (item.getUserId() == null) {
                throw new IllegalArgumentException("Encrypted key userId is required");
            }
            if (requestsByUserId.put(item.getUserId(), item) != null) {
                throw new IllegalArgumentException("Duplicate encrypted key for user " + item.getUserId());
            }
        }

        if (!requestsByUserId.keySet().equals(expectedRecipientIds)) {
            throw new IllegalArgumentException("Encrypted keys must be provided for every conversation participant");
        }
        return requestsByUserId;
    }

    private Map<Long, EventChatMessageResponse> buildPayloadsByRecipient(EventChatMessage message, Collection<Long> recipientIds) {
        Map<Long, EventChatMessageKey> perUserKey = recipientIds.stream()
                .map(userId -> eventChatMessageKeyRepository.findByMessageIdAndUserId(message.getId(), userId)
                        .orElseThrow(() -> new RuntimeException("Missing encrypted key for user " + userId)))
                .collect(Collectors.toMap(item -> item.getUser().getId(), Function.identity()));

        Map<Long, String> senderPublicKeys = chatPublicKeyRepository.findByUserIdIn(List.of(message.getSender().getId())).stream()
                .collect(Collectors.toMap(item -> item.getUser().getId(), ChatPublicKey::getPublicKey));
        Map<Long, String> senderProfilePictureUrls = userProfileRepository.findByAuthUserIdIn(List.of(message.getSender().getId())).stream()
            .collect(Collectors.toMap(item -> item.getAuthUser().getId(), item -> item.getProfilePictureUrl()));

        Map<Long, EventChatMessageResponse> payloads = new LinkedHashMap<>();
        for (Long userId : recipientIds) {
            payloads.put(
                userId,
                toMessageResponse(
                    message,
                    perUserKey.get(userId),
                    senderPublicKeys.get(message.getSender().getId()),
                    senderProfilePictureUrls.get(message.getSender().getId())
                )
            );
        }
        return payloads;
    }

    private List<EventChatMessageResponse> toResponsesForViewer(List<EventChatMessage> messages, Long viewerUserId) {
        if (messages.isEmpty()) {
            return List.of();
        }

        List<Long> messageIds = messages.stream().map(EventChatMessage::getId).toList();
        Map<Long, EventChatMessageKey> keyByMessageId = eventChatMessageKeyRepository.findByMessageIdInAndUserId(messageIds, viewerUserId)
                .stream()
                .collect(Collectors.toMap(item -> item.getMessage().getId(), Function.identity()));

        Set<Long> senderIds = messages.stream().map(item -> item.getSender().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> senderPublicKeys = chatPublicKeyRepository.findByUserIdIn(senderIds)
                .stream()
                .collect(Collectors.toMap(item -> item.getUser().getId(), ChatPublicKey::getPublicKey));
        Map<Long, String> senderProfilePictureUrls = userProfileRepository.findByAuthUserIdIn(new ArrayList<>(senderIds))
            .stream()
            .collect(Collectors.toMap(item -> item.getAuthUser().getId(), item -> item.getProfilePictureUrl()));

        return messages.stream()
                .map(message -> {
                    EventChatMessageKey key = keyByMessageId.get(message.getId());
                    if (key == null) {
                        throw new RuntimeException("No encrypted message key available for current user");
                    }
                return toMessageResponse(
                    message,
                    key,
                    senderPublicKeys.get(message.getSender().getId()),
                    senderProfilePictureUrls.get(message.getSender().getId())
                );
                })
                .toList();
    }

        private EventChatMessageResponse toMessageResponse(
            EventChatMessage message,
            EventChatMessageKey key,
            String senderPublicKey,
            String senderProfilePictureUrl
        ) {
        return EventChatMessageResponse.builder()
                .id(message.getId())
                .eventId(message.getEvent().getId())
                .scope(message.getScope().name())
                .kind(message.getKind().name())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .senderPublicKey(senderPublicKey)
            .senderProfilePictureUrl(senderProfilePictureUrl)
                .recipientId(message.getRecipient() != null ? message.getRecipient().getId() : null)
                .ciphertext(message.getCiphertext())
                .contentNonce(message.getContentNonce())
                .encryptedKey(key.getEncryptedKey())
                .keyNonce(key.getKeyNonce())
                .pinned(message.getPinned())
                .pinnedAt(format(message.getPinnedAt()))
                .pinnedById(message.getPinnedBy() != null ? message.getPinnedBy().getId() : null)
                .pinnedByName(message.getPinnedBy() != null ? message.getPinnedBy().getUsername() : null)
                .createdAt(format(message.getCreatedAt()))
                .build();
    }

    private EventChatSummaryResponse toSummary(Event event, AuthUser currentUser) {
        int participantCount = resolveGroupMembers(event).size();
        return EventChatSummaryResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .locationName(event.getLocationName())
                .startDate(format(event.getStartDate()))
                .organizerId(event.getOrganizer().getId())
                .organizerUsername(event.getOrganizer().getUsername())
                .organizer(Objects.equals(event.getOrganizer().getId(), currentUser.getId()))
                .participantCount(participantCount)
                .pinnedCount(eventChatMessageRepository.countByEventIdAndScopeAndPinnedTrue(event.getId(), EventChatScope.GROUP))
                .lastGroupMessageAt(format(eventChatMessageRepository.findLatestCreatedAtByEventIdAndScope(event.getId(), EventChatScope.GROUP)))
                .build();
    }

    private ChatPublicKeyResponse toKeyResponse(ChatPublicKey entity) {
        return ChatPublicKeyResponse.builder()
                .userId(entity.getUser().getId())
                .username(entity.getUser().getUsername())
                .algorithm(entity.getAlgorithm())
                .publicKey(entity.getPublicKey())
                .updatedAt(format(entity.getUpdatedAt()))
                .build();
    }

    private EventChatScope parseScope(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Chat scope is required");
        }
        try {
            return EventChatScope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported chat scope: " + raw);
        }
    }

    private EventChatMessageKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Chat message kind is required");
        }
        try {
            return EventChatMessageKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported chat message kind: " + raw);
        }
    }

    private String normalizeRequired(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return raw.trim();
    }

    private void ensureOrganizerOrStaff(Event event, AuthUser currentUser) {
        boolean isOrganizer = Objects.equals(event.getOrganizer().getId(), currentUser.getId());
        boolean isPrivileged = Boolean.TRUE.equals(currentUser.getIsStaff()) || Boolean.TRUE.equals(currentUser.getIsSuperuser());
        if (!isOrganizer && !isPrivileged) {
            throw new AccessDeniedException("Only the organizer can pin important notices");
        }
    }

    private String format(LocalDateTime value) {
        return value != null ? value.format(DT_FMT) : null;
    }
}
