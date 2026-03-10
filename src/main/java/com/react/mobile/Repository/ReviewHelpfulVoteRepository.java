package com.react.mobile.Repository;

import com.react.mobile.Entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    List<ReviewHelpfulVote> findByUserIdAndReviewIdIn(Long userId, List<Long> reviewIds);

    long countByReviewId(Long reviewId);

    void deleteByReviewId(Long reviewId);
}
