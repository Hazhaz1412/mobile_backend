package com.react.mobile.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "OTP phải gồm 6 chữ số")
    private String otp;

    @NotBlank(message = "Password mới không được để trống")
    @Size(min = 8, max = 64, message = "Password phải từ 8-64 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#._-]).{8,64}$",
        message = "Password phải có chữ hoa, chữ thường, số và ký tự đặc biệt"
    )
    private String newPassword;
}
