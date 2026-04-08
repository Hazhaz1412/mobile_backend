package com.react.mobile.Repository;

import com.react.mobile.Entity.EventReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventReportRepository extends JpaRepository<EventReport, Long> {

    boolean existsByEventIdAndReporterIdAndResolvedFalse(Long eventId, Long reporterId);

    long countByEventIdAndResolvedFalse(Long eventId);

    List<EventReport> findByEventIdInAndResolvedFalse(List<Long> eventIds);

    void deleteByEventId(Long eventId);
}