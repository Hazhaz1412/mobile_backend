package com.react.mobile.Repository;

import com.react.mobile.Entity.DiscoveryBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscoveryBookmarkRepository extends JpaRepository<DiscoveryBookmark, Long> {
    List<DiscoveryBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<DiscoveryBookmark> findByUserIdAndPlaceId(Long userId, String placeId);

    boolean existsByUserIdAndPlaceId(Long userId, String placeId);

    void deleteByUserIdAndPlaceId(Long userId, String placeId);

    void deleteByUserId(Long userId);
}
