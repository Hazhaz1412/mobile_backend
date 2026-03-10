package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponse {
    private List<NotificationResponse> notifications;
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private boolean hasNext;
    private long unreadCount;
    private long unreadOffers;
    private long unreadAlerts;
    private long unreadMessages;
}
