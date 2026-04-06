package com.react.mobile.Repository;

import com.react.mobile.Entity.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @Query("SELECT dm FROM DirectMessage dm " +
            "JOIN FETCH dm.sender " +
            "WHERE dm.conversation.id = :conversationId " +
            "ORDER BY dm.createdAt DESC")
    List<DirectMessage> findRecentByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
}
