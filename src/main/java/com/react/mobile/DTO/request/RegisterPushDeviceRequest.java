package com.react.mobile.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterPushDeviceRequest {

    @NotBlank(message = "deviceToken is required")
    private String deviceToken;

    @NotBlank(message = "platform is required")
    private String platform;
}
