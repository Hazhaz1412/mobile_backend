package com.react.mobile.Repository;

import com.react.mobile.Entity.EventChatMessageKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventChatMessageKeyRepository extends JpaRepository<EventChatMessageKey, Long> {

    Optional<EventChatMessageKey> findByMessageIdAndUserId(Long messageId, Long userId);

    List<EventChatMessageKey> findByMessageIdInAndUserId(Collection<Long> messageIds, Long userId);

    void deleteByMessageEventId(Long eventId);
}
