package com.react.mobile.Controller;

import com.react.mobile.DTO.request.CreateEventRequest;
import com.react.mobile.DTO.request.ReportEventRequest;
import com.react.mobile.DTO.response.EventListResponse;
import com.react.mobile.DTO.response.EventResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final AuthUserRepository authUserRepository;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateEventRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.createEvent(user, request));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.getEvent(user, eventId));
    }

    @GetMapping
    public ResponseEntity<EventListResponse> listEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.listEvents(user, status, eventType, isFree, search, latitude, longitude, maxDistanceKm, page, size));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @RequestBody CreateEventRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.updateEvent(user, eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Map<String, String>> deleteEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        AuthUser user = resolveUser(userDetails);
        eventService.deleteEvent(user, eventId);
        return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
    }

    @PostMapping("/{eventId}/report")
    public ResponseEntity<Map<String, String>> reportEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId,
            @RequestBody ReportEventRequest request
    ) {
        AuthUser user = resolveUser(userDetails);
        eventService.reportEvent(user, eventId, request);
        return ResponseEntity.ok(Map.of("message", "Event has been reported"));
    }

    @PostMapping("/{eventId}/join")
    public ResponseEntity<EventResponse> joinEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.joinEvent(user, eventId));
    }

    @PostMapping("/{eventId}/leave")
    public ResponseEntity<EventResponse> leaveEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.leaveEvent(user, eventId));
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<List<EventResponse>> getBookmarks(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.getBookmarks(user));
    }

    @PostMapping("/{eventId}/bookmark")
    public ResponseEntity<Map<String, Boolean>> toggleBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        AuthUser user = resolveUser(userDetails);
        boolean bookmarked = eventService.toggleBookmark(user, eventId);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @GetMapping("/my-events")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.getMyEvents(user));
    }

    @GetMapping("/joined")
    public ResponseEntity<List<Long>> getJoinedEventIds(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AuthUser user = resolveUser(userDetails);
        return ResponseEntity.ok(eventService.getJoinedEventIds(user));
    }

    private AuthUser resolveUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
