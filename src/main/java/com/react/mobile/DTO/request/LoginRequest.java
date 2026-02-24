package com.react.mobile.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "Email không hợp lệ")
    private String email;

    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;

    public String resolveIdentifier() {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return username == null ? null : username.trim();
    }

    @AssertTrue(message = "Vui lòng nhập username hoặc email")
    public boolean isIdentifierProvided() {
        return (email != null && !email.isBlank()) || (username != null && !username.isBlank());
    }
}
