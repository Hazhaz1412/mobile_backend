package com.react.mobile.Repository;

import com.react.mobile.Entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    boolean existsByReportedUserIdAndReporterIdAndResolvedFalse(Long reportedUserId, Long reporterId);

    long countByReportedUserIdAndResolvedFalse(Long reportedUserId);

    List<UserReport> findByReportedUserIdInAndResolvedFalse(List<Long> userIds);

    void deleteByReportedUserId(Long reportedUserId);
}