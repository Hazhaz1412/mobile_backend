package com.react.mobile.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizeRouteRequest {
    private Integer dayNumber;
    private Double startLatitude;
    private Double startLongitude;
    private String mode;
}
