package com.react.mobile.Service;

import com.react.mobile.DTO.request.CreateAnnouncementRequest;
import com.react.mobile.DTO.response.NotificationListResponse;
import com.react.mobile.DTO.response.NotificationResponse;
import com.react.mobile.DTO.response.NotificationUnreadCountResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.EventStatus;
import com.react.mobile.Entity.Event;

public interface NotificationService {
    NotificationListResponse getInbox(AuthUser authUser, String category, Boolean unreadOnly, int page, int size);

    NotificationUnreadCountResponse getUnreadCount(AuthUser authUser);

    NotificationResponse markRead(AuthUser authUser, Long notificationId);

    void markAllRead(AuthUser authUser);

    void deleteOne(AuthUser authUser, Long notificationId);

    void clearAll(AuthUser authUser);

    void registerDevice(AuthUser authUser, String deviceToken, String platform);

    void unregisterDevice(AuthUser authUser, String deviceToken);

    void sendAnnouncement(AuthUser actor, CreateAnnouncementRequest request);

    void notifyNearbyAlertsForLocation(AuthUser authUser, Double latitude, Double longitude);

    void notifyEventUpdated(Event event, AuthUser actor);

    void notifyEventStatusChanged(Event event, EventStatus previousStatus);

    void triggerCountdownReminders();

    void retryPendingPushDeliveries();
}
