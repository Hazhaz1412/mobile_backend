package com.react.mobile.DTO.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {
    private Long id;
    private UUID uuid; 
    private String username;
    private String email;
    private Boolean isActive;
    private LocalDateTime dateJoined;
}