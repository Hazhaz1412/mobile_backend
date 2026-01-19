package com.react.mobile.DTO.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SocialAccountResponse {
    private String id;  
    private String provider;  
    private String providerUserId;  
    private LocalDateTime createdAt;  
}