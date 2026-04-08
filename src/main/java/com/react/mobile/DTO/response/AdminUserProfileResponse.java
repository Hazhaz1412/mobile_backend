package com.react.mobile.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private Boolean isActive;
    private Boolean isSuperuser;
    private Boolean isStaff;
    private Long reportCount;
    private String dateJoined;
    private String lastLogin;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String travelStyle;
    private String bio;
    private String profilePictureUrl;
}
