package com.react.mobile.Controller;

import com.react.mobile.DTO.response.GDPRDataResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Service.GDPRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GDPRController {

    private final GDPRService gdprService;

    /**
     * Export tất cả dữ liệu cá nhân
     * Xuất toàn bộ dữ liệu cá nhân của người dùng theo quy định GDPR (Right to Data Portability)
     */
    @GetMapping("/export")
    public ResponseEntity<GDPRDataResponse> exportUserData(
            @AuthenticationPrincipal AuthUser authUser) {
        
        log.info("User {} requested data export", authUser.getEmail());
        GDPRDataResponse response = gdprService.exportAllData(authUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Xem lịch sử hoạt động
     * Lấy lịch sử đăng nhập và hoạt động của người dùng
     */
    @GetMapping("/activity-logs")
    public ResponseEntity<GDPRDataResponse.UserActivityLog> getActivityLogs(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        
        log.info("User {} requested activity logs with limit: {}", authUser.getEmail(), limit);
        GDPRDataResponse.UserActivityLog logs = gdprService.getActivityLogs(authUser, limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Xóa tài khoản
     * Xóa tài khoản người dùng theo quy định GDPR (Right to be Forgotten).
     * Type 'soft': vô hiệu hóa tài khoản. Type 'hard': xóa hoàn toàn dữ liệu.
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "soft") String type) {
        
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
}
