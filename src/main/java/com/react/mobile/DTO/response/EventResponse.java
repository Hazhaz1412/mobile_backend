package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String eventType;
    private String status;
    private String moderationStatus;
    private String moderationReason;
    private Boolean isFree;
    private Double price;
    private String currency;
    private String startDate;
    private String endDate;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private Integer maxAttendees;
    private Integer currentAttendees;
    private String imageUrl;
    private String organizerUsername;
    private Long organizerId;
    private Boolean bookmarked;
    private Long countdownSeconds;
    private String createdAt;
}
