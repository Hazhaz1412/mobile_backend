package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.CreateEventRequest;
import com.react.mobile.DTO.response.EventListResponse;
import com.react.mobile.DTO.response.EventResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Event;
import com.react.mobile.Entity.EventBookmark;
import com.react.mobile.Entity.Enums.EventStatus;
import com.react.mobile.Entity.Enums.EventType;
import com.react.mobile.Repository.EventBookmarkRepository;
import com.react.mobile.Repository.EventRepository;
import com.react.mobile.Service.EventService;
import com.react.mobile.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional
    public EventResponse createEvent(AuthUser user, CreateEventRequest req) {
        Event event = Event.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .eventType(EventType.valueOf(req.getEventType()))
                .isFree(req.getIsFree() != null ? req.getIsFree() : true)
                .price(req.getPrice())
                .currency(req.getCurrency() != null ? req.getCurrency() : "VND")
                .startDate(LocalDateTime.parse(req.getStartDate(), DT_FMT))
                .endDate(LocalDateTime.parse(req.getEndDate(), DT_FMT))
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .locationName(req.getLocationName())
                .maxAttendees(req.getMaxAttendees())
                .imageUrl(req.getImageUrl())
                .organizer(user)
                .status(EventStatus.INCOMING)
                .currentAttendees(0)
                .build();

        // Auto-set status based on dates
        LocalDateTime now = LocalDateTime.now();
        if (event.getEndDate().isBefore(now)) {
            event.setStatus(EventStatus.COMPLETED);
        } else if (event.getStartDate().isBefore(now) || event.getStartDate().isEqual(now)) {
            event.setStatus(EventStatus.ONGOING);
        }

        event = eventRepository.save(event);
        return toResponse(event, user.getId());
    }

    @Override
    public EventResponse getEvent(AuthUser user, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return toResponse(event, user.getId());
    }

    @Override
    @Transactional
    public EventListResponse listEvents(
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
    ) {
        // Refresh statuses before listing
        refreshEventStatuses();

        EventStatus statusEnum = null;
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            statusEnum = EventStatus.valueOf(status);
        }

        EventType typeEnum = null;
        if (eventType != null && !eventType.isEmpty() && !"ALL".equalsIgnoreCase(eventType)) {
            typeEnum = EventType.valueOf(eventType);
        }

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : "";

        List<Event> events = eventRepository.filterEvents(statusEnum, typeEnum, isFree, searchParam);

        // Distance filtering (if lat/lng provided)
        if (latitude != null && longitude != null && maxDistanceKm != null) {
            events = events.stream()
                    .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                    .filter(e -> haversineKm(latitude, longitude, e.getLatitude(), e.getLongitude()) <= maxDistanceKm)
                    .collect(Collectors.toList());
        }

        List<EventResponse> responses = events.stream()
                .map(e -> toResponse(e, user.getId()))
                .collect(Collectors.toList());

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        int total = responses.size();
        int fromIdx = safePage * safeSize;
        int toIdx = Math.min(fromIdx + safeSize, total);
        List<EventResponse> pagedResponses = fromIdx < total
                ? responses.subList(fromIdx, toIdx)
                : List.of();
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return EventListResponse.builder()
                .events(pagedResponses)
                .total((long) total)
                .filterStatus(status)
                .filterType(eventType)
                .searchQuery(search)
                .page(safePage)
                .size(safeSize)
                .totalPages(totalPages)
                .hasNext(toIdx < total)
                .build();
    }

    @Override
    @Transactional
    public EventResponse updateEvent(AuthUser user, Long eventId, CreateEventRequest req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getOrganizer().getId().equals(user.getId())) {
            throw new RuntimeException("Only the organizer can update this event");
        }

        if (req.getTitle() != null) event.setTitle(req.getTitle());
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getEventType() != null) event.setEventType(EventType.valueOf(req.getEventType()));
        if (req.getIsFree() != null) event.setIsFree(req.getIsFree());
        if (req.getPrice() != null) event.setPrice(req.getPrice());
        if (req.getCurrency() != null) event.setCurrency(req.getCurrency());
        if (req.getStartDate() != null) event.setStartDate(LocalDateTime.parse(req.getStartDate(), DT_FMT));
        if (req.getEndDate() != null) event.setEndDate(LocalDateTime.parse(req.getEndDate(), DT_FMT));
        if (req.getLatitude() != null) event.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) event.setLongitude(req.getLongitude());
        if (req.getLocationName() != null) event.setLocationName(req.getLocationName());
        if (req.getMaxAttendees() != null) event.setMaxAttendees(req.getMaxAttendees());
        if (req.getImageUrl() != null) event.setImageUrl(req.getImageUrl());

        event = eventRepository.save(event);
        notificationService.notifyEventUpdated(event, user);
        return toResponse(event, user.getId());
    }

    @Override
    @Transactional
    public void deleteEvent(AuthUser user, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getOrganizer().getId().equals(user.getId())) {
            throw new RuntimeException("Only the organizer can delete this event");
        }

        eventRepository.delete(event);
    }

    @Override
    @Transactional
    public EventResponse joinEvent(AuthUser user, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new RuntimeException("Cannot join a completed event");
        }

        if (event.getMaxAttendees() != null && event.getCurrentAttendees() >= event.getMaxAttendees()) {
            throw new RuntimeException("Event is full");
        }

        event.setCurrentAttendees(event.getCurrentAttendees() + 1);
        event = eventRepository.save(event);
        return toResponse(event, user.getId());
    }

    @Override
    @Transactional
    public EventResponse leaveEvent(AuthUser user, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getCurrentAttendees() > 0) {
            event.setCurrentAttendees(event.getCurrentAttendees() - 1);
        }

        event = eventRepository.save(event);
        return toResponse(event, user.getId());
    }

    @Override
    public List<EventResponse> getBookmarks(AuthUser user) {
        List<EventBookmark> bookmarks = eventBookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return bookmarks.stream()
                .map(bm -> toResponse(bm.getEvent(), user.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean toggleBookmark(AuthUser user, Long eventId) {
        boolean exists = eventBookmarkRepository.existsByUserIdAndEventId(user.getId(), eventId);
        if (exists) {
            eventBookmarkRepository.deleteByUserIdAndEventId(user.getId(), eventId);
            return false;
        } else {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            EventBookmark bookmark = EventBookmark.builder()
                    .user(user)
                    .event(event)
                    .build();
            eventBookmarkRepository.save(bookmark);
            return true;
        }
    }

    @Override
    public List<EventResponse> getMyEvents(AuthUser user) {
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(e -> toResponse(e, user.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(fixedDelayString = "${app.events.status-refresh-delay-ms:60000}")
    @Transactional
    public void refreshEventStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> changedEvents = new java.util.ArrayList<>();
        List<StatusTransition> transitions = new java.util.ArrayList<>();

        List<Event> incomingDue = eventRepository.findByStatusAndStartDateLessThanEqual(EventStatus.INCOMING, now);
        for (Event event : incomingDue) {
            EventStatus previousStatus = event.getStatus();
            EventStatus nextStatus = (event.getEndDate() != null && !event.getEndDate().isAfter(now))
                    ? EventStatus.COMPLETED
                    : EventStatus.ONGOING;
            if (previousStatus != nextStatus) {
                event.setStatus(nextStatus);
                changedEvents.add(event);
                transitions.add(new StatusTransition(event, previousStatus));
            }
        }

        List<Event> ongoingDue = eventRepository.findByStatusAndEndDateLessThanEqual(EventStatus.ONGOING, now);
        for (Event event : ongoingDue) {
            EventStatus previousStatus = event.getStatus();
            if (previousStatus != EventStatus.COMPLETED) {
                event.setStatus(EventStatus.COMPLETED);
                changedEvents.add(event);
                transitions.add(new StatusTransition(event, previousStatus));
            }
        }

        if (!changedEvents.isEmpty()) {
            eventRepository.saveAll(changedEvents);
            transitions.forEach(item -> notificationService.notifyEventStatusChanged(item.event, item.previousStatus));
        }
    }

    // ────────── HELPERS ──────────

    private EventResponse toResponse(Event event, Long currentUserId) {
        boolean bookmarked = eventBookmarkRepository.existsByUserIdAndEventId(currentUserId, event.getId());

        Long countdownSeconds = null;
        if (event.getStatus() == EventStatus.INCOMING) {
            countdownSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), event.getStartDate());
            if (countdownSeconds < 0) countdownSeconds = 0L;
        }

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventType(event.getEventType().name())
                .status(event.getStatus().name())
                .isFree(event.getIsFree())
                .price(event.getPrice())
                .currency(event.getCurrency())
                .startDate(event.getStartDate().format(DT_FMT))
                .endDate(event.getEndDate().format(DT_FMT))
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .locationName(event.getLocationName())
                .maxAttendees(event.getMaxAttendees())
                .currentAttendees(event.getCurrentAttendees())
                .imageUrl(event.getImageUrl())
                .organizerUsername(event.getOrganizer().getUsername())
                .organizerId(event.getOrganizer().getId())
                .bookmarked(bookmarked)
                .countdownSeconds(countdownSeconds)
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt().format(DT_FMT) : null)
                .build();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private record StatusTransition(Event event, EventStatus previousStatus) {
    }
}
