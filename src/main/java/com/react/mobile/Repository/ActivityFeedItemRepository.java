package com.react.mobile.Repository;

import com.react.mobile.Entity.ActivityFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityFeedItemRepository extends JpaRepository<ActivityFeedItem, Long> {

    @Query("SELECT a FROM ActivityFeedItem a JOIN FETCH a.actor WHERE a.actor.id IN :actorIds ORDER BY a.createdAt DESC")
    Page<ActivityFeedItem> findByActorIdIn(@Param("actorIds") List<Long> actorIds, Pageable pageable);

    @Query("SELECT a FROM ActivityFeedItem a JOIN FETCH a.actor WHERE a.actor.id = :actorId ORDER BY a.createdAt DESC")
    Page<ActivityFeedItem> findByActorId(@Param("actorId") Long actorId, Pageable pageable);
}
