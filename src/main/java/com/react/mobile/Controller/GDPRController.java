package com.react.mobile.Controller;

import com.react.mobile.DTO.response.GDPRDataResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.GDPRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GDPRController {

    private final GDPRService gdprService;
    private final AuthUserRepository authUserRepository;

     
    @GetMapping("/export")
    public ResponseEntity<GDPRDataResponse> exportUserData(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthUser authUser = getCurrentUser(userDetails);
        
        log.info("User {} requested data export", authUser.getEmail());
        GDPRDataResponse response = gdprService.exportAllData(authUser);
        return ResponseEntity.ok(response);
    }

     
    @GetMapping("/activity-logs")
    public ResponseEntity<GDPRDataResponse.UserActivityLog> getActivityLogs(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        AuthUser authUser = getCurrentUser(userDetails);
        
        log.info("User {} requested activity logs with limit: {}", authUser.getEmail(), limit);
        GDPRDataResponse.UserActivityLog logs = gdprService.getActivityLogs(authUser, limit);
        return ResponseEntity.ok(logs);
    }

    /**
     
     * Type 'soft': vô hiệu hóa tài khoản. Type 'hard': xóa hoàn toàn dữ liệu.
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "soft") String type) {
        AuthUser authUser = getCurrentUser(userDetails);
        
        log.warn("User {} requested account deletion with type: {}", authUser.getEmail(), type);
        
        try {
            gdprService.deleteAccount(authUser, type);
            
            String message = "soft".equalsIgnoreCase(type) 
                ? "Tài khoản đã được vô hiệu hóa thành công" 
                : "Tài khoản và toàn bộ dữ liệu đã được xóa hoàn toàn";
            
            return ResponseEntity.ok(message);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid delete type: {}", type);
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            log.error("Error deleting account for user: {}", authUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa tài khoản: " + e.getMessage());
        }
    }

    private AuthUser getCurrentUser(UserDetails userDetails) {
        return authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
