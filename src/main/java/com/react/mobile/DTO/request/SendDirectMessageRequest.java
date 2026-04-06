package com.react.mobile.DTO.request;

import lombok.Data;

import java.util.List;

@Data
public class SendDirectMessageRequest {
    private String kind;
    private String ciphertext;
    private String contentNonce;
    private List<EncryptedKeyEntry> encryptedKeys;

    @Data
    public static class EncryptedKeyEntry {
        private Long userId;
        private String encryptedKey;
        private String keyNonce;
    }
}
