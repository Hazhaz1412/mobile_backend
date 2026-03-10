package com.react.mobile.Repository;

import com.react.mobile.Entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReporterIdAndResolvedFalse(Long reviewId, Long reporterId);

    long countByReviewIdAndResolvedFalse(Long reviewId);

    List<ReviewReport> findByReviewIdInAndResolvedFalse(List<Long> reviewIds);

    void deleteByReviewId(Long reviewId);
}
