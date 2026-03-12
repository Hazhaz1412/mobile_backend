package com.react.mobile.Repository;

import com.react.mobile.Entity.DiscoveryBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiscoveryBookmarkRepository extends JpaRepository<DiscoveryBookmark, Long> {
    List<DiscoveryBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<DiscoveryBookmark> findByUserIdAndPlaceId(Long userId, String placeId);

    boolean existsByUserIdAndPlaceId(Long userId, String placeId);

    void deleteByUserIdAndPlaceId(Long userId, String placeId);

    void deleteByUserId(Long userId);

    @Query("SELECT b.placeId, COUNT(b) FROM DiscoveryBookmark b GROUP BY b.placeId ORDER BY COUNT(b) DESC")
    List<Object[]> findTopPlaceIds(Pageable pageable);
}
