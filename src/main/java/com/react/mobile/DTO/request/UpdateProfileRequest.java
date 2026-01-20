package com.react.mobile.DTO.request;

import com.react.mobile.Entity.Enums.InterestType;
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
    private String firstName;
    private String lastName;
    private LocalDateTime dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String address;
    private String travelStyle;
    private Set<InterestType> interests;
    private String bio;
    private String profilePictureUrl;
}
