package com.react.mobile.Repository;

import com.react.mobile.Entity.Event;
import com.react.mobile.Entity.Enums.EventStatus;
import com.react.mobile.Entity.Enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatusOrderByStartDateAsc(EventStatus status);

    List<Event> findByEventTypeOrderByStartDateAsc(EventType eventType);

    List<Event> findByStatusAndStartDateBetween(EventStatus status, LocalDateTime from, LocalDateTime to);

    List<Event> findByStatusAndStartDateLessThanEqual(EventStatus status, LocalDateTime to);

    List<Event> findByStatusAndEndDateLessThanEqual(EventStatus status, LocalDateTime to);

    List<Event> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    Optional<Event> findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(String title);

    @Query("SELECT e FROM Event e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:eventType IS NULL OR e.eventType = :eventType) AND " +
            "(:isFree IS NULL OR e.isFree = :isFree) AND " +
            "(:search = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY e.startDate ASC")
    List<Event> filterEvents(
            @Param("status") EventStatus status,
            @Param("eventType") EventType eventType,
            @Param("isFree") Boolean isFree,
            @Param("search") String search
    );

    @Modifying
    @Query("UPDATE Event e SET e.status = :newStatus WHERE e.status = :oldStatus AND e.startDate <= :now")
    int updateStatusFromIncomingToOngoing(
            @Param("oldStatus") EventStatus oldStatus,
            @Param("newStatus") EventStatus newStatus,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("UPDATE Event e SET e.status = :newStatus WHERE e.status = :oldStatus AND e.endDate <= :now")
    int updateStatusFromOngoingToCompleted(
            @Param("oldStatus") EventStatus oldStatus,
            @Param("newStatus") EventStatus newStatus,
            @Param("now") LocalDateTime now
    );
}
