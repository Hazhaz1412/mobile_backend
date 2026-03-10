package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUnreadCountResponse {
    private long unreadCount;
    private long unreadOffers;
    private long unreadAlerts;
    private long unreadMessages;
}
