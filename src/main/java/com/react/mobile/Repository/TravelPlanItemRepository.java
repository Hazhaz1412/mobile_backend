package com.react.mobile.Repository;

import com.react.mobile.Entity.TravelPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelPlanItemRepository extends JpaRepository<TravelPlanItem, Long> {

    List<TravelPlanItem> findByPlanIdOrderByDayNumberAscSortOrderAscStartTimeAsc(Long planId);

    List<TravelPlanItem> findByPlanIdAndDayNumberOrderBySortOrderAscStartTimeAsc(Long planId, Integer dayNumber);

    Optional<TravelPlanItem> findByIdAndPlanId(Long id, Long planId);

    long countByPlanId(Long planId);

    @Query("SELECT COALESCE(MAX(i.sortOrder), 0) FROM TravelPlanItem i WHERE i.plan.id = :planId AND i.dayNumber = :dayNumber")
    Integer findMaxSortOrderByPlanAndDay(@Param("planId") Long planId, @Param("dayNumber") Integer dayNumber);

    @Query("SELECT COUNT(DISTINCT i.dayNumber) FROM TravelPlanItem i WHERE i.plan.id = :planId")
    long countDistinctDays(@Param("planId") Long planId);
}
