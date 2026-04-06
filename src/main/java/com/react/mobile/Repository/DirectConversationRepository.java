package com.react.mobile.Repository;

import com.react.mobile.Entity.DirectConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, Long> {

    @Query("SELECT dc FROM DirectConversation dc " +
            "JOIN FETCH dc.user1 JOIN FETCH dc.user2 " +
            "WHERE dc.user1.id = :userId OR dc.user2.id = :userId " +
            "ORDER BY dc.lastMessageAt DESC NULLS LAST, dc.createdAt DESC")
    List<DirectConversation> findByUserId(@Param("userId") Long userId);

    @Query("SELECT dc FROM DirectConversation dc " +
            "JOIN FETCH dc.user1 JOIN FETCH dc.user2 " +
            "WHERE (dc.user1.id = :userId1 AND dc.user2.id = :userId2) " +
            "OR (dc.user1.id = :userId2 AND dc.user2.id = :userId1)")
    Optional<DirectConversation> findByUserPair(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
