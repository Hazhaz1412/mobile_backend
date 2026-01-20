package com.react.mobile.Repository;

import com.react.mobile.Entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    
    List<LoginHistory> findTop50ByUserIdOrderByLoginTimeDesc(Long userId);
    
    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);
    
    Long countByUserId(Long userId);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.userId = :userId AND lh.success = true ORDER BY lh.loginTime DESC")
    List<LoginHistory> findSuccessfulLoginsByUserId(Long userId);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.userId = :userId AND lh.loginTime >= :startDate ORDER BY lh.loginTime DESC")
    List<LoginHistory> findByUserIdAndLoginTimeAfter(Long userId, LocalDateTime startDate);
    
    void deleteByUserId(Long userId);
}
