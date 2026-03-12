package com.react.mobile.Repository;

import com.react.mobile.Entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    List<TravelPlan> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<TravelPlan> findByIdAndUserId(Long id, Long userId);

    Optional<TravelPlan> findByShareTokenAndIsPublicTrue(String shareToken);

    boolean existsByShareToken(String shareToken);
}
