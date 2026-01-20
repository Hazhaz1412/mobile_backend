package com.react.mobile.DTO.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatus {
    
    private Long userId;
    private String username;
    private StatusType status;
    private String message;
    
    public enum StatusType {
        ONLINE,
        OFFLINE,
        TYPING,
        AWAY
    }
}
