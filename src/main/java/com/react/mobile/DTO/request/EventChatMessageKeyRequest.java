package com.react.mobile.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventChatMessageKeyRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String encryptedKey;

    @NotBlank
    private String keyNonce;
}
