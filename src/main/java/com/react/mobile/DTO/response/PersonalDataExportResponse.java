package com.react.mobile.DTO.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PersonalDataExportResponse { 
    private Long userId;
    private String email;
    private LocalDateTime accountCreatedDate;
    
    // Thông tin chi tiết
    private UserProfileResponse profile;
    
    // Cài đặt
    private UserPreferencesResponse preferences;
    
    // Timestamp lúc xuất dữ liệu
    private LocalDateTime exportedAt;
}