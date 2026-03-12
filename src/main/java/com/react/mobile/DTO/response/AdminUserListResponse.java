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
public class AdminUserListResponse {
    private List<AdminUserProfileResponse> users;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Boolean hasNext;
}
