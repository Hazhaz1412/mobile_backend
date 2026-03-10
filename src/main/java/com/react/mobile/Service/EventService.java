package com.react.mobile.Service;

import com.react.mobile.DTO.request.CreateEventRequest;
import com.react.mobile.DTO.response.EventListResponse;
import com.react.mobile.DTO.response.EventResponse;
import com.react.mobile.Entity.AuthUser;

import java.util.List;

public interface EventService {

    EventResponse createEvent(AuthUser user, CreateEventRequest request);

    EventResponse getEvent(AuthUser user, Long eventId);

    EventListResponse listEvents(
            AuthUser user,
            String status,
            String eventType,
            Boolean isFree,
            String search,
            Double latitude,
            Double longitude,
            Double maxDistanceKm,
            int page,
            int size
    );

    EventResponse updateEvent(AuthUser user, Long eventId, CreateEventRequest request);

    void deleteEvent(AuthUser user, Long eventId);

    EventResponse joinEvent(AuthUser user, Long eventId);

    EventResponse leaveEvent(AuthUser user, Long eventId);

    List<EventResponse> getBookmarks(AuthUser user);

    boolean toggleBookmark(AuthUser user, Long eventId);

    List<EventResponse> getMyEvents(AuthUser user);

    void refreshEventStatuses();
}
