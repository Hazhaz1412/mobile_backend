package com.react.mobile.Controller;

import com.react.mobile.DTO.request.UpdateProfileRequest;
import com.react.mobile.DTO.request.UpdatePreferencesRequest;
import com.react.mobile.DTO.response.UserProfileResponse;
import com.react.mobile.DTO.response.UserPreferencesResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final long MAX_PROFILE_PICTURE_SIZE = 5L * 1024L * 1024L;

    private final UserService userService;
    private final AuthUserRepository authUserRepository;

    @Value("${app.upload.profile-picture-dir:uploads/profile-pictures}")
    private String profilePictureDir;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserProfileResponse profile = userService.getProfile(authUser);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserProfileResponse updatedProfile = userService.updateProfile(authUser, request);
        return ResponseEntity.ok(updatedProfile);
    }

        @PostMapping(path = "/profile/picture", consumes = {"multipart/form-data"})
        public ResponseEntity<UserProfileResponse> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file) {

        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        validateImage(file);
        String fileName = storeProfilePicture(file, authUser.getId());

        String pictureUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/public/profile-pictures/")
            .path(fileName)
            .toUriString();

        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .profilePictureUrl(pictureUrl)
            .build();

        return ResponseEntity.ok(userService.updateProfile(authUser, request));
        }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> getPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserPreferencesResponse preferences = userService.getPreferences(authUser);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        
        AuthUser authUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserPreferencesResponse updatedPreferences = userService.updatePreferences(authUser, request);
        return ResponseEntity.ok(updatedPreferences);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Profile picture is required");
        }

        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE) {
            throw new RuntimeException("Profile picture must be <= 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }
    }

    private String storeProfilePicture(MultipartFile file, Long userId) {
        try {
            Path uploadPath = Paths.get(profilePictureDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String extension = getExtension(file.getOriginalFilename());
            String fileName = "user-" + userId + "-" + UUID.randomUUID() + extension;
            Path destination = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Cannot store profile picture", ex);
        }
    }

    private String getExtension(String fileName) {
        String safeName = StringUtils.hasText(fileName) ? fileName : "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == safeName.length() - 1) {
            return ".jpg";
        }
        return safeName.substring(dotIndex);
    }
}
