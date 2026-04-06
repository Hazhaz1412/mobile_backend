package com.react.mobile.Repository;

import com.react.mobile.Entity.Enums.EventChatScope;
import com.react.mobile.Entity.EventChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventChatMessageRepository extends JpaRepository<EventChatMessage, Long> {

    @Query("SELECT m FROM EventChatMessage m " +
            "JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.pinnedBy pb " +
            "WHERE m.event.id = :eventId AND m.scope = :scope " +
            "ORDER BY m.createdAt DESC")
    List<EventChatMessage> findRecentGroupMessages(
            @Param("eventId") Long eventId,
            @Param("scope") EventChatScope scope,
            Pageable pageable
    );

    @Query("SELECT m FROM EventChatMessage m " +
            "JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.pinnedBy pb " +
            "WHERE m.event.id = :eventId AND m.scope = :scope " +
            "AND ((m.sender.id = :userId AND m.recipient.id = :counterpartId) " +
            "OR (m.sender.id = :counterpartId AND m.recipient.id = :userId)) " +
            "ORDER BY m.createdAt DESC")
    List<EventChatMessage> findRecentDirectMessages(
            @Param("eventId") Long eventId,
            @Param("scope") EventChatScope scope,
            @Param("userId") Long userId,
            @Param("counterpartId") Long counterpartId,
            Pageable pageable
    );

    @Query("SELECT m FROM EventChatMessage m " +
            "JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.pinnedBy pb " +
            "WHERE m.event.id = :eventId AND m.scope = :scope AND m.pinned = true " +
            "ORDER BY m.pinnedAt DESC, m.createdAt DESC")
    List<EventChatMessage> findPinnedMessages(
            @Param("eventId") Long eventId,
            @Param("scope") EventChatScope scope
    );

    @Query("SELECT MAX(m.createdAt) FROM EventChatMessage m WHERE m.event.id = :eventId AND m.scope = :scope")
    LocalDateTime findLatestCreatedAtByEventIdAndScope(
            @Param("eventId") Long eventId,
            @Param("scope") EventChatScope scope
    );

    long countByEventIdAndScopeAndPinnedTrue(Long eventId, EventChatScope scope);

    void deleteByEventId(Long eventId);
}
