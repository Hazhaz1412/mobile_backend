package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "location_history") // Bảng này sẽ rất to
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;
    private Double longitude;

    // Lưu tên địa điểm lúc đó (nếu có), ví dụ: "Landmark 81"
    // Giúp phân tích sở thích dễ hơn là chỉ nhìn toạ độ
    @Column(name = "location_name")
    private String locationName;

    // Thời điểm user có mặt tại đây
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    // Quan hệ ManyToOne: Một user có hàng nghìn lịch sử di chuyển
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser user;
}