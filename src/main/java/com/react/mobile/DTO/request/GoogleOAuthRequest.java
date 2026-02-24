package com.react.mobile.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoogleOAuthRequest {
    
    /**
     * ID token nhận được từ Google (phía client React Native gửi lên)
     */
    @NotBlank(message = "Google ID token không được để trống")
    private String idToken;
    
    /**
     * Access token từ Google (tuỳ chọn)
     */
    private String accessToken;
    
    /**
     * Device name/ID để tracking (tuỳ chọn)
     */
    private String deviceName;
}
