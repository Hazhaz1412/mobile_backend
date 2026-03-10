package com.react.mobile.Repository;

import com.react.mobile.Entity.EventBookmark;
import com.react.mobile.Entity.Enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventBookmarkRepository extends JpaRepository<EventBookmark, Long> {

    List<EventBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventBookmark> findByEventId(Long eventId);

    List<EventBookmark> findByEventIdAndUserIdNot(Long eventId, Long userId);

    List<EventBookmark> findByEventStatusAndEventStartDateBetween(EventStatus status, LocalDateTime from, LocalDateTime to);

    Optional<EventBookmark> findByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    void deleteByUserIdAndEventId(Long userId, Long eventId);

    void deleteByUserId(Long userId);
}
