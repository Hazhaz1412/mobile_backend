package com.react.mobile.Repository;

import com.react.mobile.Entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {

    // 1. Lấy toàn bộ lịch sử di chuyển của user (Sắp xếp mới nhất trước)
    // Dùng để vẽ "Map Journey" hoặc timeline
    List<LocationHistory> findByUserIdOrderByTimestampDesc(Long userId);

    // 2. AI SUGGESTION: Tìm những địa điểm user ghé thăm nhiều nhất
    // Giúp trả lời câu hỏi: "User này thích đi Đà Lạt hay Vũng Tàu?"
    @Query("SELECT l.locationName, COUNT(l) as visitCount " +
           "FROM LocationHistory l " +
           "WHERE l.user.id = :userId AND l.locationName IS NOT NULL " +
           "GROUP BY l.locationName " +
           "ORDER BY visitCount DESC")
    List<Object[]> findFavoriteLocations(Long userId);
    
    // 3. Xóa lịch sử (Tính năng Privacy/GDPR - Mục 10 trong đề)
    void deleteByUserId(Long userId);
}