package com.react.mobile.Repository;

import com.react.mobile.Entity.Enums.ReviewModerationStatus;
import com.react.mobile.Entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE " +
            "(:targetType IS NULL OR r.targetType = :targetType) AND " +
            "(:targetId IS NULL OR r.targetId = :targetId) AND " +
            "(:search = '' OR LOWER(COALESCE(r.comment, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(COALESCE(r.targetName, '')) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:status IS NULL OR r.moderationStatus = :status)")
    Page<Review> findFiltered(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("search") String search,
            @Param("status") ReviewModerationStatus status,
            Pageable pageable
    );

    @Query("SELECT r.rating FROM Review r WHERE " +
            "(:targetType IS NULL OR r.targetType = :targetType) AND " +
            "(:targetId IS NULL OR r.targetId = :targetId) AND " +
            "(:search = '' OR LOWER(COALESCE(r.comment, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(COALESCE(r.targetName, '')) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:status IS NULL OR r.moderationStatus = :status)")
    List<Double> findRatingsForSummary(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("search") String search,
            @Param("status") ReviewModerationStatus status
    );

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Review> findByModerationStatusOrderByUpdatedAtDesc(ReviewModerationStatus moderationStatus, Pageable pageable);

    List<Review> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime from);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime from);

    long countByModerationStatus(ReviewModerationStatus moderationStatus);

    @Query("SELECT r.targetName, COUNT(r) FROM Review r WHERE " +
            "r.targetName IS NOT NULL AND r.targetName <> '' AND " +
            "r.moderationStatus = com.react.mobile.Entity.Enums.ReviewModerationStatus.APPROVED " +
            "GROUP BY r.targetName ORDER BY COUNT(r) DESC")
    List<Object[]> findTopTargetNames(Pageable pageable);

    Optional<Review> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);
}
