package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewHelpfulResponse {
    private Long reviewId;
    private Long helpfulCount;
    private Boolean helpfulByCurrentUser;
}
