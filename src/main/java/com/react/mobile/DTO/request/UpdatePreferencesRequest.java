package com.react.mobile.DTO.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private Boolean profileVisibility;

    @Size(max = 10, message = "Language tối đa 10 ký tự")
    private String language;

    @Size(max = 50, message = "Timezone tối đa 50 ký tự")
    private String timezone;
    private Boolean darkMode;
}
