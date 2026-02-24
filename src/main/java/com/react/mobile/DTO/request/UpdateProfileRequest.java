package com.react.mobile.DTO.request;

import com.react.mobile.Entity.Enums.InterestType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    @Size(max = 50, message = "First name tối đa 50 ký tự")
    private String firstName;

    @Size(max = 50, message = "Last name tối đa 50 ký tự")
    private String lastName;

    private LocalDateTime dateOfBirth;

    @Min(value = 1, message = "Age phải lớn hơn 0")
    @Max(value = 120, message = "Age không hợp lệ")
    private Integer age;

    @Size(max = 20, message = "Gender tối đa 20 ký tự")
    private String gender;

    @Size(max = 20, message = "Phone number tối đa 20 ký tự")
    private String phoneNumber;

    @Size(max = 255, message = "Address tối đa 255 ký tự")
    private String address;

    @Pattern(regexp = "(?i)solo|family|group", message = "Travel style chỉ nhận solo/family/group")
    private String travelStyle;

    private Set<InterestType> interests;

    @Size(max = 2000, message = "Bio tối đa 2000 ký tự")
    private String bio;

    @Size(max = 255, message = "Profile picture URL tối đa 255 ký tự")
    private String profilePictureUrl;
}
