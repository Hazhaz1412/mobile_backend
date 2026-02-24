package com.react.mobile.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_location")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vĩ độ
    private Double latitude;

    // Kinh độ
    private Double longitude;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "is_manual")
    @Builder.Default
    private Boolean isManual = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
 
    @OneToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AuthUser user;
}