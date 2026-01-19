package com.react.mobile.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLoginRequest {
    @NotBlank(message = "Provider không được để trống")
    private String provider;  

    @NotBlank(message = "Token không được để trống")
    private String token; 
}