package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSnapshotResponse {
    private Double latitude;
    private Double longitude;
    private String locationName;
    private Boolean manualOverride;
    private LocalDateTime updatedAt;
}
