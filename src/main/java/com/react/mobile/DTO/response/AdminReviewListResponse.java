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
public class AdminReviewListResponse {
    private List<AdminReviewResponse> reviews;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Boolean hasNext;
}
