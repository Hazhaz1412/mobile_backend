package com.react.mobile.Repository;

import com.react.mobile.Entity.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, Long> {
    List<NotificationDevice> findByUserIdAndActiveTrue(Long userId);

    Optional<NotificationDevice> findByDeviceToken(String deviceToken);
}
