package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewListResponse {
    private List<ReviewResponse> reviews;
    private ReviewSummaryResponse summary;
    @Builder.Default
    private Long total = 0L;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
    @Builder.Default
    private Integer totalPages = 1;
    @Builder.Default
    private Boolean hasNext = false;
}
