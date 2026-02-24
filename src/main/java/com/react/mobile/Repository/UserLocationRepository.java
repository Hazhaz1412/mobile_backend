package com.react.mobile.Repository;

import com.react.mobile.Entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    // 1. Tìm vị trí hiện tại của 1 user (để update hoặc xem profile)
    Optional<UserLocation> findByUserId(Long userId);

    // 2. TÍNH NĂNG CAO CẤP: Tìm những user khác ở gần (Bán kính km)
    // Công thức Haversine để tính khoảng cách giữa 2 toạ độ trên mặt cầu
    @Query(value = "SELECT * FROM user_location u " +
            "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(u.latitude)) " +
            "* cos(radians(u.longitude) - radians(:lon)) " +
            "+ sin(radians(:lat)) * sin(radians(u.latitude)))) < :distanceKm", 
            nativeQuery = true)
    List<UserLocation> findUsersNearby(
            @Param("lat") Double latitude,
            @Param("lon") Double longitude,
            @Param("distanceKm") Double distanceKm
    );

    void deleteByUserId(Long userId);
}
