package com.react.mobile.Controller;

import com.react.mobile.DTO.request.CreateAnnouncementRequest;
import com.react.mobile.DTO.request.RegisterPushDeviceRequest;
import com.react.mobile.DTO.response.NotificationListResponse;
import com.react.mobile.DTO.response.NotificationResponse;
import com.react.mobile.DTO.response.NotificationUnreadCountResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthUserRepository authUserRepository;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getInbox(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(notificationService.getInbox(authUser, category, unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(notificationService.getUnreadCount(authUser));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(notificationService.markRead(authUser, notificationId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        notificationService.markAllRead(authUser);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        notificationService.deleteOne(authUser, notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Map<String, String>> clearAll(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        notificationService.clearAll(authUser);
        return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
    }

    @PostMapping("/devices/register")
    public ResponseEntity<Map<String, String>> registerDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegisterPushDeviceRequest request
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        notificationService.registerDevice(authUser, request.getDeviceToken(), request.getPlatform());
        return ResponseEntity.ok(Map.of("message", "Push device registered"));
    }

    @DeleteMapping("/devices/{deviceToken}")
    public ResponseEntity<Map<String, String>> unregisterDevice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String deviceToken
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        notificationService.unregisterDevice(authUser, deviceToken);
        return ResponseEntity.ok(Map.of("message", "Push device unregistered"));
    }

    @PostMapping("/announcements")
    public ResponseEntity<Map<String, String>> sendAnnouncement(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        notificationService.sendAnnouncement(authUser, request);
        return ResponseEntity.ok(Map.of("message", "Announcement sent"));
    }

    @PostMapping("/push/retry")
    public ResponseEntity<Map<String, String>> triggerPushRetry(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser authUser = getCurrentUser(userDetails);
        if (!Boolean.TRUE.equals(authUser.getIsStaff()) && !Boolean.TRUE.equals(authUser.getIsSuperuser())) {
            throw new IllegalArgumentException("Only staff/admin can trigger push retry");
        }
        notificationService.retryPendingPushDeliveries();
        return ResponseEntity.ok(Map.of("message", "Push retry triggered"));
    }

    private AuthUser getCurrentUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
