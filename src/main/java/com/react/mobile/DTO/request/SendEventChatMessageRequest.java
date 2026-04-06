package com.react.mobile.DTO.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEventChatMessageRequest {

    @NotBlank
    private String scope;

    private Long counterpartUserId;

    @NotBlank
    private String kind;

    @NotBlank
    private String ciphertext;

    @NotBlank
    private String contentNonce;

    @Valid
    @NotEmpty
    private List<EventChatMessageKeyRequest> encryptedKeys;
}
