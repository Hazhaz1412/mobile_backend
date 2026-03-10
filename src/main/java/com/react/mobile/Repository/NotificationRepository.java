package com.react.mobile.Repository;

import com.react.mobile.Entity.Enums.NotificationCategory;
import com.react.mobile.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "AND (:category IS NULL OR n.category = :category) " +
            "AND (:unreadOnly = false OR n.isRead = false)")
    Page<Notification> findInbox(
            @Param("userId") Long userId,
            @Param("category") NotificationCategory category,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndCategoryAndIsReadFalse(Long userId, NotificationCategory category);

    boolean existsByUserIdAndDedupeKey(Long userId, String dedupeKey);

    long deleteByIdAndUserId(Long id, Long userId);

    long deleteByUserId(Long userId);

    List<Notification> findByIsReadFalseAndPushDeliveredAtIsNullAndCreatedAtAfterOrderByCreatedAtAsc(
            LocalDateTime fromTime,
            Pageable pageable
    );
}
