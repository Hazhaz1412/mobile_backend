package com.react.mobile.Entity;

import com.react.mobile.Entity.Enums.TravelPlanItemType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_plan_item", indexes = {
        @Index(name = "idx_travel_plan_item_plan", columnList = "plan_id"),
        @Index(name = "idx_travel_plan_item_day", columnList = "day_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlan plan;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private TravelPlanItemType itemType;

    @Column(name = "reference_id", length = 200)
    private String referenceId;

    @Column(length = 220)
    private String title;

    @Column(name = "location_name", length = 320)
    private String locationName;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "reminder_at")
    private LocalDateTime reminderAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
