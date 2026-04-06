package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChatSummaryResponse {

    private Long eventId;
    private String title;
    private String locationName;
    private String startDate;
    private Long organizerId;
    private String organizerUsername;
    private Boolean organizer;
    private Integer participantCount;
    private Long pinnedCount;
    private String lastGroupMessageAt;
}
