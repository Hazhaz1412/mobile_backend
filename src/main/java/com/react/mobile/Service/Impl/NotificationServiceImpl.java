package com.react.mobile.Service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.react.mobile.DTO.request.CreateAnnouncementRequest;
import com.react.mobile.DTO.response.NotificationListResponse;
import com.react.mobile.DTO.response.NotificationResponse;
import com.react.mobile.DTO.response.NotificationUnreadCountResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.EventStatus;
import com.react.mobile.Entity.Enums.NotificationCategory;
import com.react.mobile.Entity.Enums.NotificationType;
import com.react.mobile.Entity.Event;
import com.react.mobile.Entity.EventBookmark;
import com.react.mobile.Entity.Notification;
import com.react.mobile.Entity.NotificationDevice;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Repository.EventBookmarkRepository;
import com.react.mobile.Repository.EventRepository;
import com.react.mobile.Repository.NotificationDeviceRepository;
import com.react.mobile.Repository.NotificationRepository;
import com.react.mobile.Repository.UserPreferencesRepository;
import com.react.mobile.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final URI EXPO_PUSH_URL = URI.create("https://exp.host/--/api/v2/push/send");
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double NEARBY_ALERT_RADIUS_KM = 3.0;

    private static final List<Long> COUNTDOWN_MILESTONE_HOURS = List.of(24L, 1L);

    private final NotificationRepository notificationRepository;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final AuthUserRepository authUserRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getInbox(AuthUser authUser, String category, Boolean unreadOnly, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        NotificationCategory parsedCategory = parseCategory(category);
        boolean unreadFilter = Boolean.TRUE.equals(unreadOnly);

        Page<Notification> inbox = notificationRepository.findInbox(
                authUser.getId(),
                parsedCategory,
                unreadFilter,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        NotificationUnreadCountResponse unreadCount = getUnreadCount(authUser);

        return NotificationListResponse.builder()
                .notifications(inbox.getContent().stream().map(this::toResponse).toList())
                .page(safePage)
                .size(safeSize)
                .total(inbox.getTotalElements())
                .totalPages(inbox.getTotalPages())
                .hasNext(inbox.hasNext())
                .unreadCount(unreadCount.getUnreadCount())
                .unreadOffers(unreadCount.getUnreadOffers())
                .unreadAlerts(unreadCount.getUnreadAlerts())
                .unreadMessages(unreadCount.getUnreadMessages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(AuthUser authUser) {
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(authUser.getId());
        long unreadOffers = notificationRepository.countByUserIdAndCategoryAndIsReadFalse(authUser.getId(), NotificationCategory.OFFERS);
        long unreadAlerts = notificationRepository.countByUserIdAndCategoryAndIsReadFalse(authUser.getId(), NotificationCategory.ALERTS);
        long unreadMessages = notificationRepository.countByUserIdAndCategoryAndIsReadFalse(authUser.getId(), NotificationCategory.MESSAGES);

        return NotificationUnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .unreadOffers(unreadOffers)
                .unreadAlerts(unreadAlerts)
                .unreadMessages(unreadMessages)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markRead(AuthUser authUser, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, authUser.getId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllRead(AuthUser authUser) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(authUser.getId());
        if (unread.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(item -> {
            item.setIsRead(true);
            item.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteOne(AuthUser authUser, Long notificationId) {
        long deleted = notificationRepository.deleteByIdAndUserId(notificationId, authUser.getId());
        if (deleted == 0) {
            throw new RuntimeException("Notification not found");
        }
    }

    @Override
    @Transactional
    public void clearAll(AuthUser authUser) {
        notificationRepository.deleteByUserId(authUser.getId());
    }

    @Override
    @Transactional
    public void registerDevice(AuthUser authUser, String deviceToken, String platform) {
        String token = normalizeDeviceToken(deviceToken);
        String normalizedPlatform = normalizePlatform(platform);

        NotificationDevice device = notificationDeviceRepository.findByDeviceToken(token)
                .orElseGet(NotificationDevice::new);

        device.setUser(authUser);
        device.setDeviceToken(token);
        device.setPlatform(normalizedPlatform);
        device.setActive(true);
        notificationDeviceRepository.save(device);
    }

    @Override
    @Transactional
    public void unregisterDevice(AuthUser authUser, String deviceToken) {
        String token = normalizeDeviceToken(deviceToken);
        Optional<NotificationDevice> existing = notificationDeviceRepository.findByDeviceToken(token);
        if (existing.isEmpty()) {
            return;
        }

        NotificationDevice device = existing.get();
        if (!Objects.equals(device.getUser().getId(), authUser.getId())) {
            throw new RuntimeException("Device does not belong to current user");
        }

        device.setActive(false);
        notificationDeviceRepository.save(device);
    }

    @Override
    @Transactional
    public void sendAnnouncement(AuthUser actor, CreateAnnouncementRequest request) {
        if (!Boolean.TRUE.equals(actor.getIsStaff()) && !Boolean.TRUE.equals(actor.getIsSuperuser())) {
            throw new IllegalArgumentException("Only staff or admin can send announcements");
        }

        NotificationCategory category = parseCategoryOrFallback(request.getCategory(), NotificationCategory.MESSAGES);

        List<AuthUser> recipients;
        if (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty()) {
            recipients = authUserRepository.findAll();
        } else {
            recipients = authUserRepository.findAllById(request.getTargetUserIds());
        }

        recipients.stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .forEach(user -> createNotification(
                        user.getId(),
                        category,
                        NotificationType.ANNOUNCEMENT,
                        request.getTitle().trim(),
                        request.getMessage().trim(),
                        "SYSTEM",
                        "ANNOUNCEMENT",
                        null
                ));
    }

    @Override
    @Transactional
    public void notifyNearbyAlertsForLocation(AuthUser authUser, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = eventRepository.findByStatusAndStartDateBetween(
                EventStatus.INCOMING,
                now,
                now.plusHours(24)
        );

        List<EventDistance> nearby = upcoming.stream()
                .filter(event -> event.getLatitude() != null && event.getLongitude() != null)
                .map(event -> new EventDistance(event, haversineKm(latitude, longitude, event.getLatitude(), event.getLongitude())))
                .filter(item -> item.distanceKm <= NEARBY_ALERT_RADIUS_KM)
                .sorted((a, b) -> Double.compare(a.distanceKm, b.distanceKm))
                .limit(2)
                .toList();

        LocalDate dayBucket = LocalDate.now();
        nearby.forEach(item -> {
            Event event = item.event;
            String dedupeKey = "nearby:%d:%s".formatted(event.getId(), dayBucket);
            String message = "%s starts %s at %s (%.1f km away).".formatted(
                    event.getTitle(),
                    formatTimeUntil(event.getStartDate()),
                    safeLocationLabel(event.getLocationName()),
                    item.distanceKm
            );

            createNotification(
                    authUser.getId(),
                    NotificationCategory.ALERTS,
                    NotificationType.NEARBY_ALERT,
                    "Nearby event alert",
                    message,
                    "EVENT",
                    String.valueOf(event.getId()),
                    dedupeKey
            );
        });
    }

    @Override
    @Transactional
    public void notifyEventUpdated(Event event, AuthUser actor) {
        List<EventBookmark> bookmarks = eventBookmarkRepository.findByEventIdAndUserIdNot(event.getId(), actor.getId());
        if (bookmarks.isEmpty()) {
            return;
        }

        String updateMarker = event.getUpdatedAt() != null
                ? event.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS).toString()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();

        bookmarks.forEach(bookmark -> createNotification(
                bookmark.getUser().getId(),
                NotificationCategory.MESSAGES,
                NotificationType.SAVED_ITEM_UPDATE,
                "Saved item updated",
                "Event \"%s\" has new details from the host. Check schedule, venue, or notes.".formatted(event.getTitle()),
                "EVENT",
                String.valueOf(event.getId()),
                "saved-update:%d:%s".formatted(event.getId(), updateMarker)
        ));
    }

    @Override
    @Transactional
    public void notifyEventStatusChanged(Event event, EventStatus previousStatus) {
        if (previousStatus == event.getStatus()) {
            return;
        }

        Set<Long> recipients = new LinkedHashSet<>();
        eventBookmarkRepository.findByEventId(event.getId())
                .forEach(bookmark -> recipients.add(bookmark.getUser().getId()));
        if (event.getOrganizer() != null) {
            recipients.add(event.getOrganizer().getId());
        }

        String title;
        String message;
        NotificationType type = NotificationType.REMINDER;

        if (event.getStatus() == EventStatus.ONGOING) {
            title = "Event is now live";
            message = "\"%s\" is now ongoing at %s.".formatted(event.getTitle(), safeLocationLabel(event.getLocationName()));
        } else if (event.getStatus() == EventStatus.COMPLETED) {
            title = "Event completed";
            message = "\"%s\" has finished. Thanks for following this event.".formatted(event.getTitle());
        } else {
            title = "Event status changed";
            message = "\"%s\" changed status to %s.".formatted(event.getTitle(), event.getStatus().name());
        }

        recipients.forEach(userId -> createNotification(
                userId,
                NotificationCategory.ALERTS,
                type,
                title,
                message,
                "EVENT",
                String.valueOf(event.getId()),
                "event-status:%d:%s".formatted(event.getId(), event.getStatus().name())
        ));
    }

    @Override
    @Scheduled(fixedDelayString = "${app.notifications.countdown-fixed-delay-ms:60000}")
    @Transactional
    public void triggerCountdownReminders() {
        LocalDateTime now = LocalDateTime.now();
        COUNTDOWN_MILESTONE_HOURS.forEach(hours -> sendCountdownAtMilestone(now, hours));
    }

    @Override
    @Scheduled(fixedDelayString = "${app.notifications.push-retry-delay-ms:300000}")
    @Transactional
    public void retryPendingPushDeliveries() {
        LocalDateTime fromTime = LocalDateTime.now().minusHours(48);
        List<Notification> pending = notificationRepository.findByIsReadFalseAndPushDeliveredAtIsNullAndCreatedAtAfterOrderByCreatedAtAsc(
                fromTime,
                PageRequest.of(0, 120)
        );
        pending.forEach(this::dispatchPushIfEligible);
    }

    private void sendCountdownAtMilestone(LocalDateTime now, long hoursBefore) {
        LocalDateTime windowFrom = now.plusHours(hoursBefore).minusMinutes(1);
        LocalDateTime windowTo = now.plusHours(hoursBefore).plusMinutes(1);

        List<EventBookmark> bookmarks = eventBookmarkRepository.findByEventStatusAndEventStartDateBetween(
                EventStatus.INCOMING,
                windowFrom,
                windowTo
        );

        bookmarks.forEach(bookmark -> {
            Event event = bookmark.getEvent();
            Long userId = bookmark.getUser().getId();
            createNotification(
                    userId,
                    NotificationCategory.ALERTS,
                    NotificationType.EVENT_COUNTDOWN,
                    "Event starts in %d hour%s".formatted(hoursBefore, hoursBefore > 1 ? "s" : ""),
                    "\"%s\" starts at %s.".formatted(event.getTitle(), safeLocationLabel(event.getLocationName())),
                    "EVENT",
                    String.valueOf(event.getId()),
                    "countdown:%d:%d:%d".formatted(event.getId(), userId, hoursBefore)
            );
        });

        List<Event> organizerEvents = eventRepository.findByStatusAndStartDateBetween(EventStatus.INCOMING, windowFrom, windowTo);
        organizerEvents.forEach(event -> {
            if (event.getOrganizer() == null) {
                return;
            }
            Long organizerId = event.getOrganizer().getId();
            createNotification(
                    organizerId,
                    NotificationCategory.ALERTS,
                    NotificationType.REMINDER,
                    "Host reminder",
                    "Your event \"%s\" starts in %d hour%s.".formatted(event.getTitle(), hoursBefore, hoursBefore > 1 ? "s" : ""),
                    "EVENT",
                    String.valueOf(event.getId()),
                    "countdown-host:%d:%d:%d".formatted(event.getId(), organizerId, hoursBefore)
            );
        });
    }

    private void createNotification(
            Long userId,
            NotificationCategory category,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            String referenceId,
            String dedupeKey
    ) {
        String normalizedDedupe = (dedupeKey == null || dedupeKey.isBlank()) ? null : dedupeKey.trim();
        if (normalizedDedupe != null && notificationRepository.existsByUserIdAndDedupeKey(userId, normalizedDedupe)) {
            return;
        }

        AuthUser recipient = authUserRepository.findById(userId).orElse(null);
        if (recipient == null || !Boolean.TRUE.equals(recipient.getIsActive())) {
            return;
        }

        Notification saved = notificationRepository.save(
                Notification.builder()
                        .user(recipient)
                        .category(category)
                        .type(type)
                        .title(title)
                        .message(message)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .dedupeKey(normalizedDedupe)
                        .isRead(false)
                        .build()
        );

        dispatchPushIfEligible(saved);
    }

    private void dispatchPushIfEligible(Notification notification) {
        if (!isPushEnabled(notification.getUser().getId())) {
            return;
        }

        List<NotificationDevice> devices = notificationDeviceRepository.findByUserIdAndActiveTrue(notification.getUser().getId());
        if (devices.isEmpty()) {
            return;
        }

        boolean delivered = false;
        List<String> errors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (NotificationDevice device : devices) {
            PushResult result = sendExpoPush(device.getDeviceToken(), notification);
            if (result.deviceNotRegistered) {
                device.setActive(false);
                notificationDeviceRepository.save(device);
            }
            if (result.success) {
                delivered = true;
            } else if (!result.errorMessage.isBlank()) {
                errors.add(result.errorMessage);
            }
        }

        notification.setPushAttemptedAt(now);
        if (delivered) {
            notification.setPushDeliveredAt(now);
            notification.setPushError(null);
        } else if (!errors.isEmpty()) {
            notification.setPushError(truncate(String.join(" | ", errors), 380));
        }
        notificationRepository.save(notification);
    }

    private PushResult sendExpoPush(String deviceToken, Notification notification) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("to", deviceToken);
            payload.put("title", notification.getTitle());
            payload.put("body", notification.getMessage());
            payload.put("sound", "default");

            ObjectNode data = payload.putObject("data");
            data.put("notificationId", notification.getId());
            data.put("category", notification.getCategory().name());
            data.put("type", notification.getType().name());
            if (notification.getReferenceType() != null) {
                data.put("referenceType", notification.getReferenceType());
            }
            if (notification.getReferenceId() != null) {
                data.put("referenceId", notification.getReferenceId());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(EXPO_PUSH_URL)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return PushResult.error("HTTP " + response.statusCode(), false);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray() && dataNode.size() > 0) {
                dataNode = dataNode.get(0);
            }

            String status = dataNode.path("status").asText("");
            if ("ok".equalsIgnoreCase(status)) {
                return PushResult.success();
            }

            String detailCode = dataNode.path("details").path("error").asText("");
            String message = dataNode.path("message").asText("Push failed");
            boolean deviceNotRegistered = "DeviceNotRegistered".equalsIgnoreCase(detailCode);
            return PushResult.error(message, deviceNotRegistered);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return PushResult.error(ex.getMessage() == null ? "Push send failed" : ex.getMessage(), false);
        }
    }

    private boolean isPushEnabled(Long userId) {
        return userPreferencesRepository.findByAuthUserId(userId)
                .map(pref -> !Boolean.FALSE.equals(pref.getPushNotifications()))
                .orElse(true);
    }

    private NotificationCategory parseCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        return parseCategoryOrFallback(category, null);
    }

    private NotificationCategory parseCategoryOrFallback(String category, NotificationCategory fallback) {
        if (category == null || category.isBlank()) {
            return fallback;
        }
        try {
            return NotificationCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid category. Allowed: OFFERS, ALERTS, MESSAGES, ALL");
        }
    }

    private String normalizeDeviceToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("deviceToken is required");
        }
        return token.trim();
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "UNKNOWN";
        }
        return platform.trim().toUpperCase(Locale.ROOT);
    }

    private String safeLocationLabel(String locationName) {
        if (locationName == null || locationName.isBlank()) {
            return "your saved location";
        }
        return locationName;
    }

    private String formatTimeUntil(LocalDateTime targetTime) {
        long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), targetTime);
        if (minutes <= 0) {
            return "soon";
        }
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;
        if (hours <= 0) {
            return "in %d minutes".formatted(remainMinutes);
        }
        if (remainMinutes == 0) {
            return "in %d hour%s".formatted(hours, hours > 1 ? "s" : "");
        }
        return "in %d hour%s %d minutes".formatted(hours, hours > 1 ? "s" : "", remainMinutes);
    }

    private NotificationResponse toResponse(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .category(entity.getCategory().name())
                .type(entity.getType().name())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record EventDistance(Event event, double distanceKm) {
    }

    private static final class PushResult {
        private final boolean success;
        private final String errorMessage;
        private final boolean deviceNotRegistered;

        private PushResult(boolean success, String errorMessage, boolean deviceNotRegistered) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.deviceNotRegistered = deviceNotRegistered;
        }

        static PushResult success() {
            return new PushResult(true, "", false);
        }

        static PushResult error(String errorMessage, boolean deviceNotRegistered) {
            String safeError = errorMessage == null ? "Push send failed" : errorMessage;
            return new PushResult(false, safeError, deviceNotRegistered);
        }
    }
}
