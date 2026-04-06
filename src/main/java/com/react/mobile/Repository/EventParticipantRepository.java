package com.react.mobile.Repository;

import com.react.mobile.Entity.Enums.EventParticipantRole;
import com.react.mobile.Entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<EventParticipant> findByEventIdOrderByJoinedAtAsc(Long eventId);

    List<EventParticipant> findByUserIdOrderByJoinedAtDesc(Long userId);

    long countByEventIdAndRole(Long eventId, EventParticipantRole role);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventId(Long eventId);
}
