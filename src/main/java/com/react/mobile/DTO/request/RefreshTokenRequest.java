package com.react.mobile.DTO.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token không được để trống")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
