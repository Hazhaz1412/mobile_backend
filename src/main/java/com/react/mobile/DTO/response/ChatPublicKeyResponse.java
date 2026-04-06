package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPublicKeyResponse {

    private Long userId;
    private String username;
    private String algorithm;
    private String publicKey;
    private String updatedAt;
}
